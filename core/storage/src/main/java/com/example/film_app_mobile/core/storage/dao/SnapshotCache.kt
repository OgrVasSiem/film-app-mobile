package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

abstract class SnapshotCache {
    @Query("SELECT EXISTS(SELECT 1 FROM snapshots WHERE owner=:owner AND scope=:scope)")
    abstract suspend fun cached(owner: Long, scope: String): Boolean

    @Upsert protected abstract suspend fun mark(snapshot: Snapshot)
}
