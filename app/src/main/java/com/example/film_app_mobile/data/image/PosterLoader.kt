package com.example.film_app_mobile.data.image

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun loadPoster(context: Context, url: String) = withContext(Dispatchers.IO) {
    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
    try {
        require(url.startsWith("https://"))
        val directory = File(context.cacheDir, "posters").apply { mkdirs() }
        val file = File(directory, digest(url.toByteArray()))
        if (file.exists()) BitmapFactory.decodeFile(file.path, options)?.let {
            file.setLastModified(System.currentTimeMillis())
            return@withContext it
        }
        val temp = File.createTempFile("poster-", ".tmp", directory)
        try {
            val connection = URL(url).openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }
            connection.getInputStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
            BitmapFactory.decodeFile(temp.path, options)?.also {
                android.system.Os.rename(temp.path, file.path)
                val files = directory.listFiles().orEmpty().filter { it.name.length == 64 }.sortedBy { it.lastModified() }
                var size = files.sumOf { it.length() }
                for (old in files) {
                    if (size <= 64L * 1024 * 1024) break
                    val length = old.length()
                    if (old.delete()) size -= length
                }
            }
        } finally { temp.delete() }
    } catch (_: java.io.IOException) { null }
}
