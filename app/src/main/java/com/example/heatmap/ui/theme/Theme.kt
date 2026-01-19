package com.example.heatmap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LeetCodeOrange,
    onPrimary = BackgroundDark,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = TextPrimary,
    secondary = LeetCodeGreen,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderDark,
    surfaceVariant = SurfaceLighter,
    onSurfaceVariant = TextSecondary,
    error = ErrorMuted
)

private val LightColorScheme = lightColorScheme(
    primary = LeetCodeOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2F2F7),
    onPrimaryContainer = Color.Black,
    secondary = LeetCodeGreen,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color(0xFFD1D1D6),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93)
)

@Composable
fun HeatMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
