package com.example.film_app_mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.core.dataStore.AppThemeMode
import com.example.film_app_mobile.core.dataStore.ThemeDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class MainViewModel
@Inject
constructor(
    private val account: com.example.film_app_mobile.data.session.AccountSession,
    private val profiles: com.example.film_app_mobile.data.repository.ProfileRepository,
    connection: com.example.film_app_mobile.data.session.InternetConnection,
    private val themeStore: ThemeDataStore,
) : ViewModel() {
    val internetAvailable =
        connection.available.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val themeMode: StateFlow<AppThemeMode> =
        themeStore.themeFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeMode.DARK,
        )

    private var restoring: kotlinx.coroutines.Job? = null

    fun restore() {
        if (restoring?.isActive != true) restoring = viewModelScope.launch { account.restore() }
    }

    suspend fun observeAccountWhileVisible() {
        internetAvailable.collectLatest { connected ->
            if (connected != true) return@collectLatest
            account.restore()
            account.session.collectLatest { mode ->
                if (mode != com.example.film_app_mobile.data.session.SessionMode.Server)
                    return@collectLatest
                while (true) {
                    try {
                        profiles.fresh()
                    } catch (_: com.example.film_app_mobile.data.error.RequestFailure) {
                        // Keep the last confirmed entitlement during a temporary outage.
                    }
                    kotlinx.coroutines.delay(60_000)
                }
            }
        }
    }
}
