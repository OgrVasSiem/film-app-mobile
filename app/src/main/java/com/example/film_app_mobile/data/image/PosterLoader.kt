package com.example.film_app_mobile.data.image

import android.graphics.BitmapFactory
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun loadPoster(url: String) =
    withContext(Dispatchers.IO) {
        try {
            val connection =
                URL(url).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
            connection.getInputStream().use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = 2 },
                )
            }
        } catch (_: java.io.IOException) {
            null
        }
    }
