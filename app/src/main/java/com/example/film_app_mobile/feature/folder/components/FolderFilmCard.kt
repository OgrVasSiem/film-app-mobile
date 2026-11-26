package com.example.film_app_mobile.feature.folder.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.FilmDto

@Composable
internal fun FolderFilmCard(
    film: FilmDto,
    type: ContentType,
    status: String?,
    busy: Boolean,
    details: () -> Unit,
    choose: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(if (status == null) 206.dp else 178.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppTheme.colors.surface)
                .clickable(onClick = details),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(.35f).background(AppTheme.colors.border)) {
            Poster(
                url = film.posterUrl,
                modifier = Modifier.fillMaxSize(),
                scale = ContentScale.Crop,
            )
        }

        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            RecommendationTitle(title = film.title)

            val duration = film.durationMinutes?.let { stringResource(type.durationLabel, it) }

            AppText(
                text =
                    listOfNotNull(
                            film.year?.toString(),
                            duration,
                            film.genres.take(2).joinToString().ifBlank { null },
                        )
                        .joinToString(" · "),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
            )
            film.rating?.let {
                AppText(
                    stringResource(R.string.kp_rating, it.value),
                    style = AppTheme.typography.caption,
                )
            }
            if (film.description.isNotBlank()) {
                AppText(text = film.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            DecisionControls(enabled = !busy, exclude = status, decide = choose)
        }
    }
}
