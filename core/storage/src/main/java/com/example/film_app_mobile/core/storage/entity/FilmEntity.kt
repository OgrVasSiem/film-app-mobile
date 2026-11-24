package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity

@Entity(tableName = "films", primaryKeys = ["owner", "id"])
data class FilmEntity(
    val owner: Long,
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
