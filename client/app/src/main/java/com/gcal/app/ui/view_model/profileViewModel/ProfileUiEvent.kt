package com.gcal.app.ui.view_model.profileViewModel

/**
 * Defines the strict communication contract for the Profile, Settings, and Friends screens.
 * Centralizing these intents ensures that sub-navigation, dialog states, and
 * error handling remain consistent, even if the user rotates the device or
 * minimizes the app while deeply nested in a settings menu.
 */
sealed interface ProfileUiEvent {

    // --- Main Profile Interactions ---

    /**
     * Elevates dialog triggers to the ViewModel rather than handling them locally in the View.
     * This allows the ViewModel to intercept destructive actions (like logout/delete)
     * to check if background syncing is currently happening before prompting the user.
     */
    data object ShowLevelInfoClicked : ProfileUiEvent
    data object RequestLogoutClicked : ProfileUiEvent
    data object RequestDeleteAccountClicked : ProfileUiEvent

    /**
     * Resets transient dialog flags in the state.
     * Routing this through the ViewModel prevents the "reappearing dialog" bug
     * that happens on configuration changes when a View holds its own dialog state.
     */
    data object DismissDialog : ProfileUiEvent

    // Navigation
    data object OpenSettingsClicked : ProfileUiEvent

    // Auth Actions
    /**
     * Confirms the destructive intent. The ViewModel takes over to coordinate
     * token clearing, database wiping, and routing the user back to the Login screen.
     */
    data object ConfirmLogout : ProfileUiEvent
    data object ConfirmDeleteAccount : ProfileUiEvent

    /**
     * Clears the current transient error state.
     * Essential for MVVM so the View can explicitly acknowledge that an error
     * (e.g., a Snackbar) has been shown and should not be triggered again.
     */
    data object ErrorDismissed : ProfileUiEvent

    // --- Friends Interactions ---

    /**
     * Manages sub-screen navigation entirely within the ViewModel's state.
     * This avoids the overhead of using the Jetpack Navigation Component for simple
     * UI swaps (like switching from the main profile view to the friends list).
     */
    data object OpenFriendsMenuClicked : ProfileUiEvent
    data object OpenAddFriendClicked : ProfileUiEvent
    data object BackToFriendsMenu : ProfileUiEvent

    /**
     * Streams raw user input during the "Add Friend" flow.
     * Enables real-time validation in the ViewModel (e.g., ensuring the username
     * meets format requirements or isn't already on the friends list).
     */
    data class FriendUsernameChanged(val username: String) : ProfileUiEvent

    /**
     * Executes the actual friend addition.
     * We don't pass the username here because the ViewModel already holds the
     * validated [FriendUsernameChanged] input in its current draft state.
     */
    data object AddFriendClicked : ProfileUiEvent

    /**
     * Executes the removal of a friend.
     * We explicitly pass the [username] as a parameter here to make the event
     * stateless and context-independent. The View doesn't need to "select" a friend
     * first; it just fires this event directly from a list item's delete button.
     */
    data class RemoveFriendClicked(val username: String) : ProfileUiEvent


    // --- Settings Sub-Screen Interactions ---

    data object CloseSettingsClicked : ProfileUiEvent

    // The following events handle immediate user preference updates.
    // The ViewModel processes these to optimistically update the UI state
    // while simultaneously persisting the choice to local storage or the backend.
    data class ToggleDarkMode(val enabled: Boolean) : ProfileUiEvent
    data class SelectLanguage(val language: AppLanguage) : ProfileUiEvent
    data class ToggleNotifications(val enabled: Boolean) : ProfileUiEvent

    /**
     * Resets the UI toggle to 'false' if the user denies the OS-level permission.
     * Keeps the View's toggle button synchronized with the actual system reality.
     */
    data object NotificationPermissionDenied : ProfileUiEvent

    /**
     * Reason: View-Layer Responsibility.
     * Opening the system file manager requires an Intent and an ActivityResultLauncher,
     * which are strictly UI-layer components. The ViewModel cannot "click" a system dialog.
     * The View colleague handles the click locally to open the picker; the ViewModel
     * only needs to know this intent to set a loading state or prepare for the result.
     */
    data object ImportCalendarClicked : ProfileUiEvent

    /**
     * Reason: Error State Synchronization.
     * The design doc requires a Snackbar and a return to the "Ready" state if file access fails.
     * Since the ViewModel manages the `UiErrorType` inside the ProfileUiState, the View
     * uses this event to "inject" a failure (e.g., IO Exception from the file picker)
     * back into the unidirectional data flow so the Snackbar knows what message to display.
     */
    data object ImportFileError : ProfileUiEvent
}
