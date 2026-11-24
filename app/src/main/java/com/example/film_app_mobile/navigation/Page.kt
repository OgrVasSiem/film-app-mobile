package com.example.film_app_mobile.navigation

import androidx.navigation3.runtime.NavKey
import com.example.film_app_mobile.ui.model.Pack
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Page : NavKey {
    @Serializable data object Auth : Page

    @Serializable data object Packs : Page

    @Serializable data object Profile : Page

    @Serializable data object Library : Page

    @Serializable data class Deck(val pack: Pack) : Page
}
