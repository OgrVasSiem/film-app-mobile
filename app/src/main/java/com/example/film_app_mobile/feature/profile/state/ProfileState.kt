package com.example.film_app_mobile.feature.profile.state

import com.example.film_app_mobile.core.dataStore.AppThemeMode
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.presentation.RequestStatus
import com.example.film_app_mobile.ui.model.Profile

data class ProfileState(
    val dialog: ProfileDialog? = null,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val request: RequestStatus = RequestStatus.Idle,
    val hasPremium: Boolean? = null,
    val profile: Profile? = null,
    val login: String = "",
    val availability: Int? = null,
) {
    val normalizedLogin
        get() = login.trim().lowercase(java.util.Locale.ROOT)

    val validLogin
        get() = normalizedLogin.matches(loginPattern) && normalizedLogin != profile?.login

    private companion object {
        val loginPattern = Regex("[a-z0-9._-]{3,32}")
    }
}

enum class ProfileDialog {
    Rename,
    Password,
    Delete,
}
