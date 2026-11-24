package com.example.film_app_mobile.feature.deck

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.data.image.loadPoster
import com.example.film_app_mobile.feature.completion.*
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.*

@Composable
internal fun DeckPoster(movie: Film, defocus: Float = 0f) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bitmap by
            produceState<android.graphics.Bitmap?>(null, movie.posterUrl) {
                movie.posterUrl
                    ?.takeIf { it.startsWith("https://") }
                    ?.let { value = loadPoster(it) }
            }
        val image = remember(bitmap) { bitmap?.asImageBitmap() }
        val backdrop = remember(bitmap) { bitmap?.blurSource()?.asImageBitmap() }
        val panelHeight = maxHeight * 0.40f
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S &&
                    backdrop != null &&
                    defocus > 0f
            ) {
                Image(
                    backdrop,
                    null,
                    Modifier.fillMaxSize(),
                    alpha = defocus,
                    contentScale = ContentScale.Crop,
                )
            }
            if (backdrop != null)
                Image(
                    backdrop,
                    null,
                    Modifier.fillMaxSize()
                        .drawWithContent {
                            clipRect(top = size.height - panelHeight.toPx()) {
                                this@drawWithContent.drawContent()
                            }
                        }
                        .blur(18.dp),
                    contentScale = ContentScale.Crop,
                )
        } else {
            AppText(
                stringResource(R.string.movie_placeholder),
                Modifier.align(Alignment.TopCenter).padding(24.dp),
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(panelHeight)
                .background(Color.Black.copy(alpha = 0.73f))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppText(movie.title, color = Color.White, style = AppTheme.typography.title)
            AppText(metadata(movie), color = Color.White, style = AppTheme.typography.caption)
            AppText(
                movie.genres.joinToString(stringResource(R.string.metadata_separator)),
                style = AppTheme.typography.caption,
                color = Color.White,
            )
            if (movie.description.isNotBlank())
                AppText(
                    movie.description,
                    color = Color.White,
                    style = AppTheme.typography.synopsis,
                )
            movie.director?.let {
                AppText(
                    stringResource(R.string.director, it),
                    color = Color.White,
                    style = AppTheme.typography.caption,
                )
            }
            if (movie.actors.isNotEmpty())
                AppText(
                    stringResource(R.string.cast, movie.actors.take(5).joinToString()),
                    color = Color.White.copy(alpha = 0.75f),
                    style = AppTheme.typography.caption,
                )
        }
    }
}
