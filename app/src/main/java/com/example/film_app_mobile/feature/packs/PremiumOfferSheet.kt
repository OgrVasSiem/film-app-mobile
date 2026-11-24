package com.example.film_app_mobile.feature.packs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun PremiumOfferSheet(dismiss: () -> Unit) {
    AppBottomSheet(dismiss) {
        AppText(
            stringResource(R.string.locked_premium),
            color = AppTheme.colors.gold,
            style = AppTheme.typography.title,
        )
        AppText(stringResource(R.string.premium_offer))
        Link(stringResource(R.string.close_sheet), true, dismiss)
    }
}
