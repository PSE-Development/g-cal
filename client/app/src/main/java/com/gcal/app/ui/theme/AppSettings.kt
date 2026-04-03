package com.gcal.app.ui.theme

import com.gcal.app.ui.view_model.profileViewModel.AppLanguage

/**
 * AppSettings — Global app settings data class.
 *
 */
data class AppSettings(
    val isDarkMode: Boolean = false,
    val language: AppLanguage = AppLanguage.GERMAN
) {
    companion object {
        /** Creates a default settings instance with German language and light mode. */
        fun default() = AppSettings()
    }
}