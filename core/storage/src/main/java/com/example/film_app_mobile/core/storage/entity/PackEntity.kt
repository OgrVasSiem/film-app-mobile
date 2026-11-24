package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity

@Entity(tableName = "packs", primaryKeys = ["owner", "id"])
data class PackEntity(
    val owner: Long,
    val id: Long,
    val title: String,
    val description: String,
    val premium: Boolean,
    val locked: Boolean,
    val position: Int,
    val coverUrl: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
    val coverHash: String? = null,
    val coverMime: String? = null,
)
