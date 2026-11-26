package com.example.film_app_mobile.feature.folder

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.feature.folder.components.FolderFilmCard
import kotlinx.coroutines.flow.drop

@Composable
internal fun FolderRoute(
    folder: LibraryFolder,
    back: () -> Unit,
    selection: ContentSelectionViewModel = hiltViewModel(),
) {
    val selected by selection.selection.collectAsStateWithLifecycle(initialValue = null)
    selected?.let { type -> key(type) { FolderScreen(type, folder, back) } }
}

@Composable
internal fun FolderScreen(
    type: ContentType,
    folder: LibraryFolder,
    back: () -> Unit,
    vm: FolderViewModel = hiltViewModel(key = type.name),
) {
    val status = folder.name
    val title =
        stringResource(
            when (folder) {
                LibraryFolder.WILL_WATCH -> R.string.will_watch
                LibraryFolder.WATCHED -> R.string.watched
                LibraryFolder.WONT_WATCH -> R.string.archive_title
            }
        )
    val ui by vm.viewState.collectAsStateWithLifecycle()
    val busy = vm.request.status.busy
    LifecycleResumeEffect(vm) {
        vm.load(type, status)
        onPauseOrDispose {}
    }
    LaunchedEffect(vm) { vm.changes.drop(1).collect { vm.load(type, status) } }
    ui.detail?.let { CatalogDetailSheet(type, it) { vm.dismissDetails() } }
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title,
                action = {},
                back = { AppText("‹", Modifier.clickable(onClick = back).padding(12.dp)) },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            RequestFeedback(vm.request.status)
            if (vm.request.status.error != null)
                Link(stringResource(R.string.retry), true, vm.request::retry)
            ui.undo?.let {
                Link(stringResource(R.string.undo_choice), !busy) {
                    vm.choose(type, status, it, null)
                }
            }
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.cards.size, key = { ui.cards[it].id }) { index ->
                    val film = ui.cards[index]
                    FolderFilmCard(film, type, status, busy, { vm.details(type, film.id) }) { target
                        ->
                        vm.choose(type, status, film.id, target)
                    }
                    Link(stringResource(R.string.reset_decision), !busy) {
                        vm.choose(type, status, film.id, null)
                    }
                }
                if (ui.cursor != null)
                    item {
                        Link(stringResource(R.string.more_items), !busy) { vm.more(type, status) }
                    }
                if (ui.cards.isEmpty() && !busy)
                    item { AppText(stringResource(R.string.empty_list)) }
            }
        }
    }
}
