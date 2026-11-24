package com.example.film_app_mobile.feature.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.ui.model.Film

@Composable
internal fun FilmDetailSheet(film: Film, dismiss: () -> Unit) {
    val c = AppTheme.colors
    AppBottomSheet(dismiss) {
        if (!film.posterUrl.isNullOrBlank())
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))) {
                MovieArt(film)
            }
        AppText(film.title, style = AppTheme.typography.title)
        val summary = metadata(film)
        if (summary.isNotBlank()) AppText(summary, color = c.textSecondary)
        if (film.genres.isNotEmpty())
            AppText(
                film.genres.joinToString(stringResource(R.string.metadata_separator)),
                color = c.accent,
            )
        if (film.description.isNotBlank())
            AppText(film.description, style = AppTheme.typography.synopsis)
        film.director
            ?.takeIf { it.isNotBlank() }
            ?.let { AppText(stringResource(R.string.director, it)) }
        if (film.actors.isNotEmpty())
            AppText(
                stringResource(R.string.cast, film.actors.take(5).joinToString()),
                style = AppTheme.typography.caption,
                color = c.textSecondary,
            )
        Link(stringResource(R.string.close_sheet), true, dismiss)
    }
}
