package com.example.film_app_mobile.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.example.film_app_mobile.feature.home.bottomSheets.PremiumOfferSheet
import com.example.film_app_mobile.feature.home.components.HomeCatalog
import com.example.film_app_mobile.feature.home.components.HomeStreakBadge
import kotlinx.coroutines.flow.drop

@Composable
internal fun HomeScreen(
    open: (DeckSource) -> Unit,
    selection: ContentSelectionViewModel = hiltViewModel(),
) {
    val selected by selection.selection.collectAsStateWithLifecycle(initialValue = null)
    selected?.let { type ->
        key(type) { HomeScreenContent(type = type, switch = selection::select, open) }
    }
}

@Composable
private fun HomeScreenContent(
    type: ContentType,
    switch: (ContentType) -> Unit,
    open: (DeckSource) -> Unit,
    vm: HomeViewModel = hiltViewModel(key = type.name),
) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    val busy = vm.request.status.busy

    LifecycleResumeEffect(vm) {
        vm.load(type)
        onPauseOrDispose {}
    }

    LaunchedEffect(vm) { vm.changes.drop(1).collect { vm.load(type) } }

    ui.detail?.let { CatalogDetailSheet(type, it) { vm.dismissDetails() } }

    if (ui.showPremiumOffer) PremiumOfferSheet { vm.premiumOffer(false) }

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
        ui.catalog?.catalog?.sections?.mapNotNull { it.nextRefreshAt }?.minOrNull(),
        { vm.request.status.busy },
    ) {
        vm.load(type)
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                icon = painterResource(R.drawable.round_app_icon),
                action = {
                    ui.streak?.let {
                        HomeStreakBadge(days = it, modifier = Modifier.padding(end = 12.dp))
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            ContentTypeSwitch(type = type, switch = switch)

            RequestFeedback(vm.request.status)

            if (vm.request.status.error != null)
                Link(stringResource(R.string.retry), true, vm.request::retry)

            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.catalog?.let {
                    HomeCatalog(
                        snapshot = it,
                        vm = vm,
                        type = type,
                        busy = busy,
                        labels = labels,
                        open = open,
                    ) {
                        vm.premiumOffer(true)
                    }
                }
            }
        }
    }
}
