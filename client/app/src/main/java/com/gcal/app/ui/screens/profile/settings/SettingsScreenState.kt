package com.gcal.ui.screens.profile.settings

/**
 * SettingsScreenState -- Represents the state of the settings screen.
 *
 */
data class SettingsScreenState(
    val isDarkMode: Boolean = false,
    val selectedLanguage: AppLanguage = AppLanguage.GERMAN,
    val areNotificationsEnabled: Boolean = false,
    val isImporting: Boolean = false,
    val showNotificationPermissionDenied: Boolean = false
) {
    companion object {
        fun initial() = SettingsScreenState()
    }
}