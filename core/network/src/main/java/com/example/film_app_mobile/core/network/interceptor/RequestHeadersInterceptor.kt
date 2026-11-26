package com.example.film_app_mobile.core.network.interceptor

import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

internal const val REQUEST_ID_HEADER = "X-Request-Id"

internal class RequestHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request =
            original
                .newBuilder()
                .header("Accept", "application/json")
                .header(
                    REQUEST_ID_HEADER,
                    original.header(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString(),
                )
                .build()
        // OkHttp may replay a 503 with Retry-After: 0 even with connection retries disabled.
        val body = request.body
        if (request.url.encodedPath.endsWith("/auth/start") && body != null) {
            val once =
                object : okhttp3.RequestBody() {
                    override fun contentType() = body.contentType()

                    override fun contentLength() = body.contentLength()

                    override fun writeTo(sink: okio.BufferedSink) = body.writeTo(sink)

                    override fun isOneShot() = true
                }
            return chain.proceed(request.newBuilder().method(request.method, once).build())
        }
        return chain.proceed(request)
    }
}
