package com.example.film_app_mobile.core.network

import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.network.dto.*
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceContractsTest {
    @Test
    fun servicesUseVersionedPathsAndSharedFilmSchema() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val retrofit =
                NetworkStack(
                        NetworkConfig(server.url("/api/v1/").toString(), allowCleartext = true)
                    )
                    .retrofit
            val auth = retrofit.create(AuthApi::class.java)
            val profile = retrofit.create(ProfileApi::class.java)
            val packs = retrofit.create(PacksApi::class.java)
            val library = retrofit.create(LibraryApi::class.java)
            val account = """{"id":1,"login":"filmfan","premium":true,"adsEnabled":false}"""
            val session = """{"token":"test","account":$account}"""
            val films =
                """[{"id":2,"title":"Film","description":"","posterUrl":null,"year":null,"director":null,"actors":[],"durationMinutes":null,"genres":[]}]"""
            val credentials = CredentialsDto("filmfan", "password")
            val bearer = "Bearer test"

            fun respond(body: String, code: Int = 200) {
                server.enqueue(MockResponse.Builder().code(code).body(body).build())
            }
            fun verify(method: String, path: String, authenticated: Boolean = true) {
                val request = server.takeRequest()
                assertEquals(method, request.method)
                assertEquals(
                    "/api/v1/$path",
                    request.url.encodedPath + (request.url.encodedQuery?.let { "?$it" } ?: ""),
                )
                assertEquals(if (authenticated) bearer else null, request.headers["Authorization"])
            }

            respond(session, 201)
            assertEquals(true, auth.register(credentials).account.premium)
            verify("POST", "auth/register", false)
            respond(session)
            assertEquals("test", auth.login(credentials).token)
            verify("POST", "auth/login", false)
            respond(account)
            assertEquals(false, profile.profile(bearer).adsEnabled)
            verify("GET", "me")
            respond("""[{"id":3,"title":"Pack","description":"","premium":true,"locked":false}]""")
            assertEquals(false, packs.packs(bearer).single().locked)
            verify("GET", "packs")
            respond(films)
            val film = packs.films(bearer, 3).single()
            verify("GET", "packs/3/films")
            server.enqueue(MockResponse.Builder().code(204).build())
            library.decide(bearer, film.id, DecisionDto("WATCHED"))
            verify("PUT", "library/2")
            respond(films)
            assertEquals(film, library.library(bearer, "WATCHED").single())
            verify("GET", "library?status=WATCHED")
            server.enqueue(MockResponse.Builder().code(204).build())
            library.reset(bearer, film.id)
            verify("DELETE", "library/2")
            server.enqueue(MockResponse.Builder().code(204).build())
            auth.logout(bearer)
            verify("POST", "auth/logout")
        }
    }
}
