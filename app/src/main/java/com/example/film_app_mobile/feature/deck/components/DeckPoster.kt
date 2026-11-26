package com.example.film_app_mobile.feature.deck.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.ui.model.*

@Composable
internal fun DeckPoster(
    movie: Film,
    type: ContentType,
    defocus: Float = 0f,
    openDetail: ((Long) -> Unit)? = null,
) {
    var details by remember(movie.id) { mutableStateOf(false) }
    if (details)
        com.example.film_app_mobile.components.FilmDetailSheet(movie, type) { details = false }
    BoxWithConstraints(
        Modifier.fillMaxSize().clickable {
            if (openDetail != null) openDetail(movie.id) else details = true
        }
    ) {
        Poster(
            movie.posterUrl,
            Modifier.fillMaxSize(),
            androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = maxHeight * .225f)
                .background(AppTheme.colors.posterScrim)
                .padding(12.dp)
        ) {
            AppText(
                movie.title,
                style =
                    AppTheme.typography.title.copy(
                        fontSize = AppTheme.typography.title.fontSize * 2,
                        lineHeight = AppTheme.typography.title.lineHeight * 2,
                    ),
                color = AppTheme.colors.onPoster,
                maxLines = 1,
            )
            AppText(
                listOf(metadata(movie, type), movie.genres.joinToString())
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style =
                    AppTheme.typography.caption.copy(
                        fontSize = (AppTheme.typography.caption.fontSize.value + 2).sp
                    ),
                color = AppTheme.colors.onPosterSecondary,
                maxLines = 1,
            )
            AppText(
                movie.description,
                style =
                    AppTheme.typography.synopsis.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                    ),
                color = AppTheme.colors.onPoster,
                maxLines = 2,
            )
        }
    }
}
