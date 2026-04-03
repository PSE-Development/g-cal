package com.gcal.app.ui.screens.rangliste

import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardEntryUi

/**
 * RanglisteUiState - Dynamic data for the loaded state
 *
 */
data class RanglisteUiState(
    /**
     * The sorted ranking list.
     */
    val entries: List<LeaderboardEntryUi> = emptyList(),

    /**
     * Index of the current user in the list (for highlighting).
     * -1 if the user is not in the list.
     */
    val userRankIndex: Int = -1,

    /**
     * The current loading state of the view.
     */
    val screenState: RanglisteScreenState = RanglisteScreenState.Loading,

    /**
     * Error message in case of an error state (optional).
     */
    val errorMessage: String? = null
) {
    companion object {
        /**
         * Creates a Loading-State
         */
        fun loading() = RanglisteUiState(
            screenState = RanglisteScreenState.Loading
        )

        /**
         * Creates a Loaded-State with data
         */
        fun loaded(
            entries: List<LeaderboardEntryUi>,
            userRankIndex: Int = -1
        ) = RanglisteUiState(
            entries = entries,
            userRankIndex = userRankIndex,
            screenState = RanglisteScreenState.Loaded
        )

        /**
         * Creates an Error-State
         */
        fun error(message: String = "Ein Fehler ist aufgetreten") = RanglisteUiState(
            screenState = RanglisteScreenState.Error,
            errorMessage = message
        )
    }
}
