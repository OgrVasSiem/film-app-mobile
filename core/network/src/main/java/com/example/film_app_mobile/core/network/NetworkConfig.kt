package com.example.film_app_mobile.core.network

data class NetworkConfig(
    val baseUrl: String,
    val allowCleartext: Boolean = false,
    val enableHttpLogging: Boolean = false,
)
