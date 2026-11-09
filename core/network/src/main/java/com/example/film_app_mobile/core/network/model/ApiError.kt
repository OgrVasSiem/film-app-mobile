package com.example.film_app_mobile.core.network.model

sealed interface ApiError {
    data class Http(
        val httpCode: Int,
        val code: String?,
        val message: String?,
        val requestId: String?,
    ) : ApiError

    data class Timeout(val cause: java.io.InterruptedIOException) : ApiError
    data class Connection(val cause: java.io.IOException) : ApiError
    data class Tls(val cause: javax.net.ssl.SSLException) : ApiError
    data class InvalidResponse(val cause: Exception) : ApiError
    data class EmptyResponse(val metadata: ResponseMetadata) : ApiError
}
