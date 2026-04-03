package com.gcal.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gcal.app.ui.view_model.profileViewModel.AppLanguage

/**
 * AppSettingsController — Manages global app settings for theme and language.
 *
 * @param initialSettings The initial settings snapshot (read from system locale on startup).
 */
@Stable
class AppSettingsController(
    initialSettings: AppSettings = AppSettings.default()
) {
    var settings by mutableStateOf(initialSettings)
        private set

    /** Whether dark mode is currently enabled. */
    val isDarkMode: Boolean
        get() = settings.isDarkMode

    /** The currently selected app language. */
    val language: AppLanguage
        get() = settings.language

    /** Toggles dark mode on or off. */
    fun toggleDarkMode(enabled: Boolean) {
        settings = settings.copy(isDarkMode = enabled)
    }

    /**
     * Updates the selected language.
     *
     * Called by SettingsScreenView when the user picks a language
     * via the SegmentedButton. The caller is also responsible for
     * invoking [LocaleHelper.setLocale] to apply the Android locale change.
     */
    fun setLanguage(language: AppLanguage) {
        settings = settings.copy(language = language)
    }
}

/**
 * CompositionLocal for [AppSettingsController].
 *
 * Enables access to the global settings from anywhere in the Compose hierarchy.
 * Must be provided by [GCalTheme] — will throw if accessed without a provider.
 */
val LocalAppSettings = compositionLocalOf<AppSettingsController> {
    error("No AppSettingsController provided! Wrap your app with GCalTheme.")
}

/**
 * Creates and remembers an [AppSettingsController] instance.
 *
 * @param initialSettings The initial settings to use (typically derived from the system locale).
 */
@Composable
fun rememberAppSettingsController(
    initialSettings: AppSettings = AppSettings.default()
): AppSettingsController {
    return remember { AppSettingsController(initialSettings) }
}