package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface PacksApi {
    @GET("packs") suspend fun packs(@Header("Authorization") auth: String): List<PackDto>

    @GET("packs/{id}/films")
    suspend fun films(@Header("Authorization") auth: String, @Path("id") id: Long): List<FilmDto>
}
