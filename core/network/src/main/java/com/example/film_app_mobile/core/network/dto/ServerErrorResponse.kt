package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ServerErrorResponse(
    val code: String? = null,
    val message: String? = null,
)
