package com.gcal.app.ui.screens.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gcal.app.ui.screens.login.components.LoadingOverlay
import com.gcal.app.ui.screens.login.components.LoginContentView
import com.gcal.app.ui.screens.login.components.OnboardingContentView



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewLoginNormal() {
    MaterialTheme {
        LoginContentView(
            isLoading = false,
            onLoginClicked = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewLoginLoading() {
    MaterialTheme {
        LoginContentView(
            isLoading = true,
            onLoginClicked = { }
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOnboardingEmpty() {
    MaterialTheme {
        OnboardingContentView(
            username = "",
            errorMsg = null,
            isEnabled = false,
            isLoading = false,
            onUsernameChanged = { },
            onSubmitClicked = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOnboardingFilled() {
    MaterialTheme {
        OnboardingContentView(
            username = "max_mustermann",
            errorMsg = null,
            isEnabled = true,
            isLoading = false,
            onUsernameChanged = { },
            onSubmitClicked = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOnboardingError() {
    MaterialTheme {
        OnboardingContentView(
            username = "ab",
            errorMsg = "Benutzername muss mindestens 3 Zeichen haben",
            isEnabled = false,
            isLoading = false,
            onUsernameChanged = { },
            onSubmitClicked = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOnboardingLoading() {
    MaterialTheme {
        OnboardingContentView(
            username = "max_mustermann",
            errorMsg = null,
            isEnabled = false,
            isLoading = true,
            onUsernameChanged = { },
            onSubmitClicked = { }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewLoadingOverlay() {
    MaterialTheme {
        LoadingOverlay()
    }
}