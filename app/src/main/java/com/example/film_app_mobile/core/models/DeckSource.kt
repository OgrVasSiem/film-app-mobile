package com.example.film_app_mobile.core.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface DeckSource {
    @Serializable data object Personal : DeckSource

    @Serializable data class Periodic(val period: Period) : DeckSource

    @Serializable data class Editorial(val packId: Long) : DeckSource
}

@Serializable
enum class Period {
    DAY,
    WEEK,
    MONTH,
}
