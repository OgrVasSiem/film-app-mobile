package com.example.film_app_mobile.feature.library.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
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
    openList: (String) -> Unit,
    openDetail: (Long) -> Unit,
) {
    for ((key, label) in folders) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = stringResource(label),
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.title,
            )

            AppText(
                text = stringResource(R.string.see_all),
                modifier = Modifier.clickable { openList(key) }.padding(8.dp),
                style = AppTheme.typography.button,
                color = AppTheme.colors.accent,
            )
        }

        val items = state.sections[key].orEmpty()

        if (items.isEmpty() && !state.request.busy && state.request.error == null)
            AppText(text = stringResource(R.string.empty_list), style = AppTheme.typography.caption)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.size, key = { items[it].id }) { index ->
                val movie = items[index]
                Column(
                    Modifier.width(102.dp)
                        .semantics { contentDescription = movie.title }
                        .clickable { openDetail(movie.id) }
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(10.dp))
                    ) {
                        Poster(
                            url = movie.posterUrl,
                            modifier = Modifier.fillMaxSize(),
                            scale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}
