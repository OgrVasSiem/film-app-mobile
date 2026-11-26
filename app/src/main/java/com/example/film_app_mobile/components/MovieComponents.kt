package com.example.film_app_mobile.components

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
        AppText(stringResource(R.string.movie_placeholder), color = AppTheme.colors.textSecondary)
        Poster(movie.posterUrl)
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
        AppText(label, style = AppTheme.typography.button, color = AppTheme.colors.accent)
    }
}

@Composable
internal fun Chip(
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    action: (() -> Unit)? = null,
) {
    val click =
        if (action != null)
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = action)
        else Modifier
    Box(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (selected) AppTheme.colors.accent else AppTheme.colors.border)
            .then(click)
            .padding(horizontal = 9.dp, vertical = if (action != null) 12.dp else 4.dp)
    ) {
        AppText(
            label,
            style = AppTheme.typography.caption,
            color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.textPrimary,
        )
    }
}

@Composable
internal fun Poster(
    url: String?,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp),
    scale: ContentScale = ContentScale.Fit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    if (url == null) return
    val bitmap by
        produceState<android.graphics.Bitmap?>(null, url) {
            value = com.example.film_app_mobile.data.image.loadPoster(context, url)
        }
    bitmap?.let { Image(it.asImageBitmap(), null, modifier, contentScale = scale) }
}
