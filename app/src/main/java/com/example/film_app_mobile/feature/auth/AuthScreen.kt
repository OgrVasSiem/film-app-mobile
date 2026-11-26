package com.example.film_app_mobile.feature.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun AuthRoute(vm: AuthViewModel = hiltViewModel()) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    val request = vm.request.status
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(stringResource(R.string.app_name)) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RequestFeedback(request)
            var existing by rememberSaveable { mutableStateOf(false) }
            Image(
                androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher_foreground),
                stringResource(R.string.app_name),
                Modifier.size(84.dp).align(Alignment.CenterHorizontally),
            )
            if (existing) {
                AccountField(stringResource(R.string.username), ui.login, vm::login)
                AccountField(
                    stringResource(R.string.password),
                    ui.password,
                    vm::password,
                    secret = true,
                )
                Action(
                    stringResource(R.string.login_action),
                    !request.busy && ui.login.isNotBlank() && ui.password.isNotEmpty(),
                    vm::submit,
                )
                Link(stringResource(R.string.cancel), !request.busy) {
                    existing = false
                    vm.password("")
                }
            } else {
                Action(stringResource(R.string.account_start), !request.busy, vm::begin)
                Link(stringResource(R.string.existing_account), !request.busy) {
                    existing = true
                    vm.request.clearError()
                }
            }
        }
    }
}
