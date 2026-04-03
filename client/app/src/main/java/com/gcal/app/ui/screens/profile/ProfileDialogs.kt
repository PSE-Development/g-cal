package com.gcal.app.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * LogoutConfirmDialog — Confirmation prompt before logging out (F30).
 */
@Composable
fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.dialog_logout_confirm),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_logout_message_v2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_logout_confirm), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/**
 * DeleteAccountConfirmDialog — Warning dialog before account deletion (F40).
 */
@Composable
fun DeleteAccountConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_delete_account_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_delete_account_message_v2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isProcessing,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(if (isProcessing) R.string.dialog_delete_processing else R.string.dialog_delete_confirm_final))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(text = stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}