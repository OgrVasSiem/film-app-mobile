package com.example.film_app_mobile.feature.home.state

import com.example.film_app_mobile.core.network.dto.*

data class HomeState(
    val hasPremium: Boolean? = null,
    val showPremiumOffer: Boolean = false,
    val periodsExpanded: Boolean = false,
    val detail: FilmDto? = null,
    val streak: Int? = null,
    val catalog: CachedCatalog? = null,
    val single: FilmDto? = null,
    val undo: Long? = null,
)
