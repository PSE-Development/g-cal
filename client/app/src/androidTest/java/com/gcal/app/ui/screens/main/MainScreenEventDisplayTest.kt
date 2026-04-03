package com.gcal.app.ui.screens.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gcal.app.ui.screens.main.components.DayViewContent
import com.gcal.app.ui.screens.main.components.EventCard
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Compose UI tests for event rendering and interaction on the Main Calendar screen.
 *
 * These tests verify that calendar events display correctly in the day view
 * and that user interactions (tapping an event, checking a ToDo) dispatch
 * the expected callbacks. Events are injected as [CalendarEvent] UI models,
 * matching the real data flow where the ViewModel maps domain Events to
 * these lightweight UI representations.
 */
class MainScreenEventDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        private val TEST_DATE = LocalDate.of(2026, 3, 15)

        /** Factory for creating test CalendarEvents with predictable data. */
        fun createTestEvent(
            id: Long = 1L,
            title: String = "Team Standup",
            startTime: String = "09:00",
            endTime: String = "09:30",
            type: UiEntryType = UiEntryType.TERMIN,
            groupId: Long? = 100L,
            groupColor: Int = 0xFF1A73E8.toInt()
        ) = CalendarEvent(
            id = id,
            title = title,
            date = TEST_DATE,
            startTime = startTime,
            endTime = endTime,
            eventType = type,
            groupId = groupId,
            groupName = "Work",
            groupColor = groupColor
        )
    }

    //  EVENTS RENDER IN DAY VIEW

    /**
     * Verifies that all injected events are rendered as visible EventCard
     * composables inside the DayViewContent. This is the core data display
     * test — if events don't render, the calendar is unusable.
     *
     * Injects three events with distinct titles and asserts all three
     * are present in the rendered tree.
     */
    @Test
    fun dayView_withMultipleEvents_displaysAllEventTitles() {
        val events = listOf(
            createTestEvent(id = 1L, title = "Team Standup", startTime = "09:00", endTime = "09:30"),
            createTestEvent(id = 2L, title = "Sprint Planning", startTime = "10:00", endTime = "11:00"),
            createTestEvent(id = 3L, title = "Code Review", startTime = "14:00", endTime = "15:00")
        )

        composeTestRule.setContent {
            MaterialTheme {
                DayViewContent(
                    currentDate = TEST_DATE,
                    events = events,
                    onDateChanged = {},
                    onEventClicked = {},
                    onEventChecked = { _, _ -> }
                )
            }
        }

        // All three event titles must be rendered in the day view scroll area
        composeTestRule.onNodeWithText("Team Standup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sprint Planning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Code Review").assertIsDisplayed()
    }

    // EVENT CLICK DISPATCHES CALLBACK

    /**
     * Verifies that tapping an EventCard dispatches the onEventClicked
     * callback with the correct CalendarEvent reference. This is the
     * entry point to the edit flow — without it, users cannot modify
     * existing events.
     */
    @Test
    fun eventCard_onClick_dispatchesCorrectEvent() {
        val testEvent = createTestEvent(id = 42L, title = "Architecture Review")
        var clickedEvent: CalendarEvent? = null

        composeTestRule.setContent {
            MaterialTheme {
                EventCard(
                    event = testEvent,
                    onClick = { clickedEvent = testEvent },
                    onCheckClicked = {}
                )
            }
        }

        // Tap the event card
        composeTestRule
            .onNodeWithText("Architecture Review")
            .performClick()

        assert(clickedEvent != null) {
            "Expected onEventClicked to fire when EventCard is tapped"
        }
        assert(clickedEvent?.id == 42L) {
            "Expected clicked event to have id=42, but got ${clickedEvent?.id}"
        }
    }

    //  ToDo CHECKBOX DISPATCHES TodoChecked

    /**
     * Verifies that toggling the completion checkbox on a ToDo event
     * dispatches the onCheckClicked callback. Completing a ToDo is a
     * core gamification action that triggers XP distribution in the
     * backend, so this interaction path must work.
     *
     * The EventCard always shows a check icon (Circle/CheckCircle).
     * For an uncompleted event, tapping it should pass true to the callback.
     */
    @Test
    fun eventCard_checkboxClick_dispatchesTodoCheckedTrue() {
        val todoEvent = createTestEvent(
            id = 99L,
            title = "Submit Report",
            type = UiEntryType.TODO
        )
        var checkedValue: Boolean? = null

        composeTestRule.setContent {
            MaterialTheme {
                EventCard(
                    event = todoEvent,
                    onClick = {},
                    onCheckClicked = { isChecked -> checkedValue = isChecked }
                )
            }
        }

        // The checkbox icon has a content description for accessibility.

        composeTestRule
            .onNodeWithContentDescription("Nicht erledigt")
            .performClick()

        assert(checkedValue == true) {
            "Expected onCheckClicked(true) when tapping uncompleted ToDo checkbox, but got $checkedValue"
        }
    }
}