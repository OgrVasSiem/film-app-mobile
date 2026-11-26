package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface ProfileApi {
    @GET("me") suspend fun profile(@Header("Authorization") auth: String): ProfileDto

    @PUT("me/password")
    suspend fun protect(
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>,
    ): ProfileDto

    @PUT("me/login")
    suspend fun rename(
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>,
    ): ProfileDto

    @GET("me/login-availability")
    suspend fun availability(
        @Header("Authorization") auth: String,
        @Query("login") login: String,
    ): LoginAvailability

    @HTTP(method = "DELETE", path = "me", hasBody = true)
    suspend fun delete(@Header("Authorization") auth: String, @Body body: Map<String, Boolean>)
}
