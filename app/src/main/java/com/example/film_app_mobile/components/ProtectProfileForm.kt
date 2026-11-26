package com.example.film_app_mobile.components

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.film_app_mobile.R
import com.example.film_app_mobile.core.designsystem.component.*

@Composable
internal fun ProtectProfileForm(busy: Boolean, submit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    AppText(stringResource(R.string.password_rule))
    AccountField(stringResource(R.string.password), password, { password = it }, secret = true)
    AccountField(stringResource(R.string.repeat_password), repeat, { repeat = it }, secret = true)
    Action(
        stringResource(R.string.save_changes),
        !busy && password.length in 8..128 && repeat == password,
    ) {
        submit(password)
    }
}
