package com.gcal.app.ui.view_model.loginViewModel

/**
 * Defines the strict communication contract from the View to the LoginViewModel.
 * By routing all user intents through these specific events, the UI remains entirely
 * unaware of how authentication, validation, or backend requests are handled.
 */
sealed interface LoginUiEvent {

    /**
     * Triggers the primary authentication flow.
     * By delegating this to the ViewModel, we decouple the UI from the specific
     * authentication provider (e.g., Google Auth). The UI simply says "the user wants in",
     * and the ViewModel coordinates with the Model layer to make it happen.
     */
    data object LoginClicked : LoginUiEvent

    /**
     * Streams raw user input to the ViewModel on every keystroke.
     * We pass this continuously rather than just on submit so the ViewModel can perform
     * real-time business logic (like debounced availability checks via the Model layer
     * or format validation) without the View needing to know the validation rules.
     */
    data class UsernameChanged(val input: String) : LoginUiEvent

    /**
     * Signals the intent to finalize account creation or onboarding.
     * Acts as the confirmation trigger after the user has provided their details,
     * shifting the responsibility to the ViewModel to package the state and
     * push it to the server/Model layer.
     */
    data object RegisterUsernameClicked : LoginUiEvent

    /**
     * Clears the current transient error state.
     * Because the ViewModel exposes state continuously (e.g., via StateFlow),
     * the UI must explicitly inform the ViewModel when an error has been consumed
     * (like a Snackbar dismissing). Otherwise, the UI would re-trigger the error
     * UI upon device rotation or recomposition.
     */
    data object ErrorDismissed : LoginUiEvent
}