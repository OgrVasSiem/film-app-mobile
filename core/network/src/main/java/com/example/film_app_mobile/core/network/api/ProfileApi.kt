package com.example.film_app_mobile.core.network.api

import com.example.film_app_mobile.core.network.dto.*
import retrofit2.http.*

interface ProfileApi {
    @GET("me") suspend fun profile(@Header("Authorization") auth: String): ProfileDto
}
