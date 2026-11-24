package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.PacksApi
import com.example.film_app_mobile.core.storage.dao.DeckDao
import com.example.film_app_mobile.core.storage.dao.PackDao
import com.example.film_app_mobile.data.cache.refresh
import com.example.film_app_mobile.data.mapper.entity
import com.example.film_app_mobile.data.mapper.ui
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import javax.inject.Inject

class PacksRepository
@Inject
constructor(
    private val account: AccountSession,
    private val packsApi: PacksApi,
    private val packs: PackDao,
    private val decks: DeckDao,
    private val sync: DecisionSync,
) {
    val catalogChanges
        get() = account.catalogChanges

    suspend fun packs() =
        account.operation {
            val id = owner()
            refresh({ packs.cached(id, "packs") }) {
                sync.flush(id, auth)
                packs.replacePacks(
                    id,
                    packsApi.packs(auth).mapIndexed { index, pack -> pack.entity(id, index) },
                )
            }
            packs.packs(id).map { it.ui() }
        }

    suspend fun films(pack: Long, allowCache: Boolean = true) =
        account.operation {
            val id = owner()
            refresh({
                allowCache &&
                    (decks.cached(id, "deck/" + pack) ||
                        decks.deck(id, pack).isNotEmpty() ||
                        (decks.hasLinks(id, pack) && decks.pendingCount(id) > 0))
            }) {
                sync.flush(id, auth)
                decks.replaceDeck(id, pack, packsApi.films(auth, pack).map { it.entity(id) })
            }
            decks.deck(id, pack).map { it.ui() }
        }
}
