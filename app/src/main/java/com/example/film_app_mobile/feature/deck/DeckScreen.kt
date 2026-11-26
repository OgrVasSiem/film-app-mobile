package com.example.film_app_mobile.feature.deck

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.feature.deck.components.DeckCarousel
import com.example.film_app_mobile.feature.deck.state.DeckState
import kotlinx.coroutines.flow.drop

@Composable
internal fun DeckRoute(
    deck: DeckSource,
    back: () -> Unit,
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    if (state.contentType == null) {
        InitialLoading()
    } else {
        key(state.contentType) {
            DeckScreen(deck = deck, back = back, viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun DeckScreen(
    deck: DeckSource,
    back: () -> Unit,
    viewModel: DeckViewModel,
    state: DeckState,
) {
    val type = state.contentType ?: return
    val busy = state.request.busy

    LifecycleResumeEffect(viewModel, type) {
        viewModel.load(type, deck)
        onPauseOrDispose {}
    }

    LaunchedEffect(viewModel, type) {
        viewModel.changes.drop(1).collect { viewModel.load(type, deck) }
    }

    state.detail?.let { CatalogDetailSheet(type = type, film = it) { viewModel.dismissDetails() } }

    CatalogDeadline(
        state.catalog
            ?.sections
            ?.firstOrNull { (deck as? DeckSource.Periodic)?.period?.name == it.key }
            ?.nextRefreshAt,
        { state.request.busy },
    ) {
        viewModel.load(type = type, deck = deck)
    }

    val visibleCards = remember(state.cards) { state.cards.take(3).map { it.ui() } }
    val downward = state.downwardProgress
    val drag = state.horizontalProgress
    val background =
        lerp(
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
                            state.catalog?.packs?.find { it.id == deck.packId }?.title.orEmpty()

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
                                stringResource(R.string.session_decisions, state.sessionDecisions),
                                if (state.cards.isNotEmpty())
                                    stringResource(
                                        R.string.deck_progress,
                                        state.sessionDecisions + 1,
                                        state.sessionDecisions + state.cards.size,
                                    )
                                else null,
                            )
                            .joinToString(" · "),
                    action = {
                        state.undo?.let { id ->
                            AppText(
                                stringResource(R.string.undo_choice),
                                Modifier.clickable(enabled = !busy) {
                                        viewModel.choose(type, deck, id, null)
                                    }
                                    .padding(8.dp),
                            )
                        }
                    },
                    back = back,
                )
            }
        },
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            RequestFeedback(state.request)

            if (state.request.error != null)
                Link(
                    label = stringResource(R.string.retry),
                    enabled = true,
                    action = viewModel.request::retry,
                )

            if (state.cards.isNotEmpty()) {
                DeckCarousel(
                    cards = visibleCards,
                    busy = busy,
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("v2-deck"),
                    type = type,
                    downwardProgress = viewModel::downwardProgress,
                    progress = viewModel::horizontalProgress,
                    openDetail = { viewModel.details(type, it) },
                ) { f, target, reject ->
                    viewModel.choose(
                        type = type,
                        deck = deck,
                        id = f.id,
                        target = target,
                        reject = reject,
                    )
                }

                if (
                    state.cards.size <= 3 &&
                        state.cursor != null &&
                        !busy &&
                        state.request.error == null
                ) {
                    SideEffect { viewModel.more(type, deck) }
                }
            } else if (!busy)
                AppText(
                    stringResource(
                        when (state.deckState) {
                            "NEEDS_SELECTION" -> R.string.need_selection_v2
                            "NEEDS_GENRES" -> R.string.need_genres_v2
                            else -> R.string.empty_list
                        }
                    )
                )
        }
    }
}
