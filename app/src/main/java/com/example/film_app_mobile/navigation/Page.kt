package com.example.film_app_mobile.navigation

import androidx.navigation3.runtime.NavKey
import com.example.film_app_mobile.core.models.*
import kotlinx.serialization.Serializable

@Serializable
sealed interface Page : NavKey {
    @Serializable data object Auth : Page

    @Serializable data object Home : Page

    @Serializable data object Profile : Page

    @Serializable data object Library : Page

    @Serializable data class Folder(val folder: LibraryFolder) : Page

    @Serializable data class Deck(val deck: DeckSource) : Page
}
