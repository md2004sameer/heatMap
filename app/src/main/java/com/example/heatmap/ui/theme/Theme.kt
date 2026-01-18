package com.example.heatmap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LeetCodeOrange,
    onPrimary = BackgroundDark,
    primaryContainer = LeetCodeOrange.copy(alpha = 0.1f),
    onPrimaryContainer = LeetCodeOrange,
    secondary = LeetCodeGreen,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = BorderDark,
    surfaceVariant = InputDark,
    onSurfaceVariant = TextSecondaryDark
)

// We focus on a high-quality Dark Mode as it's the standard for developer tools
private val LightColorScheme = lightColorScheme(
    primary = LeetCodeOrange,
    secondary = LeetCodeGreen,
    background = android.graphics.Color.WHITE.let { androidx.compose.ui.graphics.Color(it) },
    surface = android.graphics.Color.parseColor("#F2F2F7").let { androidx.compose.ui.graphics.Color(it) }
)

@Composable
fun HeatMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Force dark for premium dev feel

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
