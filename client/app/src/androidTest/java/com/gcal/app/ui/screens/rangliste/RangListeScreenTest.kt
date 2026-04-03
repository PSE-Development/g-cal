package com.gcal.app.ui.screens.rangliste
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardEntryUi
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardScreenState
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardUiState
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the Leaderboard (Rangliste) screen.
 *
 * The screen uses a tri-state model ([LeaderboardScreenState]) to
 * control rendering: LOADING, LOADED, and ERROR. Each state maps to
 * a dedicated Composable (LoadingStateView, LeaderboardList, ErrorStateView).
 *
 * Tests use the stateless [RanglisteScreenContent] composable which
 * accepts a [LeaderboardUiState] directly, allowing state injection
 * without requiring a ViewModel instance.
 */
class RanglisteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        /** Fixture data representing a populated leaderboard. */
        val SAMPLE_ENTRIES = listOf(
            LeaderboardEntryUi(
                rank = 1,
                username = "ProGamer",
                level = 10,
                starCount = 10,
                xpDisplayString = "XP: 1500",
                nextLevelProgress = 0.75f
            ),
            LeaderboardEntryUi(
                rank = 2,
                username = "Alice",
                level = 7,
                starCount = 7,
                xpDisplayString = "XP: 980",
                nextLevelProgress = 0.3f
            ),
            LeaderboardEntryUi(
                rank = 3,
                username = "Bob",
                level = 5,
                starCount = 5,
                xpDisplayString = "XP: 600",
                nextLevelProgress = 0.1f
            ),
            LeaderboardEntryUi(
                rank = 4,
                username = "Charlie",
                level = 3,
                starCount = 3,
                xpDisplayString = "XP: 300",
                nextLevelProgress = 0.5f
            ),
            LeaderboardEntryUi(
                rank = 5,
                username = "Diana",
                level = 2,
                starCount = 2,
                xpDisplayString = "XP: 150",
                nextLevelProgress = 0.2f
            )
        )
    }

    // LOADING STATE RENDERS SPINNER

    /**
     * Verifies that the LOADING state shows a CircularProgressIndicator.
     * This is the first thing users see when navigating to the leaderboard,
     * so the loading feedback must be visible to prevent the impression
     * that the app has frozen.
     */
    @Test
    fun loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            MaterialTheme {
                RanglisteScreenContent(
                    state = LeaderboardUiState(
                        screenState = LeaderboardScreenState.LOADING
                    ),
                    onRetryClicked = {}
                )
            }
        }

        // The LoadingStateView renders a CircularProgressIndicator
        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
    }

    //  LOADED STATE RENDERS LEADERBOARD LIST

    /**
     * Verifies that the LOADED state renders all leaderboard entries.
     * Each entry should display the username, which is the primary
     * identifier visible to users in the ranking list.
     *
     * Injects 5 entries and asserts all 5 usernames are displayed.
     */
    @Test
    fun loadedState_displaysAllLeaderboardEntries() {
        composeTestRule.setContent {
            MaterialTheme {
                RanglisteScreenContent(
                    state = LeaderboardUiState(
                        screenState = LeaderboardScreenState.LOADED,
                        entries = SAMPLE_ENTRIES
                    ),
                    onRetryClicked = {}
                )
            }
        }

        // All five usernames from the fixture data must be rendered
        composeTestRule.onNodeWithText("ProGamer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Charlie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Diana").assertIsDisplayed()
    }

    //  ERROR STATE RENDERS MESSAGE AND RETRY

    /**
     * Verifies that the ERROR state displays the error message text
     * and a retry button. Network failures are common on mobile, so
     * a clear error message with a recovery action is essential.
     */
    @Test
    fun errorState_showsErrorMessageAndRetryButton() {
        val errorMsg = "Netzwerkfehler: Bitte prüfen Sie Ihre Internetverbindung."
        var retryClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                RanglisteScreenContent(
                    state = LeaderboardUiState(
                        screenState = LeaderboardScreenState.ERROR,
                        errorMessage = errorMsg
                    ),
                    onRetryClicked = { retryClicked = true }
                )
            }
        }

        // Verify the error message text is displayed
        composeTestRule
            .onNodeWithText(errorMsg, substring = true)
            .assertIsDisplayed()

        // Find the retry button by click action and tap it.

        composeTestRule
            .onAllNodes(hasClickAction())
            .onLast()
            .performClick()

        assert(retryClicked) {
            "Expected onRetryClicked callback to fire when retry button is tapped"
        }
    }
}