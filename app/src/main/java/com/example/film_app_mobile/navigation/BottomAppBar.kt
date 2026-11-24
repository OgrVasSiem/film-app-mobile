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
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

private data class Tab(val page: Page, val label: Int, val icon: Int)

private val tabs =
    listOf(
        Tab(Page.Packs, R.string.collections, R.drawable.nav_collections),
        Tab(Page.Library, R.string.library_title, R.drawable.nav_library),
        Tab(Page.Profile, R.string.profile_title, R.drawable.nav_profile),
    )

@Composable
internal fun BottomAppBar(current: androidx.navigation3.runtime.NavKey, select: (Page) -> Unit) {
    val colors = AppTheme.colors
    Column(Modifier.background(colors.surface).navigationBarsPadding()) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            for (tab in tabs) {
                val selected =
                    current == tab.page || (current is Page.Deck && tab.page == Page.Packs)
                val tint = if (selected) colors.accent else colors.textSecondary
                Column(
                    Modifier.weight(1f)
                        .selectable(selected = selected, role = Role.Tab) { select(tab.page) }
                        .heightIn(min = 60.dp)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Image(
                        painterResource(tab.icon),
                        null,
                        Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(tint),
                    )
                    AppText(
                        stringResource(tab.label),
                        style = AppTheme.typography.navigation,
                        color = tint,
                    )
                }
            }
        }
    }
}
