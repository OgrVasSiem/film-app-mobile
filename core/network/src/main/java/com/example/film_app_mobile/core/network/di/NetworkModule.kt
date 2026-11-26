package com.example.film_app_mobile.core.network.di

import com.example.film_app_mobile.core.network.NetworkConfig
import com.example.film_app_mobile.core.network.NetworkStack
import com.example.film_app_mobile.core.network.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    fun personal(stack: NetworkStack): PersonalApi = stack.retrofit.create(PersonalApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(stack: NetworkStack): AuthApi = stack.retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(stack: NetworkStack): ProfileApi =
        stack.retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun providePacksApi(stack: NetworkStack): PacksApi = stack.retrofit.create(PacksApi::class.java)

    @Provides
    @Singleton
    fun provideLibraryApi(stack: NetworkStack): LibraryApi =
        stack.retrofit.create(LibraryApi::class.java)

    @Provides
    @Singleton
    fun provideNetworkStack(config: NetworkConfig): NetworkStack = NetworkStack(config)
}
