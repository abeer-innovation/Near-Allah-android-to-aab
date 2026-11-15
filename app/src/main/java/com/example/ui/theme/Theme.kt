package com.example.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = SageGreenPrimary,
    onPrimary = CreamSurface,
    primaryContainer = SageGreenLight,
    onPrimaryContainer = SageGreenDark,
    secondary = SoftSage,
    onSecondary = CreamSurface,
    secondaryContainer = CreamSurfaceVariant,
    onSecondaryContainer = DarkBrownText,
    tertiary = WarmGold,
    onTertiary = CreamSurface,
    tertiaryContainer = WarmGoldLight,
    onTertiaryContainer = DarkBrownText,
    background = CreamBackground,
    onBackground = DarkBrownText,
    surface = CreamSurface,
    onSurface = DarkBrownText,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = MediumBrownText,
    outline = CreamBorder,
    outlineVariant = CreamBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSageGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkSageContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondary = SoftSage,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = WarmGold,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

@Composable
fun NearAllahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
