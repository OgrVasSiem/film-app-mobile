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
)

internal val LightColors = AppColors(
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF181A20),
    textSecondary = Color(0xFF606574),
    accent = Color(0xFF5840C6),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFFD5D8E1),
)

internal val DarkColors = AppColors(
    background = Color(0xFF111217),
    surface = Color(0xFF1C1E26),
    textPrimary = Color(0xFFF4F5FA),
    textSecondary = Color(0xFFA5AABB),
    accent = Color(0xFFBAABFF),
    onAccent = Color(0xFF23135C),
    border = Color(0xFF444957),
)
