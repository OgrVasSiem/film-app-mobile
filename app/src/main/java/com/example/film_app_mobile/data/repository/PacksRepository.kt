package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.PacksApi
import com.example.film_app_mobile.core.storage.dao.*
import com.example.film_app_mobile.core.storage.entity.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import com.example.film_app_mobile.data.image.CoverFiles
import com.example.film_app_mobile.ui.model.Film
import javax.inject.Inject
import retrofit2.HttpException

data class LocalDeck(val cards: List<Film>, val next: String?)

@javax.inject.Singleton
class PacksRepository @Inject constructor(
    private val account: AccountSession, private val packsApi: PacksApi,
    private val packs: PackDao, private val decks: DeckDao, private val sync: DecisionSync,
    private val files: CoverFiles,
) {
    val catalogChanges get() = account.catalogChanges

    suspend fun cachedPacks(): List<com.example.film_app_mobile.ui.model.Pack> {
        val owner = account.cachedOwner
        val stamp = packs.stamp(owner) ?: return emptyList()
        val rows = packs.packs(owner)
        return if (rows.size == stamp.count) rows.map { it.ui() } else emptyList()
    }

    private val flights = mutableMapOf<String, kotlinx.coroutines.CompletableDeferred<List<com.example.film_app_mobile.ui.model.Pack>>>()

    suspend fun packs(force: Boolean = false): List<com.example.film_app_mobile.ui.model.Pack> {
        val key = account.auth
        val result = kotlinx.coroutines.CompletableDeferred<List<com.example.film_app_mobile.ui.model.Pack>>()
        val pending = synchronized(flights) { flights.putIfAbsent(key, result) }
        if (pending != null) return pending.await()
        try {
            return fetchCatalog(force).also { result.complete(it) }
        } catch (failure: Throwable) {
            result.completeExceptionally(failure)
            throw failure
        } finally { synchronized(flights) { flights.remove(key, result) } }
    }

    private suspend fun fetchCatalog(force: Boolean): List<com.example.film_app_mobile.ui.model.Pack> {
        return account.operation {
            val id = owner()
            val rows = packs.packs(id)
            val stamp = packs.stamp(id)
            val complete = stamp != null && rows.size == stamp.count &&
                rows.withIndex().all { (index, row) -> row.position == index && (row.coverUrl == null ||
                    (row.coverHash != null && row.coverMime != null && files.valid(row.coverHash!!))) }
            val fresh = complete && packs.cached(id,"packs") &&
                System.currentTimeMillis() - stamp.checked in 0 until 300_000L
            if (fresh && !force) return@operation rows.map { it.ui() }
            sync.flush(id, auth)
            val tag = stamp?.etag.takeIf { complete }
            val response = packsApi.catalog(auth, tag)
            if (response.code() == 304 && complete && tag != null) {
                packs.checked(stamp.copy(checked = System.currentTimeMillis()))
            } else {
                if (!response.isSuccessful) throw HttpException(response)
                val catalog = requireNotNull(response.body())
                require(catalog.packs.map { it.id }.distinct().size == catalog.packs.size)
                val hashes = catalog.covers.mapValues { files.save(it.value) }
                val entities = catalog.packs.mapIndexed { index, pack ->
                    pack.entity(id,index).copy(coverHash = pack.coverUrl?.let { hashes.getValue(it) },
                        coverMime = pack.coverUrl?.let { catalog.covers.getValue(it).mimeType })
                }
                packs.replacePacks(id, entities, CatalogStamp(id, response.headers()["ETag"], entities.size, System.currentTimeMillis()))
            }
            packs.packs(id).map { it.ui() }
        }
    }

    suspend fun localDeck(pack: Long): LocalDeck? {
        val id = account.cachedOwner
        if (packs.packs(id).none { it.id == pack && !it.locked }) return null
        val page = decks.page(id, pack) ?: return null
        return LocalDeck(decks.deck(id, pack).map { it.ui() }, page.cursor)
    }

    suspend fun page(pack: Long, cursor: String? = null, restart: Boolean = false, limit: Int = 10): LocalDeck =
        account.operation {
            val id = owner()
            sync.flush(id, auth)
            val saved = decks.page(id, pack)
            if (packs.packs(id).any { it.id == pack && it.locked })
                throw com.example.film_app_mobile.data.error.RequestFailure(com.example.film_app_mobile.R.string.premium_required)
            if (saved == null || restart || (cursor != null && saved.cursor == cursor)) {
                val after = if (saved == null || restart) null else cursor
                val result = try { packsApi.page(auth, pack, limit, after) } catch (e: HttpException) {
                    if (e.code() == 403) { packs.invalidatePage(id, pack); account.catalogChanged() }
                    throw e
                }
                require(after == null || result.nextCursor != after)
                decks.storePage(DeckPage(id, pack, result.nextCursor), after == null, result.items.map { it.entity(id) })
            }
            checkNotNull(localDeck(pack))
        }
}
