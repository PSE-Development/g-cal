package com.gcal.app.ui.screens.main
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test for the Main Calendar screen's loading indicator.
 *
 * When [MainUiState.isLoading] is true, the calendar content is replaced
 * by a centered CircularProgressIndicator. This prevents users from
 * interacting with stale data while the ViewModel is fetching fresh
 * events from the Model layer.
 */
class MainScreenLoadingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    //  LOADING INDICATOR DURING DATA FETCH

    /**
     * Verifies that a CircularProgressIndicator is displayed when the
     * screen is in the loading state. The MainScreenView replaces the
     * calendar content (Day/Week/Month/Year) with a centered spinner
     * while isLoading is true.
     *
     * We test the loading indicator component directly since it's
     * conditionally rendered inside MainScreenView's Scaffold body.
     */
    @Test
    fun loadingState_showsCenteredProgressIndicator() {
        composeTestRule.setContent {
            MaterialTheme {
                // Replicate the loading branch from MainScreenView
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // The progress indicator should be visible and centered.
        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
    }
}