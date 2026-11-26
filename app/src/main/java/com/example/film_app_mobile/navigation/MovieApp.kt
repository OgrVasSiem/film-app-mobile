package com.example.film_app_mobile.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.auth.AuthRoute
import com.example.film_app_mobile.feature.deck.DeckRoute
import com.example.film_app_mobile.feature.folder.FolderRoute
import com.example.film_app_mobile.feature.home.HomeScreen
import com.example.film_app_mobile.feature.library.LibraryRoute
import com.example.film_app_mobile.feature.profile.ProfileRoute

@Composable
internal fun MovieApp(signedIn: Boolean) =
    key(signedIn) {
        val stack = rememberNavBackStack(if (signedIn) Page.Home else Page.Auth)
        val current = stack.last()
        fun show(page: Page) {
            if (stack.last() == page) return
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            if (page != Page.Home) stack.add(page)
        }

        Column(
            Modifier.fillMaxSize()
                .background(AppTheme.colors.surface)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        if (current is Page.Deck) WindowInsetsSides.Horizontal
                        else WindowInsetsSides.Horizontal + WindowInsetsSides.Top
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
                        entry<Page.Home> {
                            HomeScreen(open = { deck -> stack.add(Page.Deck(deck)) })
                        }
                        entry<Page.Library> {
                            LibraryRoute(open = { folder -> stack.add(Page.Folder(folder)) })
                        }
                        entry<Page.Deck> { DeckRoute(it.deck, { stack.removeAt(stack.lastIndex) }) }
                        entry<Page.Folder> {
                            FolderRoute(it.folder, { stack.removeAt(stack.lastIndex) })
                        }
                        entry<Page.Profile> { ProfileRoute() }
                    },
            )
            if (signedIn && current !is Page.Deck) BottomAppBar(current, ::show)
        }
    }
