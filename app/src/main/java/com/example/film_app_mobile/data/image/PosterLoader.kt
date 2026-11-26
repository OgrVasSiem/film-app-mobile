package com.example.film_app_mobile.data.image

import android.content.Context
import android.graphics.BitmapFactory
import com.example.film_app_mobile.BuildConfig
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun posterFile(context: Context, url: String) =
    File(File(context.noBackupFilesDir, "posters"), digest(url.toByteArray()))

internal suspend fun preloadPoster(context: Context, url: String) {
    withContext(Dispatchers.IO) {
        if (!posterFile(context, url).exists())
            loadPoster(context, url, retryFailures = true)?.recycle()
    }
}

internal suspend fun loadPoster(context: Context, url: String, retryFailures: Boolean = false) =
    withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        try {
            val server = URL(BuildConfig.API_BASE_URL)
            val source = URL(server, url)
            val address =
                if (source.path.startsWith("/api/v1/posters/")) {
                    URL(server.protocol, server.host, server.port, source.file)
                } else source
            if (address.protocol != "https") {
                if (!BuildConfig.DEBUG || address.protocol != "http") return@withContext null
                if (
                    server.protocol != address.protocol ||
                        server.host != address.host ||
                        server.port != address.port
                )
                    return@withContext null
            }
            val directory = File(context.noBackupFilesDir, "posters").apply { mkdirs() }
            val file = posterFile(context, url)
            if (file.exists())
                BitmapFactory.decodeFile(file.path, options)?.let {
                    file.setLastModified(System.currentTimeMillis())
                    return@withContext it
                }
            val temp = File.createTempFile("poster-", ".tmp", directory)
            try {
                val connection =
                    (address.openConnection() as java.net.HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                connection.getInputStream().use { input ->
                    temp.outputStream().use { input.copyTo(it) }
                }
                BitmapFactory.decodeFile(temp.path, options)?.also {
                    android.system.Os.rename(temp.path, file.path)
                }
            } finally {
                temp.delete()
            }
        } catch (_: java.io.FileNotFoundException) {
            null
        } catch (e: java.io.IOException) {
            if (retryFailures) throw e
            null
        }
    }
