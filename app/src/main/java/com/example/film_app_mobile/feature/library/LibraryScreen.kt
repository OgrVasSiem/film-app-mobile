package com.example.film_app_mobile.feature.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.feature.library.components.LibraryPreview
import com.example.film_app_mobile.feature.library.state.LibraryState
import kotlinx.coroutines.flow.drop

@Composable
internal fun LibraryRoute(
    open: (LibraryFolder) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    if (state.contentType == null) {
        InitialLoading()
    } else {
        key(state.contentType) {
            LibraryRouteContent(open = open, viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun LibraryRouteContent(
    open: (LibraryFolder) -> Unit,
    viewModel: LibraryViewModel,
    state: LibraryState,
) {
    val type = state.contentType ?: return

    LifecycleResumeEffect(viewModel, type) {
        viewModel.load(type)
        onPauseOrDispose {}
    }

    LaunchedEffect(viewModel, type) { viewModel.changes.drop(1).collect { viewModel.load(type) } }

    state.detail?.let { CatalogDetailSheet(type = type, film = it) { viewModel.dismissDetails() } }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.library_title),
                action = {
                    IconButton(onClick = { open(LibraryFolder.WONT_WATCH) }) {
                        Icon(
                            painterResource(R.drawable.archive_box),
                            stringResource(R.string.archive_n, state.archiveCount),
                            tint = AppTheme.colors.textPrimary,
                        )
                    }
                },
                back = {},
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            ContentTypeSwitch(type = type, switch = viewModel::selectContentType)

            RequestFeedback(status = state.request)

            if (state.request.error != null)
                Link(
                    label = stringResource(R.string.retry),
                    enabled = true,
                    action = viewModel.request::retry,
                )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LibraryPreview(
                    state = state,
                    openList = { key -> open(LibraryFolder.valueOf(key)) },
                    openDetail = { viewModel.details(type = type, id = it) },
                )
            }
        }
    }
}
