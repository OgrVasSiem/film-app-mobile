package com.example.film_app_mobile.ui.model

data class Profile(
    val id: Long,
    val login: String,
    val premium: Boolean,
    val adsEnabled: Boolean,
    val passwordProtected: Boolean,
    val streakDays: Int,
)
