package com.example.film_app_mobile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.presentation.RequestStatus

@Composable
internal fun RequestFeedback(status: RequestStatus) {
    when (status) {
        RequestStatus.Idle,
        RequestStatus.Success -> Unit

        RequestStatus.Loading -> Unit
        is RequestStatus.Error ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                AppText("⚠", color = AppTheme.colors.accent)
                AppText(
                    stringResource(status.message),
                    style =
                        AppTheme.typography.body.copy(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                )
            }
    }
}