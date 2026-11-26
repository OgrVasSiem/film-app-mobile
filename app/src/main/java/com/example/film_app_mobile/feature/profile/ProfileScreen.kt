package com.example.film_app_mobile.feature.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.dataStore.AppThemeMode
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.profile.components.DangerAction
import com.example.film_app_mobile.feature.profile.state.ProfileDialog
import com.example.film_app_mobile.feature.profile.state.ProfileState

@Composable
internal fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.open()
        onPauseOrDispose {}
    }

    ProfileScreenContent(
        state = state,
        viewModel = viewModel
    )
}

@Composable
private fun ProfileScreenContent(
    state: ProfileState,
    viewModel: ProfileViewModel
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            state.profile?.let { profile ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (profile.passwordProtected) {
                        DangerAction(
                            stringResource(R.string.logout),
                            !state.request.busy,
                            viewModel::logout
                        )
                    }

                    DangerAction(stringResource(R.string.delete_account), !state.request.busy) {
                        viewModel.showDialog(ProfileDialog.Delete)
                    }
                }
            }
        },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
                back = {}
            )
        },
    ) { padding ->
        if (state.request.busy && state.profile == null) {
            InitialLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RequestFeedback(status = state.request)

            state.profile?.let { profile ->
                Column(
                    Modifier
                        .fillMaxWidth()
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
                        IconButton(
                            enabled = !state.request.busy,
                            onClick = { viewModel.showDialog(ProfileDialog.Rename) },
                        ) {
                            Icon(
                                androidx.compose.ui.res.painterResource(R.drawable.edit_login),
                                stringResource(R.string.rename_login),
                                Modifier
                                    .size(32.dp)
                                    .background(AppTheme.colors.accent, CircleShape)
                                    .padding(7.dp),
                                tint = AppTheme.colors.onAccent,
                            )
                        }
                    }
                    state.hasPremium?.let { active ->
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
                    Action(stringResource(R.string.protect_profile), !state.request.busy) {
                        viewModel.showDialog(ProfileDialog.Password)
                    }
                } else AppText(stringResource(R.string.protected_account))

                state.dialog?.let { dialog ->
                    AppBottomSheet(viewModel::closeDialog) {
                        when (dialog) {
                            ProfileDialog.Password -> ProtectProfileForm(
                                state.request.busy,
                                viewModel::protect
                            )

                            ProfileDialog.Rename -> {
                                AppText(stringResource(R.string.login_rules))

                                AccountField(
                                    stringResource(R.string.username),
                                    state.login,
                                    viewModel::editLogin,
                                )

                                state.availability?.let { AppText(stringResource(it)) }

                                Action(
                                    stringResource(R.string.save_changes),
                                    !state.request.busy && state.validLogin,
                                    viewModel::rename,
                                )
                            }

                            ProfileDialog.Delete -> {
                                AppText(stringResource(R.string.delete_warning))
                                DangerAction(
                                    stringResource(R.string.delete_account),
                                    !state.request.busy,
                                    viewModel::delete,
                                )
                            }
                        }
                        RequestFeedback(state.request)
                        Link(stringResource(R.string.cancel), !state.request.busy, viewModel::closeDialog)
                    }
                }
            }

            if (state.request.error != null)
                Link(
                    label = stringResource(R.string.retry),
                    enabled = !state.request.busy,
                    action = viewModel.request::retry,
                )

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(stringResource(R.string.dark_theme), Modifier.weight(1f))

                Switch(
                    checked = state.themeMode == AppThemeMode.DARK,
                    onCheckedChange = viewModel::setTheme,
                )
            }
        }
    }
}
