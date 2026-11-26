package com.example.film_app_mobile.di

import com.example.film_app_mobile.BuildConfig
import com.example.film_app_mobile.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkConfigModule {
    @Provides
    @Named("serverNamespace")
    fun namespace(config: NetworkConfig): String =
        if (config.baseUrl == "http://127.0.0.1:8081/api/v1/") ""
        else "-" + java.security.MessageDigest.getInstance("SHA-256").digest(config.baseUrl.toByteArray()).joinToString("") { "%02x".format(it) }

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig =
        NetworkConfig(
            baseUrl = BuildConfig.API_BASE_URL,
            allowCleartext = BuildConfig.DEBUG,
            enableHttpLogging = false,
        )
}
