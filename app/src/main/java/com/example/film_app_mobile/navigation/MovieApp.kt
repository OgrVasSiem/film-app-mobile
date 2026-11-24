package com.example.film_app_mobile.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.auth.AuthRoute
import com.example.film_app_mobile.feature.deck.DeckRoute
import com.example.film_app_mobile.feature.library.LibraryRoute
import com.example.film_app_mobile.feature.packs.PacksRoute
import com.example.film_app_mobile.feature.profile.ProfileRoute

@Composable
internal fun MovieApp(signedIn: Boolean, dark: Boolean, changeTheme: (Boolean) -> Unit) =
    key(signedIn) {
        val stack = rememberNavBackStack(if (signedIn) Page.Packs else Page.Auth)
        val current = stack.last()
        fun show(page: Page) {
            if (stack.last() == page) return
            if (stack.size > 1) stack.removeAt(stack.lastIndex)
            if (page != Page.Packs) stack.add(page)
        }

        Column(
            Modifier.fillMaxSize()
                .background(AppTheme.colors.background)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                .imePadding()
        ) {
            NavDisplay(
                modifier =
                    Modifier.weight(1f)
                        .then(if (signedIn) Modifier else Modifier.navigationBarsPadding()),
                backStack = stack,
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                entryProvider =
                    entryProvider {
                        entry<Page.Auth> { AuthRoute() }
                        entry<Page.Packs> {
                            PacksRoute(hiltViewModel()) { pack -> show(Page.Deck(pack)) }
                        }
                        entry<Page.Profile> { ProfileRoute(dark, changeTheme) }
                        entry<Page.Library> { LibraryRoute(packs = { show(Page.Packs) }) }
                        entry<Page.Deck> {
                            DeckRoute(
                                it.pack,
                                library = { show(Page.Library) },
                                packs = { show(Page.Packs) },
                            )
                        }
                    },
            )
            if (signedIn) BottomAppBar(current, ::show)
        }
    }
