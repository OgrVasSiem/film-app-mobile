package com.example.film_app_mobile.feature.profile.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun DangerAction(label: String, enabled: Boolean, action: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(
                if (enabled) AppTheme.colors.wontWatch else AppTheme.colors.border,
                RoundedCornerShape(12.dp),
            )
            .clickable(
                enabled = enabled,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = action,
            )
            .heightIn(min = 48.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            label,
            style = AppTheme.typography.button,
            color = if (enabled) AppTheme.colors.onDecision else AppTheme.colors.textSecondary,
        )
    }
}
