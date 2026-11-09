package com.example.film_app_mobile.core.network.model

sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val metadata: ResponseMetadata,
    ) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

data class ResponseMetadata(
    val httpCode: Int,
    val requestId: String?,
)

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data), metadata)
    is ApiResult.Failure -> this
}
