package com.example.film_app_mobile

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.dataStore.ContentTypeDataStore
import com.example.film_app_mobile.core.dataStore.ThemeDataStore
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.network.*
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.data.repository.*
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.data.session.*
import com.example.film_app_mobile.feature.auth.*
import com.example.film_app_mobile.feature.home.HomeViewModel
import com.example.film_app_mobile.feature.home.bottomSheets.PremiumOfferSheet
import com.example.film_app_mobile.feature.profile.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Opt-in live integration journey, run create / restore / secondDevice / delete in order. */
class AccountJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var account: AccountSession
    private lateinit var auth: AuthRepository
    private lateinit var profiles: ProfileRepository
    private lateinit var profileVm: ProfileViewModel
    private val stack by lazy { NetworkStack(NetworkConfig(BuildConfig.API_BASE_URL, true)) }
    private val network
        get() = stack.retrofit

    private val profileApi by lazy { network.create(ProfileApi::class.java) }
    private val authApi by lazy { network.create(AuthApi::class.java) }
    private val library by lazy { network.create(LibraryApi::class.java) }
    private val fixture
        get() = File(context.noBackupFilesDir, "account-journey")

    private val namespace = "-account-ui-test"

    @Before
    fun setup() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveAccount") == "true")
        account = AccountSession(context, profileApi, namespace)
        auth = AuthRepository(account, authApi)
        profiles = ProfileRepository(account, profileApi)
        profileVm = ProfileViewModel(profiles, auth, ThemeDataStore(context))
    }

    private fun render(premium: Boolean = false) {
        val authVm = AuthViewModel(auth)
        val homeVm =
            if (premium)
                HomeViewModel(
                    SectionRepository(account, stack),
                    profiles,
                    ContentTypeDataStore(context),
                )
            else null
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    val mode by account.session.collectAsState()
                    if (mode == SessionMode.Guest) AuthRoute(authVm)
                    else if (homeVm != null) {
                        val homeState by homeVm.viewState.collectAsState()
                        PremiumOfferSheet(homeState, homeVm)
                    } else ProfileRoute(profileVm)
                }
            }
        }
    }

    private fun label(id: Int) = context.getString(id)

    private fun waitFor(id: Int) {
        compose.waitUntil(15000) {
            compose.onAllNodesWithText(label(id)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(id: Int) {
        waitFor(id)
        compose.onAllNodesWithText(label(id)).onLast().performClick()
    }

    private fun input(id: Int, text: String) =
        compose.onNodeWithContentDescription(label(id)).performTextReplacement(text)

    @Test
    fun create() {
        check(account.session.value == SessionMode.Guest && !fixture.exists())
        render()
        click(R.string.account_start)
        waitFor(R.string.protect_profile)
        val initial = runBlocking { profiles.fresh() }
        assertFalse(initial.passwordProtected)
        compose.onNodeWithText(label(R.string.logout)).assertDoesNotExist()
        val other = runBlocking { authApi.start() }
        try {
            compose.onNodeWithContentDescription(label(R.string.rename_login)).performClick()
            input(R.string.username, other.account.login)
            waitFor(R.string.login_taken)
            click(R.string.save_changes)
            compose.waitUntil(10000) { profileVm.request.status.error == R.string.login_taken }
            click(R.string.cancel)
        } finally {
            runBlocking { profileApi.delete("Bearer " + other.token, mapOf("confirmed" to true)) }
        }
        val film = runBlocking {
            val packs = network.create(PacksApi::class.java)
            val pack = packs.catalog(account.auth, null).body()!!.packs.first { !it.locked }
            packs.page(account.auth, pack.id, 10, null).items.first().ui()
        }
        val decisions = SectionRepository(account, stack)
        val saved = kotlinx.coroutines.CompletableDeferred<Unit>()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    val scope = rememberCoroutineScope()
                    com.example.film_app_mobile.feature.deck.components.DeckCarousel(
                        listOf(film),
                        false,
                        androidx.compose.ui.Modifier.fillMaxSize().testTag("journey-card"),
                    ) { movie, status, _ ->
                        scope.launch {
                            runCatching {
                                    decisions.decide(
                                        com.example.film_app_mobile.core.models.ContentType.FILM,
                                        movie.id,
                                        status,
                                    )
                                }
                                .onSuccess { saved.complete(Unit) }
                                .onFailure { saved.completeExceptionally(it) }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("journey-card").performTouchInput { swipeRight() }
        compose.waitUntil(15000) { saved.isCompleted }
        runBlocking { saved.await() }
        val password = UUID.randomUUID().toString()
        render(premium = true)
        click(R.string.check_purchase)
        waitFor(R.string.protect_purchase)
        input(R.string.password, password)
        input(R.string.repeat_password, password)
        click(R.string.save_changes)
        waitFor(R.string.no_checkout)
        val updated = runBlocking { profiles.protect(UUID.randomUUID().toString()) }
        assertTrue(updated.passwordProtected && !updated.premium)
        val newLogin = "journey-" + UUID.randomUUID().toString().take(12)
        render()
        compose.onNodeWithContentDescription(label(R.string.rename_login)).performClick()
        input(R.string.username, newLogin.uppercase())
        waitFor(R.string.login_free)
        click(R.string.save_changes)
        compose.waitUntil(15000) { profileVm.viewState.value.profile?.login == newLogin }
        fixture.writeText(listOf(newLogin, password, film.id.toString()).joinToString("\n"))
    }

    @Test
    fun restore() = runBlocking {
        check(account.session.value == SessionMode.Restoring)
        account.restore()
        assertEquals(SessionMode.Server, account.session.value)
        val data = fixture.readLines()
        assertTrue(profiles.fresh().login == data[0])
        assertTrue(library.library(account.auth, "WILL_WATCH").any { it.id == data[2].toLong() })
    }

    @Test
    fun secondDevice() {
        check(account.session.value == SessionMode.Guest)
        val data = fixture.readLines()
        render()
        click(R.string.existing_account)
        input(R.string.username, data[0])
        input(R.string.password, data[1])
        click(R.string.login_action)
        waitFor(R.string.protected_account)
        assertTrue(
            runBlocking {
                library.library(account.auth, "WILL_WATCH").any { it.id == data[2].toLong() }
            }
        )
        click(R.string.logout)
        waitFor(R.string.account_start)
        fixture.delete()
    }

    @Test
    fun delete() {
        runBlocking { account.restore() }
        val data = fixture.readLines()
        val id = account.cachedOwner
        check(profilesLogin() == data[0])
        render()
        click(R.string.delete_account)
        waitFor(R.string.delete_warning)
        click(R.string.delete_account)
        waitFor(R.string.account_start)
        assertFalse(account.signedIn)
        val failure =
            runCatching { runBlocking { authApi.login(CredentialsDto(data[0], data[1])) } }
                .exceptionOrNull()
        assertTrue(failure is retrofit2.HttpException && failure.code() == 401)
        fixture.delete()
    }

    private fun profilesLogin() = runBlocking { profiles.fresh().login }
}
