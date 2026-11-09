package com.example.film_app_mobile.core.network

import com.example.film_app_mobile.core.network.api.HealthApi
import com.example.film_app_mobile.core.network.model.ApiError
import com.example.film_app_mobile.core.network.model.ApiResult
import com.example.film_app_mobile.core.network.repository.DefaultHealthRepository
import com.example.film_app_mobile.core.network.repository.HealthStatus
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

class HealthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var stack: NetworkStack
    private lateinit var repository: DefaultHealthRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        stack = NetworkStack(NetworkConfig(server.url("/api/").toString(), allowCleartext = true))
        repository = DefaultHealthRepository(stack.retrofit.create(HealthApi::class.java), stack.executor)
    }

    @After
    fun tearDown() {
        stack.client.dispatcher.executorService.shutdown()
        stack.client.connectionPool.evictAll()
        server.close()
    }

    @Test
    fun healthParsesStatusAndUsesConfiguredBasePath() = runBlocking {
        server.enqueue(jsonResponse(200, """{"status":"ok","extra":"ignored"}"""))
        val result = repository.checkServer()
        assertTrue(result is ApiResult.Success)
        val success = result as ApiResult.Success<HealthStatus>
        assertEquals("ok", success.data.status)
        assertEquals(200, success.metadata.httpCode)
        val request = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        assertEquals("/api/health", request.url.encodedPath)
        assertEquals("GET", request.method)
        assertEquals("application/json", request.headers["Accept"])
        UUID.fromString(request.headers["X-Request-Id"])
        assertEquals(request.headers["X-Request-Id"], success.metadata.requestId)
    }

    @Test
    fun databaseUnavailablePreservesServerErrorAndRequestId() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(503)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Request-Id", "server-123")
                .body("""{"code":"DATABASE_UNAVAILABLE","message":"Database is unavailable"}""")
                .build(),
        )
        val result = repository.checkDatabase() as ApiResult.Failure
        val error = result.error as ApiError.Http
        assertEquals(503, error.httpCode)
        assertEquals("DATABASE_UNAVAILABLE", error.code)
        assertEquals("Database is unavailable", error.message)
        assertEquals("server-123", error.requestId)
        assertEquals("/api/health/db", requireNotNull(server.takeRequest(2, TimeUnit.SECONDS)).url.encodedPath)
    }

    @Test
    fun htmlErrorKeepsHttpStatus() = runBlocking {
        server.enqueue(MockResponse.Builder().code(502).body("<html>Bad gateway</html>").build())
        val error = (repository.checkServer() as ApiResult.Failure).error as ApiError.Http
        assertEquals(502, error.httpCode)
        assertNull(error.code)
        assertNull(error.message)
    }

    @Test
    fun invalidSuccessJsonIsReported() = runBlocking {
        server.enqueue(jsonResponse(200, """{"unexpected":true}"""))
        val result = repository.checkServer() as ApiResult.Failure
        assertTrue(result.error is ApiError.InvalidResponse)
    }

    @Test
    fun server204IsNotTreatedAsValidHealthDto() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        val error = (repository.checkServer() as ApiResult.Failure).error as ApiError.EmptyResponse
        assertEquals(204, error.metadata.httpCode)
    }

    @Test
    fun database204AcceptsAnEmptyBody() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.checkDatabase() as ApiResult.Success<Unit>
        assertEquals(Unit, result.data)
        assertEquals(204, result.metadata.httpCode)
    }

    @Test
    fun database200DoesNotAssumeBodySchema() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).body("database check passed").build())
        assertTrue(repository.checkDatabase() is ApiResult.Success)
    }

    @Test
    fun redirectsAreReturnedWithoutFollowingThem() = runBlocking {
        server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "/other").build())
        val error = (repository.checkServer() as ApiResult.Failure).error as ApiError.Http
        assertEquals(302, error.httpCode)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun requestIdsAreDifferentForSeparateCalls() = runBlocking {
        server.enqueue(jsonResponse(200, """{"status":"ok"}"""))
        server.enqueue(jsonResponse(200, """{"status":"ok"}"""))
        val first = repository.checkServer() as ApiResult.Success<HealthStatus>
        val second = repository.checkServer() as ApiResult.Success<HealthStatus>
        assertNotEquals(first.metadata.requestId, second.metadata.requestId)
    }

    @Test
    fun oversizedErrorPayloadKeepsHttpCode() = runBlocking {
        val body = "{\"message\":\"" + "x".repeat(70_000) + "\"}"
        server.enqueue(jsonResponse(500, body))
        val error = (repository.checkServer() as ApiResult.Failure).error as ApiError.Http
        assertEquals(500, error.httpCode)
        assertNull(error.message)
    }

    private fun jsonResponse(code: Int, body: String) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
