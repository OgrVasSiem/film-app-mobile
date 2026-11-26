package com.example.film_app_mobile

import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.*
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.data.session.AccountSession
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class CatalogV2DeviceTest {

    @Test
    fun failedDecisionIsNotReplayedAndNextReadUsesServer() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        mockwebserver3.MockWebServer().use { server ->
            server.start()
            val stack = NetworkStack(NetworkConfig(server.url("/api/v1/").toString(), true))
            val session =
                AccountSession(
                    app,
                    stack.retrofit.create(ProfileApi::class.java),
                    UUID.randomUUID().toString(),
                )
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
            val repo = SectionRepository(session, stack)
            try {
                server.enqueue(mockwebserver3.MockResponse.Builder().code(503).build())
                assertTrue(runCatching { repo.decide(ContentType.FILM, 42, "WATCHED") }.isFailure)
                assertEquals("PUT", server.takeRequest().method)
                val body = """{"packs":[],"sections":[],"covers":{}}"""
                server.enqueue(mockwebserver3.MockResponse.Builder().code(200).body(body).build())
                repo.catalog(ContentType.FILM)
                val catalog = server.takeRequest()
                assertEquals("/api/v2/films/catalog", catalog.url.encodedPath)
                assertNull(catalog.headers["If-None-Match"])
                assertEquals(2, server.requestCount)
                server.enqueue(mockwebserver3.MockResponse.Builder().code(503).build())
                assertTrue(runCatching { repo.catalog(ContentType.FILM) }.isFailure)
            } finally {
                session.endSession()
            }
        }
    }

    @Test
    fun liveSections() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveCatalog") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val stack = NetworkStack(NetworkConfig(BuildConfig.API_BASE_URL, true))
        val profile = stack.retrofit.create(ProfileApi::class.java)
        val namespace = UUID.randomUUID().toString()
        val session = AccountSession(app, profile, namespace)
        val temporary = stack.retrofit.create(AuthApi::class.java).start()
        try {
            session.start(temporary)
            val restored = AccountSession(app, profile, namespace)
            restored.restore()
            assertTrue(restored.signedIn)
            val repo = SectionRepository(restored, stack)
            for (type in ContentType.entries) {
                val first = repo.catalog(type)
                first.packs
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
                assertTrue(first.sections.isNotEmpty())
                val pack =
                    first.packs
                        .filter { !it.locked && it.availableCount > 0 }
                        .maxByOrNull { it.availableCount } ?: continue
                val path = "packs/" + pack.id + "/films/page"
                val page = repo.page(type, path, null, null)
                val film = page.items.first()
                assertEquals(film.id, repo.card(type, film.id).id)
                page.nextCursor?.let {
                    assertTrue(
                        repo.page(type, path, null, it, page.items).items.size > page.items.size
                    )
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
        } finally {
            profile.delete("Bearer " + temporary.token, mapOf("confirmed" to true))
            session.endSession()
        }
    }
}
