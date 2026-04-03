package com.gcal.app.ui.view_model.profileViewModel

import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelFacade.general.User

// --- Enums for UI Control ---

/**
 * Acts as a finite state machine for modal dialogs.
 * WHY AN ENUM? Using a single enum instead of multiple boolean flags (e.g., `isLevelInfoOpen`,
 * `isLogoutOpen`) guarantees that only one dialog can ever be visible at a time,
 * preventing the UI from entering an invalid state where dialogs overlap.
 */
enum class ProfileDialogType {
    NONE,
    LEVEL_INFO,              // Gamification status (F150)
    LOGOUT_CONFIRM,          // Security check before logout (F30)
    DELETE_ACCOUNT_CONFIRM,  // Warning before deletion (F40)
    FRIENDS_MENU,            // Main friends menu
    ADD_FRIEND               // Add friend by username
}

/**
 * Supported languages for the App.
 * We attach the [code] directly to the enum so the View layer can pass it straight
 * to the system's LocaleHelper without needing to write its own mapping logic (`when` block).
 */
enum class AppLanguage(val displayName: String, val code: String) {
    GERMAN("Deutsch", "de"),
    ENGLISH("English", "en")
}

/**
 * Strongly typed error events tailored for the UI.
 * WHY NOT JUST EXCEPTIONS? Passing raw domain/network exceptions to the View forces
 * the UI to understand backend logic. A sealed interface allows the View to easily
 * resolve localized strings (e.g., translating "NetworkUnavailable" to "Kein Internet")
 * or show specific retry buttons.
 */
sealed interface ProfileUiErrorType {
    data object NetworkUnavailable : ProfileUiErrorType
    data object ServerError : ProfileUiErrorType
    data class Unknown(val message: String) : ProfileUiErrorType
}

// --- Main State ---

/**
 * The single, cohesive snapshot of the user's profile and settings.
 * Centralizing this guarantees that a change in one area (like changing the language)
 * renders cleanly without desyncing other areas (like the gamification progress).
 */
data class ProfileUiState(

    // --- User Identity ---
    var displayName: String = ClientAPI.username,
    val avatarUrl: String? = null,

    // --- Gamification Data ---
    val currentLevel: Int = 1,
    val currentXp: Int = 0,
    val xpForNextLevel: Int = 100,

    /**
     * Pre-calculated math for the UI.
     * Providing a normalized float (0.0 to 1.0) means the View's ProgressBar
     * can simply bind to this value without needing to know the formula
     * `(currentXp / xpForNextLevel)`.
     */
    val progressToNextLevel: Float = 0.0f,

    // --- Friends Data ---
    val friendUsernameInput: String = "",
    val friendRequestError: String? = null,
    val friends: List<User> = emptyList(), // Confirmed connections
    val isFriendRequestLoading: Boolean = false,

    // --- Navigation & Process State ---

    // Controls the currently active overlay.
    val activeDialog: ProfileDialogType = ProfileDialogType.NONE,

    /**
     * Architectural Note (UI Contract):
     * Acts as a lightweight internal router. If the View colleague strictly uses
     * Jetpack Navigation (NavController) to move between Profile and Settings,
     * this flag becomes redundant and should be removed to avoid conflicting navigation logic.
     */
    val isSettingsOpen: Boolean = false,

    // Signal for the coordinator/router to clear the backstack and return to Login.
    val isLoggedOut: Boolean = false,

    // Central loading flag to lock interactive elements during heavy backend calls.
    val isLoading: Boolean = false,

    /**
     * Proactive safety flag.
     * If true, the View should disable destructive actions (like "Delete Account")
     * to prevent the user from initiating requests that are guaranteed to fail and timeout.
     */
    val isOffline: Boolean = false,

    // --- Error State ---
    val error: ProfileUiErrorType? = null,

    // --- Local Settings ---
    val isDarkMode: Boolean = false,
    val language: AppLanguage = AppLanguage.GERMAN,
    val areNotificationsEnabled: Boolean = false
)
