package com.example.film_app_mobile.feature.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.flow.drop

@Composable
internal fun LibraryRoute(
    open: (LibraryFolder) -> Unit,
    selection: ContentSelectionViewModel = hiltViewModel(),
) {
    val selected by selection.selection.collectAsStateWithLifecycle(initialValue = null)
    selected?.let { type -> key(type) { LibraryRouteContent(type, selection::select, open) } }
}

@Composable
private fun LibraryRouteContent(
    type: ContentType,
    switch: (ContentType) -> Unit,
    open: (LibraryFolder) -> Unit,
    vm: LibraryViewModel = hiltViewModel(key = type.name),
) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    val busy = vm.request.status.busy
    androidx.lifecycle.compose.LifecycleResumeEffect(vm) {
        vm.load(type)
        onPauseOrDispose {}
    }
    LaunchedEffect(vm) { vm.changes.drop(1).collect { vm.load(type) } }
    ui.detail?.let { CatalogDetailSheet(type, it) { vm.dismissDetails() } }
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                stringResource(R.string.library_title),
                action = {
                    androidx.compose.material3.IconButton(
                        onClick = { open(LibraryFolder.WONT_WATCH) }
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.ui.res.painterResource(R.drawable.archive_box),
                            stringResource(R.string.archive_n, ui.archiveCount),
                            tint = AppTheme.colors.textPrimary,
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            ContentTypeSwitch(type, switch)
            RequestFeedback(vm.request.status)
            if (vm.request.status.error != null)
                Link(stringResource(R.string.retry), true, vm.request::retry)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                com.example.film_app_mobile.feature.library.components.LibraryPreview(
                    ui,
                    busy,
                    vm.request.status.error,
                    { key -> open(LibraryFolder.valueOf(key)) },
                    { vm.details(type, it) },
                )
            }
        }
    }
}
