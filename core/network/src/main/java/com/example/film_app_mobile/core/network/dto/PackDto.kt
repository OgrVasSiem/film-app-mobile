@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PackDto(
    val id: Long,
    val title: String,
    val description: String,
    val premium: Boolean,
    val locked: Boolean,
    val coverUrl: String? = null,
    val isCompleted: Boolean = false,
)
