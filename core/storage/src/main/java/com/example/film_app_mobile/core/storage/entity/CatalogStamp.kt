package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity

@Entity(tableName = "catalog_stamps", primaryKeys = ["owner"])
data class CatalogStamp(
    val owner: Long,
    val etag: String?,
    val count: Int,
    val checked: Long,
    val deckState: String? = null,
    val availableCount: Long? = null,
)

@Entity(tableName = "deck_pages", primaryKeys = ["owner", "pack"])
data class DeckPage(val owner: Long, val pack: Long, val cursor: String?)

@Entity(tableName = "personal_cache", primaryKeys = ["owner", "scope"])
data class PersonalCache(val owner: Long, val scope: String, val body: String)
