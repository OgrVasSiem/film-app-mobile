package com.example.film_app_mobile

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.FilmDatabase
import com.example.film_app_mobile.data.repository.AuthRepository
import com.example.film_app_mobile.data.repository.LibraryRepository
import com.example.film_app_mobile.data.repository.PacksRepository
import com.example.film_app_mobile.data.repository.ProfileRepository
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OfflineSyncTest {
    @Test
    fun premiumTracksServerAndRestoresOfflineForTheActiveAccount() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val namespace = UUID.randomUUID().toString()
        val db = Room.inMemoryDatabaseBuilder(context, FilmDatabase::class.java).build()
        val server = Server()
        fun session() = AccountSession(context, server, db.profile(), namespace)
        val account = session()
        try {
            account.start(server.login(CredentialsDto("first", "pass")))
            val profiles = ProfileRepository(account, server, db.profile())
            assertEquals(false, profiles.premium.first())
            server.account = server.account.copy(premium = true)
            profiles.fresh()
            assertEquals(true, profiles.premium.first())
            server.online = false
            val restored = session()
            restored.restore()
            assertEquals(true, restored.premium.first())
            server.online = true
            server.account = server.account.copy(premium = false)
            profiles.fresh()
            assertEquals(false, restored.premium.first())
            account.endSession()
            assertNull(account.premium.first())
            account.start(server.login(CredentialsDto("second", "pass")))
            assertEquals(false, account.premium.first())
        } finally {
            account.endSession()
            db.close()
        }
    }

    @Test
    fun cachedDeckAndLatestDecisionSurviveRestartAndAccountsStaySeparate() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "sync-test-" + UUID.randomUUID()
        val context =
            object : ContextWrapper(app) {
                override fun getSharedPreferences(key: String, mode: Int) =
                    app.getSharedPreferences(name, mode)
            }
        val server = Server()
        fun open() = Room.databaseBuilder(app, FilmDatabase::class.java, name).build()
        var db = open()
        class Repositories {
            private val session = AccountSession(context, server, db.profile())
            val sync = DecisionSync(session, server, db.library()) {}
            val auth = AuthRepository(session, server, sync)
            val profile = ProfileRepository(session, server, db.profile())
            val packs =
                PacksRepository(
                    session,
                    server,
                    db.pack(),
                    db.deck(),
                    sync,
                    com.example.film_app_mobile.data.image.CoverFiles(context),
                )
            val library = LibraryRepository(session, server, db.library(), sync)
        }
        try {
            var repository = Repositories()
            repository.auth.signIn("first", "pass")
            repository.packs.packs()
            val film = repository.packs.page(1).cards.single()
            server.online = false
            repository.library.decide(film, "WILL_WATCH")
            repository.library.decide(film, "WATCHED")
            assertTrue(repository.packs.localDeck(1)!!.cards.isEmpty())
            db.close()
            db = open()
            repository = Repositories()
            assertEquals("first", repository.profile.profile().login)
            assertEquals(1, repository.packs.cachedPacks().size)
            assertEquals(film, repository.library.library("WATCHED").single())
            assertNull(server.status)
            server.online = true
            repository.sync.sync()
            assertEquals("WATCHED", server.status)
            repository.sync.sync()
            assertEquals(1, server.writes)
            server.online = false
            repository.library.reset(film.id)
            assertEquals(film, repository.packs.localDeck(1)!!.cards.single())
            server.online = true
            repository.sync.sync()
            assertNull(server.status)
            repository.auth.logout()
            repository.auth.signIn("second", "pass")
            server.online = false
            assertEquals("second", repository.profile.profile().login)
            assertTrue(runCatching { repository.packs.packs() }.isFailure)
        } finally {
            db.close()
            app.deleteDatabase(name)
            app.getSharedPreferences(name, 0).edit().clear().commit()
        }
    }

    @Test
    fun restoreRetriesTemporaryFailureAndDropsRejectedToken() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val namespace = "-restore-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences("film_session" + namespace, 0)
        val db = Room.inMemoryDatabaseBuilder(context, FilmDatabase::class.java).build()
        val server = Server()
        fun session() = AccountSession(context, server, db.profile(), namespace)
        try {
            session().start(server.login(CredentialsDto("first", "pass")))
            assertFalse(prefs.contains("token"))
            assertTrue(prefs.getString("encrypted", "") != "test-token")
            val restored = session()
            server.online = false
            restored.restore()
            assertEquals(
                com.example.film_app_mobile.data.session.SessionMode.Server,
                restored.session.value,
            )
            assertTrue(restored.signedIn)
            server.online = true
            restored.restore()
            assertEquals(
                com.example.film_app_mobile.data.session.SessionMode.Server,
                restored.session.value,
            )
            val expired = session()
            server.expired = true
            expired.restore()
            assertFalse(expired.signedIn)
            assertEquals(
                com.example.film_app_mobile.data.session.SessionMode.Guest,
                session().session.value,
            )
        } finally {
            prefs.edit().clear().commit()
            db.close()
        }
    }

    private class Server : AuthApi, ProfileApi, PacksApi, LibraryApi {
        var expired = false
        var online = true
        var status: String? = null
        var writes = 0
        var account = ProfileDto(1, "first", false, true, true, 0)
        val film = FilmDto(2, "Film", "", null, 2020, null, listOf("Actor"), 90, listOf("Drama"))

        fun connected() {
            if (!online) throw IOException("offline")
        }

        override suspend fun login(body: CredentialsDto): SessionDto {
            connected()
            account =
                ProfileDto(if (body.login == "first") 1 else 2, body.login, false, true, true, 0)
            return SessionDto("test-token", account)
        }

        override suspend fun start() = error("unused")

        override suspend fun protect(auth: String, body: Map<String, String>) = error("unused")

        override suspend fun rename(auth: String, body: Map<String, String>) = error("unused")

        override suspend fun availability(auth: String, login: String) = error("unused")

        override suspend fun delete(auth: String, body: Map<String, Boolean>) = error("unused")

        override suspend fun logout(auth: String) = connected()

        override suspend fun profile(auth: String): ProfileDto {
            connected()
            if (expired)
                throw retrofit2.HttpException(
                    retrofit2.Response.error<Any>(
                        401,
                        okhttp3.ResponseBody.Companion.run { "".toResponseBody() },
                    )
                )
            return account
        }

        override suspend fun catalog(auth: String, etag: String?) =
            retrofit2.Response.success(CatalogDto(packs(auth), emptyMap()))

        override suspend fun page(auth: String, id: Long, limit: Int, cursor: String?) =
            FilmPageDto(films(auth, id), null)

        private suspend fun packs(auth: String): List<PackDto> {
            connected()
            return listOf(PackDto(1, "Pack", "", false, false))
        }

        private suspend fun films(auth: String, id: Long): List<FilmDto> {
            connected()
            return if (status == null) listOf(film) else emptyList()
        }

        override suspend fun library(auth: String, status: String): List<FilmDto> {
            connected()
            return if (this.status == status) listOf(film) else emptyList()
        }

        override suspend fun decide(auth: String, id: Long, body: DecisionDto) {
            connected()
            writes++
            status = body.status
        }

        override suspend fun reset(auth: String, id: Long) {
            connected()
            status = null
        }
    }
}
