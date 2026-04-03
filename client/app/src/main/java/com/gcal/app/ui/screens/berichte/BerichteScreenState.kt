package com.gcal.ui.screens.berichte

import java.time.LocalDate
import com.gcal.app.ui.view_model.reportViewModel.UiErrorType

/**
 * BerichteScreenState — Legacy screen state for the Reports tab.
 *
 *
 * @param selectedPreset    The active date filter preset (default: DAY).
 * @param customFrom        Start date for custom range (null if not using CUSTOM).
 * @param customTo          End date for custom range (null if not using CUSTOM).
 * @param isSearchEnabled   Whether the search button is enabled (date validation state).
 * @param isLoading         Whether the report is currently being computed.
 * @param reportResult      The computed statistics (null if no report has been generated).
 * @param showResultDialog  Controls the visibility of the result overlay dialog.
 * @param activeError       The current error state (null if no error).
 * @param showFromDatePicker Controls visibility of the start date picker.
 * @param showToDatePicker  Controls visibility of the end date picker.
 */
data class BerichteScreenState(
    val selectedPreset: DateRangePreset = DateRangePreset.DAY,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val isSearchEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val reportResult: ReportResultUi? = null,
    val showResultDialog: Boolean = false,
    val activeError: UiErrorType? = null,
    val showFromDatePicker: Boolean = false,
    val showToDatePicker: Boolean = false
) {
    companion object {
        /** Creates the default initial state with no report loaded. */
        fun initial() = BerichteScreenState()
    }
}