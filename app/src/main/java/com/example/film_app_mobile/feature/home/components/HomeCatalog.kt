package com.example.film_app_mobile.feature.home.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.feature.home.HomeViewModel
import java.io.IOException
import kotlin.collections.get

private val card = RoundedCornerShape(16.dp)

@Composable
internal fun HomeCatalog(
    snapshot: CachedCatalog,
    vm: HomeViewModel,
    type: ContentType,
    busy: Boolean,
    labels: Map<String, Int>,
    open: (DeckSource) -> Unit,
    onLockedPack: () -> Unit,
) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    val sections = snapshot.catalog.sections.associateBy { it.key }
    val periods = listOf("DAY", "WEEK", "MONTH").mapNotNull(sections::get)
    val expanded = ui.periodsExpanded

    if (periods.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                stringResource(R.string.home_daily),
                Modifier.weight(1f),
                style = AppTheme.typography.title,
            )

            AppText(
                stringResource(if (expanded) R.string.home_fold else R.string.home_all),
                Modifier.clickable(onClick = vm::togglePeriods).padding(8.dp),
                color = AppTheme.colors.accent,
            )
        }
        val tile: @Composable (CatalogSection) -> Unit = { section ->
            val title = stringResource(labels.getValue(section.key))
            val posters = preview(vm, type, section, snapshot.etag)
            Box(
                Modifier.width(if (expanded) 340.dp else 240.dp)
                    .height(160.dp)
                    .clip(card)
                    .background(AppTheme.colors.surface)
                    .clickable(enabled = section.state == "READY") {
                        open(DeckSource.Periodic(Period.valueOf(section.key)))
                    }
            ) {
                Poster(posters.firstOrNull(), Modifier.fillMaxSize(), ContentScale.Crop)

                Shade()

                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    AppText(
                        stringResource(R.string.home_count, section.availableCount),
                        color = AppTheme.colors.onPoster,
                        style = AppTheme.typography.caption,
                    )
                    AppText(
                        title,
                        color = AppTheme.colors.onPoster,
                        style = AppTheme.typography.title,
                    )
                }
            }
        }

        if (expanded) periods.forEach { tile(it) }
        else
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(periods.size) { tile(periods[it]) }
            }
    }

    sections["FOR_YOU"]?.let { section ->
        val title = stringResource(R.string.home_evening)
        val posters = preview(vm, type, section, snapshot.etag)
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .clip(card)
                .background(AppTheme.colors.surface)
                .clickable(enabled = section.state == "READY") { open(DeckSource.Personal) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(78.dp, 60.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.offset(x = (i * 14 + 5).dp, y = 3.dp)
                            .rotate((i - 1) * 12f)
                            .size(36.dp, 53.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AppTheme.colors.border)
                    ) {
                        Poster(posters.getOrNull(i), Modifier.fillMaxSize(), ContentScale.Crop)
                    }
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                AppText(title, style = AppTheme.typography.button)
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
            ui.undo?.let { id ->
                AppText(
                    stringResource(R.string.undo_choice),
                    Modifier.clickable(enabled = !busy) { vm.choose(type, id, null) }.padding(8.dp),
                    color = AppTheme.colors.accent,
                )
            }
        }
        ui.single?.let { film ->
            Column(
                Modifier.fillMaxWidth().background(AppTheme.colors.surface, card).padding(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { vm.details(type, film.id) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth(.35f)
                            .aspectRatio(2f / 3f)
                            .clip(card)
                            .background(AppTheme.colors.border)
                    ) {
                        Poster(film.posterUrl, Modifier.fillMaxSize(), ContentScale.Crop)
                        film.rating?.let {
                            AppText(
                                "★ " + it.value,
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

        for (pack in snapshot.catalog.packs) {
            Box(
                Modifier.fillMaxWidth().height(148.dp).clip(card).clickable {
                    if (pack.locked) onLockedPack() else open(DeckSource.Editorial(pack.id))
                }
            ) {
                PackCover(snapshot.hashes[pack.coverUrl], Modifier.fillMaxSize(), pack.isCompleted)
                Shade()
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    AppText(
                        if (pack.premium)
                            stringResource(
                                if (pack.locked) R.string.locked_premium else R.string.open_premium
                            )
                        else stringResource(R.string.home_count, pack.availableCount),
                        color = if (pack.premium) AppTheme.colors.gold else AppTheme.colors.onPoster,
                    )
                    AppText(
                        pack.title,
                        style = AppTheme.typography.title,
                        color = AppTheme.colors.onPoster,
                    )
                    AppText(
                        if (pack.isCompleted) stringResource(R.string.completed_v2)
                        else pack.description,
                        maxLines = 2,
                        color = AppTheme.colors.onPosterSecondary,
                        style = AppTheme.typography.caption,
                    )
                }
            }
        }
        if (snapshot.catalog.packs.isEmpty()) AppText(stringResource(R.string.empty_list))
    }
}

@Composable
private fun Shade() {
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppTheme.colors.transparent, AppTheme.colors.posterScrim)
                )
            )
    )
}

@Composable
private fun preview(
    vm: HomeViewModel,
    type: ContentType,
    section: CatalogSection,
    etag: String?,
): List<String?> {
    val urls by
        produceState(emptyList(), type, section, etag) {
            value =
                if (section.state == "READY")
                    try {
                        vm.preview(type, section.key)
                    } catch (_: IOException) {
                        emptyList()
                    }
                else emptyList()
        }
    return urls
}
