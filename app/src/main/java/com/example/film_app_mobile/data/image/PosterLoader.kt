package com.example.film_app_mobile.data.image

import android.graphics.BitmapFactory
import com.example.film_app_mobile.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun loadPoster(url: String) =
    withContext(Dispatchers.IO) {
        val server = URL(BuildConfig.API_BASE_URL)
        val source =
            try {
                URL(server, url)
            } catch (_: IOException) {
                return@withContext null
            }
        val address =
            if (source.path.startsWith("/api/v1/posters/")) {
                URL(server.protocol, server.host, server.port, source.file)
            } else source
        val localDebug =
            BuildConfig.DEBUG &&
                address.protocol == "http" &&
                server.protocol == address.protocol &&
                server.host == address.host &&
                server.port == address.port
        if (address.protocol != "https" && !localDebug) return@withContext null

        val connection =
            (address.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                useCaches = false
                connectTimeout = 5000
                readTimeout = 5000
            }

        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            connection.inputStream.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }
