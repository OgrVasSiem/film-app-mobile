package com.example.film_app_mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun StreakBadge(days: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.streak_accessibility, days)
    AppText(
        stringResource(R.string.streak_badge, days),
        modifier
            .background(AppTheme.colors.goldSurface, CircleShape)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        style = AppTheme.typography.streak,
        color = AppTheme.colors.gold,
    )
}
