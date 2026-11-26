package com.example.film_app_mobile.feature.auth.state

import com.example.film_app_mobile.presentation.RequestStatus

data class AuthState(
    val request: RequestStatus = RequestStatus.Idle,
    val login: String = "",
    val password: String = "",
    val existingAccount: Boolean = false,
)
