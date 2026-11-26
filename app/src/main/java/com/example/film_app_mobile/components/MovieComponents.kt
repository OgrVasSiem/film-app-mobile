package com.example.film_app_mobile.components

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.data.image.loadPoster
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.*

@Composable
internal fun metadata(movie: Film, type: ContentType = ContentType.FILM) =
    listOfNotNull(
            movie.year?.toString(),
            movie.durationMinutes?.let { stringResource(type.durationLabel, it) },
        )
        .joinToString(stringResource(R.string.metadata_separator))

@Composable
internal fun MovieArt(movie: Film) {

    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.border),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = stringResource(R.string.movie_placeholder),
            color = AppTheme.colors.textSecondary,
        )

        Poster(url = movie.posterUrl)
    }
}

@Composable
internal fun Link(label: String, enabled: Boolean, action: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.colors.border)
            .clickable(enabled = enabled, role = Role.Button, onClick = action)
            .heightIn(min = 44.dp)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = label, style = AppTheme.typography.button, color = AppTheme.colors.accent)
    }
}

@Composable
internal fun Poster(
    url: String?,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp),
    scale: ContentScale = ContentScale.Fit,
) {
    if (url != null) {
        val bitmap by produceState<Bitmap?>(null, url) { value = loadPoster(url) }

        bitmap?.let { Image(it.asImageBitmap(), null, modifier, contentScale = scale) }
    } else {
        Box(modifier = Modifier.background(AppTheme.colors.backgroundSecondary).fillMaxSize())
    }
}
