package com.example.film_app_mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val border: Color,
    val gold: Color,
    val goldSurface: Color,
    val errorSurface: Color,
)

internal val LightColors =
    AppColors(
        background = Color(0xFFF4EFED),
        surface = Color(0xFFFBF7F4),
        textPrimary = Color(0xFF332A2A),
        textSecondary = Color(0xFF685953),
        accent = Color(0xFF924B50),
        onAccent = Color(0xFFFFF6F2),
        border = Color(0xFFE6DAD5),
        gold = Color(0xFF775928),
        goldSurface = Color(0xFFEBDFC9),
        errorSurface = Color(0xFFF1DEDD),
    )

internal val DarkColors =
    AppColors(
        background = Color(0xFF191617),
        surface = Color(0xFF252021),
        textPrimary = Color(0xFFE9DEDA),
        textSecondary = Color(0xFFBBACA6),
        accent = Color(0xFFC78686),
        onAccent = Color(0xFF311A1D),
        border = Color(0xFF413536),
        gold = Color(0xFFC7A66E),
        goldSurface = Color(0xFF373026),
        errorSurface = Color(0xFF452E32),
    )
