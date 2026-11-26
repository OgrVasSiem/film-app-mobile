package com.example.film_app_mobile.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.FilmDto
import com.example.film_app_mobile.data.mapper.ui

@Composable
internal fun CatalogDetailSheet(type: ContentType, film: FilmDto, dismiss: () -> Unit) {

    AppBottomSheet(dismiss) {
        AppText(film.title, style = AppTheme.typography.title)

        Poster(url = film.posterUrl, modifier = Modifier.fillMaxWidth().height(180.dp))

        AppText(metadata(film.ui(), type))

        film.rating?.let {
            AppText(stringResource(R.string.kp_rating, it.value))
            it.votes?.let { n -> AppText(stringResource(R.string.kp_votes, n)) }
        }

        film.premiereDate?.let { AppText(it) }

        AppText(film.description)

        AppText(film.genres.joinToString())

        AppText(film.actors.take(5).joinToString())

        film.director?.let { AppText(stringResource(R.string.director, it)) }
    }
}
