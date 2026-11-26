package com.example.film_app_mobile.feature.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.auth.state.AuthState

@Composable
internal fun AuthRoute(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    AuthScreenContent(
        state = state,
        viewModel = viewModel
    )
}

@Composable
private fun AuthScreenContent(
    state: AuthState,
    viewModel: AuthViewModel
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RequestFeedback(status = state.request)

            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.CenterHorizontally),
            )
            if (state.existingAccount) {
                AccountField(
                    label = stringResource(R.string.username),
                    value = state.login,
                    change = viewModel::login
                )

                AccountField(
                    label = stringResource(R.string.password),
                    value = state.password,
                    change = viewModel::password,
                    secret = true,
                )

                Action(
                    label = stringResource(R.string.login_action),
                    enabled = !state.request.busy && state.login.isNotBlank() && state.password.isNotEmpty(),
                    action = viewModel::submit,
                )

                Link(
                    label = stringResource(R.string.cancel),
                    enabled = !state.request.busy
                ) {
                    viewModel.showLogin(false)
                }
            } else {
                Action(
                    label = stringResource(R.string.account_start),
                    enabled = !state.request.busy,
                    action = viewModel::begin
                )

                Link(
                    label = stringResource(R.string.existing_account),
                    enabled = !state.request.busy
                ) {
                    viewModel.showLogin(true)
                }
            }
        }
    }
}
