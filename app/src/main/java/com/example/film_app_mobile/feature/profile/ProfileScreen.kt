package com.example.film_app_mobile.feature.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun ProfileRoute(
    dark: Boolean,
    changeTheme: (Boolean) -> Unit,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val colors = AppTheme.colors
    val request = vm.request.status
    ScreenContent(request, stringResource(R.string.profile_title)) {
        vm.profile?.let { profile ->
            Row(
                Modifier.fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(48.dp).background(colors.border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        profile.login.take(1).uppercase(),
                        style = AppTheme.typography.title,
                        color = colors.accent,
                    )
                }
                Column {
                    AppText(profile.login, style = AppTheme.typography.button)
                    AppText(
                        stringResource(
                            if (profile.premium) R.string.premium_active
                            else R.string.premium_inactive
                        ),
                        style = AppTheme.typography.caption,
                        color = if (profile.premium) colors.accent else colors.textSecondary,
                    )
                }
            }
        }
        if (vm.profile == null && request.error != null)
            Link(stringResource(R.string.retry), !request.busy, vm::load)
        Row(
            Modifier.fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .toggleable(value = dark, role = Role.Switch, onValueChange = changeTheme)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(stringResource(R.string.dark_theme), Modifier.weight(1f))
            Box(
                Modifier.size(40.dp, 24.dp)
                    .background(if (dark) colors.accent else colors.border, CircleShape)
                    .padding(3.dp),
                contentAlignment = if (dark) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Spacer(
                    Modifier.size(18.dp)
                        .background(
                            if (dark) colors.onAccent else colors.textSecondary,
                            CircleShape,
                        )
                )
            }
        }
        Link(stringResource(R.string.logout), !request.busy, vm::logout)
    }
}
