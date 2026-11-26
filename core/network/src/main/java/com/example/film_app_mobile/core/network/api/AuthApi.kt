package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface AuthApi {
    @POST("auth/start") suspend fun start(): SessionDto

    @POST("auth/login") suspend fun login(@Body body: CredentialsDto): SessionDto

    @POST("auth/logout") suspend fun logout(@Header("Authorization") auth: String)
}
