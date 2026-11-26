@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CatalogDto(
    val packs: List<PackDto>,
    val covers: Map<String, CoverDto>,
    val recommendations: RecommendationSummary? = null,
    val sections: List<CatalogSection> = emptyList(),
    val contentType: String? = null,
)

@Serializable data class CoverDto(val mimeType: String, val sha256: String, val base64: String)

@Serializable
data class FilmPageDto(
    val items: List<FilmDto>,
    val nextCursor: String?,
    val state: String? = null,
)

@Serializable data class RecommendationSummary(val deckState: String, val availableCount: Long)

@Serializable data class FilmPick(val film: FilmDto?, val source: String)

@Serializable data class Overview(val sections: Map<String, Section>)

@Serializable data class Section(val totalCount: Long, val items: List<FilmDto>)

@Serializable
data class CatalogSection(
    val key: String,
    val state: String,
    val availableCount: Long,
    val nextRefreshAt: String? = null,
    val periodStart: String? = null,
)
