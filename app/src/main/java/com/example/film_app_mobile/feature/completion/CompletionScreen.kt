package com.example.film_app_mobile.feature.completion

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.Action
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.*

@Composable
internal fun CompletionScreen(state: CompletionState, library: () -> Unit, packs: () -> Unit) {
    val c = AppTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Chip(stringResource(R.string.completed), true)
        AppText(stringResource(R.string.deck_empty), style = AppTheme.typography.title)
        AppText(stringResource(R.string.summary, state.saved, state.skipped))
        Action(stringResource(R.string.open_watchlist), action = library)
        Link(stringResource(R.string.back_to_packs), true, packs)
    }
}
