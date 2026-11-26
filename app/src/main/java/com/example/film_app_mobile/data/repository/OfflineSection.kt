package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.dao.LibraryDao
import com.example.film_app_mobile.core.storage.entity.PersonalCache
import kotlinx.serialization.json.Json

internal class OfflineSection(
    private val dao: LibraryDao,
    private val json: Json,
    val owner: Long,
    val type: ContentType,
) {
    val prefix = "offline/${type.apiKey}/"
    private val snapshot = prefix + "snapshot/"
    private val folders = listOf("WILL_WATCH", "WATCHED", "WONT_WATCH")

    fun row(key: String, value: String) = PersonalCache(owner, prefix + key, value)

    suspend fun revision() = dao.body(owner, prefix + "revision")

    suspend fun edits() =
        dao.entries(owner, prefix + "local/").associate {
            it.scope.substringAfterLast('/').toLong() to it.body
        }

    suspend fun remember(id: Long, status: String?, film: FilmDto) {
        val value = status ?: "RESET"
        val memberships =
            if (status == null) emptyList()
            else
                dao.entries(owner, snapshot + "page/packs/")
                    .filter { entry ->
                        json.decodeFromString<FilmPageDto>(entry.body).items.any { it.id == id }
                    }
                    .map { entry ->
                        val path =
                            entry.scope.removePrefix(snapshot + "page/").substringBefore("/null/")
                        row("membership/" + path + "/" + id, "1")
                    }
        dao.saveAll(
            memberships +
                listOf(
                    row("pending/$id", value),
                    row("local/$id", value),
                    row("card/$id", json.encodeToString(film)),
                    row("revision", java.util.UUID.randomUUID().toString()),
                )
        )
    }

    suspend fun card(id: Long): FilmDto? {
        val body = dao.body(owner, prefix + "card/$id")
        if (body != null) return json.decodeFromString<FilmDto>(body)
        return rawPages().flatMap { it.items }.firstOrNull { it.id == id }
            ?: pickRaw()?.film?.takeIf { it.id == id }
    }

    suspend fun recentCard(id: Long): FilmDto? {
        val base = "../v2/${type.apiKey}/data/"
        for (row in dao.entries(owner, base)) {
            val candidates =
                when (row.scope.removePrefix(base)) {
                    "overview" ->
                        json.decodeFromString<Overview>(row.body).sections.values.flatMap {
                            it.items
                        }
                    "single" -> listOfNotNull(json.decodeFromString<FilmPick>(row.body).film)
                    else -> json.decodeFromString<FilmPageDto>(row.body).items
                }
            candidates
                .firstOrNull { it.id == id }
                ?.let {
                    return it
                }
        }
        return null
    }

    private suspend fun rawPages() =
        dao.entries(owner, snapshot + "page/").map { json.decodeFromString<FilmPageDto>(it.body) }

    suspend fun posterUrls(): Set<String> = buildSet {
        rawPages().forEach { page -> page.items.mapNotNullTo(this) { it.posterUrl } }
        pickRaw()?.film?.posterUrl?.let(::add)
    }

    private suspend fun pickRaw() =
        dao.body(owner, snapshot + "pick")?.let { json.decodeFromString<FilmPick>(it) }

    suspend fun page(path: String, status: String?): FilmPageDto? {
        val raw =
            dao.entries(owner, snapshot + "page/" + path + "/" + status + "/")
                .map { json.decodeFromString<FilmPageDto>(it.body) }
                .let { pages -> pages.firstOrNull()?.copy(items = pages.flatMap { it.items }) }
                ?: dao.body(owner, "../v2/${type.apiKey}/data/" + path + "/" + status)?.let {
                    json.decodeFromString<FilmPageDto>(it)
                }
        val overview =
            if (status != null && raw == null)
                dao.body(owner, "../v2/${type.apiKey}/data/overview")?.let {
                    json.decodeFromString<Overview>(it).sections[status]
                }
            else null
        val local = edits()
        val items =
            (raw?.items ?: overview?.items.orEmpty()).filter { film ->
                if (status == null) local[film.id] in listOf(null, "RESET") else film.id !in local
            }
        if (status == null) {
            val restored =
                local
                    .filterValues { it == "RESET" }
                    .keys
                    .filter { dao.body(owner, prefix + "membership/" + path + "/" + it) != null }
                    .mapNotNull { card(it) }
            return raw?.copy(items = (items + restored).distinctBy { it.id }, nextCursor = null)
        }
        val added =
            local.entries
                .toList()
                .asReversed()
                .filter { it.value == status }
                .mapNotNull { card(it.key) }
        return FilmPageDto((added + items).distinctBy { it.id }, null)
    }

    suspend fun overview() =
        Overview(
            folders.associateWith { status ->
                val items = page("library/page", status)?.items.orEmpty()
                Section(items.size.toLong(), items.take(5))
            }
        )

    suspend fun pick(): FilmPick {
        val local = edits()
        val original =
            pickRaw()
                ?: dao.body(owner, "../v2/${type.apiKey}/data/single")?.let {
                    json.decodeFromString<FilmPick>(it)
                }
        if (original?.film != null && local[original.film!!.id] in listOf(null, "RESET"))
            return original
        val film =
            dao.entries(owner, snapshot + "page/")
                .filterNot { "/library/page/" in it.scope }
                .asSequence()
                .map { json.decodeFromString<FilmPageDto>(it.body) }
                .flatMap { it.items.asSequence() }
                .firstOrNull { local[it.id] in listOf(null, "RESET") }
        return FilmPick(film, if (film == null) "EXHAUSTED" else "DISCOVERY")
    }
}
