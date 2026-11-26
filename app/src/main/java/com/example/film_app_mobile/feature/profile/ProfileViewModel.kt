package com.example.film_app_mobile.feature.profile

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.R
import com.example.film_app_mobile.data.repository.AuthRepository
import com.example.film_app_mobile.data.repository.ProfileRepository
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
constructor(private val profiles: ProfileRepository, private val auth: AuthRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    private val _viewState = MutableStateFlow(ProfileState())
    val viewState: StateFlow<ProfileState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.premium.collect { premium ->
                _viewState.update { it.copy(hasPremium = premium) }
            }
        }
        viewModelScope.launch {
            profiles.changes.collect { profile -> _viewState.update { it.copy(profile = profile) } }
        }
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
    val normalized
        get() = login.trim().lowercase(java.util.Locale.ROOT)

    val validLogin
        get() = normalized.matches(Regex("[a-z0-9._-]{3,32}")) && normalized != profile?.login

    fun editLogin(value: String) {
        login = value
        checkJob?.cancel()
        availability = null
        if (!validLogin) return
        val candidate = normalized
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

    fun rename(done: () -> Unit) =
        request.run {
            profile = profiles.rename(normalized)
            editLogin("")
            done()
        }

    fun protect(password: String, done: () -> Unit) =
        request.run {
            profile = profiles.protect(password)
            if (profile?.passwordProtected == true) {
                done()
            }
        }

    fun checkPurchase(done: (Boolean) -> Unit) =
        request.run {
            val current = profiles.fresh()
            profile = current
            done(current.passwordProtected)
        }

    fun delete() = request.run { profiles.delete() }

    private var opened = false

    fun open() {
        if (opened) return
        opened = true
        load()
    }

    fun load() =
        request.run {
            if (profile == null) profile = profiles.cached()
            profile = profiles.profile()
        }

    fun logout() = request.run { auth.logout() }
}
