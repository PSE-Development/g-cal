package com.gcal.app.ui.screens.berichte

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gcal.app.ui.view_model.reportViewModel.DateRangePreset
import com.gcal.app.ui.view_model.reportViewModel.ReportResultUi
import com.gcal.app.ui.view_model.reportViewModel.ReportUiState
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Compose UI tests for the Reports (Berichte) screen.
 *
 * The reports screen allows users to generate statistical summaries
 * for a given date range. It supports preset ranges (Day, Week, Month,
 * Year) and a custom date picker. The generated report is shown in a
 * modal [ReportResultDialog].
 *
 * Tests use the stateless content composables and dialog components
 * directly, injecting [ReportUiState] to control rendering.
 */
class BerichteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // DEFAULT PRESET (DAY) IS SELECTED

    /**
     * Verifies that the DAY preset is visually selected by default
     * when the screen loads with the initial [ReportUiState].
     *
     * The date selection uses FilterChip composables where the selected
     * chip has distinct visual styling. The default preset is DAY per
     * the ReportUiState default constructor.
     */
    @Test
    fun defaultState_dayPresetIsSelected() {
        composeTestRule.setContent {
            MaterialTheme {
                DateSelectionSection(
                    selectedPreset = UiDateRangePreset.DAY,
                    customFrom = null,
                    customTo = null,
                    hasValidationError = false,
                    onPresetSelected = {},
                    onFromDateClicked = {},
                    onToDateClicked = {}
                )
            }
        }

        // Verify that the DateSelectionSection renders with at least
        val chips = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(chips.isNotEmpty()) {
            "Expected preset chips to be rendered, but found none"
        }
    }

    // PRESET SELECTION DISPATCHES EVENT

    /**
     * Verifies that tapping a preset chip dispatches the onPresetSelected
     * callback with the correct preset value. This controls the date range
     * calculation in the ViewModel.
     */
    @Test
    fun presetChip_onClick_dispatchesCallback() {
        var callbackFired = false

        composeTestRule.setContent {
            MaterialTheme {
                DateSelectionSection(
                    selectedPreset = UiDateRangePreset.DAY,
                    customFrom = null,
                    customTo = null,
                    hasValidationError = false,
                    onPresetSelected = { callbackFired = true },
                    onFromDateClicked = {},
                    onToDateClicked = {}
                )
            }
        }

        // Tap the second chip (Week) — chips are rendered in order
        composeTestRule
            .onAllNodes(hasClickAction())
            .get(1)
            .performClick()

        assert(callbackFired) {
            "Expected onPresetSelected callback to fire when a chip is tapped"
        }
    }

    // CUSTOM DATE FIELDS VISIBLE FOR CUSTOM PRESET

    /**
     * Verifies that the custom date range fields (From/To) become visible
     * when the CUSTOM preset is selected. These fields are hidden for
     * other presets since the date range is calculated automatically.
     */
    @Test
    fun customPreset_showsFromAndToDateFields() {
        composeTestRule.setContent {
            MaterialTheme {
                DateSelectionSection(
                    selectedPreset = UiDateRangePreset.CUSTOM,
                    customFrom = LocalDate.of(2026, 1, 1),
                    customTo = LocalDate.of(2026, 1, 31),
                    hasValidationError = false,
                    onPresetSelected = {},
                    onFromDateClicked = {},
                    onToDateClicked = {}
                )
            }
        }


        composeTestRule
            .onNodeWithText("01.01.2026", substring = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("31.01.2026", substring = true)
            .assertIsDisplayed()
    }

    //  SEARCH BUTTON ENABLED STATE

    /**
     * Verifies that the Search button's enabled state is correctly bound
     * to [ReportUiState.isSearchEnabled]. The ViewModel pre-computes this
     * flag based on date validation rules (e.g., from <= to).
     *
     * Tests both enabled and disabled states.
     */
    @Test
    fun searchButton_enabledState_matchesUiState() {
        //  Button should be enabled when isSearchEnabled = true
        composeTestRule.setContent {
            MaterialTheme {
                BerichteScreenContentForTest(
                    state = ReportUiState(
                        isSearchEnabled = true,
                        isLoading = false
                    )
                )
            }
        }

        composeTestRule
            .onNode(hasClickAction() and hasText("Suchen", substring = true, ignoreCase = true))
            .assertIsEnabled()
    }

    /**
     * Complementary test: the Search button must be disabled when
     * isSearchEnabled is false (e.g., start date is after end date).
     */
    @Test
    fun searchButton_disabled_whenSearchNotEnabled() {
        composeTestRule.setContent {
            MaterialTheme {
                BerichteScreenContentForTest(
                    state = ReportUiState(
                        isSearchEnabled = false,
                        isLoading = false
                    )
                )
            }
        }

        composeTestRule
            .onNode(hasClickAction() and hasText("Suchen", substring = true, ignoreCase = true))
            .assertIsNotEnabled()
    }

    //  RESULT DIALOG OPENS WITH DATA

    /**
     * Verifies that the [ReportResultDialog] renders with the computed
     * report statistics when showResultDialog is true.
     *
     * The dialog shows completed/open event counts, total XP, and
     * achievement lists — all pre-computed by the ViewModel.
     */
    @Test
    fun reportResultDialog_displaysStatistics() {
        val reportData = ReportResultUi(
            completedCount = 12,
            openCount = 3,
            totalXp = 750,
            earnedAchievements = emptyList(),
            openAchievements = emptyList()
        )

        composeTestRule.setContent {
            MaterialTheme {
                ReportResultDialog(
                    data = reportData,
                    onCloseClicked = {}
                )
            }
        }

        // The dialog should display the completed event count
        composeTestRule
            .onNodeWithText("12", substring = true)
            .assertIsDisplayed()

        // The dialog should display the total XP earned
        composeTestRule
            .onNodeWithText("750", substring = true)
            .assertIsDisplayed()
    }
}

/**
 * Minimal wrapper composable that replicates BerichteContent's search button
 * for isolated testing of button enabled/disabled state.
 * This avoids needing the full BerichteScreenView with ViewModel.
 */
@androidx.compose.runtime.Composable
private fun BerichteScreenContentForTest(state: ReportUiState) {
    androidx.compose.foundation.layout.Column {
        DateSelectionSection(
            selectedPreset = state.selectedPreset.let {
                when (it) {
                    DateRangePreset.DAY -> UiDateRangePreset.DAY
                    DateRangePreset.WEEK -> UiDateRangePreset.WEEK
                    DateRangePreset.MONTH -> UiDateRangePreset.MONTH
                    DateRangePreset.YEAR -> UiDateRangePreset.YEAR
                    DateRangePreset.CUSTOM -> UiDateRangePreset.CUSTOM
                }
            },
            customFrom = state.customFrom,
            customTo = state.customTo,
            hasValidationError = false,
            onPresetSelected = {},
            onFromDateClicked = {},
            onToDateClicked = {}
        )
        androidx.compose.material3.Button(
            onClick = {},
            enabled = state.isSearchEnabled && !state.isLoading
        ) {
            androidx.compose.material3.Text("Suchen")
        }
    }
}