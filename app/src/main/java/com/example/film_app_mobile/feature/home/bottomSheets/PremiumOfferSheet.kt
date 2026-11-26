package com.example.film_app_mobile.feature.home.bottomSheets

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.Action
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.profile.ProfileViewModel

@Composable
internal fun PremiumOfferSheet(
    vm: ProfileViewModel = hiltViewModel(),
    dismiss: () -> Unit,
) {
    val ui by vm.viewState.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf("offer") }
    val request = vm.request.status
    AppBottomSheet(dismiss) {
        AppText(
            stringResource(R.string.locked_premium),
            color = AppTheme.colors.gold,
            style = AppTheme.typography.title,
        )
        when (step) {
            "offer" -> {
                AppText(stringResource(R.string.premium_offer))
                Action(stringResource(R.string.check_purchase), !request.busy) {
                    vm.checkPurchase { step = if (it) "checkout" else "protect" }
                }
            }
            "protect" -> {
                AppText(stringResource(R.string.protect_purchase))
                AppText(ui.profile?.login.orEmpty())
                ProtectProfileForm(request.busy) { vm.protect(it) { step = "checkout" } }
            }
            "checkout" -> AppText(stringResource(R.string.no_checkout))
        }
        RequestFeedback(request)
    }
}
