package com.meow.lnctattendance.ui.theme

import android.app.Activity
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

// ──────────────────────────────────────────────
// Dark scheme — deep space indigo
// ──────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF1E1B4B),
    onPrimaryContainer   = Color(0xFFE0E7FF),
    secondary            = Secondary,
    onSecondary          = Color.White,
    background           = DarkBackground,
    surface              = DarkSurface,
    surfaceVariant       = DarkCard,
    onBackground         = Color(0xFFF1F5F9),
    onSurface            = Color(0xFFF1F5F9),
    onSurfaceVariant     = Color(0xFF94A3B8),
    outline              = Color(0xFF334155),
    error                = Red,
    onError              = Color.White,
)

// ──────────────────────────────────────────────
// Light scheme — clean white + indigo
// ──────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFEEEDFF),
    onPrimaryContainer   = Color(0xFF1A005E),
    secondary            = Color(0xFF018786),
    onSecondary          = Color.White,
    background           = LightBackground,   // 0xFFF5F5FA
    surface              = LightSurface,      // 0xFFFFFFFF
    surfaceVariant       = LightCard,         // 0xFFF0F0FF
    onBackground         = Color(0xFF1A1A2E),
    onSurface            = Color(0xFF1A1A2E),
    onSurfaceVariant     = Color(0xFF555570),
    outline              = Color(0xFFCCCCDD),
    error                = Red,
    onError              = Color.White,
)

// ──────────────────────────────────────────────
// Theme entry point
// ──────────────────────────────────────────────
@Composable
fun LNCTAttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent so it blends with background
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Same for nav bar
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content,
    )
}
