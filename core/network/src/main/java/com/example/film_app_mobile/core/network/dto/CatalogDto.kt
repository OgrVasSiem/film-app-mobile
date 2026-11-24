@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CatalogDto(val packs: List<PackDto>, val covers: Map<String, CoverDto>)

@Serializable
data class CoverDto(val mimeType: String, val sha256: String, val base64: String)

@Serializable
data class FilmPageDto(val items: List<FilmDto>, val nextCursor: String?)
