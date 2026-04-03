package com.gcal.app.viewmodel.reportViewModel

import app.cash.turbine.test
import org.junit.Rule
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.model.modelFacade.general.Achievement
import com.gcal.app.model.modelFacade.general.ReportData
import com.gcal.app.ui.view_model.reportViewModel.DateRangePreset
import com.gcal.app.ui.view_model.reportViewModel.ReportUiEvent
import com.gcal.app.ui.view_model.reportViewModel.ReportViewModel
import com.gcal.app.ui.view_model.reportViewModel.UiErrorType
import junit.framework.TestCase.assertNull
import com.gcal.app.viewmodel.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ReportViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var modelFacade: ModelFacade
    private lateinit var viewModel: ReportViewModel

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        modelFacade = mockk()
        viewModel = ReportViewModel(modelFacade)
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun `initial state sets custom dates to today and search is enabled`() = runTest {
        val today = LocalDate.now()

        viewModel.uiState.test {
            val initialState = awaitItem()

            assertEquals(DateRangePreset.DAY, initialState.selectedPreset)
            assertEquals(today, initialState.customFrom)
            assertEquals(today, initialState.customTo)
            assertTrue(initialState.isSearchEnabled)
            assertEquals(null, initialState.activeError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picking end date before start date disables search and shows validation error`() = runTest {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(ReportUiEvent.OnDatePicked(date = tomorrow, isStartDate = true))

            skipItems(1)
            val updatedState = awaitItem()

            assertEquals(DateRangePreset.CUSTOM, updatedState.selectedPreset)
            assertEquals(tomorrow, updatedState.customFrom)
            assertEquals(today, updatedState.customTo)

            assertFalse(updatedState.isSearchEnabled)
            assertEquals(UiErrorType.Validation, updatedState.activeError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generate report calculates XP, filters achievements and opens dialog`() = runTest {
        val mockAchievement1 = mockk<Achievement>(relaxed = true) {
            coEvery { achievementName() } returns "Erster Termin"
        }
        val mockAchievement2 = mockk<Achievement>(relaxed = true) {
            coEvery { achievementName() } returns "Fleißig"
        }

        coEvery { modelFacade.getAllAchievements() } coAnswers {
            delay(10)
            Result.success(listOf(mockAchievement1, mockAchievement2))
        }

        coEvery { modelFacade.getEventsIn(any(), any()) } returns flow {
            delay(10)
            emit(emptyList())
        }

        val mockReportData = mockk<ReportData>(relaxed = true) {
            coEvery { completedAchievements() } returns listOf(mockAchievement1)
        }

        coEvery { modelFacade.generateReport(any(), any()) } coAnswers {
            delay(10)
            Result.success(mockReportData)
        }

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(ReportUiEvent.OnSearchClicked)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val resultState = awaitItem()
            assertFalse(resultState.isLoading)
            assertTrue(resultState.showResultDialog)

            val resultUi = resultState.reportResult!!
            assertEquals(1, resultUi.earnedAchievements.size)
            assertEquals("Erster Termin", resultUi.earnedAchievements.first().achievementName())

            assertEquals(1, resultUi.openAchievements.size)
            assertEquals("Fleißig", resultUi.openAchievements.first().achievementName())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generate report failure shows network error in state`() = runTest {
        val errorMessage = "Datenbank offline"

        coEvery { modelFacade.getEventsIn(any(), any()) } returns flow {
            delay(10)
            emit(emptyList())
        }
        coEvery { modelFacade.getAllAchievements() } coAnswers {
            delay(10)
            Result.success(emptyList())
        }
        coEvery { modelFacade.generateReport(any(), any()) } coAnswers {
            delay(10)
            Result.failure(Exception(errorMessage))
        }

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(ReportUiEvent.OnSearchClicked)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertFalse(errorState.showResultDialog)

            assertTrue(errorState.activeError is UiErrorType.Network)
            assertEquals(errorMessage, (errorState.activeError as UiErrorType.Network).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `closing dialog updates state correctly`() = runTest {

        viewModel.onEvent(ReportUiEvent.OnCloseDialogClicked)

        assertFalse(viewModel.uiState.value.showResultDialog)
    }

    @Test
    fun `dismissing error resets activeError to null`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(ReportUiEvent.OnDatePicked(LocalDate.now().plusDays(1), true))
            skipItems(1)
            assertTrue(awaitItem().activeError is UiErrorType.Validation)

            viewModel.onEvent(ReportUiEvent.ErrorDismissed)
            assertNull(awaitItem().activeError)
        }
    }


    @Test
    fun `generateReport exits early if search is disabled`() = runTest {
        viewModel.uiState.test {
            awaitItem()


            viewModel.onEvent(ReportUiEvent.OnDatePicked(LocalDate.now().plusDays(5), true))

            skipItems(1)
            val stateBefore = awaitItem()
            assertFalse(stateBefore.isSearchEnabled)


            viewModel.onEvent(ReportUiEvent.OnSearchClicked)

            expectNoEvents()
        }
    }

    @Test
    fun `generateReport handles unexpected exceptions gracefully`() = runTest {
        val exceptionMessage = "Fatal DB Error"

        coEvery { modelFacade.getEventsIn(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(10)
            throw RuntimeException(exceptionMessage)
        }

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(ReportUiEvent.OnSearchClicked)


            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)


            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertTrue(errorState.activeError is UiErrorType.Network)
            assertEquals(exceptionMessage, (errorState.activeError as UiErrorType.Network).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picking end date updates customTo and changes preset to CUSTOM`() = runTest {
        val endDate = LocalDate.now().plusDays(5)
        viewModel.onEvent(ReportUiEvent.OnDatePicked(endDate, isStartDate = false))

        val state = viewModel.uiState.value
        assertEquals(DateRangePreset.CUSTOM, state.selectedPreset)
        assertEquals(endDate, state.customTo)
    }

    @Test
    fun `selecting different presets updates state and calculates time ranges`() = runTest {
        coEvery { modelFacade.getEventsIn(any(), any()) } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { modelFacade.getAllAchievements() } returns Result.success(emptyList())
        coEvery { modelFacade.generateReport(any(), any()) } returns Result.success(mockk(relaxed = true))

        viewModel.onEvent(ReportUiEvent.OnPresetSelected(DateRangePreset.WEEK))
        assertEquals(DateRangePreset.WEEK, viewModel.uiState.value.selectedPreset)
        viewModel.onEvent(ReportUiEvent.OnSearchClicked)


        viewModel.onEvent(ReportUiEvent.OnPresetSelected(DateRangePreset.MONTH))
        assertEquals(DateRangePreset.MONTH, viewModel.uiState.value.selectedPreset)
        viewModel.onEvent(ReportUiEvent.OnSearchClicked)

        viewModel.onEvent(ReportUiEvent.OnPresetSelected(DateRangePreset.YEAR))
        assertEquals(DateRangePreset.YEAR, viewModel.uiState.value.selectedPreset)
        viewModel.onEvent(ReportUiEvent.OnSearchClicked)
    }

    @Test
    fun `generateReport handles failure of getAllAchievements gracefully`() = runTest {
        coEvery { modelFacade.getEventsIn(any(), any()) } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { modelFacade.generateReport(any(), any()) } returns Result.success(mockk(relaxed = true))

        coEvery { modelFacade.getAllAchievements() } returns Result.failure(Exception("Pool empty"))


        viewModel.onEvent(ReportUiEvent.OnSearchClicked)

        assertTrue(viewModel.uiState.value.showResultDialog)
        assertTrue(viewModel.uiState.value.reportResult?.openAchievements?.isEmpty() == true)
    }
}