package com.example.film_app_mobile.data.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.film_app_mobile.core.network.dto.CoverDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
    .joinToString("") { "%02x".format(it) }

class CoverFiles @Inject constructor(@ApplicationContext context: Context) {
    private val directory = File(context.filesDir, "covers")
    suspend fun valid(hash: String) = withContext(Dispatchers.IO) {
        val file = File(directory, hash)
        hash.matches(Regex("[a-f0-9]{64}")) && file.isFile && digest(file.readBytes()) == hash
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun save(cover: CoverDto): String = withContext(Dispatchers.IO) {
        val hash = cover.sha256.lowercase()
        require(hash.matches(Regex("[a-f0-9]{64}")))
        val bytes = Base64.decode(cover.base64, Base64.DEFAULT)
            require(digest(bytes) == hash)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outMimeType.equals(cover.mimeType, true))
        if (!valid(hash)) {
            directory.mkdirs()
            val temp = File.createTempFile("cover-", ".tmp", directory)
            try {
                temp.writeBytes(bytes)
                android.system.Os.rename(temp.path, File(directory, hash).path)
            } finally { temp.delete() }
        }
        hash
    }
}
