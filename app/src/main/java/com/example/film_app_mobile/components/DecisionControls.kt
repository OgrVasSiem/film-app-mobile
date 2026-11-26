package com.example.film_app_mobile.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.ui.model.Film
import kotlinx.coroutines.delay

internal data class SavedChoice(val film: Film, val status: String)

@Composable
internal fun DecisionControls(
    enabled: Boolean,
    progress: Float = 0f,
    large: Boolean = false,
    exclude: String? = null,
    decide: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = if (large) 20.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(if (large) 2.dp else 4.dp, Alignment.CenterHorizontally),
    ) {
        for ((status, icon, label) in
            listOf(
                Triple("WONT_WATCH", R.drawable.decision_cross, R.string.wont_watch),
                Triple("WILL_WATCH", R.drawable.decision_heart, R.string.will_watch),
                Triple("WATCHED", R.drawable.decision_eye, R.string.watched),
            )) {
            if (status == exclude) continue
            val tint =
                when (status) {
                    "WILL_WATCH" -> AppTheme.colors.willWatch
                    "WONT_WATCH" -> AppTheme.colors.wontWatch
                    else -> AppTheme.colors.watched
                }
            val active =
                if (status == "WILL_WATCH") progress.coerceAtLeast(0f)
                else if (status == "WONT_WATCH") (-progress).coerceAtLeast(0f) else 0f
            Box(
                Modifier.size(
                        if (large && status == "WATCHED") 66.dp else if (large) 96.dp else 48.dp
                    )
                    .clickable(
                        enabled = enabled,
                        role = androidx.compose.ui.semantics.Role.Button,
                    ) {
                        decide(status)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(
                            if (large && status == "WATCHED") 64.dp
                            else if (large) (80 + 16 * (active * 3).coerceAtMost(1f)).dp
                            else if (status == "WATCHED") 32.dp
                            else (40 + 8 * (active * 3).coerceAtMost(1f)).dp
                        )
                        .background(tint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(icon),
                        stringResource(label),
                        modifier = Modifier.size(if (large) 40.dp else 24.dp),
                        colorFilter =
                            ColorFilter.tint(
                                if (status == "WATCHED") AppTheme.colors.onWatched
                                else AppTheme.colors.onDecision
                            ),
                    )
                }
            }
        }
    }
}
