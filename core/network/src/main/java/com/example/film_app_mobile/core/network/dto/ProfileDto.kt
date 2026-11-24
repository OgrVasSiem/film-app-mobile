@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: Long,
    val login: String,
    val premium: Boolean,
    val adsEnabled: Boolean,
)
