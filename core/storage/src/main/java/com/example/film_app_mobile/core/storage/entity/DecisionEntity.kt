package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity

@Entity(tableName = "decisions", primaryKeys = ["owner", "film"])
data class DecisionEntity(
    val owner: Long,
    val film: Long,
    val status: String?,
    val pending: Boolean,
    val updated: Long,
)
