package com.example.film_app_mobile.feature.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.components.ScreenContent
import com.example.film_app_mobile.core.designsystem.component.Action
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.*

private val folders =
    linkedMapOf(
        "WILL_WATCH" to R.string.will_watch,
        "WONT_WATCH" to R.string.wont_watch,
        "WATCHED" to R.string.watched,
    )

@Composable
internal fun LibraryScreen(
    state: LibraryState,
    busy: Boolean,
    error: Int?,
    library: (String) -> Unit,
    move: (Film?) -> Unit,
    choose: (Film, String) -> Unit,
    reset: (Film) -> Unit,
    openPacks: () -> Unit,
) {
    val c = AppTheme.colors
    val (cards, folder, moving) = state
    var detailId by
        androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Long?>(null) }
    cards.firstOrNull { it.id == detailId }?.let { FilmDetailSheet(it) { detailId = null } }
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for ((status, label) in folders) Chip(stringResource(label), status == folder, !busy) {
            library(status)
        }
    }
    if (cards.isEmpty()) {
        AppText(stringResource(R.string.empty_list), style = AppTheme.typography.title)
        AppText(stringResource(R.string.empty_hint))
        Action(stringResource(R.string.select_pack), !busy) { openPacks() }
    }
    for (movie in cards) Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .clickable { detailId = movie.id }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(58.dp, 86.dp).clip(RoundedCornerShape(6.dp))) { MovieArt(movie) }
        Column(Modifier.weight(1f)) {
            AppText(movie.title, style = AppTheme.typography.button)
            AppText(metadata(movie), style = AppTheme.typography.caption)
            Box(Modifier.align(androidx.compose.ui.Alignment.End).widthIn(max = 128.dp)) {
                Link(stringResource(R.string.move), !busy) { move(movie) }
            }
        }
    }

    moving?.let { movie ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { if (!busy) move(null) }) {
            Column(
                Modifier.background(c.surface, RoundedCornerShape(20.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppText(stringResource(R.string.move_movie), style = AppTheme.typography.title)
                AppText(movie.title)
                for ((status, label) in folders) if (status != folder)
                    Action(stringResource(label), !busy) { choose(movie, status) }
                Link(stringResource(R.string.reset_decision), !busy) { reset(movie) }
                if (error != null) AppText(stringResource(error))
                Link(stringResource(R.string.cancel), !busy) { move(null) }
            }
        }
    }
}

@Composable
internal fun LibraryRoute(packs: () -> Unit, vm: LibraryViewModel = hiltViewModel()) {
    val request = vm.request.status
    ScreenContent(request, stringResource(R.string.library_title)) {
        LibraryScreen(
            vm.state,
            request.busy,
            request.error,
            vm::load,
            vm::move,
            vm::choose,
            vm::reset,
            packs,
        )
    }
}
