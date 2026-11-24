package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity

@Entity(tableName = "pack_films", primaryKeys = ["owner", "pack", "film"])
data class PackFilm(val owner: Long, val pack: Long, val film: Long, val position: Int)
