package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.AuthApi
import com.example.film_app_mobile.core.network.dto.CredentialsDto
import com.example.film_app_mobile.data.session.AccountSession
import javax.inject.Inject

class AuthRepository
@Inject
constructor(private val account: AccountSession, private val authApi: AuthApi) {
    suspend fun signIn(login: String, password: String) =
        account.operation {
            val credentials = CredentialsDto(login.trim(), password)
            val result = authApi.login(credentials)
            start(result)
        }

    suspend fun begin() =
        account.operation {
            if (!signedIn) {
                start(authApi.start())
            }
        }

    suspend fun logout() =
        account.operation {
            requireProtectedProfile()

            authApi.logout(auth)
            endSession()
        }
}
