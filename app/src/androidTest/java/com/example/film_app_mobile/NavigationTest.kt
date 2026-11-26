package com.example.film_app_mobile

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.deck.components.DeckCarousel
import com.example.film_app_mobile.feature.library.components.LibraryPreview
import com.example.film_app_mobile.feature.library.state.LibraryState
import com.example.film_app_mobile.navigation.MovieApp
import com.example.film_app_mobile.ui.model.Film
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun backFromLibraryReturnsToPacks() {
        val activity = compose.activity
        compose.runOnUiThread { activity.setContent { AppTheme { MovieApp(signedIn = true) } } }
        compose.onNodeWithText(activity.getString(R.string.series_v2)).performClick()
        compose.onNodeWithText(activity.getString(R.string.library_title)).performClick()
        compose.onNodeWithText(activity.getString(R.string.series_v2)).assertIsSelected()
        compose.onNodeWithText(activity.getString(R.string.films_v2)).performClick()
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText(activity.getString(R.string.watched), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val seeAll = activity.getString(R.string.see_all)
        val tab = hasText(activity.getString(R.string.library_title)) and isSelectable()
        for ((index, title) in
            listOf(R.string.will_watch, R.string.watched, R.string.archive_title).withIndex()) {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(seeAll).fetchSemanticsNodes().size == 2
            }
            if (index < 2) {
                compose.onAllNodesWithText(seeAll)[index].performClick()
            } else {
                compose
                    .onNode(
                        hasContentDescription(
                            activity.getString(R.string.archive_n, 0).substringBefore("0"),
                            substring = true,
                        ) and hasClickAction()
                    )
                    .performClick()
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(seeAll).fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText(activity.getString(title)).assertIsDisplayed()
            compose.onNode(tab).assertIsSelected()
            compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(seeAll).fetchSemanticsNodes().size == 2
        }
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose
            .onAllNodesWithText(activity.getString(R.string.home_title))
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun libraryCardOpensDetailsAndCloses() {
        val activity = compose.activity
        val film =
            Film(
                42,
                "Fixture film",
                "Fixture synopsis",
                null,
                2020,
                "Director",
                listOf("Actor"),
                90,
                listOf("Drama"),
            )
        compose.runOnUiThread {
            activity.setContent {
                AppTheme {
                    Column {
                        LibraryPreview(
                            LibraryState(sections = mapOf("WILL_WATCH" to listOf(film))),
                            false,
                            null,
                            {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription(film.title, useUnmergedTree = true).performTouchInput {
            click()
        }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText(film.description).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(film.description).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.director, "Director")).assertExists()
        compose.onNodeWithText(activity.getString(R.string.close_sheet)).performClick()
        compose.onNodeWithText(film.description).assertDoesNotExist()
    }

    @Test
    fun carouselCancelsShortDragAndRestoresCardAfterRejectedSwipe() {
        val film = Film(1, "First", "", null, null, null, emptyList(), null, emptyList())
        val choices = mutableListOf<String>()
        val activity = compose.activity
        compose.runOnUiThread {
            activity.setContent {
                AppTheme {
                    Column {
                        DeckCarousel(
                            listOf(film, film.copy(id = 2, title = "Second")),
                            false,
                            Modifier.weight(1f).fillMaxWidth().testTag("carousel"),
                        ) { _, decision, reject ->
                            choices.add(decision)
                            reject()
                        }
                    }
                }
            }
        }
        val carousel = compose.onNodeWithTag("carousel")
        carousel.performTouchInput { swipe(center, center.copy(x = center.x + width * .12f), 300) }
        compose.runOnIdle { org.junit.Assert.assertTrue(choices.isEmpty()) }
        carousel.performTouchInput { swipeRight() }
        carousel.performTouchInput { swipeLeft() }
        carousel.performTouchInput { swipeDown() }
        compose.runOnIdle {
            org.junit.Assert.assertEquals(listOf("WILL_WATCH", "WONT_WATCH", "WATCHED"), choices)
        }
    }
}
