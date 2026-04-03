package com.gcal.app.ui.view_model.reportViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcal.app.model.modelFacade.ModelFacade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Manages the generation of user activity reports and achievement tracking.
 * This ViewModel centralizes complex calendar mathematics and data aggregation,
 * ensuring the View layer only deals with high-level presets and pre-formatted results.
 */
class ReportViewModel(
    private val model: ModelFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    companion object{
        private const val TAG = "ReportViewModel"
    }

    init {
        // Initializes with today's date to ensure the custom date pickers
        // have a valid baseline even before the user interacts with them.
        val today = LocalDate.now()
        _uiState.update {
            it.copy(customFrom = today, customTo = today)
        }
        uiState.onEach { state ->
            Log.d(TAG, "-> OUT (New State): preset=${state.selectedPreset}, searchEnabled=${state.isSearchEnabled}, loading=${state.isLoading}, error=${state.activeError}")
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ReportUiEvent) {
        Log.d(TAG, "<- IN (View Event): $event")
        when (event) {
            // --- Navigation ---
            is ReportUiEvent.OnBackClicked -> {
                /* Navigation handled by View layer */
            }
            is ReportUiEvent.OnCloseDialogClicked -> {
                _uiState.update { it.copy(showResultDialog = false) }
            }
            is ReportUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(activeError = null) }
            }

            // --- Selection ---
            is ReportUiEvent.OnPresetSelected -> {
                _uiState.update {
                    it.copy(
                        selectedPreset = event.preset,
                        // Presets are pre-validated in code, so we can clear errors immediately.
                        activeError = null
                    )
                }
                validateSearchState()
            }
            is ReportUiEvent.OnDatePicked -> handleDatePicked(event.date, event.isStartDate)

            // --- Actions ---
            is ReportUiEvent.OnSearchClicked -> generateReport()
            is ReportUiEvent.OnRetryClicked -> generateReport()
        }
    }

    /**
     * Synchronizes manual date picking with the UI state.
     * Automatically switches the preset to [DateRangePreset.CUSTOM] to reflect
     * that the user has diverged from standard timeframes.
     */
    private fun handleDatePicked(date: LocalDate, isStart: Boolean) {
        _uiState.update { state ->
            val newFrom = if (isStart) date else state.customFrom
            val newTo = if (!isStart) date else state.customTo

            state.copy(
                selectedPreset = DateRangePreset.CUSTOM,
                customFrom = newFrom,
                customTo = newTo
            )
        }
        validateSearchState()
    }

    /**
     * Evaluates the chronological integrity of the selected range.
     * By preventing isSearchEnabled from being true during invalid ranges (Start > End),
     * we avoid sending nonsensical queries to the Model layer.
     */
    private fun validateSearchState() {
        _uiState.update { state ->
            // Presets are always valid
            if (state.selectedPreset != DateRangePreset.CUSTOM) {
                return@update state.copy(isSearchEnabled = true, activeError = null)
            }

            val from = state.customFrom
            val to = state.customTo

            if (from != null && to != null && from.isAfter(to)) {
                state.copy(
                    isSearchEnabled = false,
                    activeError = UiErrorType.Validation
                )
            } else {
                state.copy(
                    isSearchEnabled = true,
                    activeError = null
                )
            }
        }
    }

    /**
     * Aggregates data from multiple Model sources to build the final report.
     * This method demonstrates why a ViewModel is necessary: it takes raw streams (events)
     * and one-shot requests (achievements), performs set logic (unearned = total - earned),
     * and packages them into a single [ReportResultUi] for the View.
     */
    private fun generateReport() {
        val currentState = _uiState.value
        if (!currentState.isSearchEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, activeError = null) }

            // Precise time calculation ensures the backend receives
            // exact nanosecond boundaries for database queries.
            val (start, end) = calculateTimeRange(currentState)
            Log.d(TAG, "-> OUT (Model Request): start=$start, end=$end")

            try {
                val allEvents = model.getEventsIn(start, end).first()
                val countOpen = allEvents.count { !it.checkCompletion().completed() }
                val countDone = allEvents.count { it.checkCompletion().completed() }

                val totalEarnedXp = allEvents
                    .filter { it.checkCompletion().completed() }
                    .sumOf { it.experiencePoints().value() }

                // Fetch the complete "pool" of available achievements from the Model layer.
                val allPossibleResult = model.getAllAchievements()


                // Extract the list from the Result wrapper.
                // Using .getOrDefault ensures 'allPossible' is always a valid List<Achievement>,
                // providing a safe fallback (emptyList) to prevent crashes if the Model call fails.
                val allPossible = allPossibleResult.getOrDefault(emptyList())

                val reportResult = model.generateReport(start, end)

                reportResult.fold(
                    onSuccess = { reportData ->
                        Log.d(TAG, "<- IN (Model Success): ${reportData.completedAchievements().size} achievements earned")
                        val earnedList = reportData.completedAchievements()

                        val unearnedList = allPossible.filter { possible ->
                            earnedList.none { earned ->
                                earned.achievementName() == possible.achievementName()
                            }
                        }

                        val resultUi = ReportResultUi(
                            completedCount = countDone,
                            openCount = countOpen,
                            totalXp = totalEarnedXp,
                            earnedAchievements = earnedList,
                            openAchievements = unearnedList
                        )

                        _uiState.update {
                            it.copy(isLoading = false, reportResult = resultUi, showResultDialog = true)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "<- IN (Model Failure): ${exception.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeError = UiErrorType.Network(exception.message ?: "Unbekannter Fehler")
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "<- IN (Model Exception): ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, activeError = UiErrorType.Network(e.message ?: "Unbekannter Fehler"))
                }
            }
        }
    }


    /**
     * Centralized calendar math.
     * Converts abstract UI concepts (like "This Week") into concrete ISO-compliant
     * time boundaries. This ensures consistency across the entire app regardless
     * of which screen requests a time-based report.
     */
    private fun calculateTimeRange(state: ReportUiState): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDate.now()

        val (startDate, endDate) = when (state.selectedPreset) {
            DateRangePreset.DAY -> Pair(now, now)
            DateRangePreset.WEEK -> {
                // Assuming standard ISO week (Mon-Sun)
                val start = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val end = now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                Pair(start, end)
            }
            DateRangePreset.MONTH -> {
                val start = now.withDayOfMonth(1)
                val end = now.with(TemporalAdjusters.lastDayOfMonth())
                Pair(start, end)
            }
            DateRangePreset.YEAR -> {
                val start = now.withDayOfYear(1)
                val end = now.with(TemporalAdjusters.lastDayOfYear())
                Pair(start, end)
            }
            DateRangePreset.CUSTOM -> {
                // Fallback to now if null (should be prevented by validation)
                val start = state.customFrom ?: now
                val end = state.customTo ?: now
                Pair(start, end)
            }
        }

        return Pair(
            startDate.atStartOfDay(),
            endDate.atTime(LocalTime.MAX) // Spans the entire final day until 23:59:59.999
        )
    }
}