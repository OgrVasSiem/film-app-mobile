package com.example.film_app_mobile.data.sync

import android.content.Context
import androidx.work.*
import com.example.film_app_mobile.data.error.RequestFailure
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SyncDependencies {
    fun decisionSync(): DecisionSync
}

class DecisionSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sync =
            EntryPointAccessors.fromApplication(applicationContext, SyncDependencies::class.java)
                .decisionSync()
        return try {
            sync.sync()
            Result.success()
        } catch (e: RequestFailure) {
            val http = e.cause as? HttpException
            if (http == null || http.code() >= 500 || http.code() in listOf(408, 429))
                Result.retry()
            else Result.failure()
        }
    }
}
