package com.gcal.app.ui.view_model.loginViewModel

/**
 * Defines the mutually exclusive phases of the authentication journey.
 * Using an enum acts as a finite state machine for the UI, ensuring that the View
 * cannot accidentally render the initial login prompt and the onboarding form
 * at the same time.
 */
enum class LoginUiStep {
    /**
     * The entry point of the auth flow. The user is entirely unauthenticated.
     * The View should present the primary third-party or native login options here.
     */
    START,

    /**
     * The user has partially authenticated (e.g., via a Google token) but lacks
     * mandatory domain profile data (like a unique username) to proceed.
     * The View must collect this missing information before finalizing the login.
     */
    ONBOARDING
}

/**
 * The Single Source of Truth for the Login UI.
 * This state object completely dictates the View's behavior. By pre-calculating
 * flags like [isSubmitEnabled], we ensure the View remains entirely passive and
 * devoid of business or validation logic.
 */
data class LoginUiState(

    // Drives the high-level layout/composable to display.
    val currentStep: LoginUiStep = LoginUiStep.START,

    /**
     * Centralized loading flag.
     * The View should use this to disable interactive elements (buttons, text fields)
     * while true, preventing the user from firing duplicate network requests to the Model.
     */
    val isLoading: Boolean = false,

    // Mirrors the current input so the ViewModel can run real-time validation.
    val usernameInput: String = "",

    /**
     * Contains the result of the ViewModel's domain validation (e.g., "username taken").
     * A null value explicitly means the input is currently valid or unchecked.
     */
    val usernameError: String? = null,

    /**
     * Pre-computed by the ViewModel based on [usernameInput] validity and [isLoading] status.
     * The View simply binds its button's `enabled` property to this, completely
     * unaware of the underlying rules (e.g., min length, regex, network state).
     */
    val isSubmitEnabled: Boolean = false,

    /**
     * The ultimate success flag.
     * When true, the Model has successfully authenticated the user and established a session.
     * The View/Coordinator should react to this by navigating to the MainScreen.
     */
    val isAuthenticated: Boolean = false,

    /**
     * Transient error state for unexpected failures (e.g., HTTP 500, no internet).
     * Provided as a string so the View can simply display it in a Snackbar without
     * needing to parse complex Exception types from the Model layer.
     */
    val generalErrorMessage: String? = null
)