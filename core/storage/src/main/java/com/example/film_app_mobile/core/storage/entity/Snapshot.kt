package com.example.film_app_mobile.core.storage.entity

import androidx.room.Entity

@Entity(tableName = "snapshots", primaryKeys = ["owner", "scope"])
data class Snapshot(val owner: Long, val scope: String)
