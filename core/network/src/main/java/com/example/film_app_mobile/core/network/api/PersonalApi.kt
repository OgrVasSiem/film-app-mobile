package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface PersonalApi {
    @GET("recommendations/film") suspend fun pick(@Header("Authorization") auth: String): FilmPick

    @GET("recommendations/deck")
    suspend fun deck(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): FilmPageDto

    @GET("library/overview") suspend fun overview(@Header("Authorization") auth: String): Overview

    @GET("library/page")
    suspend fun page(
        @Header("Authorization") auth: String,
        @Query("status") status: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): FilmPageDto
}
