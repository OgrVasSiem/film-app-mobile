package com.example.film_app_mobile.feature.home.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.feature.home.HomeViewModel
import com.example.film_app_mobile.feature.home.state.HomeState
import kotlin.collections.firstOrNull
import kotlin.collections.get
import kotlin.collections.orEmpty

private val card = RoundedCornerShape(16.dp)

@Composable
internal fun HomeCatalog(
    snapshot: CatalogDto,
    state: HomeState,
    vm: HomeViewModel,
    type: ContentType,
    busy: Boolean,
    labels: Map<String, Int>,
    open: (DeckSource) -> Unit,
    onLockedPack: () -> Unit,
) {
    val sections = snapshot.sections.associateBy { it.key }
    val periods = listOf("DAY", "WEEK", "MONTH").mapNotNull(sections::get)

    if (periods.isNotEmpty()) {
        AppText(text = stringResource(R.string.home_daily), style = AppTheme.typography.title)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(periods) { section ->
                HomeDaily(
                    labels = labels,
                    previewPosters = state.previewPosters,
                    open = open,
                    section = section,
                )
            }
        }
    }

    sections["FOR_YOU"]?.let { section ->
        val title = stringResource(R.string.home_evening)
        val posters = state.previewPosters[section.key].orEmpty()

        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .clip(card)
                .background(AppTheme.colors.surface)
                .clickable(enabled = section.state == "READY") { open(DeckSource.Personal) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(78.dp, 60.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.offset(x = (i * 14 + 5).dp, y = 3.dp)
                            .rotate((i - 1) * 12f)
                            .size(36.dp, 53.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AppTheme.colors.border)
                    ) {
                        Poster(
                            url = posters.getOrNull(i),
                            modifier = Modifier.fillMaxSize(),
                            scale = ContentScale.Crop,
                        )
                    }
                }
            }

            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                AppText(text = title, style = AppTheme.typography.button)

                AppText(
                    when (section.state) {
                        "READY" -> stringResource(R.string.home_count, section.availableCount)
                        "NEEDS_SELECTION" -> stringResource(R.string.need_selection_v2)
                        "NEEDS_GENRES" -> stringResource(R.string.need_genres_v2)
                        else -> stringResource(R.string.empty_list)
                    },
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }

            AppText("→", color = AppTheme.colors.accent, style = AppTheme.typography.title)
        }
    }

    if ("SINGLE" in sections) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                stringResource(R.string.home_pick),
                Modifier.weight(1f),
                style = AppTheme.typography.title,
            )
            state.undo?.let { id ->
                AppText(
                    stringResource(R.string.undo_choice),
                    Modifier.clickable(enabled = !busy) { vm.choose(type, id, null) }.padding(8.dp),
                    color = AppTheme.colors.accent,
                )
            }
        }

        state.single?.let { film ->
            Column(
                modifier =
                    Modifier.fillMaxWidth().background(AppTheme.colors.surface, card).padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { vm.details(type, film.id) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(.35f)
                                .aspectRatio(2f / 3f)
                                .clip(card)
                                .background(AppTheme.colors.border)
                    ) {
                        Poster(
                            url = film.posterUrl,
                            modifier = Modifier.fillMaxSize(),
                            scale = ContentScale.Crop,
                        )

                        film.rating?.let {
                            AppText(
                                text = "★ " + it.value,
                                modifier =
                                    Modifier.align(Alignment.TopStart)
                                        .padding(5.dp)
                                        .background(AppTheme.colors.goldSurface, card)
                                        .padding(4.dp),
                                color = AppTheme.colors.gold,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppText(film.title, style = AppTheme.typography.title)

                        AppText(
                            listOfNotNull(
                                    film.year?.toString(),
                                    film.durationMinutes?.let {
                                        stringResource(type.durationLabel, it)
                                    },
                                    film.genres.joinToString().ifBlank { null },
                                )
                                .joinToString(" · "),
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textSecondary,
                        )

                        AppText(film.description, maxLines = 4, overflow = TextOverflow.Ellipsis)

                        film.director?.let {
                            AppText(
                                stringResource(R.string.director, it),
                                style = AppTheme.typography.caption,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row {
                    for ((key, label, icon) in
                        listOf(
                            Triple("WONT_WATCH", R.string.wont_watch, R.drawable.decision_cross),
                            Triple("WATCHED", R.string.watched, R.drawable.decision_eye),
                            Triple("WILL_WATCH", R.string.will_watch, R.drawable.decision_heart),
                        )) {
                        val tint =
                            when (key) {
                                "WILL_WATCH" -> AppTheme.colors.swipeLike
                                "WONT_WATCH" -> AppTheme.colors.swipeDislike
                                else -> AppTheme.colors.accent
                            }
                        Column(
                            Modifier.weight(1f).clickable(enabled = !busy) {
                                vm.choose(type, film.id, key)
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painterResource(icon),
                                null,
                                Modifier.size(44.dp)
                                    .background(tint.copy(alpha = .16f), RoundedCornerShape(50))
                                    .padding(11.dp),
                                colorFilter = ColorFilter.tint(tint),
                            )
                            AppText(stringResource(label), style = AppTheme.typography.caption)
                        }
                    }
                }
            }
        } ?: AppText(stringResource(R.string.empty_list))
    }

    if ("EDITORIAL" in sections) {
        AppText(
            stringResource(R.string.editorial_recommendations),
            style = AppTheme.typography.title,
        )

        for ((id, title, description, premium, locked, coverUrl, isCompleted, availableCount) in
            snapshot.packs) {
            Box(
                Modifier.fillMaxWidth().height(148.dp).clip(card).clickable {
                    if (locked) onLockedPack() else open(DeckSource.Editorial(id))
                }
            ) {
                PackCover(snapshot.covers[coverUrl], Modifier.fillMaxSize(), isCompleted)

                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(AppTheme.colors.transparent, AppTheme.colors.posterScrim)
                                )
                            )
                )

                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    AppText(
                        if (premium)
                            stringResource(
                                if (locked) R.string.locked_premium else R.string.open_premium
                            )
                        else stringResource(R.string.home_count, availableCount),
                        color = if (premium) AppTheme.colors.gold else AppTheme.colors.onPoster,
                    )
                    AppText(
                        title,
                        style = AppTheme.typography.title,
                        color = AppTheme.colors.onPoster,
                    )
                    AppText(
                        if (isCompleted) stringResource(R.string.completed_v2) else description,
                        maxLines = 2,
                        color = AppTheme.colors.onPosterSecondary,
                        style = AppTheme.typography.caption,
                    )
                }
            }
        }
        if (snapshot.packs.isEmpty()) AppText(stringResource(R.string.empty_list))
    }
}

@Composable
fun HomeDaily(
    labels: Map<String, Int>,
    previewPosters: Map<String, List<String?>> = emptyMap(),
    open: (DeckSource) -> Unit,
    section: CatalogSection,
) {
    val title = stringResource(labels.getValue(section.key))
    val posters = previewPosters[section.key].orEmpty()

    Box(
        Modifier.width(240.dp)
            .height(160.dp)
            .clip(card)
            .background(AppTheme.colors.surface)
            .clickable(enabled = section.state == "READY") {
                open(DeckSource.Periodic(Period.valueOf(section.key)))
            }
    ) {
        Poster(
            url = posters.firstOrNull(),
            modifier = Modifier.fillMaxSize(),
            scale = ContentScale.Crop,
        )

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(AppTheme.colors.transparent, AppTheme.colors.posterScrim)
                        )
                    )
        )

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            AppText(
                stringResource(R.string.home_count, section.availableCount),
                color = AppTheme.colors.onPoster,
                style = AppTheme.typography.caption,
            )

            AppText(title, color = AppTheme.colors.onPoster, style = AppTheme.typography.title)
        }
    }
}
