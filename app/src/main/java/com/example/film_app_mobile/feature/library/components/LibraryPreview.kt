package com.example.film_app_mobile.feature.library.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.components.FilmDetailSheet
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.feature.library.state.LibraryState
import com.example.film_app_mobile.ui.model.*

private val folders =
    linkedMapOf("WILL_WATCH" to R.string.will_watch, "WATCHED" to R.string.watched)

@Composable
internal fun LibraryPreview(
    state: LibraryState,
    busy: Boolean,
    error: Int?,
    openList: (String) -> Unit,
    openDetail: ((Long) -> Unit)? = null,
) {
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    state.sections.values
        .flatten()
        .firstOrNull { it.id == detailId }
        ?.let { FilmDetailSheet(it) { detailId = null } }
    for ((key, label) in folders) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            AppText(stringResource(label), Modifier.weight(1f), style = AppTheme.typography.title)
            AppText(
                stringResource(R.string.see_all),
                Modifier.clickable { openList(key) }.padding(8.dp),
                style = AppTheme.typography.button,
                color = AppTheme.colors.accent,
            )
        }
        val items = state.sections[key].orEmpty()
        if (items.isEmpty() && !busy && error == null)
            AppText(stringResource(R.string.empty_list), style = AppTheme.typography.caption)
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items.size, key = { items[it].id }) { index ->
                val movie = items[index]
                Column(
                    Modifier.width(102.dp)
                        .semantics { contentDescription = movie.title }
                        .clickable {
                            if (openDetail != null) openDetail(movie.id) else detailId = movie.id
                        }
                ) {
                    Box(Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(10.dp))) {
                        Poster(
                            movie.posterUrl,
                            Modifier.fillMaxSize(),
                            androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}
