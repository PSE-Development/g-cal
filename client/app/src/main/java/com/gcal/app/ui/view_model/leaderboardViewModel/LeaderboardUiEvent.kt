package com.gcal.app.ui.view_model.leaderboardViewModel

/**
 * Represents all possible user actions originating from the Leaderboard UI.
 * Consolidating these actions into a sealed interface for MVVM helps to keep
 * the ViewModel's public API clean and enforces a clear separation of concerns,
 * rather than exposing multiple individual functions for every UI interaction.
 */
sealed interface LeaderboardUiEvent {

    /**
     * Triggered explicitly by the UI upon initial composition/display.
     * We use an explicit event rather than the ViewModel's `init` block to safely
     * trigger side-effects (like fetching data) only when the UI is actually ready
     * to observe them, preventing data loss during configuration changes.
     */
    data object OnScreenEntered : LeaderboardUiEvent

    /**
     * Triggered when the user explicitly requests a data update.
     * Kept separate from [OnScreenEntered] to allow for different caching or
     * loading strategies (e.g., forcing a network request and bypassing the local cache).
     */
    data object OnRefresh : LeaderboardUiEvent

    /**
     * Triggered to recover from a previous failure state.
     * Signals the ViewModel to reset its error state and re-attempt the data fetch.
     */
    data object OnRetryClicked : LeaderboardUiEvent

    /**
     * Delegates back navigation to the ViewModel/Coordinator.
     * We route this through the ViewModel instead of handling it directly in the UI
     * to allow intercepting the action (e.g., for analytics tracking or saving states
     * before leaving the screen).
     */
    data object OnBackClicked : LeaderboardUiEvent
}