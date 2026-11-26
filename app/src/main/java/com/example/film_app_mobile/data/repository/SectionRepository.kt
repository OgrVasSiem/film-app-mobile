package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.models.DeckSource
import com.example.film_app_mobile.core.network.NetworkStack
import com.example.film_app_mobile.core.network.api.CatalogV2Api
import com.example.film_app_mobile.core.network.dto.DecisionDto
import com.example.film_app_mobile.core.network.dto.FilmDto
import com.example.film_app_mobile.data.session.AccountSession
import javax.inject.Inject
import retrofit2.HttpException

class SectionRepository
@Inject
constructor(private val account: AccountSession, stack: NetworkStack) {
    private val api = stack.retrofit.create(CatalogV2Api::class.java)
    val changes
        get() = account.catalogChanges

    internal fun deckPath(deck: DeckSource): String =
        when (deck) {
            DeckSource.Personal -> "recommendations/deck"
            is DeckSource.Editorial -> "packs/${deck.packId}/films/page"
            is DeckSource.Periodic -> "decks/${deck.period.name}"
        }

    private fun root(type: ContentType) = "../v2/${type.apiKey}/"

    suspend fun catalog(type: ContentType) =
        account.operation {
            val response = api.catalog(root(type) + "catalog", auth, null)
            if (!response.isSuccessful) throw HttpException(response)
            checkNotNull(response.body())
        }

    suspend fun preview(type: ContentType, key: String) =
        account.operation {
            val path = if (key == "FOR_YOU") "recommendations/deck" else "decks/$key"
            api.page(root(type) + path, auth, if (key == "FOR_YOU") 3 else 1, null, null)
                .items
                .map { it.posterUrl }
        }

    suspend fun pick(type: ContentType) =
        account.operation { api.pick(root(type) + "recommendations/film", auth) }

    suspend fun overview(type: ContentType) =
        account.operation { api.overview(root(type) + "library/overview", auth) }

    suspend fun card(type: ContentType, id: Long) =
        account.operation { api.card(root(type) + "cards/$id", auth) }

    suspend fun page(
        type: ContentType,
        path: String,
        status: String?,
        cursor: String?,
        previous: List<FilmDto> = emptyList(),
    ) =
        account.operation {
            val url = root(type) + path
            try {
                val result = api.page(url, auth, 10, cursor, status)
                if (cursor == null) result
                else result.copy(items = (previous + result.items).distinctBy { it.id })
            } catch (error: HttpException) {
                if (cursor == null || error.code() !in listOf(400, 409)) throw error
                api.page(url, auth, 10, null, status)
            }
        }

    suspend fun decide(type: ContentType, id: Long, status: String?) =
        account.operation {
            val url = root(type) + "library/$id"
            if (status == null) api.reset(url, auth) else api.decide(url, auth, DecisionDto(status))
            catalogChanged()
        }
}
