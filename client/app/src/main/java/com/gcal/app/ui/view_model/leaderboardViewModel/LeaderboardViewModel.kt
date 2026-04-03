package com.gcal.app.ui.view_model.leaderboardViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcal.app.model.modelFacade.*
import com.gcal.app.model.modelFacade.general.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Acts as the bridge between the View (UI) and the Model (Data/Network).
 * Responsible for transforming raw, unformatted domain data into UI-ready states
 * so the View layer remains completely passive and logic-free.
 */
class LeaderboardViewModel(
    private val model: ModelFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    // Kept in the ViewModel to prevent hardcoding business logic in the View layer.
    private val XP_PER_LEVEL = 100

    companion object {
        private const val TAG = "LeaderboardViewModel"
    }
    private var observeJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: LeaderboardUiEvent) {
        Log.d(TAG, "onEvent triggered: ${event::class.simpleName}")
        when (event) {
            is LeaderboardUiEvent.OnRefresh,
            is LeaderboardUiEvent.OnRetryClicked,
            is LeaderboardUiEvent.OnScreenEntered -> {
                Log.d(TAG, "Routing event to observeLeaderboardData()")
                observeLeaderboardData()
            }
            is LeaderboardUiEvent.OnBackClicked -> {
                // Currently unhandled. Intended for future routing/coordinator logic
                // if we need to perform cleanup before the screen pops.
            }
        }
    }

    private fun observeLeaderboardData() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            Log.d(TAG, "Setting screenState to LOADING")
            _uiState.update { it.copy(screenState = LeaderboardScreenState.LOADING) }

            try {
                Log.d(TAG, "Subscribing to model.globalLeaderboard() flow...")
                model.globalLeaderboard().collect { users ->
                    Log.d(TAG, "Flow emitted new list with ${users.size} users.")
                    if (users.isEmpty()) {
                        Log.w(TAG, "Warning: Received empty user list from ModelFacade.")
                    }

                    // Defensive programming: We sort the list here rather than trusting
                    // the Model layer's output. This guarantees the UI displays the
                    // leaderboard correctly even if the backend/DB returns unordered data.
                    val sortedUsers = users.sortedByDescending { it.experiencePoints().value() }

                    // Mapping is done here so the UI only has to render a static list,
                    // unaware of how ranks are calculated (based on list index).
                    val uiEntries = sortedUsers.mapIndexed { index, user ->
                        mapToUiModel(user, index + 1)
                    }

                    _uiState.update {
                        Log.d(TAG, "Updating state to LOADED with ${uiEntries.size} entries.")
                        it.copy(
                            screenState = LeaderboardScreenState.LOADED,
                            entries = uiEntries,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "ObserveJob cancelled safely (expected behavior).")
                    throw e
                }

                Log.e(TAG, "Real error caught in observeLeaderboardData: ${e.message}", e)
                _uiState.update {
                    it.copy(screenState = LeaderboardScreenState.ERROR, errorMessage = e.message)
                }
            }
        }
    }

    private fun mapToUiModel(user: User, rank: Int): LeaderboardEntryUi {
        val totalXp = user.experiencePoints().value()
        if (totalXp < 0) {
            Log.w(TAG, "Edge-Case detected: User ${user.username()} has negative XP ($totalXp).")
        }
        val level = (totalXp / XP_PER_LEVEL) + 1
        val remainder = totalXp % XP_PER_LEVEL

        // Calculated in advance because the UI progress bar requires a normalized Float (0.0 to 1.0).
        // Forcing the UI to do this math would leak business logic into the View layer.
        val progress = remainder.toFloat() / XP_PER_LEVEL.toFloat()

        return LeaderboardEntryUi(
            rank = rank,
            username = user.name(),
            level = level,
            starCount = level,
            xpDisplayString = "XP: $totalXp",
            nextLevelProgress = progress
        )
    }
}