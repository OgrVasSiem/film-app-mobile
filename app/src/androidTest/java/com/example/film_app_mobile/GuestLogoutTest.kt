package com.example.film_app_mobile

import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.network.*
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.FilmDatabase
import com.example.film_app_mobile.data.repository.*
import com.example.film_app_mobile.data.session.*
import com.example.film_app_mobile.data.sync.DecisionSync
import com.example.film_app_mobile.feature.auth.*
import com.example.film_app_mobile.feature.profile.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.*
import org.junit.Assert.*

class GuestLogoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun unprotectedProfileCanOnlyBeDeleted() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val ns = UUID.randomUUID().toString()
        val db = Room.inMemoryDatabaseBuilder(app, FilmDatabase::class.java).build()
        try {
            MockWebServer().use { server ->
                server.start()
                val network =
                    NetworkStack(NetworkConfig(server.url("/api/v1/").toString(), true)).retrofit
                val api = network.create(ProfileApi::class.java)
                val session = AccountSession(app, api, db.profile(), ns)
                val sync =
                    DecisionSync(session, network.create(LibraryApi::class.java), db.library()) {}
                val auth = AuthRepository(session, network.create(AuthApi::class.java), sync)
                val profile = ProfileViewModel(ProfileRepository(session, api, db.profile()), auth)
                val authVm = AuthViewModel(auth)
                runBlocking {
                    session.start(
                        SessionDto(
                            UUID.randomUUID().toString(),
                            ProfileDto(1, "demo", false, true, false, 0),
                        )
                    )
                }
                server.enqueue(
                    MockResponse.Builder()
                        .body(
                            """{"id":1,"login":"demo","premium":false,"adsEnabled":true,"passwordProtected":false,"streakDays":0}"""
                        )
                        .build()
                )
                compose.activityRule.scenario.onActivity { activity ->
                    activity.setContent {
                        AppTheme {
                            if (session.session.collectAsState().value == SessionMode.Guest)
                                AuthRoute(authVm)
                            else ProfileRoute(profile)
                        }
                    }
                }
                fun click(id: Int) =
                    compose.onAllNodesWithText(app.getString(id)).onLast().performClick()
                compose.waitUntil(10000) { profile.viewState.value.profile != null && !profile.request.status.busy }
                compose.onNodeWithText(app.getString(R.string.logout)).assertDoesNotExist()
                assertTrue(
                    runCatching { runBlocking { auth.logout() } }.exceptionOrNull()
                        is com.example.film_app_mobile.data.error.RequestFailure
                )
                assertTrue(session.signedIn)
                assertEquals(1, server.requestCount)
                click(R.string.delete_account)
                compose.onNodeWithText(app.getString(R.string.delete_warning)).assertExists()
                click(R.string.cancel)
                assertTrue(session.signedIn)
            }
        } finally {
            db.close()
            app.getSharedPreferences("film_session" + ns, 0).edit().clear().commit()
        }
    }
}
