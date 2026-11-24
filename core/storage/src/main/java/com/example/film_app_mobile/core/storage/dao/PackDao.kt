package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

@Dao
abstract class PackDao : SnapshotCache() {
    @Query("SELECT * FROM packs WHERE owner = :owner ORDER BY position")
    abstract suspend fun packs(owner: Long): List<PackEntity>

    @Upsert protected abstract suspend fun putPacks(rows: List<PackEntity>)

    @Query("DELETE FROM packs WHERE owner=:owner")
    protected abstract suspend fun clearPacks(owner: Long)

    @Transaction
    open suspend fun replacePacks(owner: Long, rows: List<PackEntity>) {
        clearPacks(owner)
        putPacks(rows)
        mark(Snapshot(owner, "packs"))
    }
}
