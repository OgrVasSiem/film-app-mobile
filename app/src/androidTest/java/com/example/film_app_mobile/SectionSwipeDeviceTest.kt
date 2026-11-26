package com.example.film_app_mobile

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.*
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.storage.FilmDatabase
import com.example.film_app_mobile.data.image.CoverFiles
import com.example.film_app_mobile.data.repository.*
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.feature.deck.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class SectionSwipeDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun swipeUndoAndCrossDeckFiltering() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveCatalog") == "true")
        val app = compose.activity
        val stack = NetworkStack(NetworkConfig(BuildConfig.API_BASE_URL, true))
        val db = Room.inMemoryDatabaseBuilder(app, FilmDatabase::class.java).build()
        val api = stack.retrofit.create(ProfileApi::class.java)
        val account = AccountSession(app, api, db.profile(), UUID.randomUUID().toString())
        val temporary = runBlocking { stack.retrofit.create(AuthApi::class.java).start() }
        try {
            runBlocking { account.start(temporary) }
            val repo = SectionRepository(account, stack, db.library(), CoverFiles(app))
            val vm = DeckViewModel(repo, ProfileRepository(account, api, db.profile()))
            val deck = runBlocking {
                val pack =
                    repo.catalog(ContentType.FILM).catalog.packs.first {
                        !it.locked && it.availableCount > 0
                    }
                DeckSource.Editorial(pack.id)
            }
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent { AppTheme { DeckScreen(ContentType.FILM, deck, {}, vm) } }
            }
            compose.waitUntil(15000) {
                vm.viewState.value.cards.isNotEmpty() && !vm.request.status.busy
            }
            val id = vm.viewState.value.cards.first().id
            compose.onNodeWithTag("v2-deck").performTouchInput { swipeRight() }
            compose.waitUntil(15000) { vm.viewState.value.undo == id && !vm.request.status.busy }
            runBlocking {
                assertTrue(
                    repo.overview(ContentType.FILM).sections.getValue("WILL_WATCH").items.any {
                        it.id == id
                    }
                )
                val catalog = repo.catalog(ContentType.FILM)
                for (section in
                    catalog.catalog.sections.filter {
                        it.key in listOf("DAY", "WEEK", "MONTH") && it.state == "READY"
                    }) {
                    var page = repo.page(ContentType.FILM, "decks/" + section.key, null, null)
                    while (page.nextCursor != null) page =
                        repo.page(ContentType.FILM, "decks/" + section.key, null, page.nextCursor)
                    assertFalse(page.items.any { it.id == id })
                }
            }
            compose.onNodeWithText(app.getString(R.string.undo_choice)).performClick()
            compose.waitUntil(15000) {
                vm.viewState.value.undo == null && vm.viewState.value.cards.any { it.id == id }
            }
            compose.waitUntil(15000) { !vm.request.status.busy }
            val watched = vm.viewState.value.cards.first().id
            compose.onNodeWithTag("v2-deck").performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(12.dp.toPx(), 2.dp.toPx()))
                repeat(8) { moveBy(androidx.compose.ui.geometry.Offset(5.dp.toPx(), 6.dp.toPx())) }
                up()
            }
            compose.waitUntil(15000) {
                vm.viewState.value.undo == watched && !vm.request.status.busy
            }
            assertTrue(
                runBlocking {
                    repo.overview(ContentType.FILM).sections.getValue("WATCHED").items.any {
                        it.id == watched
                    }
                }
            )
        } finally {
            runBlocking { api.delete("Bearer " + temporary.token, mapOf("confirmed" to true)) }
            runBlocking { account.endSession() }
            db.close()
        }
    }
}
