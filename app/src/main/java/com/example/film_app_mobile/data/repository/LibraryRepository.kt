package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.LibraryApi
import com.example.film_app_mobile.core.storage.dao.LibraryDao
import com.example.film_app_mobile.core.storage.entity.DecisionEntity
import com.example.film_app_mobile.data.cache.refresh
import com.example.film_app_mobile.data.mapper.entity
import com.example.film_app_mobile.data.mapper.ui
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import com.example.film_app_mobile.ui.model.Film
import javax.inject.Inject

class LibraryRepository
@Inject
constructor(
    private val account: AccountSession,
    private val libraryApi: LibraryApi,
    private val dao: LibraryDao,
    private val sync: DecisionSync,
) {
    suspend fun library(status: String) =
        account.operation {
            val id = owner()
            refresh({
                dao.cached(id, "library/" + status) ||
                    dao.library(id, status).isNotEmpty() ||
                    dao.pending(id).isNotEmpty()
            }) {
                sync.flush(id, auth)
                dao.replaceFolder(
                    id,
                    status,
                    libraryApi.library(auth, status).map { it.entity(id) },
                )
            }
            dao.library(id, status).map { it.ui() }
        }

    suspend fun decide(movie: Film, status: String) = saveDecision(movie.id, status)

    suspend fun reset(id: Long) = saveDecision(id, null)

    private suspend fun saveDecision(film: Long, status: String?) =
        account.operation {
            val decision = DecisionEntity(owner(), film, status, true, System.currentTimeMillis())
            try {
                if (status == null) libraryApi.reset(auth, film)
                else
                    libraryApi.decide(
                        auth,
                        film,
                        com.example.film_app_mobile.core.network.dto.DecisionDto(status),
                    )
            } catch (offline: java.io.IOException) {
                dao.putDecision(decision)
                sync.scheduleSync()
                return@operation
            }
            dao.commitDecision(decision)
            account.catalogChanged()
        }
}
