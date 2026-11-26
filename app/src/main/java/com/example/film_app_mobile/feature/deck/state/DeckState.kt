package com.example.film_app_mobile.feature.deck.state

import com.example.film_app_mobile.core.network.dto.*

data class DeckState(
    val detail: FilmDto? = null,
    val cards: List<FilmDto> = emptyList(),
    val cursor: String? = null,
    val sessionDecisions: Int = 0,
    val undo: Long? = null,
    val catalog: CachedCatalog? = null,
    val deckState: String? = null,
)
