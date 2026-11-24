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
import androidx.core.view.WindowCompat
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.deck.DeckCarousel
import com.example.film_app_mobile.feature.library.LibraryScreen
import com.example.film_app_mobile.feature.library.LibraryState
import com.example.film_app_mobile.navigation.MovieApp
import com.example.film_app_mobile.ui.model.Film
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun backFromLibraryReturnsToPacks() {
        val activity = compose.activity
        compose.runOnUiThread {
            activity.setContent {
                AppTheme { MovieApp(signedIn = true, dark = false, changeTheme = {}) }
            }
        }
        compose.onNodeWithText(activity.getString(R.string.library_title)).performClick()
        compose.onNodeWithText(activity.getString(R.string.watched)).assertExists()
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(activity.getString(R.string.packs_title)).assertIsDisplayed()
    }

    @Test
    fun themeSwitchChangesIconsAndPersistsChoice() {
        val activity = compose.activity
        val prefs =
            activity.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE)
        val original = prefs.all["dark"] as? Boolean
        try {
            prefs.edit().putBoolean("dark", false).commit()
            compose.runOnUiThread { activity.setContent { activity.AppContent(true) } }
            compose.onNodeWithText(activity.getString(R.string.profile_title)).performClick()
            val toggle = compose.onNodeWithText(activity.getString(R.string.dark_theme))
            toggle.assertIsOff().performClick()
            toggle.assertIsOn()
            compose.runOnIdle {
                org.junit.Assert.assertTrue(prefs.getBoolean("dark", false))
                org.junit.Assert.assertFalse(
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .isAppearanceLightStatusBars
                )
            }
            assertSystemBars(dark = true)
            toggle.performClick()
            toggle.assertIsOff()
            compose.runOnIdle {
                org.junit.Assert.assertTrue(
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .isAppearanceLightStatusBars
                )
            }
            assertSystemBars(dark = false)
            compose.onNodeWithText(activity.getString(R.string.logout)).assertExists()
        } finally {
            if (original == null) prefs.edit().remove("dark").commit()
            else prefs.edit().putBoolean("dark", original).commit()
        }
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
                        LibraryScreen(
                            LibraryState(cards = listOf(film)),
                            false,
                            null,
                            {},
                            {},
                            { _, _ -> },
                            {},
                            {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(film.title, useUnmergedTree = true).performTouchInput { click() }
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
        compose.runOnIdle {
            org.junit.Assert.assertEquals(listOf("WILL_WATCH", "WONT_WATCH"), choices)
        }
    }

    private fun assertSystemBars(dark: Boolean) {
        val activity = compose.activity
        compose.runOnIdle {
            val controller =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            org.junit.Assert.assertEquals(!dark, controller.isAppearanceLightNavigationBars)
        }
        val automation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        val top = android.graphics.Color.parseColor(if (dark) "#191617" else "#F4EFED")
        val bottom = android.graphics.Color.parseColor(if (dark) "#252021" else "#FBF7F4")
        compose.waitUntil(5000) {
            val bitmap = automation.takeScreenshot() ?: return@waitUntil false
            try {
                val x = bitmap.width / 3
                bitmap.getPixel(x, 8) == top && bitmap.getPixel(x, bitmap.height - 8) == bottom
            } finally {
                bitmap.recycle()
            }
        }
    }
}
