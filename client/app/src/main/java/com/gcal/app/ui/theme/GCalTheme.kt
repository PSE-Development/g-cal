package com.gcal.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * GCal Color
 */
private val GCalPrimary = Color(0xFF6750A4)
private val GCalOnPrimary = Color(0xFFFFFFFF)
private val GCalPrimaryContainer = Color(0xFFEADDFF)
private val GCalOnPrimaryContainer = Color(0xFF21005D)

private val GCalSecondary = Color(0xFF625B71)
private val GCalOnSecondary = Color(0xFFFFFFFF)
private val GCalSecondaryContainer = Color(0xFFE8DEF8)
private val GCalOnSecondaryContainer = Color(0xFF1D192B)

private val GCalError = Color(0xFFB3261E)
private val GCalOnError = Color(0xFFFFFFFF)
private val GCalErrorContainer = Color(0xFFF9DEDC)
private val GCalOnErrorContainer = Color(0xFF410E0B)

private val GCalBackground = Color(0xFFFFFBFE)
private val GCalOnBackground = Color(0xFF1C1B1F)
private val GCalSurface = Color(0xFFFFFBFE)
private val GCalOnSurface = Color(0xFF1C1B1F)
private val GCalSurfaceVariant = Color(0xFFE7E0EC)
private val GCalOnSurfaceVariant = Color(0xFF49454F)

// Dark Theme Farben
private val GCalDarkPrimary = Color(0xFFD0BCFF)
private val GCalDarkOnPrimary = Color(0xFF381E72)
private val GCalDarkPrimaryContainer = Color(0xFF4F378B)
private val GCalDarkOnPrimaryContainer = Color(0xFFEADDFF)

private val GCalDarkSecondary = Color(0xFFCCC2DC)
private val GCalDarkOnSecondary = Color(0xFF332D41)
private val GCalDarkSecondaryContainer = Color(0xFF4A4458)
private val GCalDarkOnSecondaryContainer = Color(0xFFE8DEF8)

private val GCalDarkBackground = Color(0xFF1C1B1F)
private val GCalDarkOnBackground = Color(0xFFE6E1E5)
private val GCalDarkSurface = Color(0xFF1C1B1F)
private val GCalDarkOnSurface = Color(0xFFE6E1E5)
private val GCalDarkSurfaceVariant = Color(0xFF49454F)
private val GCalDarkOnSurfaceVariant = Color(0xFFCAC4D0)

/**
 * Light Color Scheme
 */
private val LightColorScheme = lightColorScheme(
    primary = GCalPrimary,
    onPrimary = GCalOnPrimary,
    primaryContainer = GCalPrimaryContainer,
    onPrimaryContainer = GCalOnPrimaryContainer,
    secondary = GCalSecondary,
    onSecondary = GCalOnSecondary,
    secondaryContainer = GCalSecondaryContainer,
    onSecondaryContainer = GCalOnSecondaryContainer,
    error = GCalError,
    onError = GCalOnError,
    errorContainer = GCalErrorContainer,
    onErrorContainer = GCalOnErrorContainer,
    background = GCalBackground,
    onBackground = GCalOnBackground,
    surface = GCalSurface,
    onSurface = GCalOnSurface,
    surfaceVariant = GCalSurfaceVariant,
    onSurfaceVariant = GCalOnSurfaceVariant
)

/**
 * Dark Color Scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = GCalDarkPrimary,
    onPrimary = GCalDarkOnPrimary,
    primaryContainer = GCalDarkPrimaryContainer,
    onPrimaryContainer = GCalDarkOnPrimaryContainer,
    secondary = GCalDarkSecondary,
    onSecondary = GCalDarkOnSecondary,
    secondaryContainer = GCalDarkSecondaryContainer,
    onSecondaryContainer = GCalDarkOnSecondaryContainer,
    background = GCalDarkBackground,
    onBackground = GCalDarkOnBackground,
    surface = GCalDarkSurface,
    onSurface = GCalDarkOnSurface,
    surfaceVariant = GCalDarkSurfaceVariant,
    onSurfaceVariant = GCalDarkOnSurfaceVariant
)

/**
 * GCalTheme - Main Theme for the entire app.
 *
 *
 *
 * @param appSettingsController Controller for managing app settings.
 * @param content Content to be rendered within the theme.
 */
@Composable
fun GCalTheme(
    appSettingsController: AppSettingsController = rememberAppSettingsController(),
    content: @Composable () -> Unit
) {
    val isDarkTheme = appSettingsController.isDarkMode

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme


    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    CompositionLocalProvider(LocalAppSettings provides appSettingsController) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}