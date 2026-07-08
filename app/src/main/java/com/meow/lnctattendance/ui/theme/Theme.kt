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
    primary              = Color(0xFFE3BD9A),
    onPrimary            = Color(0xFF4A2B14),
    primaryContainer     = Color(0xFF6F4E37),
    onPrimaryContainer   = Color(0xFFFBEFE3),
    secondary            = Color(0xFFD6B599),
    onSecondary          = Color(0xFF4A2B14),
    background           = DarkBackground,
    surface              = DarkSurface,
    surfaceVariant       = DarkCard,
    onBackground         = Color(0xFFEDE0D4),
    onSurface            = Color(0xFFEDE0D4),
    onSurfaceVariant     = Color(0xFFCFBFB0),
    outline              = Color(0xFF9C8A7B),
    error                = Red,
    onError              = Color.White,
)

// ──────────────────────────────────────────────
// Light scheme — warm sand + coffee
// ──────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFF5DDBE),
    onPrimaryContainer   = Color(0xFF2C1A0E),
    secondary            = Secondary,
    onSecondary          = Color.White,
    background           = LightBackground,
    surface              = LightSurface,
    surfaceVariant       = LightCard,
    onBackground         = Color(0xFF211B15),
    onSurface            = Color(0xFF211B15),
    onSurfaceVariant     = Color(0xFF534639),
    outline              = Color(0xFF857463),
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
