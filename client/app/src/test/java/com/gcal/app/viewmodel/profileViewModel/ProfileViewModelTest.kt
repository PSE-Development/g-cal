package com.gcal.app.viewmodel.profileViewModel

import app.cash.turbine.test
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.ui.view_model.profileViewModel.ProfileDialogType
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiErrorType
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiEvent
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiState
import com.gcal.app.ui.view_model.profileViewModel.ProfileViewModel
import com.gcal.app.viewmodel.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

     @get:Rule
     val mainDispatcherRule = MainDispatcherRule()

    private lateinit var modelFacade: ModelFacade
    private lateinit var viewModel: ProfileViewModel


    private val userAccountFlow = MutableSharedFlow<User>()
    private val friendsFlow = MutableSharedFlow<List<User>>()

    @Before
    fun setup() {

        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        modelFacade = mockk()

        coEvery { modelFacade.personalAccount() } returns userAccountFlow
        coEvery { modelFacade.friends() } returns friendsFlow

        viewModel = ProfileViewModel(modelFacade)
    }


    @Test
    fun `observeFriends catches upstream exception and sets error state`() = runTest {
        val errorFlow = flow<List<User>> {
            throw RuntimeException("Datenbank abgestürzt")
        }
        coEvery { modelFacade.friends() } returns errorFlow

        val errorViewModel = ProfileViewModel(modelFacade)

        errorViewModel.uiState.test {
            val state = awaitItem()
            assertEquals(
                ProfileUiErrorType.Unknown("Fehler beim Laden der Freunde: Datenbank abgestürzt"),
                state.error
            )
        }
    }

    @Test
    fun `addFriend with empty username fails instantly without calling model`() = runTest {

        viewModel.onEvent(ProfileUiEvent.AddFriendClicked)

        assertEquals("Bitte gib einen Benutzernamen ein", viewModel.uiState.value.friendRequestError)

        coVerify(exactly = 0) { modelFacade.addFriend(any()) }
    }

    @Test
    fun `addFriend with valid username shows loading and transitions to menu on success`() = runTest {
        val testUsername = "MaxMustermann"
        coEvery { modelFacade.addFriend(testUsername) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }

        viewModel.uiState.test {
            awaitItem() // Initial
            viewModel.onEvent(ProfileUiEvent.FriendUsernameChanged(testUsername))
            awaitItem() // Name changed

            viewModel.onEvent(ProfileUiEvent.AddFriendClicked)


            val loadingState = awaitItem()
            assertEquals("Loading sollte true sein", true, loadingState.isFriendRequestLoading)


            testScheduler.advanceUntilIdle()


            val finalState = awaitItem()
            assertEquals(false, finalState.isFriendRequestLoading)
            assertEquals(ProfileDialogType.FRIENDS_MENU, finalState.activeDialog)
        }
    }


    @Test
    fun `removeFriend failure sets UI error correctly`() = runTest {
        val targetUser = "Niklas"
        coEvery { modelFacade.removeFriend(targetUser) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.failure(Exception("Network Timeout"))
        }

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(ProfileUiEvent.RemoveFriendClicked(targetUser))

            val loadingState = awaitItem()
            assertEquals("Loading sollte beim Löschen kurz true sein", true, loadingState.isFriendRequestLoading)

            testScheduler.advanceUntilIdle()

            val errorState = awaitItem()
            assertEquals(false, errorState.isFriendRequestLoading)
            assertEquals(
                ProfileUiErrorType.Unknown("Fehler beim Löschen: Network Timeout"),
                errorState.error
            )
        }
    }

    @Test
    fun `RequestDeleteAccount opens delete dialog if online`() = runTest {
        viewModel.onEvent(ProfileUiEvent.RequestDeleteAccountClicked)

        assertEquals(ProfileDialogType.DELETE_ACCOUNT_CONFIRM, viewModel.uiState.value.activeDialog)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `RequestDeleteAccount sets error and blocks dialog if offline`() = runTest {
        val field = viewModel.javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as MutableStateFlow<ProfileUiState>
        stateFlow.value = stateFlow.value.copy(isOffline = true)

        viewModel.onEvent(ProfileUiEvent.RequestDeleteAccountClicked)

        assertEquals(ProfileDialogType.NONE, viewModel.uiState.value.activeDialog) // Dialog bleibt zu
        assertEquals(ProfileUiErrorType.NetworkUnavailable, viewModel.uiState.value.error) // Fehler gesetzt
    }

    @Test
    fun `ConfirmDeleteAccount success sets isLoggedOut to true`() = runTest {
        coEvery { modelFacade.deleteAccount() } returns Result.success(Unit)

        viewModel.onEvent(ProfileUiEvent.ConfirmDeleteAccount)
        testScheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isLoggedOut)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `ConfirmDeleteAccount network failure sets NetworkUnavailable error`() = runTest {
        coEvery { modelFacade.deleteAccount() } returns Result.failure(java.io.IOException("No Internet"))

        viewModel.onEvent(ProfileUiEvent.ConfirmDeleteAccount)
        testScheduler.advanceUntilIdle()

        assertEquals(ProfileUiErrorType.NetworkUnavailable, viewModel.uiState.value.error)
    }

    @Test
    fun `ConfirmDeleteAccount server failure sets ServerError`() = runTest {
        coEvery { modelFacade.deleteAccount() } returns Result.failure(Exception("Internal Server Error"))

        viewModel.onEvent(ProfileUiEvent.ConfirmDeleteAccount)
        testScheduler.advanceUntilIdle()

        assertEquals(ProfileUiErrorType.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun `observeFriends emits list updates state correctly`() = runTest {
        val mockFriend = mockk<User> { every { username() } returns "Mischa" }

        viewModel.uiState.test {
            skipItems(1)
            friendsFlow.emit(listOf(mockFriend))

            val state = awaitItem()
            assertEquals(1, state.friends.size)
            assertEquals("Mischa", state.friends.first().username())
        }
    }

    @Test
    fun `observeUserAccount flow catch block sets error on upstream crash`() = runTest {
        val errorFlow = flow<User> { throw RuntimeException("Account Sync Crash") }
        coEvery { modelFacade.personalAccount() } returns errorFlow

        val errorViewModel = ProfileViewModel(modelFacade)

        errorViewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ProfileUiErrorType.Unknown("Account-Fehler: Account Sync Crash"), state.error)
        }
    }

    @Test
    fun `addFriend backend failure sets specific error string`() = runTest {
        val testUsername = "Niklas"
        viewModel.onEvent(ProfileUiEvent.FriendUsernameChanged(testUsername))

        coEvery { modelFacade.addFriend(testUsername) } returns Result.failure(Exception("User existiert nicht"))

        viewModel.onEvent(ProfileUiEvent.AddFriendClicked)
        testScheduler.advanceUntilIdle()

        assertEquals("User existiert nicht", viewModel.uiState.value.friendRequestError)
        assertEquals(false, viewModel.uiState.value.isFriendRequestLoading)
    }

    @Test
    fun `removeFriend success removes loading state`() = runTest {
        coEvery { modelFacade.removeFriend(any()) } returns Result.success(Unit)

        viewModel.onEvent(ProfileUiEvent.RemoveFriendClicked("Arda"))
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFriendRequestLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `simple UI events update state correctly`() = runTest {
        viewModel.onEvent(ProfileUiEvent.ShowLevelInfoClicked)
        assertEquals(ProfileDialogType.LEVEL_INFO, viewModel.uiState.value.activeDialog)

        viewModel.onEvent(ProfileUiEvent.RequestLogoutClicked)
        assertEquals(ProfileDialogType.LOGOUT_CONFIRM, viewModel.uiState.value.activeDialog)

        viewModel.onEvent(ProfileUiEvent.DismissDialog)
        assertEquals(ProfileDialogType.NONE, viewModel.uiState.value.activeDialog)

        viewModel.onEvent(ProfileUiEvent.OpenSettingsClicked)
        assertEquals(true, viewModel.uiState.value.isSettingsOpen)

        viewModel.onEvent(ProfileUiEvent.CloseSettingsClicked)
        assertEquals(false, viewModel.uiState.value.isSettingsOpen)

        viewModel.onEvent(ProfileUiEvent.OpenFriendsMenuClicked)
        assertEquals(ProfileDialogType.FRIENDS_MENU, viewModel.uiState.value.activeDialog)

        viewModel.onEvent(ProfileUiEvent.OpenAddFriendClicked)
        assertEquals(ProfileDialogType.ADD_FRIEND, viewModel.uiState.value.activeDialog)
        assertEquals("", viewModel.uiState.value.friendUsernameInput)

        viewModel.onEvent(ProfileUiEvent.BackToFriendsMenu)
        assertEquals(ProfileDialogType.FRIENDS_MENU, viewModel.uiState.value.activeDialog)

        viewModel.onEvent(ProfileUiEvent.ImportFileError)
        assertEquals(ProfileUiErrorType.Unknown("Dateizugriff fehlgeschlagen."), viewModel.uiState.value.error)

        viewModel.onEvent(ProfileUiEvent.ErrorDismissed)
        assertEquals(null, viewModel.uiState.value.error)
    }


    @Test
    fun `settings toggles update local state correctly`() = runTest {
        viewModel.onEvent(ProfileUiEvent.ToggleDarkMode(true))
        assertEquals(true, viewModel.uiState.value.isDarkMode)

        viewModel.onEvent(ProfileUiEvent.SelectLanguage(com.gcal.app.ui.view_model.profileViewModel.AppLanguage.ENGLISH))
        assertEquals(com.gcal.app.ui.view_model.profileViewModel.AppLanguage.ENGLISH, viewModel.uiState.value.language)

        viewModel.onEvent(ProfileUiEvent.ToggleNotifications(true))
        assertEquals(true, viewModel.uiState.value.areNotificationsEnabled)

        viewModel.onEvent(ProfileUiEvent.NotificationPermissionDenied)
        assertEquals(false, viewModel.uiState.value.areNotificationsEnabled)
        assertEquals(ProfileUiErrorType.Unknown("Berechtigung abgelehnt."), viewModel.uiState.value.error)

        viewModel.onEvent(ProfileUiEvent.ImportCalendarClicked)
    }

    @Test
    fun `BackToFriendsMenu resets input and changes dialog type`() = runTest {
        viewModel.onEvent(ProfileUiEvent.FriendUsernameChanged("TestUser"))
        viewModel.onEvent(ProfileUiEvent.OpenAddFriendClicked)

        viewModel.onEvent(ProfileUiEvent.BackToFriendsMenu)

        val state = viewModel.uiState.value
        assertEquals(ProfileDialogType.FRIENDS_MENU, state.activeDialog)
        assertEquals("", state.friendUsernameInput) // Muss leer sein
        assertEquals(null, state.friendRequestError)
    }


    @Test
    fun `observeUserAccount flow failure updates error state`() = runTest {
        val failingAccountFlow = kotlinx.coroutines.flow.flow<User> {
            throw java.io.IOException("Disk Full")
        }
        coEvery { modelFacade.personalAccount() } returns failingAccountFlow

        val failingVm = ProfileViewModel(modelFacade)

        failingVm.uiState.test {
            val state = awaitItem()
            assertEquals(ProfileUiErrorType.Unknown("Account-Fehler: Disk Full"), state.error)
        }
    }


}