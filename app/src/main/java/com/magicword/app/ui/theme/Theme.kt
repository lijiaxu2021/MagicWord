package com.magicword.app.ui.theme

import android.app.Activity

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KraftPaperYellow,
    secondary = KraftPaperAccent,
    tertiary = KraftPaperDark,
    background = Color(0xFF3E2723), // Dark Brown Background
    surface = Color(0xFF4E342E),    // Dark Brown Surface
    onPrimary = KraftPaperDark,
    onSecondary = KraftPaperDark,
    onTertiary = KraftPaperYellow,
    onBackground = KraftPaperYellow,
    onSurface = KraftPaperYellow,
)

private val LightColorScheme = lightColorScheme(
    primary = KraftPaperDark,
    secondary = KraftPaperText,
    tertiary = KraftPaperAccent,
    background = KraftPaperYellow,
    surface = Color(0xFFF5F5DC), // Beige Surface (slightly lighter than background)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = KraftPaperText,
    onBackground = KraftPaperText,
    onSurface = KraftPaperText,
)

@Composable
fun EasyWordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce our custom Light Blue theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
