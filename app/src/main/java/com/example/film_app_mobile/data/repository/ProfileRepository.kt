package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.ProfileApi
import com.example.film_app_mobile.data.mapper.ui
import com.example.film_app_mobile.data.session.AccountSession
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class ProfileRepository
@Inject
constructor(private val account: AccountSession, private val profileApi: ProfileApi) {
    val premium
        get() = account.premium

    val changes = account.profileChanges.map { it?.ui() }

    suspend fun fresh() = account.operation { save(profileApi.profile(auth)) }

    private suspend fun save(
        dto: com.example.film_app_mobile.core.network.dto.ProfileDto
    ): com.example.film_app_mobile.ui.model.Profile {
        account.saveProfile(dto)
        return dto.ui()
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
            profileApi.delete(auth, mapOf("confirmed" to true))
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { endSession() }
        }
}
