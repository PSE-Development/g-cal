package com.gcal.app.ui.view_model.leaderboardViewModel

// --- Enums / Helper Classes ---

/**
 * Defines the mutually exclusive high-level states of the screen.
 * Using an enum rather than separate boolean flags (e.g., `isLoading`, `hasError`)
 * guarantees that the UI cannot enter an invalid or conflicting state
 * (like being in a loading AND error state simultaneously).
 */
enum class LeaderboardScreenState {
    LOADING,
    LOADED,
    ERROR
}

/**
 * Pre-computed, UI-specific representation of a leaderboard entry.
 * We map domain models to this UI model in the ViewModel to keep the View
 * entirely passive ("dumb"). The View should only bind data to pixels,
 * without performing any business logic, math, or string formatting.
 */
data class LeaderboardEntryUi(
    val rank: Int,
    val username: String,
    val level: Int,

    /**
     * Explicitly provided to hide the level-to-stars conversion logic from the View.
     * Even though it derives from [level], keeping this logic in the ViewModel
     * makes it easily unit-testable.
     */
    val starCount: Int,

    /**
     * Pre-formatted string to ensure the View doesn't handle localized formatting,
     * string concatenation, or resource resolution.
     */
    val xpDisplayString: String,

    /**
     * Pre-calculated as a normalized value (0.0f to 1.0f).
     * The View should only be responsible for drawing the progress bar,
     * not for doing the math between current XP and max XP of a level.
     */
    val nextLevelProgress: Float
)

// --- Main State ---

/**
 * The Single Source of Truth for the Leaderboard UI.
 * Grouping the state into a single data class allows the ViewModel to expose it
 * via a StateFlow. This guarantees that the UI always renders a cohesive,
 * atomic snapshot of the data, avoiding race conditions between different state fields.
 */
data class LeaderboardUiState(
    val screenState: LeaderboardScreenState = LeaderboardScreenState.LOADING,
    val entries: List<LeaderboardEntryUi> = emptyList(),

    /**
     * Provided as an index rather than a user ID or object.
     * This allows the UI (e.g., a LazyColumn or RecyclerView) to immediately
     * scroll to or highlight the user's row without needing to search
     * through the [entries] list itself.
     */
    val currentUserRankIndex: Int? = null,

    val errorMessage: String? = null
)