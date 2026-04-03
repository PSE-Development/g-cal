package com.gcal.app.ui.view_model.profileViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.model.modelFacade.general.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Orchestrates the Profile, Settings, and Gamification logic.
 * By keeping this logic out of the View layer, we ensure that complex calculations
 * (like XP to Level conversion) and critical flows (Account Deletion) are highly
 * unit-testable and detached from the UI lifecycle.
 */
class ProfileViewModel(
    private val model: ModelFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    companion object{
        private const val TAG = "ProfileViewModel"
    }

    init {
        Log.d(TAG, "INIT: ProfileViewModel gestartet. Baue Model-Subscriptions auf.")
        observeUserAccount()
        observeFriends()
    }

    /**
     * Central entry point for all UI events.
     */
    fun onEvent(event: ProfileUiEvent) {
        Log.d(TAG, "EVENT RECEIVED: ${event::class.simpleName} - Full: $event")
        when (event) {
            // --- Dialog Management ---
            is ProfileUiEvent.ShowLevelInfoClicked -> {
                _uiState.update { it.copy(activeDialog = ProfileDialogType.LEVEL_INFO) }
            }
            is ProfileUiEvent.RequestLogoutClicked -> {
                _uiState.update { it.copy(activeDialog = ProfileDialogType.LOGOUT_CONFIRM) }
            }
            is ProfileUiEvent.RequestDeleteAccountClicked -> {
                if (_uiState.value.isOffline) {
                    Log.w(TAG, "WARNUNG: DeleteAccount blockiert. Grund: Offline-State aktiv.")
                    _uiState.update { it.copy(error = ProfileUiErrorType.NetworkUnavailable) }
                } else {
                    _uiState.update { it.copy(activeDialog = ProfileDialogType.DELETE_ACCOUNT_CONFIRM) }
                }
            }
            is ProfileUiEvent.DismissDialog -> {
                Log.d(TAG, "STATE CHANGE: Schließe alle Dialoge und setze Drafts zurück.")
                _uiState.update {
                    it.copy(
                        activeDialog = ProfileDialogType.NONE,
                        friendUsernameInput = "",
                        friendRequestError = null
                    )
                }
            }
            is ProfileUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }

            // --- Navigation ---
            is ProfileUiEvent.OpenSettingsClicked -> {
                _uiState.update { it.copy(isSettingsOpen = true) }
            }
            is ProfileUiEvent.CloseSettingsClicked -> {
                _uiState.update { it.copy(isSettingsOpen = false) }
            }

            // --- Friends Interactions ---
            is ProfileUiEvent.OpenFriendsMenuClicked -> {
                _uiState.update { it.copy(activeDialog = ProfileDialogType.FRIENDS_MENU) }
            }
            is ProfileUiEvent.OpenAddFriendClicked -> {
                _uiState.update {
                    it.copy(
                        activeDialog = ProfileDialogType.ADD_FRIEND,
                        friendUsernameInput = "",
                        friendRequestError = null
                    )
                }
            }

            is ProfileUiEvent.BackToFriendsMenu -> {
                _uiState.update {
                    it.copy(
                        activeDialog = ProfileDialogType.FRIENDS_MENU,
                        friendUsernameInput = "",
                        friendRequestError = null
                    )
                }
            }
            is ProfileUiEvent.FriendUsernameChanged -> {
                Log.d(TAG, "DRAFT UPDATE: friendUsernameInput = '${event.username}'")
                _uiState.update {
                    it.copy(
                        friendUsernameInput = event.username,
                        friendRequestError = null
                    )
                }
            }
            is ProfileUiEvent.AddFriendClicked -> {
                addFriend()
            }
            is ProfileUiEvent.RemoveFriendClicked -> {
                removeFriend(event.username)
            }

            // --- Auth Actions ---
            is ProfileUiEvent.ConfirmLogout -> performLogout()
            is ProfileUiEvent.ConfirmDeleteAccount -> performAccountDeletion()

            // --- Settings Actions ---
            is ProfileUiEvent.ToggleDarkMode -> {
                //  (Persistent Settings): Currently only updates transient UI state.
                // Needs to be wired to a local DataStore/SharedPreferences to survive app restarts.
                _uiState.update { it.copy(isDarkMode = event.enabled) }
            }
            is ProfileUiEvent.SelectLanguage -> {
                _uiState.update { it.copy(language = event.language) }
            }
            is ProfileUiEvent.ToggleNotifications -> {
                _uiState.update { it.copy(areNotificationsEnabled = event.enabled) }
            }

            is ProfileUiEvent.NotificationPermissionDenied -> {
                Log.w(TAG, "OS PERMISSION DENIED: Setze Notification-Toggle auf false zurück.")
                // Synchronizes the View's toggle state back to 'false' if the system-level
                // permission request was denied, keeping the UI honest about the actual state.
                _uiState.update { it.copy(
                    areNotificationsEnabled = false,
                    error = ProfileUiErrorType.Unknown("Berechtigung abgelehnt.")
                )}
            }
            is ProfileUiEvent.ImportFileError -> {
                Log.e(TAG, "VIEW ERROR: ImportFileError aus der UI empfangen.")
                // Translates a View-layer failure (e.g., FilePicker exception) back into
                // the standard unidirectional error flow.
                _uiState.update { it.copy(
                    error = ProfileUiErrorType.Unknown("Dateizugriff fehlgeschlagen.")
                )}
            }

            is ProfileUiEvent.ImportCalendarClicked -> {
                Log.d(TAG, "INTENT LOG: View startet jetzt FilePicker für Kalender-Import.")
                // Intentionally left blank. Route exists solely to acknowledge the intent;
                // actual intent execution is strictly a View-layer responsibility.
            }
        }
    }

    // --- Internal Logic ---

    /**
     * Bridges the Model's raw data stream to the UI.
     * Continuously listens for XP/Account changes (e.g., triggered from the Calendar screen)
     * and automatically recalculates the profile UI.
     */
    private fun observeUserAccount() {
        viewModelScope.launch {
            model.personalAccount()
                .catch { e ->
                    Log.e(TAG, "FLOW ERROR (observeUserAccount): Absturz im Upstream!", e)
                    _uiState.update {
                        it.copy(error = ProfileUiErrorType.Unknown("Account-Fehler: ${e.message}"))
                    }
                }
                .collect { user ->
                    Log.d(TAG, "MODEL UPDATE: Neues User-Objekt empfangen. Berechne Gamification-Stats...")
                    updateGamificationStats(user)
                }
        }
    }

    private fun observeFriends() {
        viewModelScope.launch {
            model.friends()
                .catch { e ->
                    Log.e(TAG, "FLOW ERROR (observeFriends): Absturz beim Laden der Freunde-Liste!", e)
                    _uiState.update {
                        it.copy(error = ProfileUiErrorType.Unknown("Fehler beim Laden der Freunde: ${e.message}"))
                    }
                }
                .collect { friendList ->
                    Log.d(TAG, "MODEL UPDATE: Freunde-Liste aktualisiert. Anzahl: ${friendList.size}")
                    _uiState.update { it.copy(friends = friendList) }
                }
        }
    }


    /**
     * Encapsulates the core math for the Gamification engine.
     * Forcing the ViewModel to calculate `progressToNextLevel` guarantees the View
     * remains a "dumb" renderer, unaware of the actual 100-XP-per-level business rules.
     */
    private fun updateGamificationStats(user: User) {
        val totalXp = user.experiencePoints().value()
        val xpPerLevel = 100

        // Calculate Level (Integer division)
        val level = (totalXp / xpPerLevel) + 1

        // Calculate XP in current level
        val xpInCurrentLevel = totalXp % xpPerLevel

        // Calculate Progress (0.0 to 1.0)
        val progress = xpInCurrentLevel.toFloat() / xpPerLevel.toFloat()
        Log.d(TAG, "GAMIFICATION CALC: TotalXP=$totalXp -> Level=$level, Progress=$progress")

        _uiState.update {
            it.copy(
                displayName = user.name(),
                currentXp = totalXp,
                currentLevel = level,
                xpForNextLevel = level * xpPerLevel,
                progressToNextLevel = progress
            )
        }
    }

    private fun addFriend() {
        val username = _uiState.value.friendUsernameInput.trim()

        Log.d(TAG, "ACTION: addFriend gestartet für Username: '$username'")
        if (username.isBlank()) {
            Log.w(TAG, "VALIDATION FAILED: Username war leer. Abbruch vor Model-Call.")
            _uiState.update { it.copy(friendRequestError = "Bitte gib einen Benutzernamen ein") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFriendRequestLoading = true, friendRequestError = null) }

            val result = model.addFriend(username)

            _uiState.update { state ->
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "SUCCESS: Freund '$username' erfolgreich hinzugefügt.")
                        // Success cleanly transitions the state machine back to the main menu.
                        state.copy(
                            isFriendRequestLoading = false,
                            activeDialog = ProfileDialogType.FRIENDS_MENU,
                            friendUsernameInput = "",
                            friendRequestError = null
                        )
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "API FAILURE: Hinzufügen von '$username' fehlgeschlagen.", exception)
                        // Pushes the backend's rejection reason down to the View.
                        state.copy(
                            isFriendRequestLoading = false,
                            friendRequestError = exception.message ?: "Fehler beim Hinzufügen"
                        )
                    }
                )
            }
        }
    }

    /**
     * Stateless execution of friend removal.
     * Uses the provided [username] parameter rather than relying on an internal "selected friend"
     * state, keeping the View-to-ViewModel interaction simple and atomic.
     */
    private fun removeFriend(username: String) {
        Log.d(TAG, "ACTION: removeFriend gestartet für Username: '$username'")
        viewModelScope.launch {
            _uiState.update { it.copy(isFriendRequestLoading = true) }

            val result = model.removeFriend(username)

            _uiState.update { state ->
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "SUCCESS: Freund '$username' erfolgreich entfernt.")
                        state.copy(isFriendRequestLoading = false)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "API FAILURE: Entfernen von '$username' fehlgeschlagen.", exception)
                        state.copy(
                            isFriendRequestLoading = false,
                            error = ProfileUiErrorType.Unknown("Fehler beim Löschen: ${exception.message}")
                        )
                    }
                )
            }
        }
    }

    private fun performLogout() {
        Log.d(TAG, "ACTION: performLogout gestartet. Sperre UI.")
        viewModelScope.launch {
            // Dismisses the confirmation dialog immediately to prevent duplicate taps.
            _uiState.update { it.copy(isLoading = true, activeDialog = ProfileDialogType.NONE) }
            Log.d(TAG, "MODEL CALL: Führe model.logout() aus...")
            model.logout()

            Log.d(TAG, "SUCCESS: Logout beendet. Setze isLoggedOut Flag für View.")

            // Signals the View/Coordinator to tear down the main app component and return to Auth.
            _uiState.update { it.copy(isLoading = false, isLoggedOut = true) }
        }
    }

    private fun performAccountDeletion() {
        Log.d(TAG, "ACTION: performAccountDeletion gestartet. Sperre UI.")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, activeDialog = ProfileDialogType.NONE) }

            val result = model.deleteAccount()

            _uiState.update { state ->
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "SUCCESS: Account gelöscht. Navigiere zum Login.")
                        // Successful deletion routes back to Login.
                        state.copy(isLoading = false, isLoggedOut = true)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "API FAILURE: Account-Löschung fehlgeschlagen.", exception)
                        val errorType = if (exception is java.io.IOException) {
                            ProfileUiErrorType.NetworkUnavailable
                        } else {
                            ProfileUiErrorType.ServerError
                        }
                        state.copy(
                            isLoading = false,
                            error = errorType
                        )
                    }
                )
            }
        }
    }
}
