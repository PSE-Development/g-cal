package com.gcal.app.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gcal.app.ui.view_model.profileViewModel.AppLanguage

/**
 * Utility object for managing the application's runtime locale.
 *
 *
 */
object LocaleHelper {

    /**
     * Applies the selected language to the entire application.
     *
     * Triggers an Activity recreation so that all resource references (stringResource,
     * getString, etc.) resolve against the new locale's values directory.
     *
     * @param context Not used directly by AppCompatDelegate but retained in the signature
     *        for call-site consistency and potential future use.
     * @param language The target [AppLanguage] to apply.
     */
    fun setLocale(context: Context, language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code)
        )
    }

    /**
     * Applies the selected language without explicitly triggering Activity recreation.
     *
     *
     * @param context Retained for API symmetry with [setLocale].
     * @param language The target [AppLanguage] to apply.
     */
    fun setLocaleWithoutRecreate(context: Context, language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code)
        )
    }

    /**
     * Reads the currently active application locale from the AppCompat framework.
     *
     * Called by [GCalApp] during initial composition to seed the [AppSettingsController]
     * with the correct language after an Activity recreation caused by a locale switch.
     *
     * @param context Retained for API symmetry; not used by AppCompatDelegate.
     * @return The matching [AppLanguage], defaulting to [AppLanguage.GERMAN] if the
     *         current locale is unrecognized or empty (e.g., fresh install).
     */
    fun getCurrentLanguage(context: Context): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val currentLocale = if (!locales.isEmpty) locales[0] else null

        return when (currentLocale?.language) {
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.GERMAN
        }
    }

    /**
     * Checks whether the given language matches the currently active locale.
     *
     * @param context Application or Activity context.
     * @param language The [AppLanguage] to compare against the active locale.
     * @return True if the current locale matches the provided language.
     */
    fun isCurrentLanguage(context: Context, language: AppLanguage): Boolean {
        return getCurrentLanguage(context) == language
    }
}