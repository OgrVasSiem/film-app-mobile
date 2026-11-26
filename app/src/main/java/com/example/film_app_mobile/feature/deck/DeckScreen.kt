package com.example.film_app_mobile.feature.deck

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.feature.deck.components.DeckCarousel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first

@Composable
internal fun DeckRoute(
    deck: DeckSource,
    back: () -> Unit,
    selection: ContentSelectionViewModel = hiltViewModel(),
) {
    val selected by selection.selection.collectAsStateWithLifecycle(initialValue = null)
    selected?.let { type -> key(type) { DeckScreen(type, deck, back) } }
}

@Composable
internal fun DeckScreen(
    type: ContentType,
    deck: DeckSource,
    back: () -> Unit,
    vm: DeckViewModel = hiltViewModel(key = type.name),
) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    val busy = vm.request.status.busy
    androidx.lifecycle.compose.LifecycleResumeEffect(vm) {
        vm.load(type, deck)
        onPauseOrDispose {}
    }
    LaunchedEffect(vm) { vm.changes.drop(1).collect { vm.load(type, deck) } }
    ui.detail?.let { CatalogDetailSheet(type, it) { vm.dismissDetails() } }
    CatalogDeadline(
        ui.catalog
            ?.catalog
            ?.sections
            ?.firstOrNull { (deck as? DeckSource.Periodic)?.period?.name == it.key }
            ?.nextRefreshAt,
        { vm.request.status.busy },
    ) {
        vm.load(type, deck)
    }
    val visibleCards = remember(ui.cards) { ui.cards.take(3).map { it.entity(0).ui() } }
    var downward by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableFloatStateOf(0f) }
    val background =
        androidx.compose.ui.graphics.lerp(
            AppTheme.colors.background,
            if (downward > 0f) AppTheme.colors.watched
            else if (drag > 0f) AppTheme.colors.swipeLike else AppTheme.colors.swipeDislike,
            maxOf(downward, kotlin.math.abs(drag)).coerceIn(0f, 1f),
        )
    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Box(Modifier.background(background).statusBarsPadding()) {
                AppTopBar(
                    when (deck) {
                        DeckSource.Personal -> stringResource(R.string.home_evening)
                        is DeckSource.Editorial ->
                            ui.catalog
                                ?.catalog
                                ?.packs
                                ?.find { it.id == deck.packId }
                                ?.title
                                .orEmpty()
                        is DeckSource.Periodic ->
                            stringResource(
                                when (deck.period) {
                                    Period.DAY -> R.string.day_v2
                                    Period.WEEK -> R.string.week_v2
                                    Period.MONTH -> R.string.month_v2
                                }
                            )
                    },
                    background = background,
                    subtitle =
                        listOfNotNull(
                                stringResource(R.string.session_decisions, ui.sessionDecisions),
                                if (ui.cards.isNotEmpty())
                                    stringResource(
                                        R.string.deck_progress,
                                        ui.sessionDecisions + 1,
                                        ui.sessionDecisions + ui.cards.size,
                                    )
                                else null,
                            )
                            .joinToString(" · "),
                    action = {
                        ui.undo?.let { id ->
                            AppText(
                                stringResource(R.string.undo_choice),
                                Modifier.clickable(enabled = !busy) {
                                        vm.choose(type, deck, id, null)
                                    }
                                    .padding(8.dp),
                            )
                        }
                    },
                    back = { AppText("‹", Modifier.clickable(onClick = back).padding(12.dp)) },
                )
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            RequestFeedback(vm.request.status)
            if (vm.request.status.error != null)
                Link(stringResource(R.string.retry), true, vm.request::retry)
            if (ui.cards.isNotEmpty()) {
                val film = ui.cards.first()
                DeckCarousel(
                    visibleCards,
                    busy,
                    Modifier.fillMaxWidth().weight(1f).testTag("v2-deck"),
                    type = type,
                    downwardProgress = { downward = it },
                    progress = { drag = it },
                    openDetail = { vm.details(type, it) },
                ) { f, target, reject ->
                    vm.choose(type, deck, f.id, target, reject)
                }
                if (
                    ui.cards.size <= 3 &&
                        ui.cursor != null &&
                        !busy &&
                        vm.request.status.error == null
                )
                    SideEffect { vm.more(type, deck) }
            } else if (!busy)
                AppText(
                    stringResource(
                        when (ui.deckState) {
                            "NEEDS_SELECTION" -> R.string.need_selection_v2
                            "NEEDS_GENRES" -> R.string.need_genres_v2
                            else -> R.string.empty_list
                        }
                    )
                )
        }
    }
}
