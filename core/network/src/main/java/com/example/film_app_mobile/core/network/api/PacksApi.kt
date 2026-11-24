package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface PacksApi {
    @GET("packs/catalog")
    suspend fun catalog(@Header("Authorization") auth: String,
        @Header("If-None-Match") etag: String?): retrofit2.Response<CatalogDto>

    @GET("packs/{id}/films/page")
    suspend fun page(@Header("Authorization") auth: String, @Path("id") id: Long,
        @Query("limit") limit: Int, @Query("cursor") cursor: String?): FilmPageDto

}
