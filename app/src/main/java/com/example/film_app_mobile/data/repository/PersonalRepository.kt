package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.PersonalApi
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.dao.*
import com.example.film_app_mobile.core.storage.entity.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class PersonalRepository
@Inject
constructor(
    private val account: AccountSession,
    private val api: PersonalApi,
    private val cache: LibraryDao,
    private val decks: DeckDao,
    private val sync: DecisionSync,
) {
    suspend fun cachedPick(): FilmPick? {
        val owner = account.cachedOwner
        return cache
            .body(owner, "pick")
            ?.let { Json.decodeFromString<FilmPick>(it) }
            ?.takeUnless { it.film?.let { f -> cache.decided(owner, f.id) } == true }
    }

    suspend fun pick() =
        account.operation {
            val id = owner()
            sync.flush(id, auth)
            val result = api.pick(auth)
            result.film?.let { cache.putFilms(listOf(it.entity(id))) }
            cache.save(PersonalCache(id, "pick", Json.encodeToString(result)))
            result
        }

    suspend fun decided(id: Long) = cache.decided(account.cachedOwner, id)

    suspend fun pending() = cache.pending(account.cachedOwner).isNotEmpty()

    suspend fun cachedOverview() =
        cache.body(account.cachedOwner, "overview")?.let { Json.decodeFromString<Overview>(it) }

    suspend fun overview() =
        account.operation {
            val id = owner()
            sync.flush(id, auth)
            api.overview(auth).also { result ->
                cache.putFilms(
                    result.sections.values.flatMap { section ->
                        section.items.map { it.entity(id) }
                    }
                )
                cache.save(PersonalCache(id, "overview", Json.encodeToString(result)))
            }
        }

    suspend fun cachedList(status: String) =
        cache.body(account.cachedOwner, "list/" + status)?.let {
            Json.decodeFromString<FilmPageDto>(it)
        }

    suspend fun list(status: String, cursor: String?) =
        account.operation {
            val id = owner()
            sync.flush(id, auth)
            val page = api.page(auth, status, 10, cursor)
            val rows =
                ((if (cursor != null) cachedList(status)?.items.orEmpty() else emptyList()) +
                        page.items)
                    .distinctBy { it.id }
            cache.putFilms(page.items.map { it.entity(id) })
            FilmPageDto(rows, page.nextCursor).also {
                cache.save(PersonalCache(id, "list/" + status, Json.encodeToString(it)))
            }
        }

    suspend fun posterPreview() =
        account.operation { api.deck(auth, 3, null).items.map { it.posterUrl } }

    suspend fun deck(cursor: String?) =
        account.operation {
            val id = owner()
            sync.flush(id, auth)
            var first = cursor == null
            val page =
                try {
                    api.deck(auth, 10, cursor)
                } catch (e: HttpException) {
                    if (cursor == null || e.code() != 409) throw e
                    first = true
                    api.deck(auth, 10, null)
                }
            decks.storePage(
                DeckPage(id, -1, page.nextCursor),
                first,
                page.items.map { it.entity(id) },
            )
            LocalDeck(decks.deck(id, -1).map { it.ui() }, page.nextCursor)
        }
}
