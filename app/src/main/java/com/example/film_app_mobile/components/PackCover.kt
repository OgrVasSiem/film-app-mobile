package com.example.film_app_mobile.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.network.dto.CoverDto
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PackCover(cover: CoverDto?, modifier: Modifier, blurred: Boolean = false) {

    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.border),
        contentAlignment = Alignment.Center,
    ) {
        key(cover?.sha256) {
            var failed by remember { mutableStateOf(false) }
            val image by
                produceState<Bitmap?>(null, cover) {
                    if (cover != null) {
                        value = withContext(Dispatchers.IO) { decodeCover(cover) }
                        failed = value == null
                    }
                }
            val bitmap = remember(image, blurred) { if (blurred) image?.blurSource() else image }
            if (bitmap != null)
                Image(
                    bitmap.asImageBitmap(),
                    null,
                    Modifier.fillMaxSize().then(if (blurred) Modifier.blur(12.dp) else Modifier),
                    contentScale = ContentScale.Crop,
                )
            else
                AppText(
                    stringResource(
                        if (cover == null || failed) R.string.cover_missing
                        else R.string.cover_loading
                    ),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
        }
    }
}

private fun decodeCover(cover: CoverDto): Bitmap? {
    val bytes =
        try {
            Base64.decode(cover.base64, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            return null
        }
    val hash =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    if (!hash.equals(cover.sha256, ignoreCase = true)) return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
