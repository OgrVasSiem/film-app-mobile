package com.example.film_app_mobile.feature.folder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.feature.folder.components.FolderFilmCard
import com.example.film_app_mobile.feature.folder.state.FolderState
import kotlinx.coroutines.flow.drop

@Composable
internal fun FolderRoute(
    folder: LibraryFolder,
    back: () -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    if (state.contentType == null) {
        InitialLoading()
    } else {
        key(state.contentType) {
            FolderScreen(folder = folder, back = back, viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun FolderScreen(
    folder: LibraryFolder,
    back: () -> Unit,
    viewModel: FolderViewModel,
    state: FolderState,
) {
    val type = state.contentType ?: return
    val status = folder.name
    val title =
        stringResource(
            when (folder) {
                LibraryFolder.WILL_WATCH -> R.string.will_watch
                LibraryFolder.WATCHED -> R.string.watched
                LibraryFolder.WONT_WATCH -> R.string.archive_title
            }
        )
    val busy = state.request.busy

    LifecycleResumeEffect(viewModel, type) {
        viewModel.load(type, status)
        onPauseOrDispose {}
    }

    LaunchedEffect(viewModel, type) {
        viewModel.changes.drop(1).collect { viewModel.load(type, status) }
    }

    state.detail?.let { CatalogDetailSheet(type = type, film = it) { viewModel.dismissDetails() } }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title, action = {}, back = back) },
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            RequestFeedback(state.request)

            if (state.request.error != null)
                Link(
                    label = stringResource(R.string.retry),
                    enabled = true,
                    action = viewModel.request::retry,
                )

            state.undo?.let {
                Link(label = stringResource(R.string.undo_choice), enabled = !busy) {
                    viewModel.choose(type = type, status = status, id = it, target = null)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(count = state.cards.size, key = { state.cards[it].id }) { index ->
                    val film = state.cards[index]

                    FolderFilmCard(
                        film = film,
                        type = type,
                        status = status,
                        busy = busy,
                        details = { viewModel.details(type = type, id = film.id) },
                    ) { target ->
                        viewModel.choose(
                            type = type,
                            status = status,
                            id = film.id,
                            target = target,
                        )
                    }
                }

                if (state.cursor != null)
                    item {
                        Link(label = stringResource(R.string.more_items), enabled = !busy) {
                            viewModel.more(type = type, status = status)
                        }
                    }

                if (state.cards.isEmpty() && !busy)
                    item { AppText(text = stringResource(R.string.empty_list)) }
            }
        }
    }
}
