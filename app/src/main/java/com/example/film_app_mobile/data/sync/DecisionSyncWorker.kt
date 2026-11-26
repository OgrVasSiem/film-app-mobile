package com.example.film_app_mobile.data.sync

import android.content.Context
import androidx.work.*
import com.example.film_app_mobile.data.error.RequestFailure
import com.example.film_app_mobile.data.error.temporary
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SyncDependencies {
    fun sections(): com.example.film_app_mobile.data.repository.SectionRepository

    fun decisionSync(): DecisionSync
}

class DecisionSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sync =
            EntryPointAccessors.fromApplication(applicationContext, SyncDependencies::class.java)

        return try {
            sync.decisionSync().sync()
            sync.sections().flushCurrent()
            enqueueSnapshot(applicationContext)
            Result.success()
        } catch (e: RequestFailure) {
            val http = e.cause as? HttpException
            if (http == null || http.code() >= 500 || http.code() in listOf(408, 429))
                Result.retry()
            else Result.failure()
        } catch (e: java.io.IOException) {
            Result.retry()
        }
    }
}

private fun enqueueSnapshot(context: Context) {
    val work =
        OneTimeWorkRequestBuilder<OfflineSnapshotWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork("film_offline_snapshot", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
}

class OfflineSnapshotWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sections =
            EntryPointAccessors.fromApplication(applicationContext, SyncDependencies::class.java)
                .sections()
        return try {
            sections.synchronize(applicationContext)
            Result.success()
        } catch (e: RequestFailure) {
            if (e.temporary) Result.retry() else Result.failure()
        } catch (e: java.io.IOException) {
            Result.retry()
        }
    }
}
