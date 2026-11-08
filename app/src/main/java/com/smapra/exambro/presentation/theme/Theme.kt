package com.smapra.exambro.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Navy700,
    onPrimary = White,
    primaryContainer = Navy800,
    onPrimaryContainer = White,
    secondary = Gold600,
    onSecondary = Navy900,
    secondaryContainer = Gold400,
    onSecondaryContainer = Navy900,
    tertiary = Emerald500,
    onTertiary = White,
    background = Grey50,
    onBackground = Navy900,
    surface = White,
    onSurface = Navy900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey700,
    outline = Grey300,
    outlineVariant = Grey200,
    error = Rose600,
    onError = White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Navy500,
    onPrimary = White,
    primaryContainer = Navy700,
    onPrimaryContainer = White,
    secondary = Gold500,
    onSecondary = Navy900,
    secondaryContainer = Gold700,
    onSecondaryContainer = Navy900,
    tertiary = Emerald400,
    onTertiary = Navy900,
    background = DarkBackground,
    onBackground = Grey100,
    surface = DarkSurface,
    onSurface = Grey100,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Grey400,
    outline = Grey700,
    outlineVariant = Grey800,
    error = Rose400,
    onError = Navy900,
)

@Composable
fun ExamBroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExamBroTypography,
        content = content,
    )
}
