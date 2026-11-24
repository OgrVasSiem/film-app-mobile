package com.example.film_app_mobile.feature.library

import com.example.film_app_mobile.ui.model.Film

data class LibraryState(
    val cards: List<Film> = emptyList(),
    val folder: String = "WILL_WATCH",
    val moving: Film? = null,
)
