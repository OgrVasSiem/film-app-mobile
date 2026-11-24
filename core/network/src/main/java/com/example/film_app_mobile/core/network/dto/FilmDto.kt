@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class FilmDto(
    val id: Long,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val year: Int?,
    val director: String?,
    val actors: List<String>,
    val durationMinutes: Int?,
    val genres: List<String>,
)
