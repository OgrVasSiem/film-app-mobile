package com.example.film_app_mobile.feature.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.ScreenContent
import com.example.film_app_mobile.core.designsystem.component.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun AuthScreen(
    login: String,
    password: String,
    error: Int?,
    busy: Boolean,
    changeLogin: (String) -> Unit,
    changePassword: (String) -> Unit,
    clearError: () -> Unit,
    submit: (Boolean) -> Unit,
) {
    val c = AppTheme.colors
    var register by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(56.dp).shadow(1.dp, RoundedCornerShape(12.dp)).background(c.surface),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                stringResource(R.string.brand_icon),
                color = c.accent,
                style = AppTheme.typography.title,
            )
        }
        AppText(stringResource(R.string.app_name), style = AppTheme.typography.title)
        AppText(stringResource(R.string.tagline), color = c.textSecondary)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().background(c.border, RoundedCornerShape(12.dp)).padding(4.dp)) {
            for (mode in listOf(false, true)) {
                val selected = mode == register
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) c.surface else Color.Transparent)
                        .selectable(selected, enabled = !busy, role = Role.Tab) {
                            register = mode
                            clearError()
                        }
                        .heightIn(min = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        if (mode) stringResource(R.string.register_tab)
                        else stringResource(R.string.login_tab),
                        style = AppTheme.typography.button,
                        color = if (selected) c.accent else c.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        AuthField(stringResource(R.string.username), login, changeLogin)
        AuthField(
            stringResource(R.string.password),
            password,
            changePassword,
            secret = true,
            invalid = error == R.string.invalid_credentials,
        )
        if (error != null)
            AppText(stringResource(error), Modifier.fillMaxWidth(), color = c.accent)
        if (register)
            AppText(
                stringResource(R.string.auth_help),
                style = AppTheme.typography.caption,
                color = c.textSecondary,
            )
        Action(
            if (busy) stringResource(R.string.loading)
            else if (register) stringResource(R.string.register_action)
            else stringResource(R.string.login_action),
            !busy && login.isNotBlank() && password.isNotEmpty(),
        ) {
            submit(register)
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    change: (String) -> Unit,
    secret: Boolean = false,
    invalid: Boolean = false,
) {
    val c = AppTheme.colors
    var reveal by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(if (invalid) c.errorSurface else c.surface)
            .heightIn(min = 54.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            if (secret) stringResource(R.string.password_icon)
            else stringResource(R.string.login_icon),
            color = c.textSecondary,
        )
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            AppText(label, style = AppTheme.typography.caption, color = c.textSecondary)
            BasicTextField(
                value,
                change,
                Modifier.fillMaxWidth().semantics { contentDescription = label },
                singleLine = true,
                textStyle = AppTheme.typography.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.accent),
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
                    color = c.accent,
                )
            }
    }
}

@Composable
internal fun AuthRoute(vm: AuthViewModel = hiltViewModel()) {
    val request = vm.request.status
    ScreenContent(request, "") {
        AuthScreen(
            vm.state.login,
            vm.state.password,
            request.error,
            request.busy,
            vm::login,
            vm::password,
            vm.request::clearError,
            vm::submit,
        )
    }
}
