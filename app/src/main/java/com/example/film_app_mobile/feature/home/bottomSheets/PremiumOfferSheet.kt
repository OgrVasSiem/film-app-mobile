package com.example.film_app_mobile.feature.home.bottomSheets

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.Action
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.home.HomeViewModel
import com.example.film_app_mobile.feature.home.state.HomeState
import com.example.film_app_mobile.feature.home.state.PremiumStep

@Composable
internal fun PremiumOfferSheet(state: HomeState, vm: HomeViewModel) {
    val request = state.request
    AppBottomSheet({ vm.premiumOffer(false) }) {
        AppText(
            stringResource(R.string.locked_premium),
            color = AppTheme.colors.gold,
            style = AppTheme.typography.title,
        )
        when (state.premiumStep) {
            PremiumStep.Offer -> {
                AppText(stringResource(R.string.premium_offer))
                Action(stringResource(R.string.check_purchase), !request.busy, vm::checkPurchase)
            }
            PremiumStep.Protect -> {
                AppText(stringResource(R.string.protect_purchase))
                AppText(state.purchaseLogin)
                ProtectProfileForm(request.busy, vm::protectForPurchase)
            }
            PremiumStep.Checkout -> AppText(stringResource(R.string.no_checkout))
        }
        RequestFeedback(request)
    }
}
