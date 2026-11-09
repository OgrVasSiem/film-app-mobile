package com.example.film_app_mobile.core.network

import com.example.film_app_mobile.core.network.model.ApiError
import com.example.film_app_mobile.core.network.model.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ApiExecutorTest {
    private val executor = ApiExecutor(Json { ignoreUnknownKeys = true })

    @Test
    fun cancellationPropagates() = runBlocking {
        try {
            executor.execute<String> { throw CancellationException("cancelled") }
            fail("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun timeoutsAreSeparatedFromConnectionErrors() = runBlocking {
        val error = SocketTimeoutException("timeout")
        val result = executor.execute<String> { throw error } as ApiResult.Failure
        assertEquals(ApiError.Timeout(error), result.error)
    }

    @Test
    fun dnsFailureIsAConnectionError() = runBlocking {
        val error = UnknownHostException("unresolved")
        val result = executor.execute<String> { throw error } as ApiResult.Failure
        assertEquals(ApiError.Connection(error), result.error)
    }

    @Test
    fun tlsFailureIsSeparatedFromConnectionErrors() = runBlocking {
        val error = SSLHandshakeException("invalid certificate")
        val result = executor.execute<String> { throw error } as ApiResult.Failure
        assertEquals(ApiError.Tls(error), result.error)
    }

    @Test
    fun programmingErrorsAreNotHiddenAsNetworkErrors() = runBlocking {
        try {
            executor.execute<String> { error("programming error") }
            fail("Programming error must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("programming error", expected.message)
        }
    }

    @Test
    fun releaseRejectsCleartext() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkStack(NetworkConfig("http://localhost:8080/"))
        }
    }

    @Test
    fun rejectsMissingTrailingSlash() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkStack(NetworkConfig("https://example.com/api"))
        }
    }

    @Test
    fun rejectsUrlCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkStack(NetworkConfig("https://user:password@example.com/"))
        }
    }
}
