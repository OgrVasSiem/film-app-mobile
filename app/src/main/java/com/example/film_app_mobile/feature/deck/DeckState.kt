package com.example.film_app_mobile.feature.deck

import com.example.film_app_mobile.ui.model.Film

data class DeckState(
    val cards: List<Film> = emptyList(),
    val total: Int = 0,
    val saved: Int = 0,
    val skipped: Int = 0,
    val loaded: Boolean = false,
)
