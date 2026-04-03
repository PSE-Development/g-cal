package com.gcal.app.ui.view_model.reportViewModel

import com.gcal.app.model.modelFacade.general.Achievement
import java.time.LocalDate

// --- Enums ---

/**
 * Dictates the active time-filtering strategy for the report.
 *
 * Using an explicit [CUSTOM] state allows the ViewModel to cleanly disconnect
 * the UI's manual date pickers from the automatic calendar math applied by
 * the other presets. The ViewModel translates each preset into concrete
 * LocalDateTime boundaries inside [ReportViewModel.calculateTimeRange].
 */
enum class DateRangePreset {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    CUSTOM
}

/**
 * Represents strongly-typed failure modes specific to report generation.
 *
 * Segregating [Validation] (e.g., end date before start date) from [Network]
 * errors allows the View to render them differently — inline red text below
 * the date picker for validation, but a global Snackbar for network drops.
 */
sealed interface UiErrorType {
    /** Start date is after end date — shown as inline text near the date picker. */
    data object Validation : UiErrorType

    /** Backend or connectivity failure — shown as a Snackbar with the error message. */
    data class Network(val message: String) : UiErrorType

    /** Catch-all for unexpected errors. */
    data object Unknown : UiErrorType
}

// --- View Data Models ---

/**
 * Pre-aggregated data package for the report result dialog.
 *
 * This UI model is built by the ViewModel from raw Model-layer data so the
 * View never has to perform filtering, counting, or math on the UI thread.
 *
 * Fields:
 * - [completedCount] / [openCount]: Pre-counted event totals.
 * - [totalXp]: Sum of experience points from all completed events in the
 *   selected time range. Required by the Entwurf (page 58-59) which specifies:
 *   *"Der Dialog enthält: ... 3. Die Summe der in diesem Zeitraum gesammelten XP."*
 * - [earnedAchievements] / [openAchievements]: Full [Achievement] objects so the
 *   View has immediate access to names and descriptions for rich UI rendering,
 *   without needing secondary data requests.
 */
data class ReportResultUi(
    val completedCount: Int = 0,
    val openCount: Int = 0,


    /**
     * Sum of XP earned from completed events within the report's time range.
     *
     * Computed by the ViewModel as:
     *   completedEvents.sumOf { it.experiencePoints().value() }
     *
     * Displayed in the report result dialog's "Gesammelte XP" row.
     */
    val totalXp: Int = 0,

    /**
     * Achievements the user has earned in the selected time range.
     *
     * Passed as full [Achievement] objects (not just IDs) so the View can
     * display achievement names and descriptions without secondary lookups.
     */
    val earnedAchievements: List<Achievement> = emptyList(),

    /**
     * Achievements the user has NOT yet earned — the complement of [earnedAchievements]
     * against the full pool returned by [ModelFacade.getAllAchievements()].
     */
    val openAchievements: List<Achievement> = emptyList()
)

// --- Main State ---

/**
 * Single source of truth for the Report screen's filter configuration and results.
 *
 * By combining the filter configuration and the result data into one state object,
 * we guarantee that the UI never displays a report result that doesn't match
 * the currently selected date range.
 *
 * The ViewModel updates this atomically via [MutableStateFlow.update {}], and the
 * View collects it as StateFlow for purely declarative rendering.
 */
data class ReportUiState(

    // --- Filter Configuration ---
    val selectedPreset: DateRangePreset = DateRangePreset.DAY,

    // Kept nullable to represent an uninitialized custom range.
    // The View should display a placeholder (e.g., "Datum wählen") when null.
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,

    // --- UI Logic Flags ---

    /**
     * Pre-computed authorization for the "Generate Report" button.
     *
     * Continuously evaluated by the ViewModel based on date validation
     * (e.g., from <= to). Enforces the "Dumb View" pattern by keeping
     * validation rules out of the Composable layer.
     */
    val isSearchEnabled: Boolean = true,

    // Central lock for the UI during heavy database/network queries.
    val isLoading: Boolean = false,

    // Declarative trigger for the result modal overlay.
    val showResultDialog: Boolean = false,

    // --- Data & Error ---

    // Holds the actual statistics once generated. Null implies no report has been run yet.
    val reportResult: ReportResultUi? = null,

    val activeError: UiErrorType? = null
)
