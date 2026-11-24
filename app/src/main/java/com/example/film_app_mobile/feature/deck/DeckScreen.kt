package com.example.film_app_mobile.feature.deck

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.Action
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.completion.*
import com.example.film_app_mobile.ui.model.*
import com.example.film_app_mobile.ui.model.Pack
import kotlinx.coroutines.*

@Composable
internal fun ColumnScope.DeckScreen(
    state: DeckState,
    busy: Boolean,
    choose: (Film, String, () -> Unit) -> Unit,
) {
    val c = AppTheme.colors
    val cards = state.cards
    val total = state.total
    if (cards.isEmpty()) return
    AppText(
        stringResource(R.string.deck_progress, total - cards.size + 1, total),
        style = AppTheme.typography.caption,
        color = c.accent,
    )
    Canvas(Modifier.fillMaxWidth().height(3.dp)) {
        val count = total.coerceAtLeast(cards.size)
        drawRect(c.border)
        drawRect(c.accent, size = size.copy(width = size.width * (count - cards.size + 1) / count))
    }
    DeckCarousel(cards, busy, Modifier.fillMaxWidth().weight(1f), choose)
}

@Composable
internal fun DeckRoute(pack: Pack, library: () -> Unit, packs: () -> Unit) {
    val vm =
        hiltViewModel<DeckViewModel, DeckViewModel.Factory>(creationCallback = { it.create(pack) })
    val state = vm.state
    val request = vm.request.status
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val back = stringResource(R.string.back_to_packs)
            Box(
                Modifier.size(48.dp).clickable(onClick = packs).semantics {
                    contentDescription = back
                },
                contentAlignment = Alignment.Center,
            ) {
                val tint = AppTheme.colors.textPrimary
                Canvas(Modifier.size(24.dp)) {
                    val left = androidx.compose.ui.geometry.Offset(size.width * .18f, center.y)
                    drawLine(tint, left, center.copy(x = size.width * .85f), 2.dp.toPx())
                    drawLine(tint, left, center.copy(y = size.height * .18f), 2.dp.toPx())
                    drawLine(tint, left, center.copy(y = size.height * .82f), 2.dp.toPx())
                }
            }
            AppText(pack.title, style = AppTheme.typography.title, maxLines = 2)
        }
        if (request.busy && state.cards.isEmpty()) AppText(stringResource(R.string.loading))
        request.error?.let { AppText(stringResource(it)) }
        Link(stringResource(R.string.refresh_deck), !request.busy, vm::restart)
        if (request.error != null) Action(stringResource(R.string.retry), action = vm::retry)
        if (state.loaded && state.nextCursor == null && state.cards.isEmpty()) {
            CompletionScreen(CompletionState(state.saved, state.skipped), library, packs)
        } else {
            DeckScreen(state, request.busy, vm::choose)
        }
    }
}
