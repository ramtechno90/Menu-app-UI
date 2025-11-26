package com.pizzaparadize.menuapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = BackgroundLight,
    surface = BackgroundLight,
    onPrimary = TextOnPrimary,
    onSecondary = TextOnSecondary,
    onBackground = TextOnBackground,
    onSurface = TextOnBackground,
    surfaceVariant = CardBackgroundColor,
    onSurfaceVariant = TextOnBackground
)

private fun Color.isDark() = (red * 299 + green * 587 + blue * 114) / 1000 < 0.5

@Composable
fun MenuAppTheme(
    statusBarColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val color = statusBarColor ?: colorScheme.background
            window.statusBarColor = color.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !color.isDark()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
