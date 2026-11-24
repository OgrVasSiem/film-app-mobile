package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

@Dao
abstract class DeckDao : FilmCache() {
    @Query("SELECT EXISTS(SELECT 1 FROM pack_films WHERE owner=:owner AND pack=:pack)")
    abstract suspend fun hasLinks(owner: Long, pack: Long): Boolean

    @Query("SELECT count(*) FROM decisions WHERE owner=:owner AND pending")
    abstract suspend fun pendingCount(owner: Long): Int

    @Upsert protected abstract suspend fun putLinks(rows: List<PackFilm>)

    @Query(
        "DELETE FROM pack_films WHERE owner=:owner AND pack=:pack AND film NOT IN (SELECT film FROM decisions WHERE owner=:owner)"
    )
    protected abstract suspend fun clearDeck(owner: Long, pack: Long)

    @Query(
        "SELECT f.* FROM films f JOIN pack_films p ON p.owner=f.owner AND p.film=f.id JOIN packs k ON k.owner=p.owner AND k.id=p.pack LEFT JOIN decisions d ON d.owner=f.owner AND d.film=f.id WHERE p.owner=:owner AND p.pack=:pack AND NOT k.locked AND d.status IS NULL ORDER BY p.position, f.id"
    )
    abstract suspend fun deck(owner: Long, pack: Long): List<FilmEntity>

    @Query("SELECT * FROM deck_pages WHERE owner=:owner AND pack=:pack")
    abstract suspend fun page(owner: Long, pack: Long): DeckPage?

    @Upsert protected abstract suspend fun savePage(page: DeckPage)

    @Query("DELETE FROM pack_films WHERE owner=:owner AND pack=:pack")
    protected abstract suspend fun clearLinks(owner: Long, pack: Long)

    @Query("SELECT film FROM pack_films WHERE owner=:owner AND pack=:pack ORDER BY position")
    protected abstract suspend fun ids(owner: Long, pack: Long): List<Long>

    @Transaction
    open suspend fun storePage(page: DeckPage, first: Boolean, rows: List<FilmEntity>) {
        val (owner, pack) = page
        if (first) clearLinks(owner, pack)
        val existing = ids(owner, pack).toHashSet()
        val offset = existing.size
        val newRows = rows.filter { existing.add(it.id) }
        putFilms(rows)
        putLinks(newRows.mapIndexed { i, f -> PackFilm(owner, pack, f.id, offset + i) })
        rows.forEach { forgetSyncedDecision(owner, it.id) }
        savePage(page)
    }

    @Query("DELETE FROM decisions WHERE owner=:owner AND film=:id AND NOT pending")
    protected abstract suspend fun forgetSyncedDecision(owner: Long, id: Long)
}
