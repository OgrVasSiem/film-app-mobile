package com.example.film_app_mobile.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.data.repository.AuthRepository
import com.example.film_app_mobile.presentation.ScreenRequest
import com.example.film_app_mobile.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    val request = ScreenRequest(viewModelScope)
    var state by mutableStateOf(AuthState())
        private set

    fun login(value: String) {
        request.clearError()
        state = state.copy(login = value)
    }

    fun password(value: String) {
        request.clearError()
        state = state.copy(password = value)
    }

    fun submit(register: Boolean) =
        request.run { authRepository.signIn(state.login, state.password, register) }
}
