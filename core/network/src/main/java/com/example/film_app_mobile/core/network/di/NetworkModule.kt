package com.example.film_app_mobile.core.network.di

import com.example.film_app_mobile.core.network.NetworkConfig
import com.example.film_app_mobile.core.network.NetworkStack
import com.example.film_app_mobile.core.network.api.HealthApi
import com.example.film_app_mobile.core.network.repository.DefaultHealthRepository
import com.example.film_app_mobile.core.network.repository.HealthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkStack(config: NetworkConfig): NetworkStack = NetworkStack(config)

    @Provides
    @Singleton
    fun provideHealthRepository(stack: NetworkStack): HealthRepository =
        DefaultHealthRepository(
            api = stack.retrofit.create(HealthApi::class.java),
            executor = stack.executor,
        )
}
