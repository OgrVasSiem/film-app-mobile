package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.core.network.NetworkStack
import com.example.film_app_mobile.core.network.api.CatalogV2Api
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.dao.LibraryDao
import com.example.film_app_mobile.core.storage.entity.PersonalCache
import com.example.film_app_mobile.data.error.temporary
import com.example.film_app_mobile.data.image.CoverFiles
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.SyncScheduler
import javax.inject.Inject
import retrofit2.HttpException

class SectionRepository
@Inject
constructor(
    private val account: AccountSession,
    stack: NetworkStack,
    private val cache: LibraryDao,
    private val files: CoverFiles,
    private val scheduler: SyncScheduler = SyncScheduler {},
) {
    private val api = stack.retrofit.create(CatalogV2Api::class.java)
    private val json = stack.json

    private fun local(type: ContentType) = OfflineSection(cache, json, account.cachedOwner, type)

    private suspend fun ready(type: ContentType) =
        cache.body(account.cachedOwner, local(type).prefix + "snapshot/ready") != null

    private suspend fun <T> withFallback(cached: suspend () -> T?, remote: suspend () -> T): T {
        return try {
            remote()
        } catch (e: com.example.film_app_mobile.data.error.RequestFailure) {
            if (!e.temporary) throw e
            cached() ?: throw e
        }
    }

    val changes
        get() = account.catalogChanges

    internal fun deckPath(deck: DeckSource): String =
        when (deck) {
            DeckSource.Personal -> "recommendations/deck"
            is DeckSource.Editorial -> "packs/${deck.packId}/films/page"
            is DeckSource.Periodic -> "decks/${deck.period.name}"
        }

    private fun root(type: ContentType) = "../v2/${type.apiKey}/"

    suspend fun preview(type: ContentType, key: String) =
        account.operation {
            val path = if (key == "FOR_YOU") "recommendations/deck" else "decks/" + key
            if (ready(type))
                return@operation local(type)
                    .page(path, null)
                    ?.items
                    .orEmpty()
                    .take(if (key == "FOR_YOU") 3 else 1)
                    .map { it.posterUrl }
            api.page(
                    root(type) + (if (key == "FOR_YOU") "recommendations/deck" else "decks/" + key),
                    auth,
                    if (key == "FOR_YOU") 3 else 1,
                    null,
                    null,
                )
                .items
                .map { it.posterUrl }
        }

    suspend fun cached(type: ContentType) =
        cache.body(account.cachedOwner, root(type))?.let {
            json.decodeFromString<CachedCatalog>(it)
        }

    suspend fun catalog(type: ContentType): CachedCatalog =
        if (ready(type)) {
            val copy = checkNotNull(cached(type))
            val local = local(type)
            if (local.edits().isEmpty()) copy
            else
                copy.copy(
                    catalog =
                        copy.catalog.copy(
                            packs =
                                copy.catalog.packs.map { pack ->
                                    val page =
                                        if (pack.locked) null
                                        else local.page("packs/" + pack.id + "/films/page", null)
                                    if (page == null) pack
                                    else
                                        pack.copy(
                                            availableCount = page.items.size.toLong(),
                                            isCompleted = page.items.isEmpty(),
                                        )
                                }
                        )
                )
        } else withFallback({ cached(type) }) { fetchCatalog(type) }

    private suspend fun fetchCatalog(type: ContentType) =
        account.operation {
            val old = cached(type)
            val tag = old?.etag?.takeIf { old.hashes.values.all { files.valid(it) } }
            val response = api.catalog(root(type) + "catalog", auth, tag)
            if (response.code() == 304) return@operation checkNotNull(old)
            if (!response.isSuccessful) throw HttpException(response)
            val body = checkNotNull(response.body())
            val hashes =
                body.covers.mapValues { (_, cover) ->
                    if (files.valid(cover.sha256)) cover.sha256 else files.save(cover)
                }
            require(body.packs.all { it.coverUrl == null || it.coverUrl in hashes })
            CachedCatalog(body.copy(covers = emptyMap()), hashes, response.headers()["ETag"]).also {
                cache.save(PersonalCache(owner(), root(type), json.encodeToString(it)))
            }
        }

    suspend fun cachedPage(type: ContentType, path: String, status: String?) =
        if (ready(type)) local(type).page(path, status)
        else
            cache.body(account.cachedOwner, root(type) + "data/" + path + "/" + status)?.let {
                json.decodeFromString<FilmPageDto>(it)
            }

    suspend fun cachedPick(type: ContentType) =
        cache.body(account.cachedOwner, root(type) + "data/single")?.let {
            json.decodeFromString<FilmPick>(it)
        }

    suspend fun pick(type: ContentType) =
        if (ready(type) || local(type).edits().isNotEmpty()) local(type).pick()
        else
            withFallback({ cachedPick(type) }) {
                account.operation {
                    api.pick(root(type) + "recommendations/film", auth).also {
                        cache.save(
                            PersonalCache(
                                owner(),
                                root(type) + "data/single",
                                json.encodeToString(it),
                            )
                        )
                    }
                }
            }

    suspend fun cachedOverview(type: ContentType) =
        cache.body(account.cachedOwner, root(type) + "data/overview")?.let {
            json.decodeFromString<Overview>(it)
        }

    suspend fun overview(type: ContentType) =
        if (ready(type) || local(type).edits().isNotEmpty()) local(type).overview()
        else
            withFallback({ cachedOverview(type) }) {
                account.operation {
                    api.overview(root(type) + "library/overview", auth).also {
                        cache.save(
                            PersonalCache(
                                owner(),
                                root(type) + "data/overview",
                                json.encodeToString(it),
                            )
                        )
                    }
                }
            }

    suspend fun card(type: ContentType, id: Long) =
        local(type).card(id)
            ?: account.operation {
                try {
                    api.card(root(type) + "cards/$id", auth)
                } catch (e: java.io.IOException) {
                    local(type).recentCard(id) ?: throw e
                }
            }

    suspend fun page(type: ContentType, path: String, status: String?, cursor: String?) =
        withFallback({ local(type).page(path, status) }) {
            account.operation {
                if (ready(type) || local(type).edits().isNotEmpty()) {
                    if (path.startsWith("packs/")) {
                        val id = path.substringAfter("packs/").substringBefore('/').toLongOrNull()
                        if (cached(type)?.catalog?.packs?.any { it.id == id && it.locked } == true)
                            throw com.example.film_app_mobile.data.error.RequestFailure(
                                com.example.film_app_mobile.R.string.premium_required
                            )
                    }
                    local(type).page(path, status)?.let {
                        return@operation it
                    }
                }
                val (page, restarted) =
                    try {
                        api.page(root(type) + path, auth, 10, cursor, status) to false
                    } catch (e: HttpException) {
                        if (cursor == null || e.code() !in listOf(400, 409)) throw e
                        api.page(root(type) + path, auth, 10, null, status) to true
                    }
                val merged =
                    if (cursor == null || restarted) page
                    else
                        page.copy(
                            items =
                                (cachedPage(type, path, status)?.items.orEmpty() + page.items)
                                    .distinctBy { it.id }
                        )
                cache.save(
                    PersonalCache(
                        owner(),
                        root(type) + "data/" + path + "/" + status,
                        json.encodeToString(merged),
                    )
                )
                merged
            }
        }

    suspend fun decide(
        type: ContentType,
        id: Long,
        status: String?,
        remaining: FilmPageDto? = null,
        path: String = "",
    ) =
        account.operation {
            val local = local(type)
            val film = local.card(id)
            if (film != null) {
                local.remember(id, status, film)
                scheduler.enqueue()
                catalogChanged()
                return@operation
            }
            val url = root(type) + "library/$id"
            try {
                if (status == null) api.reset(url, auth)
                else api.decide(url, auth, DecisionDto(status))
            } catch (e: Exception) {
                if (
                    e !is java.io.IOException &&
                        !(e is HttpException &&
                            (e.code() in 500..599 || e.code() in listOf(408, 429)))
                )
                    throw e
                val cached = local.recentCard(id) ?: throw e
                local.remember(id, status, cached)
                scheduler.enqueue()
                catalogChanged()
                return@operation
            }
            cache.clearSection(owner(), root(type) + "data/")
            if (remaining != null)
                cache.save(
                    PersonalCache(
                        owner(),
                        root(type) + "data/" + path + "/null",
                        json.encodeToString(remaining),
                    )
                )
        }

    internal suspend fun flush(owner: Long, auth: String) {
        for (type in ContentType.entries) {
            for (row in cache.entries(owner, "offline/${type.apiKey}/pending/")) {
                val url = root(type) + "library/" + row.scope.substringAfterLast('/')
                if (row.body == "RESET") api.reset(url, auth)
                else api.decide(url, auth, DecisionDto(row.body))
                cache.remove(owner, row.scope)
            }
        }
    }

    internal suspend fun flushCurrent() = account.operation { if (signedIn) flush(owner(), auth) }

    internal suspend fun synchronize(context: android.content.Context) {
        val owner = account.cachedOwner
        if (owner < 0) return
        account.operation { if (owner == cachedOwner) flush(owner, auth) }
        val posters = mutableSetOf<String>()
        for (type in ContentType.entries) {
            val local = OfflineSection(cache, json, owner, type)
            val revision = local.revision()
            val stamp = cache.body(owner, local.prefix + "snapshot/fetched")?.toLongOrNull()
            if (
                stamp != null &&
                    System.currentTimeMillis() - stamp in 0L..60_000L &&
                    cache.body(owner, local.prefix + "snapshot/revision") == revision.orEmpty()
            ) {
                posters.addAll(local.posterUrls())
                continue
            }
            val catalog = fetchCatalog(type).catalog
            val rows = mutableListOf<PersonalCache>()
            val paths = mutableListOf<Pair<String, String?>>()
            for (pack in catalog.packs.filterNot { it.locked }) paths +=
                "packs/" + pack.id + "/films/page" to null
            for (section in catalog.sections) {
                when (section.key) {
                    "DAY",
                    "WEEK",
                    "MONTH" -> paths += "decks/" + section.key to null
                    "FOR_YOU" ->
                        if (section.state == "READY") paths += "recommendations/deck" to null
                }
            }
            for (status in listOf("WATCHED", "WILL_WATCH", "WONT_WATCH")) paths +=
                "library/page" to status
            for ((path, status) in paths.distinct()) {
                val items = mutableListOf<FilmDto>()
                var cursor: String? = null
                val seen = mutableSetOf<String?>()
                var page: FilmPageDto
                do {
                    check(seen.add(cursor))
                    page =
                        account.operation {
                            if (owner != cachedOwner)
                                throw kotlinx.coroutines.CancellationException()
                            api.page(root(type) + path, auth, 30, cursor, status)
                        }
                    items.addAll(page.items)
                    cursor = page.nextCursor
                } while (cursor != null)
                posters.addAll(items.mapNotNull { it.posterUrl })
                val key = "snapshot/page/" + path + "/" + status + "/"
                val batches = items.distinctBy { it.id }.chunked(30).ifEmpty { listOf(emptyList()) }
                batches.forEachIndexed { index, batch ->
                    rows +=
                        local.row(
                            key + index,
                            json.encodeToString(page.copy(items = batch, nextCursor = null)),
                        )
                }
            }
            val pick =
                account.operation {
                    if (owner != cachedOwner) throw kotlinx.coroutines.CancellationException()
                    api.pick(root(type) + "recommendations/film", auth)
                }
            pick.film?.posterUrl?.let(posters::add)
            rows += local.row("snapshot/pick", json.encodeToString(pick))
            rows += local.row("snapshot/ready", "1")
            rows += local.row("snapshot/fetched", System.currentTimeMillis().toString())
            rows += local.row("snapshot/revision", revision.orEmpty())
            account.operation {
                if (
                    owner == cachedOwner &&
                        local.revision() == revision &&
                        cache.entries(owner, local.prefix + "pending/").isEmpty()
                )
                    cache.replaceOffline(owner, local.prefix, rows)
            }
        }
        for (url in posters) com.example.film_app_mobile.data.image.preloadPoster(context, url)
    }
}
