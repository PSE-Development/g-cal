package com.gcal.app.viewmodel.leaderboardViewModel

import android.util.Log
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardEntryUi
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardScreenState
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardUiEvent
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardViewModel
import com.gcal.app.viewmodel.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var modelFacade: ModelFacade
    private lateinit var viewModel: LeaderboardViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        modelFacade = mockk(relaxed = true)
        viewModel = LeaderboardViewModel(modelFacade)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `initial state is LOADING and list is empty`() {
        val state = viewModel.uiState.value
        assertEquals(LeaderboardScreenState.LOADING, state.screenState)
        assertEquals(emptyList<LeaderboardEntryUi>(), state.entries)
        assertNull(state.errorMessage)
    }

    @Test
    fun `OnScreenEntered handles flow exceptions and sets ERROR state`() = runTest {
        val errorMessage = "Network Timeout"
        every { modelFacade.globalLeaderboard() } returns flow {
            throw Exception(errorMessage)
        }

        viewModel.onEvent(LeaderboardUiEvent.OnScreenEntered)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LeaderboardScreenState.ERROR, state.screenState)
        assertEquals(errorMessage, state.errorMessage)
    }

    @Test
    fun `OnRefresh cancels previous job and restarts data observation`() = runTest {
        every { modelFacade.globalLeaderboard() } returns flowOf(emptyList())

        viewModel.onEvent(LeaderboardUiEvent.OnScreenEntered)
        viewModel.onEvent(LeaderboardUiEvent.OnRefresh)
        advanceUntilIdle()

        verify(exactly = 2) { modelFacade.globalLeaderboard() }
    }

    @Test
    fun `mapToUiModel handles negative XP gracefully without crashing`() = runTest {
        val buggyUser = mockUser(name = "Niklas", xp = -50)
        every { modelFacade.globalLeaderboard() } returns flowOf(listOf(buggyUser))

        viewModel.onEvent(LeaderboardUiEvent.OnScreenEntered)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val entry = state.entries[0]
        assertEquals(LeaderboardScreenState.LOADED, state.screenState)
        assertEquals("XP: -50", entry.xpDisplayString)
    }

    private fun mockUser(name: String, xp: Int): User {
        val userMock = mockk<User>(relaxed = true)
        every { userMock.username() } returns name
        every { userMock.experiencePoints().value() } returns xp
        return userMock
    }
}