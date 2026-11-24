package com.example.film_app_mobile.di

import android.content.Context
import androidx.work.*
import com.example.film_app_mobile.data.sync.DecisionSyncWorker
import com.example.film_app_mobile.data.sync.SyncScheduler
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@dagger.Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @dagger.Provides
    fun scheduler(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context
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
