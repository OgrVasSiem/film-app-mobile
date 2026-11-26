package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

abstract class FilmCache : SnapshotCache() {
    @Upsert abstract suspend fun putFilms(rows: List<FilmEntity>)
}
