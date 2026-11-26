package com.example.film_app_mobile.feature.folder.state

import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.presentation.RequestStatus

data class FolderState(
    val contentType: ContentType? = null,
    val request: RequestStatus = RequestStatus.Idle,
    val detail: FilmDto? = null,
    val cards: List<FilmDto> = emptyList(),
    val cursor: String? = null,
    val undo: Long? = null,
)
