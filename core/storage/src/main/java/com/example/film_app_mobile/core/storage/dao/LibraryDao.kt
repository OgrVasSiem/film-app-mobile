package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

@Dao
abstract class LibraryDao : FilmCache() {
    @Query(
        "SELECT f.* FROM decisions d JOIN films f ON f.owner=d.owner AND f.id=d.film WHERE d.owner=:owner AND d.status=:status ORDER BY d.updated DESC, f.id"
    )
    abstract suspend fun library(owner: Long, status: String): List<FilmEntity>

    @Query("DELETE FROM decisions WHERE owner=:owner AND status=:status AND NOT pending")
    protected abstract suspend fun clearFolder(owner: Long, status: String)

    @Upsert abstract suspend fun putDecision(value: DecisionEntity)

    @Query("SELECT * FROM decisions WHERE owner=:owner AND pending ORDER BY updated, film")
    abstract suspend fun pending(owner: Long): List<DecisionEntity>

    @Query("UPDATE decisions SET pending=0 WHERE owner=:owner AND film=:id")
    abstract suspend fun acknowledge(owner: Long, id: Long)

    @Query("DELETE FROM decisions WHERE owner=:owner AND film=:id")
    abstract suspend fun forget(owner: Long, id: Long)

    @Transaction
    open suspend fun replaceFolder(owner: Long, status: String, rows: List<FilmEntity>) {
        clearFolder(owner, status)
        putFilms(rows)
        val dirty = pending(owner).map { it.film }.toSet()
        val now = System.currentTimeMillis()
        rows.forEachIndexed { index, film ->
            if (film.id !in dirty)
                putDecision(DecisionEntity(owner, film.id, status, false, now - index))
        }
        mark(Snapshot(owner, "library/" + status))
    }

    @Query("DELETE FROM snapshots WHERE owner=:owner AND (scope='packs' OR scope LIKE 'deck/%')")
    protected abstract suspend fun invalidate(owner: Long)

    @Transaction
    open suspend fun commitDecision(decision: DecisionEntity) {
        if (decision.status == null) forget(decision.owner, decision.film)
        else putDecision(decision.copy(pending = false))
        invalidate(decision.owner)
    }
}
