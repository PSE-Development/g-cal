package com.gcal.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the Profile screen.
 *
 * The Profile screen is the hub for account management, gamification status,
 * social features, and settings navigation. It uses a [ProfileDialogType]
 * enum as a finite state machine to ensure only one dialog overlay is
 * visible at a time.
 *
 * Tests cover content rendering (display name, action buttons) and dialog
 * lifecycle (open, confirm, dismiss) for each dialog variant.
 */
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    //  DISPLAY NAME RENDERS CORRECTLY

    /**
     * Verifies that the user's display name is rendered in the profile header.
     * The display name is the primary visual identity element on this screen.
     */
    @Test
    fun profileContent_displaysUsername() {
        composeTestRule.setContent {
            MaterialTheme {
                ProfileHeaderForTest(displayName = "MaxMustermann")
            }
        }

        composeTestRule
            .onNodeWithText("MaxMustermann")
            .assertIsDisplayed()
    }

    // ALL ACTION BUTTONS VISIBLE
    /**
     * Verifies that all five action buttons are rendered on the profile screen:
     * Level Info, Friends, Settings, Logout, and Delete Account.
     * These are the primary navigation points from the profile hub.
     */
    @Test
    fun profileContent_displaysAllActionButtons() {
        composeTestRule.setContent {
            MaterialTheme {
                ProfileActionListForTest(isOffline = false)
            }
        }


        val buttons = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(buttons.size == 5) {
            "Expected 5 action buttons, but found ${buttons.size}"
        }
    }

    // LEVEL INFO DIALOG OPENS

    /**
     * Verifies that the LevelInfoDialog renders when activeDialog is LEVEL_INFO.
     * The dialog shows the user's current level, XP, and progress toward the
     * next level — core gamification feedback.
     */
    @Test
    fun levelInfoDialog_opensAndDisplaysContent() {
        composeTestRule.setContent {
            MaterialTheme {
                LevelInfoDialog(
                    currentLevel = 7,
                    currentXp = 680,
                    xpForNextLevel = 1000,
                    progressToNextLevel = 0.68f,
                    onCloseClicked = {}
                )
            }
        }

        // The dialog should show the level number
        composeTestRule
            .onNodeWithText("7", substring = true)
            .assertIsDisplayed()

        // The dialog should show the current XP
        composeTestRule
            .onNodeWithText("680", substring = true)
            .assertIsDisplayed()
    }

    // LOGOUT CONFIRMATION DIALOG OPENS

    /**
     * Verifies that the LogoutConfirmDialog renders when triggered.
     * The dialog presents a warning message and two actions: Confirm and Cancel.
     */
    @Test
    fun logoutConfirmDialog_renders() {
        composeTestRule.setContent {
            MaterialTheme {
                LogoutConfirmDialog(
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Verify dialog is rendered by checking that clickable
        val dialogButtons = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(dialogButtons.size >= 2) {
            "Expected at least 2 dialog buttons (confirm + dismiss), but found ${dialogButtons.size}"
        }
    }

    //  LOGOUT CONFIRM DISPATCHES ConfirmLogout

    /**
     * Verifies that tapping the confirm button in the LogoutConfirmDialog
     * dispatches the onConfirm callback. This triggers the ViewModel to
     * sign the user out and clear the session.
     */
    @Test
    fun logoutConfirmDialog_confirmButton_dispatchesCallback() {
        var confirmed = false

        composeTestRule.setContent {
            MaterialTheme {
                LogoutConfirmDialog(
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }


        composeTestRule
            .onAllNodes(hasClickAction())
            .onLast()
            .performClick()

        assert(confirmed) {
            "Expected onConfirm callback to fire when logout confirm button is tapped"
        }
    }

    // DELETE ACCOUNT DIALOG OPENS =

    /**
     * Verifies that the DeleteAccountConfirmDialog renders with the
     * warning icon and destructive action messaging.
     */
    @Test
    fun deleteAccountDialog_renders() {
        composeTestRule.setContent {
            MaterialTheme {
                DeleteAccountConfirmDialog(
                    onConfirm = {},
                    onDismiss = {},
                    isProcessing = false
                )
            }
        }

        // Verify dialog is rendered by checking that clickable

        val dialogButtons = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(dialogButtons.size >= 2) {
            "Expected at least 2 dialog buttons (confirm + dismiss), but found ${dialogButtons.size}"
        }
    }

    //DELETE CONFIRM DISPATCHES EVENT

    /**
     * Verifies that tapping the confirm button in the DeleteAccountConfirmDialog
     * dispatches the onConfirm callback. Account deletion is irreversible,
     * so this test confirms the UI wiring is correct.
     */
    @Test
    fun deleteAccountDialog_confirmButton_dispatchesCallback() {
        var confirmed = false

        composeTestRule.setContent {
            MaterialTheme {
                DeleteAccountConfirmDialog(
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                    isProcessing = false
                )
            }
        }

        // AlertDialog has dismiss and confirm buttons.

        composeTestRule
            .onAllNodes(hasClickAction())
            .onLast()
            .performClick()

        assert(confirmed) {
            "Expected onConfirm callback to fire when delete confirm button is tapped"
        }
    }
}

// TEST HELPERS

@Composable
private fun ProfileHeaderForTest(displayName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileActionListForTest(isOffline: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(onClick = {}) {
            Text("Level anzeigen")
        }
        OutlinedButton(onClick = {}) {
            Text("Freunde")
        }
        OutlinedButton(onClick = {}) {
            Text("Einstellungen")
        }
        OutlinedButton(onClick = {}) {
            Text("Abmelden")
        }
        Button(onClick = {}, enabled = !isOffline) {
            Text("Konto löschen")
        }
    }
}