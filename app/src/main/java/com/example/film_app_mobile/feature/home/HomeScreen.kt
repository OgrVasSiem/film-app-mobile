package com.example.film_app_mobile.feature.home

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
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.feature.home.bottomSheets.PremiumOfferSheet
import com.example.film_app_mobile.feature.home.components.HomeCatalog
import com.example.film_app_mobile.feature.home.components.HomeStreakBadge
import com.example.film_app_mobile.feature.home.state.HomeState
import kotlinx.coroutines.flow.drop

@Composable
internal fun HomeScreen(open: (DeckSource) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    if (state.contentType == null) {
        InitialLoading()
    } else {
        key(state.contentType) {
            HomeScreenContent(open = open, viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun HomeScreenContent(
    open: (DeckSource) -> Unit,
    viewModel: HomeViewModel,
    state: HomeState,
) {
    val type = state.contentType ?: return
    val busy = state.request.busy

    LifecycleResumeEffect(viewModel, type) {
        viewModel.load(type)
        onPauseOrDispose {}
    }

    LaunchedEffect(viewModel, type) { viewModel.changes.drop(1).collect { viewModel.load(type) } }

    state.detail?.let { CatalogDetailSheet(type = type, film = it) { viewModel.dismissDetails() } }

    if (state.showPremiumOffer) PremiumOfferSheet(state, viewModel)

    val labels =
        mapOf(
            "FOR_YOU" to R.string.for_you_v2,
            "SINGLE" to type.recommendationTitle,
            "DAY" to R.string.day_v2,
            "WEEK" to R.string.week_v2,
            "MONTH" to R.string.month_v2,
            "NEW_MONTH" to R.string.new_month_v2,
            "NEW_YEAR" to R.string.new_year_v2,
            "EDITORIAL" to R.string.editorial_recommendations,
        )

    CatalogDeadline(
        state.catalog?.sections?.mapNotNull { it.nextRefreshAt }?.minOrNull(),
        { state.request.busy },
    ) {
        viewModel.load(type)
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                action = {
                    state.streak?.let {
                        HomeStreakBadge(days = it, modifier = Modifier.padding(end = 12.dp))
                    }
                },
                back = {},
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            ContentTypeSwitch(type = type, switch = viewModel::selectContentType)

            RequestFeedback(status = state.request)

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.catalog?.let {
                    HomeCatalog(
                        snapshot = it,
                        state = state,
                        vm = viewModel,
                        type = type,
                        busy = busy,
                        labels = labels,
                        open = open,
                    ) {
                        viewModel.premiumOffer(true)
                    }
                }
            }
        }
    }
}
