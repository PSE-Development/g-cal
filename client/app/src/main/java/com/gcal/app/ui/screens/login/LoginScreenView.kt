package com.gcal.app.ui.screens.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gcal.app.ui.screens.login.components.LoadingOverlay
import com.gcal.app.ui.screens.login.components.LoginContentView
import com.gcal.app.ui.screens.login.components.OnboardingContentView
import com.gcal.app.ui.view_model.loginViewModel.LoginUiEvent
import com.gcal.app.ui.view_model.loginViewModel.LoginUiStep
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel

/**
 * LoginScreenView — Entry point for the authentication and onboarding flow.
 *
 * @param viewModel The LoginViewModel (provided by GCalNavHost via ViewModelFactory).
 * @param modifier  Modifier for the screen container.
 */
@Composable
fun LoginScreenView(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.generalErrorMessage) {
        uiState.generalErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(LoginUiEvent.ErrorDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                when (uiState.currentStep) {
                    LoginUiStep.START -> {
                        LoginContentView(
                            isLoading = uiState.isLoading,
                            onLoginClicked = {
                                viewModel.onEvent(LoginUiEvent.LoginClicked)
                            }
                        )
                    }

                    LoginUiStep.ONBOARDING -> {

                        OnboardingContentView(
                            username = uiState.usernameInput,
                            errorMsg = uiState.usernameError,
                            isEnabled = uiState.isSubmitEnabled,
                            isLoading = uiState.isLoading,
                            onUsernameChanged = { newInput ->
                                viewModel.onEvent(LoginUiEvent.UsernameChanged(newInput))
                            },
                            onSubmitClicked = {
                                viewModel.onEvent(LoginUiEvent.RegisterUsernameClicked)
                            }
                        )
                    }
                }

                if (uiState.isLoading) {
                    LoadingOverlay()
                }
            }
        }
    }
}
