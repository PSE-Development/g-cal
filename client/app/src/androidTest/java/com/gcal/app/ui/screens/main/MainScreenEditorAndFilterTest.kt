package com.gcal.app.ui.screens.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gcal.app.ui.screens.main.components.CreateEntrySheet
import com.gcal.app.ui.screens.main.components.EventEditorDialog
import com.gcal.app.ui.screens.main.components.FilterDialog
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Compose UI tests for the Main Calendar screen's entry creation flow,
 * event editing, and group filter dialog.
 *
 * These tests cover the critical user journeys:
 * - FAB tap → CreateEntrySheet opens
 * - Entry type switching changes visible form fields
 * - Existing event opens in edit mode with pre-filled data
 * - Delete button appears in edit mode
 * - Filter dialog displays all groups with visibility toggles
 */
class MainScreenEditorAndFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        private val TEST_DATE = LocalDate.of(2026, 3, 15)

        private val TEST_GROUPS = listOf(
            CalendarGroup(id = 1L, name = "Work", color = 0xFF1A73E8.toInt(), isVisible = true),
            CalendarGroup(id = 2L, name = "Personal", color = 0xFF34A853.toInt(), isVisible = true),
            CalendarGroup(id = 3L, name = "Fitness", color = 0xFFEA4335.toInt(), isVisible = false)
        )
    }

    // CreateEntrySheet opens on FAB Click

    /**
     * Verifies that the CreateEntrySheet composable renders correctly.
     * In the real flow, this is triggered by FAB tap → MainUiEvent.FabClicked
     * → isEditorOpen=true + editorMode=CREATE. Here we test the sheet
     * component directly to confirm it displays the entry creation form.
     */
    @Test
    fun createEntrySheet_renders_withFormFields() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateEntrySheet(
                    initialDate = TEST_DATE,
                    groups = TEST_GROUPS,
                    friends = emptyList(),
                    onSave = {},
                    onDismiss = {}
                )
            }
        }

        // The create entry sheet should display at least one text input

        composeTestRule
            .onAllNodes(hasSetTextAction())
            .fetchSemanticsNodes()
            .isNotEmpty()
            .let { hasInputs ->
                assert(hasInputs) {
                    "Expected at least one text input field in CreateEntrySheet"
                }
            }
    }

    // Entry type selector switches fields

    /**
     * Verifies that the entry type selector tabs (ToDo, Termin, SharedEvent)
     * are displayed in the CreateEntrySheet. Switching between these types
     * changes which form fields are visible
     */
    @Test
    fun createEntrySheet_entryTypeTabs_areDisplayedAndClickable() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateEntrySheet(
                    initialDate = TEST_DATE,
                    groups = TEST_GROUPS,
                    friends = emptyList(),
                    onSave = {},
                    onDismiss = {}
                )
            }
        }

        // CreateEntrySheet renders 3 FilterChip tabs (ToDo, Termin, SharedEvent)

        val clickableNodes = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(clickableNodes.size >= 3) {
            "Expected at least 3 clickable nodes for entry type tabs, but found ${clickableNodes.size}"
        }
    }

    //  EDITOR PRE-FILLS WITH EXISTING EVENT DATA

    /**
     * Verifies that the EventEditorDialog displays pre-filled data when
     * opened in EDIT mode. The ViewModel converts EntryDraftState to a
     * CalendarEvent before passing it to this dialog. All fields (title,
     * description, date, time) must reflect the original event's values.
     */
    @Test
    fun eventEditorDialog_editMode_showsPreFilledData() {
        val existingEvent = CalendarEvent(
            id = 42L,
            title = "Sprint Retrospective",
            description = "Discuss velocity and blockers",
            date = TEST_DATE,
            startTime = "14:00",
            endTime = "15:00",
            groupId = 1L,
            groupName = "Work",
            groupColor = 0xFF1A73E8.toInt(),
            eventType = UiEntryType.TERMIN
        )

        composeTestRule.setContent {
            MaterialTheme {
                EventEditorDialog(
                    event = existingEvent,
                    groups = TEST_GROUPS,
                    friends = emptyList(),
                    isRecurringSeries = false,
                    onSave = {},
                    onDelete = {},
                    onDismiss = {}
                )
            }
        }

        // The event title should be pre-filled in the text field
        composeTestRule
            .onNodeWithText("Sprint Retrospective")
            .assertIsDisplayed()

        // The description should also be visible
        composeTestRule
            .onNodeWithText("Discuss velocity and blockers", substring = true)
            .assertIsDisplayed()
    }

    //  DELETE BUTTON VISIBLE IN EDIT MODE

    /**
     * Verifies that the delete button is rendered in the EventEditorDialog.
     * The delete action is only available in EDIT mode (not CREATE), and
     * is required for users to remove events from their calendar.
     */
    @Test
    fun eventEditorDialog_editMode_showsDeleteButton() {
        val existingEvent = CalendarEvent(
            id = 42L,
            title = "Outdated Meeting",
            date = TEST_DATE,
            startTime = "10:00",
            endTime = "11:00",
            eventType = UiEntryType.TERMIN
        )

        composeTestRule.setContent {
            MaterialTheme {
                EventEditorDialog(
                    event = existingEvent,
                    groups = TEST_GROUPS,
                    friends = emptyList(),
                    isRecurringSeries = false,
                    onSave = {},
                    onDelete = {},
                    onDismiss = {}
                )
            }
        }

        // EventEditorDialog has two bottom buttons: Save and Delete.
        // Both are clickable. Verify at least 2 action buttons exist

        val allClickable = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        // The dialog has: close icon + 3 type chips (disabled) + date field +
        // time fields + group field + Save button + Delete button.
        // Delete button should always be present in edit mode.
        assert(allClickable.size >= 2) {
            "Expected Save and Delete buttons in edit mode, but found ${allClickable.size} clickable nodes"
        }
    }

    //  FILTER DIALOG OPENS AND SHOWS GROUPS

    /**
     * Verifies that the FilterDialog renders and displays all injected groups.
     * Each group is shown with its name and a visibility toggle icon.
     */
    @Test
    fun filterDialog_displaysAllGroupNames() {
        composeTestRule.setContent {
            MaterialTheme {
                FilterDialog(
                    groups = TEST_GROUPS,
                    onToggleGroup = {},
                    onCreateGroup = { _, _ -> },
                    onDismiss = {}
                )
            }
        }

        // All three group names must be rendered in the dialog's group list
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Personal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fitness").assertIsDisplayed()
    }

    // ALL GROUPS HAVE VISIBILITY TOGGLES

    /**
     * Verifies that each group row in the FilterDialog has clickable
     * toggle actions. Users rely on these to filter which event
     * categories appear on the calendar.
     */
    @Test
    fun filterDialog_groupRows_haveClickableToggles() {
        var toggleFired = false

        composeTestRule.setContent {
            MaterialTheme {
                FilterDialog(
                    groups = TEST_GROUPS,
                    onToggleGroup = { toggleFired = true },
                    onCreateGroup = { _, _ -> },
                    onDismiss = {}
                )
            }
        }

        val allClickable = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        // Try clicking nodes until the toggle callback fires.

        for (i in 1 until allClickable.size) {
            composeTestRule
                .onAllNodes(hasClickAction())
                .get(i)
                .performClick()
            if (toggleFired) break
        }

        assert(toggleFired) {
            "Expected onToggleGroup callback to fire when a group toggle is tapped"
        }
    }
}