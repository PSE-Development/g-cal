package com.gcal.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.ui.navigation.GCalNavHost
import com.gcal.app.ui.theme.AppSettings
import com.gcal.app.ui.theme.GCalTheme
import com.gcal.app.ui.theme.LocaleHelper
import com.gcal.app.ui.theme.rememberAppSettingsController

// Shared key used by both GCalApp (read) and SettingsScreenView (write)
// to persist the dark mode preference across Activity recreations.
const val PREFS_NAME = "gcal_settings"
const val KEY_DARK_MODE = "dark_mode"

/**
 * GCalApp — Root composable and single entry point for the entire UI layer.
 *
 * Reads the persisted dark mode preference from SharedPreferences so that the
 * theme survives Activity recreations triggered by locale changes.
 *
 * @param model The application-wide ModelFacade instance.
 */
@Composable
fun GCalApp(
    model: ModelFacade,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Read the system locale once per composition to seed the initial language.
    val currentLanguage = LocaleHelper.getCurrentLanguage(context)

    
    // Defaults to false (light mode) on fresh install.
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)

    val initialSettings = AppSettings(
        isDarkMode = savedDarkMode,
        language = currentLanguage
    )

    val appSettingsController = rememberAppSettingsController(initialSettings)

    GCalTheme(appSettingsController = appSettingsController) {
        // Forward the ModelFacade to the navigation layer so every screen's
        // ViewModel can be constructed with the same backend gateway.
        GCalNavHost(
            model = model,
            modifier = modifier
        )
    }
}