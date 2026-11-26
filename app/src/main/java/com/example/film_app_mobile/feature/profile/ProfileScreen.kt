package com.example.film_app_mobile.feature.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.MainViewModel
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.dataStore.AppThemeMode
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.profile.components.DangerAction

@Composable
internal fun ProfileRoute(
    vm: ProfileViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(vm) {
        vm.open()
        onPauseOrDispose {}
    }
    fun closeDialog() {
        if (!vm.request.status.busy) {
            vm.request.clearError()
            dialog = null
        }
    }
    val request = vm.request.status
    val themeMode by
        mainViewModel.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            ui.profile?.let { profile ->
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (profile.passwordProtected)
                        DangerAction(stringResource(R.string.logout), !request.busy, vm::logout)
                    DangerAction(stringResource(R.string.delete_account), !request.busy) {
                        dialog = "delete"
                    }
                }
            }
        },
        topBar = { AppTopBar(stringResource(R.string.profile_title)) },
    ) { padding ->
        if (request.busy && ui.profile == null) {
            InitialLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RequestFeedback(request)

            ui.profile?.let { profile ->
                Column(
                    Modifier.fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    AppTheme.colors.accent.copy(alpha = .40f),
                                    AppTheme.colors.surface,
                                )
                            ),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(
                            profile.login,
                            Modifier.weight(1f, fill = false),
                            style = AppTheme.typography.accountTitle,
                        )
                        androidx.compose.material3.IconButton(
                            enabled = !request.busy,
                            onClick = {
                                vm.editLogin(profile.login)
                                dialog = "login"
                            },
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.ui.res.painterResource(R.drawable.edit_login),
                                stringResource(R.string.rename_login),
                                Modifier.size(32.dp)
                                    .background(AppTheme.colors.accent, CircleShape)
                                    .padding(7.dp),
                                tint = AppTheme.colors.onAccent,
                            )
                        }
                    }
                    ui.hasPremium?.let { active ->
                        AppText(
                            stringResource(
                                if (active) R.string.premium_active else R.string.premium_inactive
                            ),
                            style = AppTheme.typography.button,
                        )
                    }
                    StreakBadge(profile.streakDays)
                }
                if (!profile.passwordProtected) {
                    Action(stringResource(R.string.protect_profile), !request.busy) {
                        dialog = "password"
                    }
                } else AppText(stringResource(R.string.protected_account))

                if (dialog != null)
                    AppBottomSheet(::closeDialog) {
                        when (dialog) {
                            "password" ->
                                ProtectProfileForm(request.busy) {
                                    vm.protect(it) { dialog = null }
                                }
                            "login" -> {
                                AppText(stringResource(R.string.login_rules))
                                AccountField(
                                    stringResource(R.string.username),
                                    ui.login,
                                    vm::editLogin,
                                )
                                ui.availability?.let { AppText(stringResource(it)) }
                                Action(
                                    stringResource(R.string.save_changes),
                                    !request.busy && vm.validLogin,
                                ) {
                                    vm.rename { dialog = null }
                                }
                            }
                            "delete" -> {
                                AppText(stringResource(R.string.delete_warning))
                                DangerAction(
                                    stringResource(R.string.delete_account),
                                    !request.busy,
                                    vm::delete,
                                )
                            }
                        }
                        RequestFeedback(request)
                        Link(stringResource(R.string.cancel), !request.busy, ::closeDialog)
                    }
            }

            if (request.error != null)
                Link(
                    label = stringResource(R.string.retry),
                    enabled = !request.busy,
                    action = vm.request::retry,
                )

            Row(
                Modifier.fillMaxWidth()
                    .background(AppTheme.colors.surface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(stringResource(R.string.dark_theme), Modifier.weight(1f))

                Switch(
                    checked = themeMode == AppThemeMode.DARK,
                    onCheckedChange = {
                        if (themeMode == AppThemeMode.DARK) {
                            mainViewModel.setTheme(AppThemeMode.LIGHT)
                        } else {
                            mainViewModel.setTheme(AppThemeMode.DARK)
                        }
                    },
                )
            }
        }
    }
}
