package com.gcal.ui.screens.berichte

/**
 * ReportResultUi — Holds the computed statistics for the report result dialog.
 *
 *
 * @param completedCount Number of completed tasks in the selected time range.
 * @param openCount      Number of open (incomplete) tasks in the selected time range.
 * @param totalXp        Sum of XP earned from completed events in the time range.
 */
data class ReportResultUi(
    val completedCount: Int,
    val openCount: Int,
    val totalXp: Int
) {
    companion object {
        /**
         * Creates an empty result for time ranges with no tasks.
         *
         * Used when the report query returns zero events — the dialog is still
         * displayed with all values set to 0.
         */
        fun empty() = ReportResultUi(
            completedCount = 0,
            openCount = 0,
            totalXp = 0
        )
    }
}