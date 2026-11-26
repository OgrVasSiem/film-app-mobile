package com.example.film_app_mobile.navigation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.Tabs

@Composable
internal fun BottomAppBar(
    current: NavKey,
    select: (Page) -> Unit
) {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.surface)
            .navigationBarsPadding()
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppTheme.colors.border)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            for (tab in Tabs.entries) {
                val page = tab.page
                val label = tab.label
                val icon = tab.icon

                val selected = current == page || (current is Page.Folder && page == Page.Library)

                val tint = if (selected) AppTheme.colors.accent else AppTheme.colors.textSecondary
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.Tab) { select(page) }
                        .heightIn(min = 60.dp)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Image(
                        painterResource(icon),
                        null,
                        Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(tint),
                    )

                    AppText(
                        stringResource(label),
                        style = AppTheme.typography.navigation,
                        color = tint,
                    )
                }
            }
        }
    }
}
