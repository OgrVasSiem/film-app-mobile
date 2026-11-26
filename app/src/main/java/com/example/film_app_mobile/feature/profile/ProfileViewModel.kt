package com.example.film_app_mobile.feature.profile

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.dataStore.AppThemeMode
import com.example.film_app_mobile.core.dataStore.ThemeDataStore
import com.example.film_app_mobile.data.repository.AuthRepository
import com.example.film_app_mobile.data.repository.ProfileRepository
import com.example.film_app_mobile.feature.profile.state.ProfileDialog
import com.example.film_app_mobile.feature.profile.state.ProfileState
import com.example.film_app_mobile.presentation.ScreenRequest
import com.example.film_app_mobile.ui.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val profiles: ProfileRepository,
    private val auth: AuthRepository,
    private val themeStore: ThemeDataStore,
) : ViewModel() {
    private val _viewState = MutableStateFlow(ProfileState())
    val viewState: StateFlow<ProfileState> = _viewState.asStateFlow()
    val request =
        ScreenRequest(viewModelScope) { status -> _viewState.update { it.copy(request = status) } }

    init {
        viewModelScope.launch {
            themeStore.themeFlow.collect { mode -> _viewState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            profiles.premium.collect { premium ->
                _viewState.update { it.copy(hasPremium = premium) }
            }
        }
        viewModelScope.launch {
            profiles.changes.collect { profile -> _viewState.update { it.copy(profile = profile) } }
        }
    }

    fun setTheme(dark: Boolean) {
        viewModelScope.launch {
            themeStore.saveTheme(if (dark) AppThemeMode.DARK else AppThemeMode.LIGHT)
        }
    }

    fun showDialog(dialog: ProfileDialog) {
        if (dialog == ProfileDialog.Rename) editLogin(profile?.login.orEmpty())
        _viewState.update { it.copy(dialog = dialog) }
    }

    fun closeDialog() {
        if (!request.status.busy) {
            request.clearError()
            dismissDialog()
        }
    }

    private fun dismissDialog() {
        _viewState.update { it.copy(dialog = null) }
    }

    private var profile: Profile?
        get() = viewState.value.profile
        set(value) {
            _viewState.update { it.copy(profile = value) }
        }

    private var login: String
        get() = viewState.value.login
        set(value) {
            _viewState.update { it.copy(login = value) }
        }

    private var availability: Int?
        get() = viewState.value.availability
        set(value) {
            _viewState.update { it.copy(availability = value) }
        }

    private var checkJob: kotlinx.coroutines.Job? = null

    fun editLogin(value: String) {
        login = value
        checkJob?.cancel()
        availability = null
        if (!viewState.value.validLogin) return
        val candidate = viewState.value.normalizedLogin
        checkJob =
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                availability =
                    try {
                        if (profiles.available(candidate)) R.string.login_free
                        else R.string.login_taken
                    } catch (e: com.example.film_app_mobile.data.error.RequestFailure) {
                        e.messageId
                    }
            }
    }

    fun rename() =
        request.run {
            profile = profiles.rename(viewState.value.normalizedLogin)
            editLogin("")
            dismissDialog()
        }

    fun protect(password: String) =
        request.run {
            profile = profiles.protect(password)
            if (profile?.passwordProtected == true) {
                dismissDialog()
            }
        }

    fun delete() = request.run { profiles.delete() }

    private var opened = false

    fun open() {
        if (opened) return
        opened = true
        load()
    }

    fun load() = request.run { profile = profiles.fresh() }

    fun logout() = request.run { auth.logout() }
}
