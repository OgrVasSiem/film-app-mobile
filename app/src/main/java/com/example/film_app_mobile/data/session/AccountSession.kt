package com.example.film_app_mobile.data.session

import android.content.Context
import androidx.core.content.edit
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.dataStore.PremiumDataStore
import com.example.film_app_mobile.core.network.api.ProfileApi
import com.example.film_app_mobile.core.network.dto.ProfileDto
import com.example.film_app_mobile.core.network.dto.SessionDto
import com.example.film_app_mobile.core.storage.dao.ProfileDao
import com.example.film_app_mobile.data.error.RequestFailure
import com.example.film_app_mobile.data.error.temporary
import com.example.film_app_mobile.data.mapper.entity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

enum class SessionMode {
    Guest,
    Restoring,
    Retry,
    Server,
}

@Singleton
class AccountSession
@Inject
constructor(
    @ApplicationContext context: Context,
    private val profileApi: ProfileApi,
    private val dao: ProfileDao,
    @Named("serverNamespace") private val namespace: String = "",
    private val premiumStore: PremiumDataStore = PremiumDataStore(context),
    private val scheduler: com.example.film_app_mobile.data.sync.SyncScheduler =
        com.example.film_app_mobile.data.sync.SyncScheduler {},
) {
    private val prefs = context.getSharedPreferences("film_session$namespace", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    @Volatile
    private var token: String =
        prefs.getString("encrypted", null)?.let(TokenCipher::decrypt)
            ?: prefs
                .getString("token", null)
                ?.also {
                    check(
                        prefs
                            .edit()
                            .putString("encrypted", TokenCipher.encrypt(it))
                            .remove("token")
                            .commit()
                    )
                }
                .orEmpty()

    private val activeOwner = MutableStateFlow(cachedOwner.takeIf { signedIn && it >= 0 })

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val premium =
        activeOwner.flatMapLatest { id ->
            if (id == null) flowOf(null) else premiumStore.observe(namespace, id)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    internal val profileChanges =
        activeOwner.flatMapLatest { id -> if (id == null) flowOf(null) else dao.observe(id) }

    internal suspend fun saveProfile(profile: ProfileDto) {
        val changed = dao.profile(profile.id)?.premium != profile.premium
        dao.saveProfile(profile.entity())
        premiumStore.save(namespace, profile.id, profile.premium)
        if (changed) {
            dao.invalidateCatalog(profile.id)
            dao.invalidateSectionSnapshots(profile.id)
            scheduler.enqueue()
            catalogChanged()
        }
    }

    private val changes = MutableStateFlow(0L)
    internal val catalogChanges = changes.asStateFlow()

    internal fun catalogChanged() {
        changes.value++
    }

    internal val auth
        get() = "Bearer $token"

    private val mode =
        MutableStateFlow(if (token.isEmpty()) SessionMode.Guest else SessionMode.Restoring)
    val session = mode.asStateFlow()

    suspend fun restore() {
        if (mode.value !in listOf(SessionMode.Restoring, SessionMode.Retry)) return
        mode.value = SessionMode.Restoring
        try {
            operation {
                val profile = profileApi.profile(auth)
                saveProfile(profile)
                check(prefs.edit().putLong("account_id", profile.id).commit())
                activeOwner.value = profile.id
                mode.value = SessionMode.Server
            }
        } catch (e: RequestFailure) {
            if (signedIn && e.temporary)
                dao.profile(cachedOwner)?.let { premiumStore.save(namespace, it.id, it.premium) }
            if (signedIn)
                mode.value =
                    if (cachedOwner >= 0 && dao.profile(cachedOwner) != null && e.temporary)
                        SessionMode.Server
                    else SessionMode.Retry
        }
    }

    internal suspend fun requireProtectedProfile() {
        if (dao.profile(owner())?.passwordProtected != true)
            throw RequestFailure(R.string.logout_requires_password)
    }

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
            } catch (e: RequestFailure) {
                throw e
            } catch (e: IOException) {
                throw RequestFailure(R.string.connection_error, e)
            }
        }
    }

    // Existing installations have a token but no locally stored account ID.
    internal val cachedOwner
        get() = prefs.getLong("account_id", -1)

    internal suspend fun owner(): Long {
        val id = prefs.getLong("account_id", -1)
        if (id >= 0) return id
        val profile = profileApi.profile(auth)
        saveProfile(profile)
        activeOwner.value = profile.id
        prefs.edit { putLong("account_id", profile.id) }
        return profile.id
    }

    internal val signedIn
        get() = token.isNotEmpty()

    internal suspend fun start(result: SessionDto, newAccount: Boolean = false) {
        if (dao.profile(result.account.id)?.premium != result.account.premium) {
            dao.invalidateSectionSnapshots(result.account.id)
        }
        dao.switchAccount(result.account.entity(), newAccount)
        check(
            prefs
                .edit()
                .putString("encrypted", TokenCipher.encrypt(result.token))
                .putLong("account_id", result.account.id)
                .remove("token")
                .commit()
        )
        premiumStore.save(namespace, result.account.id, result.account.premium)
        activeOwner.value = result.account.id
        token = result.token
        mode.value = SessionMode.Server
    }

    internal suspend fun endSession() {
        val id = cachedOwner
        activeOwner.value = null
        check(prefs.edit().remove("encrypted").remove("token").remove("account_id").commit())
        token = ""
        mode.value = SessionMode.Guest
        if (id >= 0) premiumStore.clear(namespace, id)
    }
}
