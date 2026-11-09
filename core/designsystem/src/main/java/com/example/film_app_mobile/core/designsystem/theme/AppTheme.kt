package com.example.film_app_mobile.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf<AppColors> {
    error("AppTheme is missing")
}
private val LocalTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTheme is missing")
}
private val DefaultTypography = appTypography()

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalColors.current
    val typography: AppTypography
        @Composable get() = LocalTypography.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: AppTypography = DefaultTypography,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides if (darkTheme) DarkColors else LightColors,
        LocalTypography provides typography,
        content = content,
    )
}
