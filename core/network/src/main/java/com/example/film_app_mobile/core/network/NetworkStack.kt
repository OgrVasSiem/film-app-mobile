package com.example.film_app_mobile.core.network

import com.example.film_app_mobile.core.network.interceptor.RequestHeadersInterceptor
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

internal class NetworkStack(config: NetworkConfig) {
    private val baseUrl = config.baseUrl.trim().let { value ->
        require(value.isNotEmpty()) { "Set API_BASE_URL_RELEASE or API_BASE_URL_DEBUG in Gradle properties." }
        val url = value.toHttpUrl()
        require(config.allowCleartext || url.isHttps) { "Release API URL must use HTTPS." }
        require(url.username.isEmpty() && url.password.isEmpty()) { "Do not put credentials in the API URL." }
        require(url.query == null && url.fragment == null) { "API base URL must not contain a query or fragment." }
        require(url.encodedPath.endsWith("/")) { "API base URL must end with /." }
        url
    }

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        coerceInputValues = false
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(RequestHeadersInterceptor())
        .apply {
            if (config.enableHttpLogging) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                })
            }
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val executor = ApiExecutor(json)
}
