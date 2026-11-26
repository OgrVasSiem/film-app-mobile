package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.ProfileApi
import com.example.film_app_mobile.core.storage.dao.ProfileDao
import com.example.film_app_mobile.data.cache.refresh
import com.example.film_app_mobile.data.mapper.entity
import com.example.film_app_mobile.data.mapper.ui
import com.example.film_app_mobile.data.session.AccountSession
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class ProfileRepository
@Inject
constructor(
    private val account: AccountSession,
    private val profileApi: ProfileApi,
    private val dao: ProfileDao,
) {
    val premium
        get() = account.premium

    val changes = account.profileChanges.map { it?.ui() }

    suspend fun fresh() = account.operation { save(profileApi.profile(auth)) }

    private suspend fun save(
        dto: com.example.film_app_mobile.core.network.dto.ProfileDto
    ): com.example.film_app_mobile.ui.model.Profile {
        account.saveProfile(dto)
        return dto.entity().ui()
    }

    suspend fun protect(password: String) =
        account.operation {
            val dto =
                try {
                    profileApi.protect(auth, mapOf("password" to password))
                } catch (e: retrofit2.HttpException) {
                    if (e.code() != 409) throw e
                    profileApi.profile(auth)
                }
            save(dto)
        }

    suspend fun rename(login: String) =
        account.operation { save(profileApi.rename(auth, mapOf("login" to login))) }

    suspend fun available(login: String) =
        account.operation { profileApi.availability(auth, login).available }

    suspend fun delete() =
        account.operation {
            val id = owner()
            profileApi.delete(auth, mapOf("confirmed" to true))
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                dao.clearAccount(id)
                endSession()
            }
        }

    suspend fun cached() = dao.profile(account.cachedOwner)?.ui()

    suspend fun profile() =
        account.operation {
            val id = owner()

            refresh({ dao.profile(id) != null }) { saveProfile(profileApi.profile(auth)) }
            checkNotNull(dao.profile(id)).ui()
        }
}
