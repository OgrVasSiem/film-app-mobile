package com.example.film_app_mobile.feature.folder.state

import com.example.film_app_mobile.core.network.dto.*

data class FolderState(
    val detail: FilmDto? = null,
    val cards: List<FilmDto> = emptyList(),
    val cursor: String? = null,
    val undo: Long? = null,
)
