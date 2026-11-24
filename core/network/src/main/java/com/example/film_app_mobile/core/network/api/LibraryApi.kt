package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface LibraryApi {
    @GET("library")
    suspend fun library(
        @Header("Authorization") auth: String,
        @Query("status") status: String,
    ): List<FilmDto>

    @PUT("library/{id}")
    suspend fun decide(
        @Header("Authorization") auth: String,
        @Path("id") id: Long,
        @Body body: DecisionDto,
    )

    @DELETE("library/{id}")
    suspend fun reset(@Header("Authorization") auth: String, @Path("id") id: Long)
}
