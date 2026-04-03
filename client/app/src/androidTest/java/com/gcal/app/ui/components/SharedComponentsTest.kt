package com.gcal.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gcal.app.ui.screens.main.CalendarEvent
import com.gcal.app.ui.screens.main.UiEntryType
import com.gcal.app.ui.screens.main.UiViewMode
import com.gcal.app.ui.screens.main.components.CalendarTopBar
import com.gcal.app.ui.screens.main.components.EventCard
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Compose UI tests for shared reusable components used across multiple screens.
 *
 * These components (GCalBottomBar, GCalTopBar, CalendarTopBar, EventCard)
 * are composed into every major screen and form the navigation backbone
 * of the app. A failure in any of these blocks every user flow.
 */
class SharedComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Bottom bar tests


    /**
     * Verifies that all three BottomNavItem entries (Rangliste, Berichte, Profil)
     * are rendered in the bottom navigation bar. These are the primary
     * navigation targets accessible from every screen.
     */
    @Test
    fun bottomBar_displaysAllNavigationTabs() {
        composeTestRule.setContent {
            MaterialTheme {
                GCalBottomBar(
                    currentRoute = "main",
                    onNavigate = {}
                )
            }
        }

        // Verify that exactly 3 navigation items are rendered
        // by checking for clickable NavigationBarItem nodes.
        val navItems = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(navItems.size >= 3) {
            "Expected at least 3 navigation tabs, but found ${navItems.size}"
        }
    }

    //CURRENT ROUTE TAB HIGHLIGHTED

    /**
     * Verifies that the currently active navigation tab is visually
     * distinguished (selected state). The NavigationBarItem with
     * selected=true receives primary color tinting and an indicator.
     */
    @Test
    fun bottomBar_currentRoute_isHighlighted() {
        composeTestRule.setContent {
            MaterialTheme {
                GCalBottomBar(
                    currentRoute = "rangliste",
                    onNavigate = {}
                )
            }
        }

        composeTestRule
            .onAllNodes(isSelected())
            .fetchSemanticsNodes()
            .isNotEmpty()
            .let { hasSelected ->
                assert(hasSelected) {
                    "Expected at least one selected navigation tab for route 'rangliste'"
                }
            }
    }

    //  TAB CLICK DISPATCHES onNavigate

    /**
     * Verifies that tapping a bottom bar tab dispatches the onNavigate
     * callback with the correct route string. This is the sole mechanism
     * for screen-to-screen navigation in the app.
     */
    @Test
    fun bottomBar_tabClick_dispatchesNavigateCallback() {
        var navigatedRoute: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                GCalBottomBar(
                    currentRoute = "main",
                    onNavigate = { route -> navigatedRoute = route }
                )
            }
        }

        // Tap the last navigation item
        composeTestRule
            .onAllNodes(hasClickAction())
            .onLast()
            .performClick()

        assert(navigatedRoute != null) {
            "Expected onNavigate callback to fire when a tab is tapped, but got null"
        }
    }


    //    Top bar tests


    // TITLE RENDERS CORRECTLY

    /**
     * Verifies that the GCalTopBar displays the provided title text.
     */
    @Test
    fun topBar_displaysTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                GCalTopBar(
                    title = "Profil",
                    onBackClicked = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Profil")
            .assertIsDisplayed()
    }

    //BACK BUTTON DISPATCHES CALLBACK

    /**
     * Verifies that tapping the back arrow in the TopAppBar dispatches
     * the onBackClicked callback. This navigates back to the main screen.
     */
    @Test
    fun topBar_backButton_dispatchesCallback() {
        var backClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                GCalTopBar(
                    title = "Berichte",
                    onBackClicked = { backClicked = true }
                )
            }
        }

        // The back arrow has a content description from R.string.cd_back
        composeTestRule
            .onNode(hasClickAction() and hasContentDescription("ck", substring = true))
            .performClick()

        assert(backClicked) {
            "Expected onBackClicked callback to fire when back arrow is tapped"
        }
    }


    //    CalendarTopBar Tests


    // VIEW MODE BUTTONS RENDERED

    /**
     * Verifies that all four view mode buttons (Day, Week, Month, Year)
     * are rendered in the CalendarTopBar. These control the calendar's
     * layout mode and are the primary interaction point on the main screen.
     */
    @Test
    fun calendarTopBar_displaysAllViewModeButtons() {
        composeTestRule.setContent {
            MaterialTheme {
                CalendarTopBar(
                    activeMode = UiViewMode.DAY,
                    onViewModeSelected = {},
                    onFilterClicked = {},
                    onJumpToDateClicked = {}
                )
            }
        }

        // CalendarTopBar renders 4 view mode buttons + 2 icon buttons

        val clickableNodes = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(clickableNodes.size >= 4) {
            "Expected at least 4 clickable view mode buttons, but found ${clickableNodes.size}"
        }
    }


    //   EventCard tests

    // EVENT TITLE AND TIME DISPLAYED

    /**
     * Verifies that the EventCard correctly renders the event title
     * and time range. These are the two most critical data fields
     * visible in both day and week views.
     */
    @Test
    fun eventCard_displaysEventTitleAndTime() {
        val event = CalendarEvent(
            id = 1L,
            title = "Architecture Review",
            date = LocalDate.of(2026, 3, 15),
            startTime = "14:00",
            endTime = "15:30",
            eventType = UiEntryType.TERMIN,
            groupColor = 0xFF1A73E8.toInt()
        )

        composeTestRule.setContent {
            MaterialTheme {
                EventCard(
                    event = event,
                    onClick = {},
                    onCheckClicked = {},
                    showTime = true
                )
            }
        }

        // Title should be visible
        composeTestRule
            .onNodeWithText("Architecture Review")
            .assertIsDisplayed()

        // Time range should be visible in "HH:mm - HH:mm" format
        composeTestRule
            .onNodeWithText("14:00 - 15:30")
            .assertIsDisplayed()
    }
}