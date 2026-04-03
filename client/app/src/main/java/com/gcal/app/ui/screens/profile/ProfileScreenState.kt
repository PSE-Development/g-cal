package com.gcal.app.ui.screens.profile

import com.gcal.app.ui.view_model.profileViewModel.ProfileUiErrorType

/**
 * ProfileScreenState — Legacy UI state holder for the Profile screen.
 *
 */
data class ProfileScreenState(
    val userProfile: UserProfileUi? = null,
    val activeDialog: com.gcal.app.ui.view_model.profileViewModel.ProfileDialogType =
        com.gcal.app.ui.view_model.profileViewModel.ProfileDialogType.NONE,
    val isOffline: Boolean = false,
    val isProcessing: Boolean = false,
    val error: ProfileUiErrorType? = null,
    val successMessage: String? = null,
    val addFriendUsername: String = "",
    val isSendingFriendRequest: Boolean = false
) {
    companion object {
        /** Creates an empty initial state. */
        fun initial() = ProfileScreenState()
    }
}