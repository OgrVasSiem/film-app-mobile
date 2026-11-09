package com.example.film_app_mobile.core.network

import com.example.film_app_mobile.core.network.dto.ServerErrorResponse
import com.example.film_app_mobile.core.network.interceptor.REQUEST_ID_HEADER
import com.example.film_app_mobile.core.network.model.ApiError
import com.example.film_app_mobile.core.network.model.ApiResult
import com.example.film_app_mobile.core.network.model.ResponseMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import javax.net.ssl.SSLException

internal class ApiExecutor(
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun <T : Any> execute(request: suspend () -> Response<T>): ApiResult<T> = protect {
        val response = request()
        val metadata = response.metadata()
        if (!response.isSuccessful) {
            response.toHttpFailure()
        } else {
            val body = response.body()
            if (body == null) ApiResult.Failure(ApiError.EmptyResponse(metadata))
            else ApiResult.Success(body, metadata)
        }
    }

    suspend fun executeUnit(request: suspend () -> Response<Unit>): ApiResult<Unit> = protect {
        val response = request()
        if (response.isSuccessful) ApiResult.Success(Unit, response.metadata())
        else response.toHttpFailure()
    }

    private suspend fun <T> protect(block: suspend () -> ApiResult<T>): ApiResult<T> =
        withContext(ioDispatcher) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: SerializationException) {
                currentCoroutineContext().ensureActive()
                ApiResult.Failure(ApiError.InvalidResponse(error))
            } catch (error: SSLException) {
                currentCoroutineContext().ensureActive()
                ApiResult.Failure(ApiError.Tls(error))
            } catch (error: InterruptedIOException) {
                currentCoroutineContext().ensureActive()
                ApiResult.Failure(ApiError.Timeout(error))
            } catch (error: IOException) {
                currentCoroutineContext().ensureActive()
                ApiResult.Failure(ApiError.Connection(error))
            }
        }

    private fun Response<*>.metadata() = ResponseMetadata(
        httpCode = code(),
        requestId = headers()[REQUEST_ID_HEADER] ?: raw().request.header(REQUEST_ID_HEADER),
    )

    private fun Response<*>.toHttpFailure(): ApiResult.Failure {
        val serverError = errorBody()?.use(::decodeError)
        return ApiResult.Failure(
            ApiError.Http(
                httpCode = code(),
                code = serverError?.code,
                message = serverError?.message,
                requestId = metadata().requestId,
            ),
        )
    }

    private fun decodeError(body: ResponseBody): ServerErrorResponse? = try {
        // Limit JSON parsing of error payloads. Retrofit may already have buffered the body.
        val source = body.source()
        source.request(MAX_ERROR_BYTES + 1)
        if (source.buffer.size > MAX_ERROR_BYTES) null
        else json.decodeFromString<ServerErrorResponse>(source.readUtf8())
    } catch (_: SerializationException) {
        null // Preserve HTTP status even when a reverse proxy returns HTML instead of JSON.
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val MAX_ERROR_BYTES = 65_536L
    }
}
