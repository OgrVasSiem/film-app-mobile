package com.example.film_app_mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
fun Action(label: String, enabled: Boolean = true, action: () -> Unit) {
    val colors = AppTheme.colors
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(if (enabled) colors.accent else colors.border)
        .clickable(enabled = enabled, role = Role.Button, onClick = action)
        .heightIn(min = 48.dp).padding(10.dp), contentAlignment = Alignment.Center) {
        AppText(label, style = AppTheme.typography.button, color = if (enabled) colors.onAccent else colors.textSecondary)
    }
}