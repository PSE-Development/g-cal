package com.gcal.app.ui.screens.login

/**
 * LoginScreenState
 */
data class LoginScreenState(
    val currentStep: LoginUiStep = LoginUiStep.START,
    val usernameInput: String = "",
    val usernameError: String? = null,
    val isSubmitEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)