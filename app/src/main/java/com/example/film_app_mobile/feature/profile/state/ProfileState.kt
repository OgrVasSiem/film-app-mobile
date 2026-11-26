package com.example.film_app_mobile.feature.profile.state

import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.ui.model.Profile

data class ProfileState(
    val hasPremium: Boolean? = null,
    val profile: Profile? = null,
    val login: String = "",
    val availability: Int? = null,
)
