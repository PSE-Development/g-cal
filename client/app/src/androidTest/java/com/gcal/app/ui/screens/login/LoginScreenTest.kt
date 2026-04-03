package com.gcal.app.ui.screens.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gcal.app.ui.screens.login.components.LoadingOverlay
import com.gcal.app.ui.screens.login.components.LoginContentView
import com.gcal.app.ui.screens.login.components.OnboardingContentView
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the Login screen.
 *
 * The login flow has two mutually exclusive phases controlled by [LoginUiStep]:
 * START (initial auth prompt) and ONBOARDING (username registration).
 * These tests verify that the correct phase renders given the UiState,
 * and that user interactions dispatch the expected callbacks.
 *
 * All tests use the stateless component variants directly (LoginContentView,
 * OnboardingContentView) to avoid ViewModel dependencies in the test harness.
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // START STEP RENDERS LOGIN BUTTON

    /**
     * Verifies that the START phase of the authentication flow displays
     * the primary login button. This is the entry point of the entire app,
     * so if this button doesn't render, no user can authenticate.
     */
    @Test
    fun startStep_displaysLoginButton_andRespondsToClick() {
        var loginClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                LoginContentView(
                    isLoading = false,
                    onLoginClicked = { loginClicked = true }
                )
            }
        }

        // The login button must be visible and interactive when not loading.

        composeTestRule
            .onNode(hasClickAction() and hasText("Login", substring = true, ignoreCase = true))
            .assertIsDisplayed()
            .assertIsEnabled()

        // Verify the callback fires correctly on tap
        composeTestRule
            .onNode(hasClickAction() and hasText("Login", substring = true, ignoreCase = true))
            .performClick()

        assert(loginClicked) {
            "Expected onLoginClicked callback to fire when login button is tapped"
        }
    }

    // ONBOARDING STEP RENDERS USERNAME INPUT

    /**
     * Verifies that the ONBOARDING phase displays the username TextField.
     * This step appears after initial auth when the user needs to register
     * a unique username before the session can be finalized.
     *
     * Without this input field, new users cannot complete the registration
     * flow and are permanently stuck in the onboarding state.
     */
    @Test
    fun onboardingStep_displaysUsernameTextField_andSubmitButton() {
        composeTestRule.setContent {
            MaterialTheme {
                OnboardingContentView(
                    username = "",
                    errorMsg = null,
                    isEnabled = false,
                    isLoading = false,
                    onUsernameChanged = {},
                    onSubmitClicked = {}
                )
            }
        }

        // Verify that a text input field exists for username entry.
        composeTestRule
            .onNode(hasSetTextAction())
            .assertIsDisplayed()

        // Verify that a submit button exists (clickable node).
        val clickableNodes = composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        assert(clickableNodes.isNotEmpty()) {
            "Expected at least one clickable button (submit), but found none"
        }
    }

    //  LOADING OVERLAY BLOCKS INTERACTION

    /**
     * Verifies that the loading overlay renders a CircularProgressIndicator
     * when isLoading is true. The overlay covers the full screen with a
     * semi-transparent scrim, preventing duplicate network requests.
     *
     * This is a P0 test because without a loading state indicator, users
     * will tap buttons repeatedly during slow network calls, causing
     * duplicate authentication attempts.
     */
    @Test
    fun loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            MaterialTheme {
                LoadingOverlay()
            }
        }

        // The CircularProgressIndicator should be rendered and visible.

        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
    }
}