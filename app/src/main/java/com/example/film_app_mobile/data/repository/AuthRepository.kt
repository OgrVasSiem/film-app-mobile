package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.AuthApi
import com.example.film_app_mobile.core.network.dto.CredentialsDto
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import javax.inject.Inject

class AuthRepository
@Inject
constructor(
    private val account: AccountSession,
    private val authApi: AuthApi,
    private val sync: DecisionSync,
    private val sections: SectionRepository? = null,
) {
    suspend fun signIn(login: String, password: String) =
        account.operation {
            val credentials = CredentialsDto(login.trim(), password)
            val result = authApi.login(credentials)
            start(result)
            sync.scheduleSync()
        }

    suspend fun begin() =
        account.operation {
            if (!signedIn) {
                start(authApi.start(), newAccount = true)
                sync.scheduleSync()
            }
        }

    suspend fun logout() =
        account.operation {
            requireProtectedProfile()
            sections?.flush(owner(), auth)
            sync.flush(owner(), auth)
            authApi.logout(auth)
            endSession()
        }
}
