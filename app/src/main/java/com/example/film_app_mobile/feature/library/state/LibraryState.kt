package com.example.film_app_mobile.feature.library.state

import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.presentation.RequestStatus
import com.example.film_app_mobile.ui.model.Film

data class LibraryState(
    val contentType: ContentType? = null,
    val request: RequestStatus = RequestStatus.Idle,
    val sections: Map<String, List<Film>> = emptyMap(),
    val detail: FilmDto? = null,
    val archiveCount: Long = 0,
)
