package com.example.film_app_mobile.feature.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.network.model.ApiError

@Composable
fun HealthRoute(viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthScreen(state, viewModel::checkServer, viewModel::checkDatabase)
}

@Composable
fun HealthScreen(
    state: HealthUiState,
    onCheckServer: () -> Unit,
    onCheckDatabase: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AppText(stringResource(R.string.api_health_title), style = AppTheme.typography.title)
        EndpointBlock(stringResource(R.string.api_server), state.server, onCheckServer)
        EndpointBlock(stringResource(R.string.api_database), state.database, onCheckDatabase)
    }
}

@Composable
private fun EndpointBlock(title: String, state: EndpointState, onCheck: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(AppTheme.colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(title, style = AppTheme.typography.title)
        val text = when (state) {
            EndpointState.Idle -> stringResource(R.string.api_idle)
            EndpointState.Loading -> stringResource(R.string.api_loading)
            is EndpointState.Success -> stringResource(
                R.string.api_success,
                state.metadata.httpCode,
                state.status ?: stringResource(R.string.api_no_body_check),
            )
            is EndpointState.Failure -> errorText(state.error)
        }
        AppText(text)
        val requestId = when (state) {
            is EndpointState.Success -> state.metadata.requestId
            is EndpointState.Failure -> when (val error = state.error) {
                is ApiError.Http -> error.requestId
                is ApiError.EmptyResponse -> error.metadata.requestId
                else -> null
            }
            else -> null
        }
        requestId?.let {
            AppText(stringResource(R.string.api_request_id, it), style = AppTheme.typography.caption)
        }
        AppText(
            text = stringResource(R.string.api_check),
            style = AppTheme.typography.button,
            color = AppTheme.colors.onAccent,
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(AppTheme.colors.accent)
                .clickable(enabled = state != EndpointState.Loading, role = Role.Button, onClick = onCheck)
                .padding(12.dp),
        )
    }
}

@Composable
private fun errorText(error: ApiError): String = when (error) {
    is ApiError.Http -> stringResource(R.string.api_http_error, error.httpCode, error.code.orEmpty())
    is ApiError.Timeout -> stringResource(R.string.api_timeout)
    is ApiError.Connection -> stringResource(R.string.api_connection)
    is ApiError.Tls -> stringResource(R.string.api_tls)
    is ApiError.InvalidResponse -> stringResource(R.string.api_invalid_response)
    is ApiError.EmptyResponse -> stringResource(R.string.api_empty_response)
}
