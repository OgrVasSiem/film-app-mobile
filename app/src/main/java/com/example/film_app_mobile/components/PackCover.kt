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

@Composable
internal fun PackCover(path: String?, modifier: Modifier, blurred: Boolean = false) {
    val directory = androidx.compose.ui.platform.LocalContext.current.filesDir
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.border),
        contentAlignment = Alignment.Center,
    ) {
        key(path) {
            var failed by remember { mutableStateOf(false) }
            val image by
                produceState<android.graphics.Bitmap?>(null, path) {
                    if (path != null) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { android.graphics.BitmapFactory.decodeFile(java.io.File(directory, "covers/" + path).path) }
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
                        if (path == null || failed) R.string.cover_missing
                        else R.string.cover_loading
                    ),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
        }
    }
}
