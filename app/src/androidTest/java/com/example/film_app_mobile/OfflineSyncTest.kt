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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OfflineSyncTest {
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
            val packs = PacksRepository(session, server, db.pack(), db.deck(), sync)
            val library = LibraryRepository(session, server, db.library(), sync)
        }
        try {
            var repository = Repositories()
            repository.auth.signIn("first", "pass", false)
            repository.packs.packs()
            val film = repository.packs.films(1).single()
            server.online = false
            repository.library.decide(film, "WILL_WATCH")
            repository.library.decide(film, "WATCHED")
            assertTrue(repository.packs.films(1).isEmpty())
            db.close()
            db = open()
            repository = Repositories()
            assertEquals("first", repository.profile.profile().login)
            assertEquals(1, repository.packs.packs().size)
            assertEquals(film, repository.library.library("WATCHED").single())
            assertNull(server.status)
            server.online = true
            repository.sync.sync()
            assertEquals("WATCHED", server.status)
            repository.sync.sync()
            assertEquals(1, server.writes)
            server.online = false
            repository.library.reset(film.id)
            assertEquals(film, repository.packs.films(1).single())
            server.online = true
            repository.sync.sync()
            assertNull(server.status)
            repository.auth.logout()
            repository.auth.signIn("second", "pass", false)
            server.online = false
            assertEquals("second", repository.profile.profile().login)
            assertTrue(runCatching { repository.packs.packs() }.isFailure)
        } finally {
            db.close()
            app.deleteDatabase(name)
            app.getSharedPreferences(name, 0).edit().clear().commit()
        }
    }

    private class Server : AuthApi, ProfileApi, PacksApi, LibraryApi {
        var online = true
        var status: String? = null
        var writes = 0
        var account = ProfileDto(1, "first", false, true)
        val film = FilmDto(2, "Film", "", null, 2020, null, listOf("Actor"), 90, listOf("Drama"))

        fun connected() {
            if (!online) throw IOException("offline")
        }

        override suspend fun login(body: CredentialsDto): SessionDto {
            connected()
            account = ProfileDto(if (body.login == "first") 1 else 2, body.login, false, true)
            return SessionDto("test-token", account)
        }

        override suspend fun register(body: CredentialsDto) = login(body)

        override suspend fun logout(auth: String) = connected()

        override suspend fun profile(auth: String): ProfileDto {
            connected()
            return account
        }

        override suspend fun packs(auth: String): List<PackDto> {
            connected()
            return listOf(PackDto(1, "Pack", "", false, false))
        }

        override suspend fun films(auth: String, id: Long): List<FilmDto> {
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
