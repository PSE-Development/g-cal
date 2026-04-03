package com.gcal.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gcal.app.ui.components.GCalBottomBar
import com.gcal.app.ui.components.GCalTopBar
import com.gcal.app.ui.view_model.profileViewModel.ProfileDialogType
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiErrorType
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiEvent
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiState
import com.gcal.app.ui.view_model.profileViewModel.ProfileViewModel
import androidx.compose.ui.res.stringResource
import com.gcal.app.R


/**
 * ProfileScreenView — Central user account management screen.
 *
 * View-ViewModel Interface (Unidirectional Data Flow):
 * - State: Observes [ProfileUiState] from [ProfileViewModel] via StateFlow.
 * - Events: All interactions are dispatched as [ProfileUiEvent] to [ProfileViewModel.onEvent()].
 * - The View holds zero local state — all state lives in the ViewModel.
 *
 * Navigation:
 * - [onNavigateToSettings]: Pushes the Settings sub-screen onto the navigation stack.
 * - [ProfileUiState.isLoggedOut]: When true, triggers navigation to Login (observed via LaunchedEffect).
 *
 * @param viewModel The ProfileViewModel (provided by GCalNavHost via ViewModelFactory).
 * @param currentRoute Current navigation route for BottomBar highlighting.
 * @param onBackClicked Callback for back navigation to the Main screen.
 * @param onNavigate Callback for bottom navigation tab switches.
 * @param onNavigateToSettings Callback to push the Settings screen.
 * @param modifier Modifier for the screen container.
 */
