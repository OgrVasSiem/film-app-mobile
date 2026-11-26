package com.example.film_app_mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.*
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.storage.FilmDatabase
import com.example.film_app_mobile.data.image.CoverFiles
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.data.session.AccountSession
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class CatalogV2DeviceTest {
    @Test
    fun offlineSnapshotAndQueuedDecision() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(app, FilmDatabase::class.java).build()
        try {
            mockwebserver3.MockWebServer().use { server ->
                server.start()
                val stack = NetworkStack(NetworkConfig(server.url("/api/v1/").toString(), true))
                val profile = stack.retrofit.create(ProfileApi::class.java)
                val session =
                    AccountSession(app, profile, db.profile(), UUID.randomUUID().toString())
                session.start(
                    com.example.film_app_mobile.core.network.dto.SessionDto(
                        "fixture",
                        com.example.film_app_mobile.core.network.dto.ProfileDto(
                            1,
                            "fixture",
                            false,
                            true,
                            false,
                            0,
                        ),
                    )
                )
                var repo = SectionRepository(session, stack, db.library(), CoverFiles(app))
                fun reply(code: Int, body: String = "", etag: String? = null) {
                    val builder = mockwebserver3.MockResponse.Builder().code(code)
                    if (body.isNotEmpty()) builder.body(body)
                    etag?.let { builder.addHeader("ETag", it) }
                    server.enqueue(builder.build())
                }
                fun page(title: String) =
                    """{"items":[{"id":42,"title":"$title","description":"","posterUrl":null,"year":null,"director":null,"actors":[],"durationMinutes":null,"genres":[]}],"nextCursor":"a+/="}"""

                for (type in ContentType.entries) {
                    reply(
                        200,
                        """{"packs":[],"covers":{},"sections":[{"key":"DAY","state":"READY","availableCount":1}]}""",
                    )
                    reply(200, page(type.apiKey).replace("\"a+/=\"", "null"))
                    repeat(3) { reply(200, """{"items":[],"nextCursor":null}""") }
                    reply(200, """{"film":null,"source":"EXHAUSTED"}""")
                }
                repo.synchronize(app)
                val before = server.requestCount
                repo.decide(ContentType.FILM, 42, "WILL_WATCH")
                repo = SectionRepository(session, stack, db.library(), CoverFiles(app))
                assertTrue(repo.page(ContentType.FILM, "decks/DAY", null, null).items.isEmpty())
                assertEquals(
                    "series",
                    repo.page(ContentType.SERIES, "decks/DAY", null, null).items.single().title,
                )
                assertEquals(
                    1L,
                    repo.overview(ContentType.FILM).sections.getValue("WILL_WATCH").totalCount,
                )
                repo.decide(ContentType.FILM, 42, "WATCHED")
                assertEquals(before, server.requestCount)
                reply(204)
                repo.flush(1, session.auth)
                repeat(before) { server.takeRequest() }
                val write = server.takeRequest()
                assertEquals("PUT", write.method)
                assertEquals("/api/v2/films/library/42", write.url.encodedPath)
                assertTrue(write.body!!.utf8().contains("WATCHED"))
                repo.decide(ContentType.FILM, 42, null)
                assertEquals(
                    42L,
                    repo.page(ContentType.FILM, "decks/DAY", null, null).items.single().id,
                )
                session.endSession()
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun identicalIdsConditionalCatalogAndInvalidatedCursor() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(app, FilmDatabase::class.java).build()
        try {
            mockwebserver3.MockWebServer().use { server ->
                server.start()
                val stack = NetworkStack(NetworkConfig(server.url("/api/v1/").toString(), true))
                val profile = stack.retrofit.create(ProfileApi::class.java)
                val session =
                    AccountSession(app, profile, db.profile(), UUID.randomUUID().toString())
                session.start(
                    com.example.film_app_mobile.core.network.dto.SessionDto(
                        "fixture",
                        com.example.film_app_mobile.core.network.dto.ProfileDto(
                            1,
                            "fixture",
                            false,
                            true,
                            false,
                            0,
                        ),
                    )
                )
                val repo = SectionRepository(session, stack, db.library(), CoverFiles(app))
                fun reply(code: Int, body: String = "", etag: String? = null) {
                    val builder = mockwebserver3.MockResponse.Builder().code(code)
                    if (body.isNotEmpty()) builder.body(body)
                    etag?.let { builder.addHeader("ETag", it) }
                    server.enqueue(builder.build())
                }
                fun page(title: String) =
                    """{"items":[{"id":42,"title":"$title","description":"","posterUrl":null,"year":null,"director":null,"actors":[],"durationMinutes":null,"genres":[]}],"nextCursor":"a+/="}"""
                for (type in ContentType.entries) {
                    reply(200, """{"packs":[],"sections":[],"covers":{}}""", "\"v1\"")
                    val cached = repo.catalog(type)
                    assertNull(server.takeRequest().headers["If-None-Match"])
                    reply(304)
                    assertEquals(cached, repo.catalog(type))
                    assertEquals("\"v1\"", server.takeRequest().headers["If-None-Match"])
                    reply(200, page(type.apiKey))
                    repo.page(type, "decks/DAY", null, null)
                    assertEquals(
                        "/api/v2/${type.apiKey}/decks/DAY",
                        server.takeRequest().url.encodedPath,
                    )
                }
                reply(409, """{"code":"DECK_CHANGED"}""")
                reply(200, page("replacement"))
                val reset = repo.page(ContentType.SERIES, "decks/DAY", null, "a+/=")
                assertEquals("a+/=", server.takeRequest().url.queryParameter("cursor"))
                assertNull(server.takeRequest().url.queryParameter("cursor"))
                assertEquals("replacement", reset.items.single().title)
                assertEquals(
                    "films",
                    repo.cachedPage(ContentType.FILM, "decks/DAY", null)!!.items.single().title,
                )
                reply(503)
                repo.decide(ContentType.FILM, 42, "WATCHED")
                assertEquals(
                    42L,
                    repo.overview(ContentType.FILM).sections.getValue("WATCHED").items.single().id,
                )
                assertTrue(repo.page(ContentType.FILM, "decks/DAY", null, null).items.isEmpty())
                session.endSession()
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun liveSections() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveCatalog") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(app, FilmDatabase::class.java).build()
        val stack = NetworkStack(NetworkConfig(BuildConfig.API_BASE_URL, true))
        val profile = stack.retrofit.create(ProfileApi::class.java)
        val namespace = UUID.randomUUID().toString()
        val session = AccountSession(app, profile, db.profile(), namespace)
        val temporary = stack.retrofit.create(AuthApi::class.java).start()
        try {
            session.start(temporary)
            val restored = AccountSession(app, profile, db.profile(), namespace)
            restored.restore()
            assertTrue(restored.signedIn)
            val repo = SectionRepository(restored, stack, db.library(), CoverFiles(app))
            for (type in ContentType.entries) {
                val first = repo.catalog(type)
                val times =
                    first.hashes.values.associateWith {
                        java.io.File(app.filesDir, "covers/" + it).lastModified()
                    }
                assertEquals(first, repo.catalog(type))
                assertTrue(
                    times.all { (hash, time) ->
                        time > 0 &&
                            java.io.File(app.filesDir, "covers/" + hash).lastModified() == time
                    }
                )
                first.catalog.packs
                    .firstOrNull { it.locked }
                    ?.let { locked ->
                        val failure =
                            runCatching {
                                    repo.page(
                                        type,
                                        "packs/" + locked.id + "/films/page",
                                        null,
                                        null,
                                    )
                                }
                                .exceptionOrNull()
                        assertEquals(
                            R.string.premium_required,
                            (failure as com.example.film_app_mobile.data.error.RequestFailure)
                                .messageId,
                        )
                    }
                assertTrue(first.catalog.sections.isNotEmpty())
                val pack =
                    first.catalog.packs
                        .filter { !it.locked && it.availableCount > 0 }
                        .maxByOrNull { it.availableCount } ?: continue
                val path = "packs/" + pack.id + "/films/page"
                val page = repo.page(type, path, null, null)
                val film = page.items.first()
                assertEquals(film.id, repo.card(type, film.id).id)
                page.nextCursor?.let {
                    assertTrue(repo.page(type, path, null, it).items.size > page.items.size)
                }
                repo.decide(type, film.id, "WILL_WATCH")
                assertTrue(repo.page(type, path, null, null).items.none { it.id == film.id })
                repo.decide(type, film.id, "WATCHED")
                assertTrue(
                    repo.overview(type).sections.getValue("WATCHED").items.any { it.id == film.id }
                )
                val other = if (type == ContentType.FILM) ContentType.SERIES else ContentType.FILM
                assertEquals(0L, repo.overview(other).sections.getValue("WATCHED").totalCount)
                val pass = UUID.randomUUID().toString()
                if (type == ContentType.FILM) {
                    profile.protect(session.auth, mapOf("password" to pass))
                    val login =
                        stack.retrofit
                            .create(AuthApi::class.java)
                            .login(
                                com.example.film_app_mobile.core.network.dto.CredentialsDto(
                                    temporary.account.login,
                                    pass,
                                )
                            )
                    try {
                        assertEquals(
                            temporary.account.id,
                            profile.profile("Bearer " + login.token).id,
                        )
                    } finally {
                        stack.retrofit.create(AuthApi::class.java).logout("Bearer " + login.token)
                    }
                }
                repo.decide(type, film.id, null)
                assertTrue(repo.page(type, path, null, null).items.any { it.id == film.id })
            }
            repo.synchronize(app)
            for (type in ContentType.entries) {
                assertNotNull(
                    db.library()
                        .body(temporary.account.id, "offline/" + type.apiKey + "/snapshot/ready")
                )
                assertNotNull(repo.overview(type))
            }
        } finally {
            profile.delete("Bearer " + temporary.token, mapOf("confirmed" to true))
            session.endSession()
            db.close()
        }
    }
}
