package com.example.film_app_mobile.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.data.repository.AuthRepository
import com.example.film_app_mobile.feature.auth.state.AuthState
import com.example.film_app_mobile.presentation.ScreenRequest
import com.example.film_app_mobile.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val _viewState = MutableStateFlow(AuthState())
    val viewState: StateFlow<AuthState> = _viewState.asStateFlow()
    val request =
        ScreenRequest(viewModelScope) { status -> _viewState.update { it.copy(request = status) } }

    fun showLogin(visible: Boolean) {
        request.clearError()
        _viewState.update {
            it.copy(existingAccount = visible, password = if (visible) it.password else "")
        }
    }

    fun login(value: String) {
        request.clearError()
        _viewState.value = _viewState.value.copy(login = value)
    }

    fun password(value: String) {
        request.clearError()
        _viewState.value = _viewState.value.copy(password = value)
    }

    fun begin() = request.run { authRepository.begin() }

    fun submit() =
        request.run { authRepository.signIn(viewState.value.login, viewState.value.password) }
}
