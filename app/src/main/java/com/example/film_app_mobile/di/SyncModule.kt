package com.example.film_app_mobile.di

import android.content.Context
import androidx.work.*
import com.example.film_app_mobile.data.sync.DecisionSyncWorker
import com.example.film_app_mobile.data.sync.SyncScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @Provides
    fun scheduler(
        @ApplicationContext context: Context
    ): SyncScheduler = SyncScheduler {
        val work =
            OneTimeWorkRequestBuilder<DecisionSyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("sync_film_decisions", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }
}
