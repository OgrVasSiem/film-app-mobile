package com.example.film_app_mobile.feature.deck.state

import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.presentation.RequestStatus

data class DeckState(
    val contentType: ContentType? = null,
    val request: RequestStatus = RequestStatus.Idle,
    val horizontalProgress: Float = 0f,
    val downwardProgress: Float = 0f,
    val detail: FilmDto? = null,
    val cards: List<FilmDto> = emptyList(),
    val cursor: String? = null,
    val sessionDecisions: Int = 0,
    val undo: Long? = null,
    val catalog: CatalogDto? = null,
    val deckState: String? = null,
)
