package com.gcal.app.ui.screens.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import com.gcal.app.ui.screens.main.components.DayViewContent
import com.gcal.app.ui.screens.main.components.MonthViewContent
import com.gcal.app.ui.screens.main.components.WeekViewContent
import com.gcal.app.ui.screens.main.components.YearViewContent
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Compose UI tests for the Main Calendar screen's view mode rendering.
 *
 * Each view mode (Day, Week, Month, Year) renders a completely different
 * layout Composable. These tests verify that each component renders
 * successfully without crashing and produces a non-empty UI tree.
 */
class MainScreenViewModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        private val TEST_DATE = LocalDate.of(2026, 3, 15)
        private val TEST_MONTH = YearMonth.of(2026, 3)
    }

    //  DAY VIEW RENDERS ON ViewMode.DAY

    /**
     * Verifies that DayViewContent renders without crashing.
     * The day view is the default landing view for most users.
     */
    @Test
    fun dayViewContent_rendersSuccessfully() {
        composeTestRule.setContent {
            MaterialTheme {
                DayViewContent(
                    currentDate = TEST_DATE,
                    events = emptyList(),
                    onDateChanged = {},
                    onEventClicked = {},
                    onEventChecked = { _, _ -> }
                )
            }
        }

        // Verify the root composable is rendered and displayed
        composeTestRule
            .onRoot()
            .assertIsDisplayed()
    }

    // WEEK VIEW RENDERS ON ViewMode.WEEK

    /**
     * Verifies that WeekViewContent renders without crashing.
     * The week view shows a 7-day column layout with time slots.
     */
    @Test
    fun weekViewContent_rendersSuccessfully() {
        composeTestRule.setContent {
            MaterialTheme {
                WeekViewContent(
                    currentDate = TEST_DATE,
                    events = emptyList(),
                    onDateChanged = {},
                    onDaySelected = {},
                    onEventClicked = {}
                )
            }
        }

        composeTestRule
            .onRoot()
            .assertIsDisplayed()
    }

    // MONTH VIEW RENDERS ON ViewMode.MONTH

    /**
     * Verifies that MonthViewContent renders without crashing.
     * The month view displays a full calendar grid for the current month.
     */
    @Test
    fun monthViewContent_rendersSuccessfully() {
        composeTestRule.setContent {
            MaterialTheme {
                MonthViewContent(
                    currentMonth = TEST_MONTH,
                    events = emptyList(),
                    onMonthChanged = {},
                    onDaySelected = {}
                )
            }
        }

        composeTestRule
            .onRoot()
            .assertIsDisplayed()
    }

    //  YEAR VIEW RENDERS ON ViewMode.YEAR

    /**
     * Verifies that YearViewContent renders without crashing.
     * The year view shows a 12-month overview grid for the selected year.
     */
    @Test
    fun yearViewContent_rendersSuccessfully() {
        composeTestRule.setContent {
            MaterialTheme {
                YearViewContent(
                    currentYear = TEST_DATE.year,
                    currentMonth = TEST_MONTH,
                    events = emptyList(),
                    onYearChanged = {},
                    onMonthSelected = {}
                )
            }
        }

        composeTestRule
            .onRoot()
            .assertIsDisplayed()
    }
}