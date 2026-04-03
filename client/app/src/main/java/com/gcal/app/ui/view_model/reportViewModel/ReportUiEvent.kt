package com.gcal.app.ui.view_model.reportViewModel

import java.time.LocalDate

/**
 * Defines the strict interaction contract for the Report/Analytics screen.
 * By routing all inputs (especially complex date range selections) through this interface,
 * the ViewModel can perform continuous validation (e.g., ensuring "start date" is strictly
 * before "end date") before allowing the View to trigger a heavy database search.
 */
sealed interface ReportUiEvent {

    // --- Navigation & Dialog Management ---

    /**
     * Delegates back navigation to the ViewModel.
     * Allows the ViewModel to potentially intercept the action (e.g., to gracefully cancel
     * ongoing heavy database queries for the report) before instructing the router to pop the screen.
     */
    data object OnBackClicked : ReportUiEvent

    /**
     * Clears the result dialog state.
     * Managing dialog visibility in the ViewModel ensures that if the user rotates
     * their device while viewing a generated report, the dialog isn't accidentally
     * dismissed or duplicated upon recreation.
     */
    data object OnCloseDialogClicked : ReportUiEvent

    /**
     * Acknowledges transient error states (like Snackbars).
     * Crucial for StateFlow UIs: explicitly tells the ViewModel to reset the error flag
     * so the View doesn't endlessly reshow the error on every recomposition.
     */
    data object ErrorDismissed : ReportUiEvent

    // --- Date Range Selection ---

    /**
     * Triggers an automatic calculation of start and end dates.
     * The View simply passes the requested abstract preset (e.g., "This Week"),
     * and the ViewModel handles the complex calendar math, updating the exact dates
     * in the UI state for the View to render.
     */
    data class OnPresetSelected(val preset: DateRangePreset) : ReportUiEvent

    /**
     * Streams manual date selections to the ViewModel.
     * WHY: This enables real-time business validation. The ViewModel can instantly
     * check if the new start date is chronologically valid against the existing end date,
     * and automatically disable the "Search" button in the state if the range is invalid.
     */
    data class OnDatePicked(val date: LocalDate, val isStartDate: Boolean) : ReportUiEvent

    // --- Data Fetching Actions ---

    /**
     * Initiates the report generation based on the currently validated date range.
     * The View doesn't need to pass the dates as parameters here because the ViewModel
     * already holds the validated single source of truth in its state.
     */
    data object OnSearchClicked : ReportUiEvent

    /**
     * Re-attempts the report generation after a previous failure.
     * Kept as a distinct event from [OnSearchClicked] to explicitly signal error recovery,
     * allowing the ViewModel to potentially bypass caches or log specific retry analytics.
     */
    data object OnRetryClicked : ReportUiEvent
}
