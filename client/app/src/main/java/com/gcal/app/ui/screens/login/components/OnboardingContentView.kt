package com.gcal.app.ui.screens.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * OnboardingContentView
 */
@Composable
fun OnboardingContentView(
    username: String,
    errorMsg: String?,
    isEnabled: Boolean,
    isLoading: Boolean,
    onUsernameChanged: (String) -> Unit,
    onSubmitClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = stringResource(R.string.login_title),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))


        Text(
            text = stringResource(R.string.onboarding_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))


        Text(
            text = stringResource(R.string.onboarding_hint),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))


        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboarding_username_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_placeholder)) },
            singleLine = true,
            isError = errorMsg != null,
            supportingText = {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isEnabled && !isLoading) {
                        onSubmitClicked()
                    }
                }
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Next Button
        Button(
            onClick = onSubmitClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = isEnabled && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.onboarding_submit),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}