package com.example.film_app_mobile.components

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
import com.example.film_app_mobile.BuildConfig
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.data.image.loadPoster
import java.net.URI

internal fun resolveCover(base: String, path: String?): String? {
    if (path.isNullOrBlank()) return null
    return runCatching {
            val origin = URI(base).resolve("/")
            val url = origin.resolve(path)
            require(url.scheme == origin.scheme && url.rawAuthority == origin.rawAuthority)
            url.toASCIIString()
        }
        .getOrNull()
}

@Composable
internal fun PackCover(path: String?, modifier: Modifier, blurred: Boolean = false) {
    val url = remember(path) { resolveCover(BuildConfig.API_BASE_URL, path) }
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.border),
        contentAlignment = Alignment.Center,
    ) {
        key(url) {
            var failed by remember { mutableStateOf(false) }
            val image by
                produceState<android.graphics.Bitmap?>(null, url) {
                    if (url != null) {
                        value = loadPoster(url)
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
                        if (url == null || failed) R.string.cover_missing
                        else R.string.cover_loading
                    ),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
        }
    }
}
