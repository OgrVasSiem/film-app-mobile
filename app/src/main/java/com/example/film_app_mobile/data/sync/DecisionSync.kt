package com.example.film_app_mobile.data.sync

import com.example.film_app_mobile.core.network.api.LibraryApi
import com.example.film_app_mobile.core.network.dto.DecisionDto
import com.example.film_app_mobile.core.storage.dao.LibraryDao
import com.example.film_app_mobile.data.session.AccountSession
import javax.inject.Inject
import retrofit2.HttpException

class DecisionSync @Inject constructor(
    private val account: AccountSession,
    private val libraryApi: LibraryApi,
    private val dao: LibraryDao,
    private val scheduler: SyncScheduler,
) {
    fun scheduleSync() {
        if (account.signedIn) scheduler.enqueue()
    }

    suspend fun sync() = account.operation { if (signedIn) flush(owner(), auth) }

    internal suspend fun flush(id: Long, auth: String) {
        for (decision in dao.pending(id)) {
            val status = decision.status
            try {
                if (status == null) libraryApi.reset(auth, decision.film)
                else libraryApi.decide(auth, decision.film, DecisionDto(status))
            } catch (e: HttpException) {
                // The film was removed or access to it was revoked.
                if (e.code() != 404 && e.code() != 403) throw e
                dao.commitDecision(decision.copy(status = null))
                account.catalogChanged()
                continue
            }
            dao.commitDecision(decision)
            account.catalogChanged()
        }
    }
}
