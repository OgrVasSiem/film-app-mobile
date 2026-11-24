package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: Long,
    val login: String,
    val premium: Boolean,
    val adsEnabled: Boolean,
)
