package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface CatalogV2Api {
    @GET
    suspend fun catalog(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Header("If-None-Match") etag: String?,
    ): retrofit2.Response<CatalogDto>

    @GET
    suspend fun page(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
        @Query("status") status: String?,
    ): FilmPageDto

    @GET suspend fun pick(@Url url: String, @Header("Authorization") auth: String): FilmPick

    @GET suspend fun overview(@Url url: String, @Header("Authorization") auth: String): Overview

    @GET suspend fun card(@Url url: String, @Header("Authorization") auth: String): FilmDto

    @PUT
    suspend fun decide(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body decision: DecisionDto,
    )

    @DELETE suspend fun reset(@Url url: String, @Header("Authorization") auth: String)
}
