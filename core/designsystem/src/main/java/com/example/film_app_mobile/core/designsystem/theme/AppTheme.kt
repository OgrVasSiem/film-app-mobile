package com.example.film_app_mobile.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf<AppColors> { error("AppTheme is missing") }
private val LocalTypography =
    staticCompositionLocalOf<AppTypography> { error("AppTheme is missing") }
private val DefaultTypography = appTypography()

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalColors.current

    val typography: AppTypography
        @Composable get() = LocalTypography.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    typography: AppTypography = DefaultTypography,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides NoPressIndication,
        LocalColors provides if (darkTheme) DarkColors else LightColors,
        LocalTypography provides typography,
        content = content,
    )
}

private data object NoPressIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(
        interactionSource: androidx.compose.foundation.interaction.InteractionSource
    ): androidx.compose.ui.Modifier.Node =
        object : androidx.compose.ui.Modifier.Node(), androidx.compose.ui.node.DrawModifierNode {
            override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() =
                drawContent()
        }
}
