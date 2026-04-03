package com.gcal.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * FriendsMenuDialog — Main menu for friend-related actions.
 *
 *
 * @param friends Current friends list from ProfileUiState.friends
 * @param onAddFriendClicked Opens the AddFriendDialog
 * @param onRemoveFriendClicked Fires ProfileUiEvent.RemoveFriendClicked(username)
 * @param onDismiss Closes this dialog
 */
@Composable
fun FriendsMenuDialog(
    friends: List<com.gcal.app.model.modelFacade.general.User> = emptyList(),
    onAddFriendClicked: () -> Unit,
    onRemoveFriendClicked: (String) -> Unit = {},
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Header with close button
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.friends_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Friends list — shows all confirmed friends with remove option
                if (friends.isEmpty()) {
                    Text(
                        text = stringResource(R.string.friends_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                } else {
                    friends.forEach { user ->
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Friend avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.username().take(1).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Friend username
                            Text(
                                text = user.username(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            // Remove button
                            IconButton(
                                onClick = { onRemoveFriendClicked(user.username()) },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.friends_remove),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Friend button
                OutlinedButton(
                    onClick = onAddFriendClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.friends_add))
                }


            }
        }
    }
}

/**
 * AddFriendDialog — Dialog for adding a friend by username.
 *
 */
@Composable
fun AddFriendDialog(
    username: String,
    onUsernameChanged: (String) -> Unit,
    onSendRequest: () -> Unit,
    onBackClicked: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Header with back arrow
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }

                    Text(
                        text = stringResource(R.string.friends_add),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Search icon
                Icon(
                    imageVector = Icons.Filled.PersonSearch,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description text
                Text(
                    text = stringResource(R.string.friends_search_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username input field
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChanged,
                    label = { Text(stringResource(R.string.onboarding_username_label)) },
                    placeholder = { Text(stringResource(R.string.friends_search_hint)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = onSendRequest,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(if (isLoading) R.string.friends_sending else R.string.onboarding_submit))
                }
            }
        }
    }
}