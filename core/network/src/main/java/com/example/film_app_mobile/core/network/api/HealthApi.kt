package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.HealthResponse
import retrofit2.Response
import retrofit2.http.GET

internal interface HealthApi {
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("health/db")
    suspend fun databaseHealth(): Response<Unit>
}
