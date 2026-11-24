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

    @Query("SELECT * FROM catalog_stamps WHERE owner=:owner")
    abstract suspend fun stamp(owner: Long): CatalogStamp?

    @Upsert abstract suspend fun saveStamp(stamp: CatalogStamp)

    @Transaction
    open suspend fun checked(stamp: CatalogStamp) { saveStamp(stamp); mark(Snapshot(stamp.owner, "packs")) }

    @Query("DELETE FROM deck_pages WHERE owner=:owner AND pack=:id")
    abstract suspend fun invalidatePage(owner: Long,id: Long)

    @Transaction
    open suspend fun replacePacks(owner: Long, rows: List<PackEntity>, stamp: CatalogStamp) {
        val old = packs(owner).associateBy { it.id }
        for (row in rows) {
            val previous = old[row.id]
            if (previous == null || previous.locked != row.locked || previous.isCompleted != row.isCompleted) invalidatePage(owner,row.id)
        }
        clearPacks(owner)
        putPacks(rows)
        saveStamp(stamp)
        mark(Snapshot(owner, "packs"))
    }
}