@Composable
fun ProfileScreenView(
    viewModel: ProfileViewModel,
    currentRoute: String?,
    onBackClicked: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe the ViewModel's single source of truth
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate to Settings when the ViewModel signals it
    LaunchedEffect(uiState.isSettingsOpen) {
        if (uiState.isSettingsOpen) {
            onNavigateToSettings()
            // Reset the flag so it doesn't re-trigger on recomposition
            viewModel.onEvent(ProfileUiEvent.CloseSettingsClicked)
        }
    }

    // Show error messages in a snackbar
    val context = LocalContext.current
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = when (error) {
                is ProfileUiErrorType.NetworkUnavailable -> context.getString(R.string.error_network)
                is ProfileUiErrorType.ServerError -> context.getString(R.string.error_server_occurred)
                is ProfileUiErrorType.Unknown -> error.message
            }
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(ProfileUiEvent.ErrorDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { GCalTopBar(title = stringResource(R.string.nav_profil), onBackClicked = onBackClicked) },
        bottomBar = { GCalBottomBar(currentRoute = currentRoute, onNavigate = onNavigate) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main profile content — always visible once the screen is loaded
            ProfileContent(
                uiState = uiState,
                onShowLevelClicked = {
                    viewModel.onEvent(ProfileUiEvent.ShowLevelInfoClicked)
                },
                onFriendsClicked = {
                    viewModel.onEvent(ProfileUiEvent.OpenFriendsMenuClicked)
                },
                onSettingsClicked = {
                    viewModel.onEvent(ProfileUiEvent.OpenSettingsClicked)
                },
                onLogoutClicked = {
                    viewModel.onEvent(ProfileUiEvent.RequestLogoutClicked)
                },
                onDeleteClicked = {
                    viewModel.onEvent(ProfileUiEvent.RequestDeleteAccountClicked)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )

            // ==================== DIALOG MANAGER ====================
            // Only one dialog is visible at a time, controlled by activeDialog in the ViewModel.
            when (uiState.activeDialog) {
                ProfileDialogType.LEVEL_INFO -> {
                    LevelInfoDialog(
                        currentLevel = uiState.currentLevel,
                        currentXp = uiState.currentXp,
                        xpForNextLevel = uiState.xpForNextLevel,
                        progressToNextLevel = uiState.progressToNextLevel,
                        onCloseClicked = {
                            viewModel.onEvent(ProfileUiEvent.DismissDialog)
                        }
                    )
                }

                ProfileDialogType.LOGOUT_CONFIRM -> {
                    LogoutConfirmDialog(
                        onConfirm = {
                            viewModel.onEvent(ProfileUiEvent.ConfirmLogout)
                        },
                        onDismiss = {
                            viewModel.onEvent(ProfileUiEvent.DismissDialog)
                        }
                    )
                }

                ProfileDialogType.DELETE_ACCOUNT_CONFIRM -> {
                    DeleteAccountConfirmDialog(
                        onConfirm = {
                            viewModel.onEvent(ProfileUiEvent.ConfirmDeleteAccount)
                        },
                        onDismiss = {
                            viewModel.onEvent(ProfileUiEvent.DismissDialog)
                        },
                        isProcessing = uiState.isLoading
                    )
                }

                ProfileDialogType.FRIENDS_MENU -> {

                    FriendsMenuDialog(
                        friends = uiState.friends,
                        onAddFriendClicked = {
                            viewModel.onEvent(ProfileUiEvent.OpenAddFriendClicked)
                        },
                        onDismiss = {
                            viewModel.onEvent(ProfileUiEvent.DismissDialog)
                        }
                    )
                }

                ProfileDialogType.ADD_FRIEND -> {
                    AddFriendDialog(
                        username = uiState.friendUsernameInput,
                        onUsernameChanged = { username ->
                            viewModel.onEvent(ProfileUiEvent.FriendUsernameChanged(username))
                        },
                        onSendRequest = {
                            viewModel.onEvent(ProfileUiEvent.AddFriendClicked)
                        },
                        onBackClicked = {
                            viewModel.onEvent(ProfileUiEvent.BackToFriendsMenu)
                        },
                        onDismiss = {
                            viewModel.onEvent(ProfileUiEvent.DismissDialog)
                        },
                        isLoading = uiState.isFriendRequestLoading,
                        errorMessage = uiState.friendRequestError
                    )
                }

                // FRIEND_REQUESTS case REMOVED — ModelFacade does not support accept/decline

                ProfileDialogType.NONE -> { /* No dialog visible */ }
            }

            // Loading overlay — shown during async operations (logout, delete) when no dialog is active
            if (uiState.isLoading && uiState.activeDialog == ProfileDialogType.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// ==================== INTERNAL COMPOSABLES ====================

/**
 * ProfileContent — Main layout showing avatar, display name, and action buttons.
 *
 * Renders the user's profile header and a list of action buttons for navigating
 * to different features (level info, friends, settings, logout, account deletion).
 */
@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onShowLevelClicked: () -> Unit,
    onFriendsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Profile avatar and display name
        ProfileHeader(
            displayName = uiState.displayName,
            avatarUrl = uiState.avatarUrl
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action button list
        ProfileActionList(
            isOffline = uiState.isOffline,
            onShowLevelClicked = onShowLevelClicked,
            onFriendsClicked = onFriendsClicked,
            onSettingsClicked = onSettingsClicked,
            onLogoutClicked = onLogoutClicked,
            onDeleteClicked = onDeleteClicked
        )
    }
}

/**
 * ProfileHeader — Avatar circle and display name.
 *
 * Shows a placeholder icon when no avatar URL is available.
 * The avatar URL support is prepared for future backend integration.
 */
@Composable
private fun ProfileHeader(
    displayName: String,
    avatarUrl: String?,

    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar circle with placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // TODO: Replace with AsyncImage (Coil) when avatar URLs are available from backend
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display name
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * ProfileActionList — Vertical list of action buttons.
 *
 * Each button dispatches a [ProfileUiEvent] through the parent composable's callbacks.
 * The "Account löschen" button is disabled when offline to prevent incomplete deletions.
 */
@Composable
private fun ProfileActionList(
    isOffline: Boolean,
    onShowLevelClicked: () -> Unit,
    onFriendsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // show level
        OutlinedButton(onClick = onShowLevelClicked, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Star, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.profile_level))
        }

        // friends
        OutlinedButton(onClick = onFriendsClicked, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Group, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.friends_title))
        }

        // settings
        OutlinedButton(onClick = onSettingsClicked, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Settings, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.profile_settings))
        }

        Spacer(Modifier.height(16.dp))

        // Logout
        OutlinedButton(onClick = onLogoutClicked, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.dialog_logout_confirm))
        }

        // Account delete
        Button(
            onClick = onDeleteClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isOffline,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(Icons.Filled.Delete, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.profile_delete))
        }
    }
}