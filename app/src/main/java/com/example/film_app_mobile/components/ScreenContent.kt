package com.example.film_app_mobile.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.feature.auth.*
import com.example.film_app_mobile.feature.completion.*
import com.example.film_app_mobile.feature.deck.*
import com.example.film_app_mobile.feature.library.*
import com.example.film_app_mobile.feature.packs.*
import com.example.film_app_mobile.presentation.RequestStatus
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.*

@Composable
internal fun ScreenContent(request: RequestStatus, title: String, body: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title.isNotEmpty()) {
            AppText(
                stringResource(R.string.brand),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.accent,
            )
            AppText(title, style = AppTheme.typography.title)
            if (request.busy) AppText(stringResource(R.string.loading))
            request.error?.let { AppText(stringResource(it)) }
        }
        body()
    }
}
