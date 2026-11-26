package com.example.film_app_mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.ContentType

@Composable
internal fun ContentTypeSwitch(type: ContentType, switch: (ContentType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(AppTheme.colors.surface, RoundedCornerShape(50))
            .padding(8.dp)
    ) {
        for (option in ContentType.entries) {
            val active = option == type
            val color = if (active) AppTheme.colors.onAccent else AppTheme.colors.textSecondary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) AppTheme.colors.accent else AppTheme.colors.surface)
                    .selectable(active, role = Role.Tab) { switch(option) }
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(option.icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )

                AppText(
                    text = stringResource(option.value),
                    style = AppTheme.typography.button,
                    color = color,
                )
            }
        }
    }
}
