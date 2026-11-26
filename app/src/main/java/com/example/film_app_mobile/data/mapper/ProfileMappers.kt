package com.example.film_app_mobile.data.mapper

import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.ui.model.*

internal fun ProfileDto.ui() =
    Profile(id, login, premium, adsEnabled, passwordProtected, streakDays)
