package com.example.film_app_mobile.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

internal const val REQUEST_ID_HEADER = "X-Request-Id"

internal class RequestHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Accept", "application/json")
            .header(REQUEST_ID_HEADER, original.header(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}
