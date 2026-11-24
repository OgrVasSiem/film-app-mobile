package com.example.film_app_mobile.data.session

import android.content.Context
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.network.api.ProfileApi
import com.example.film_app_mobile.core.network.dto.SessionDto
import com.example.film_app_mobile.core.storage.dao.ProfileDao
import com.example.film_app_mobile.data.error.RequestFailure
import com.example.film_app_mobile.data.mapper.entity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

enum class SessionMode {
    Guest,
    Server,
}

@Singleton
class AccountSession
@Inject
constructor(
    @ApplicationContext context: Context,
    private val profileApi: ProfileApi,
    private val dao: ProfileDao,
) {
    private val prefs = context.getSharedPreferences("film_session", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val token
        get() = prefs.getString("token", "").orEmpty()

    private val changes = MutableStateFlow(0L)
    internal val catalogChanges = changes.asStateFlow()

    internal fun catalogChanged() {
        changes.value++
    }

    internal val auth
        get() = "Bearer " + token

    private val mode =
        MutableStateFlow(if (token.isEmpty()) SessionMode.Guest else SessionMode.Server)
    val session = mode.asStateFlow()

    internal suspend fun <T> operation(block: suspend AccountSession.() -> T): T {
        val expectedToken = token
        return mutex.withLock {
            if (token != expectedToken)
                throw kotlinx.coroutines.CancellationException("Session changed")
            try {
                block()
            } catch (e: HttpException) {
                if (e.code() == 401) endSession()
                throw RequestFailure(
                    when (e.code()) {
                        401 -> R.string.invalid_credentials
                        403 -> R.string.premium_required
                        409 -> R.string.login_taken
                        400 -> R.string.invalid_input
                        else -> R.string.server_error
                    },
                    e,
                )
            } catch (e: IOException) {
                throw RequestFailure(R.string.connection_error, e)
            }
        }
    }

    // Existing installations have a token but no locally stored account ID.
    internal suspend fun owner(): Long {
        val id = prefs.getLong("account_id", -1)
        if (id >= 0) return id
        val profile = profileApi.profile(auth)
        dao.saveProfile(profile.entity())
        prefs.edit().putLong("account_id", profile.id).apply()
        return profile.id
    }

    internal val signedIn
        get() = token.isNotEmpty()

    internal suspend fun start(result: SessionDto) {
        dao.switchAccount(result.account.entity())
        prefs
            .edit()
            .putString("token", result.token)
            .putLong("account_id", result.account.id)
            .apply()
        mode.value = SessionMode.Server
    }

    internal fun endSession() {
        prefs.edit().remove("token").remove("account_id").apply()
        mode.value = SessionMode.Guest
    }
}
