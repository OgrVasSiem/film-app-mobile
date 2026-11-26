package com.example.film_app_mobile.feature.library.state

import com.example.film_app_mobile.core.network.dto.*

data class LibraryState(
    val sections: Map<String, List<com.example.film_app_mobile.ui.model.Film>> = emptyMap(),
    val detail: FilmDto? = null,
    val archiveCount: Long = 0,
)
