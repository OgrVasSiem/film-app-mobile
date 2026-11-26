package com.example.film_app_mobile.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun AccountField(
    label: String,
    value: String,
    change: (String) -> Unit,
    secret: Boolean = false,
    invalid: Boolean = false,
) {
    var reveal by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(if (invalid) AppTheme.colors.errorSurface else AppTheme.colors.surface)
            .heightIn(min = 54.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            if (secret) stringResource(R.string.password_icon)
            else stringResource(R.string.login_icon),
            color = AppTheme.colors.textSecondary,
        )
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            AppText(
                label,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            BasicTextField(
                value,
                change,
                Modifier.fillMaxWidth().semantics { contentDescription = label },
                singleLine = true,
                textStyle = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                cursorBrush = SolidColor(AppTheme.colors.accent),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text
                    ),
                visualTransformation =
                    if (secret && !reveal) PasswordVisualTransformation()
                    else VisualTransformation.None,
            )
        }
        if (secret)
            Box(
                Modifier.clickable(role = Role.Button) { reveal = !reveal }.heightIn(min = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    if (reveal) stringResource(R.string.hide) else stringResource(R.string.show),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.accent,
                )
            }
    }
}
