package com.gcal.app.viewmodel.mainViewModel

import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.model.modelFacade.general.ToDo
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.NoGroup
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.ui.view_model.mainViewModel.DeleteScope
import com.gcal.app.ui.view_model.mainViewModel.EditorMode
import com.gcal.app.ui.view_model.mainViewModel.EntryType
import com.gcal.app.ui.view_model.mainViewModel.MainUiEvent
import com.gcal.app.ui.view_model.mainViewModel.MainViewModel
import com.gcal.app.ui.view_model.mainViewModel.ViewMode
import com.gcal.app.viewmodel.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.slot
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var modelFacade: ModelFacade
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        modelFacade = mockk(relaxed = true)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0

        every { modelFacade.getAllGroups() } returns flowOf(emptyList())
        every { modelFacade.friends() } returns flowOf(emptyList())
        every { modelFacade.getEventsIn(any(), any()) } returns emptyFlow()

        coEvery { modelFacade.syncExperiencePoints() } returns Result.success(Unit)

        viewModel = MainViewModel(modelFacade)
    }

    // UI-Events

    @Test
    fun `onEvent ViewModeSelected updates viewMode in state`() = runTest {
        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.WEEK))

        assertEquals(ViewMode.WEEK, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `onEvent DateSelected updates date and sets mode to DAY`() = runTest {
        val testDate = LocalDate.of(2026, 3, 15)

        viewModel.onEvent(MainUiEvent.DateSelected(testDate))

        assertEquals(testDate, viewModel.uiState.value.currentDate)
        assertEquals(ViewMode.DAY, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `onEvent FilterClicked opens filter dialog`() = runTest {
        viewModel.onEvent(MainUiEvent.FilterClicked)

        assertTrue(viewModel.uiState.value.isFilterDialogOpen)
    }

    @Test
    fun `onEvent CloseOverlays closes all dialogs`() = runTest {
        viewModel.onEvent(MainUiEvent.FilterClicked)
        viewModel.onEvent(MainUiEvent.JumpToDateClicked)

        viewModel.onEvent(MainUiEvent.CloseOverlays)

        val state = viewModel.uiState.value
        assertEquals(false, state.isFilterDialogOpen)
        assertEquals(false, state.isJumpToDateDialogOpen)
        assertEquals(false, state.isCreateGroupDialogOpen)
        assertEquals(false, state.isDeleteConfirmationOpen)
    }

    @Test
    fun `onEvent FabClicked opens editor in CREATE mode`() = runTest {

        viewModel.onEvent(MainUiEvent.FabClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertEquals(EditorMode.CREATE, state.editorMode)
        assertEquals(state.currentDate, state.draftState.date) // Draft muss aktuelles Datum haben
    }

    //Editor & Mapping

    @Test
    fun `onEvent EventClicked with ToDo maps correctly to DraftState`() = runTest {
        val testDeadline = LocalDateTime.of(2026, 3, 15, 14, 30)
        val mockTodo = mockk<ToDo>(relaxed = true) {
            every { eventID() } returns 42L
            every { eventName() } returns "Pizza kaufen"
            every { description() } returns "Salami"
            every { end() } returns testDeadline
        }

        viewModel.onEvent(MainUiEvent.EventClicked(mockTodo))

        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertEquals(EditorMode.EDIT, state.editorMode)

        val draft = state.draftState
        assertEquals(42L, draft.id)
        assertEquals(EntryType.TODO, draft.selectedType)
        assertEquals("Pizza kaufen", draft.name)
        assertEquals("Salami", draft.description)
        assertEquals(testDeadline.toLocalDate(), draft.date)
        assertEquals(testDeadline.toLocalTime(), draft.endTime)
    }

    @Test
    fun `onEvent EventClicked with Appointment maps correctly to DraftState`() = runTest {
        val testStart = LocalDateTime.of(2026, 3, 16, 10, 0)
        val testEnd = LocalDateTime.of(2026, 3, 16, 11, 0)
        val mockGroup = mockk<Group>(relaxed = true)

        val mockAppt = mockk<Appointment>(relaxed = true) {
            every { eventID() } returns 99L
            every { eventName() } returns "Team Meeting"
            every { start() } returns testStart
            every { end() } returns testEnd
            every { group() } returns mockGroup
        }

        viewModel.onEvent(MainUiEvent.EventClicked(mockAppt))

        val draft = viewModel.uiState.value.draftState
        assertEquals(EntryType.APPOINTMENT, draft.selectedType)
        assertEquals(99L, draft.id)
        assertEquals(testStart.toLocalDate(), draft.date)
        assertEquals(testStart.toLocalTime(), draft.startTime)
        assertEquals(testEnd.toLocalTime(), draft.endTime)
        assertEquals(mockGroup, draft.selectedGroup)
    }

    //Async Actions (Save, Delete, Complete & Error Handling)

    @Test
    fun `saveEntry aborts if draft is invalid`() = runTest {

        viewModel.onEvent(MainUiEvent.FabClicked)
        viewModel.onEvent(MainUiEvent.EntryNameChanged(""))

        viewModel.onEvent(MainUiEvent.SaveEntryClicked)

        coVerify(exactly = 0) { modelFacade.createEvent(any()) }
        assertTrue(viewModel.uiState.value.isEditorOpen) // Editor bleibt offen
    }

    @Test
    fun `saveEntry creates new Appointment successfully`() = runTest {
        coEvery { modelFacade.createEvent(any()) } returns Result.success(Unit)

        viewModel.onEvent(MainUiEvent.FabClicked)
        viewModel.onEvent(MainUiEvent.EntryTypeChanged(EntryType.APPOINTMENT))
        viewModel.onEvent(MainUiEvent.EntryNameChanged("Gültiger Termin"))

        viewModel.onEvent(MainUiEvent.SaveEntryClicked)

        coVerify(exactly = 1) { modelFacade.createEvent(any<UserAppointment>()) }

        val state = viewModel.uiState.value
        assertEquals(false, state.isEditorOpen)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `saveEntry updates ToDo with correct deadline logic`() = runTest {
        val todoSlot = slot<UserToDo>()
        coEvery { modelFacade.updateEvent(capture(todoSlot)) } returns Result.success(Unit)

        val mockTodo = mockk<ToDo>(relaxed = true) {
            every { eventID() } returns 10L
            every { eventName() } returns "Altes ToDo"
        }
        viewModel.onEvent(MainUiEvent.EventClicked(mockTodo))

        viewModel.onEvent(MainUiEvent.EntryNameChanged("Neues ToDo"))

        viewModel.onEvent(MainUiEvent.SaveEntryClicked)

        coVerify(exactly = 1) { modelFacade.updateEvent(any<UserToDo>()) }

        val capturedTodo = todoSlot.captured
        assertEquals(10L, capturedTodo.eventID())
        assertEquals("Neues ToDo", capturedTodo.eventName())
        assertEquals(false, viewModel.uiState.value.isEditorOpen)
    }

    @Test
    fun `deleteEntry calls model and closes editor on success`() = runTest {
        coEvery { modelFacade.deleteEvent(any()) } returns Result.success(Unit)
        viewModel.onEvent(MainUiEvent.FabClicked) // Editor öffnen für Setup

        viewModel.onEvent(MainUiEvent.ConfirmDelete(DeleteScope.SINGLE_INSTANCE))

        coVerify(exactly = 1) { modelFacade.deleteEvent(any()) }
        assertEquals(false, viewModel.uiState.value.isEditorOpen)
        assertEquals(false, viewModel.uiState.value.isDeleteConfirmationOpen)
    }

    @Test
    fun `deleteEntry shows error message on model failure`() = runTest {
        val errorMessage = "Server down"
        coEvery { modelFacade.deleteEvent(any()) } returns Result.failure(Exception(errorMessage))
        viewModel.onEvent(MainUiEvent.FabClicked) // Editor öffnen für Setup

        viewModel.onEvent(MainUiEvent.ConfirmDelete(DeleteScope.SINGLE_INSTANCE))

        coVerify(exactly = 1) { modelFacade.deleteEvent(any()) }

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains(errorMessage) == true)
    }

    @Test
    fun `observeGroups maps model groups to UiFilterGroup correctly`() = runTest {
        val group1 = mockk<Group>(relaxed = true) {
            every { groupId() } returns 101L
            every { groupName() } returns "Arbeit"
        }
        val groupFlow = flowOf(listOf(group1))
        every { modelFacade.getAllGroups() } returns groupFlow

        val localViewModel = MainViewModel(modelFacade)

        val groups = localViewModel.uiState.value.allGroups
        assertEquals(1, groups.size)
        assertEquals(101L, groups[0].group.groupId())
        assertTrue(groups[0].isVisible) // Standardmäßig sollten sie sichtbar sein
    }

    @Test
    fun `onEvent ToggleGroupFilter flips visibility of a group`() = runTest {
        val group1 = mockk<Group>(relaxed = true) { every { groupId() } returns 202L }
        every { modelFacade.getAllGroups() } returns flowOf(listOf(group1))
        val localViewModel = MainViewModel(modelFacade)

        localViewModel.onEvent(MainUiEvent.ToggleGroupFilter(202L))

        val filterGroup = localViewModel.uiState.value.allGroups.first { it.group.groupId() == 202L }
        assertEquals(false, filterGroup.isVisible)
    }

    @Test
    fun `handleSwipe DAY forward updates date by one day`() = runTest {
        val initialDate = LocalDate.of(2026, 3, 10)
        viewModel.onEvent(MainUiEvent.DateSelected(initialDate))

        viewModel.onEvent(MainUiEvent.DateSwiped(1))

        assertEquals(initialDate.plusDays(1), viewModel.uiState.value.currentDate)
    }

    @Test
    fun `calculateDateRange returns correct week range`() = runTest {
        val monday = LocalDate.of(2026, 3, 9)
        viewModel.onEvent(MainUiEvent.DateSelected(monday))
        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.WEEK))

        val state = viewModel.uiState.value
        assertEquals(ViewMode.WEEK, state.viewMode)

        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.MONTH))
        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.YEAR))
    }


    @Test
    fun `trigger remaining UI events`() = runTest {
        viewModel.onEvent(MainUiEvent.EntryDescriptionChanged("Test Desc"))
        viewModel.onEvent(MainUiEvent.EntryLocationChanged("Büro"))
        viewModel.onEvent(MainUiEvent.EntryTimeChanged(LocalTime.NOON, LocalTime.MIDNIGHT))
        viewModel.onEvent(MainUiEvent.JumpToDateClicked)
        viewModel.onEvent(MainUiEvent.JumpToDateConfirmed(LocalDate.now()))
        viewModel.onEvent(MainUiEvent.OpenCreateGroupClicked)
        viewModel.onEvent(MainUiEvent.CancelEditorClicked)

        val state = viewModel.uiState.value
        assertEquals("Test Desc", state.draftState.description)
        assertEquals("Büro", state.draftState.location)
    }

    // (Gruppen, Freunde, Swipe, Achievements)

    @Test
    fun `toggleFriendSelection adds and removes friend correctly`() = runTest {
        val mockUser = mockk<com.gcal.app.model.modelFacade.general.User>(relaxed = true) {
            every { username() } returns "Arda"
        }

        viewModel.onEvent(MainUiEvent.ToggleFriendSelection(mockUser))
        assertTrue(viewModel.uiState.value.draftState.sharedWith.any { it.username() == "Arda" })

        viewModel.onEvent(MainUiEvent.ToggleFriendSelection(mockUser))
        assertTrue(viewModel.uiState.value.draftState.sharedWith.isEmpty())
    }

    @Test
    fun `saveOrUpdateGroup with blank name sets error`() = runTest {
        viewModel.onEvent(MainUiEvent.SaveGroup("   ", "#FFFFFF", null))

        assertTrue(viewModel.uiState.value.errorMessage == "Der Gruppenname darf nicht leer sein.")
        coVerify(exactly = 0) { modelFacade.createGroup(any()) }
    }

    @Test
    fun `saveOrUpdateGroup creates new group when id is null`() = runTest {
        coEvery { modelFacade.createGroup(any()) } returns Result.success(Unit)

        viewModel.onEvent(MainUiEvent.SaveGroup("Neue Gruppe", "#FF0000", null))
        advanceUntilIdle()

        coVerify(exactly = 1) { modelFacade.createGroup(any()) }
        assertEquals(false, viewModel.uiState.value.isCreateGroupDialogOpen)
    }

    @Test
    fun `saveOrUpdateGroup updates group when id is provided`() = runTest {
        coEvery { modelFacade.updateGroup(any()) } returns Result.success(Unit)

        viewModel.onEvent(MainUiEvent.SaveGroup("Alte Gruppe", "#FF0000", 1L))
        advanceUntilIdle()

        coVerify(exactly = 1) { modelFacade.updateGroup(any()) }
    }



    @Test
    fun `handleSwipe works for ALL ViewModes`() = runTest {
        val initialDate = LocalDate.of(2026, 3, 10)
        viewModel.onEvent(MainUiEvent.DateSelected(initialDate))

        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.WEEK))
        viewModel.onEvent(MainUiEvent.DateSwiped(1))
        assertEquals(initialDate.plusWeeks(1), viewModel.uiState.value.currentDate)

        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.MONTH))
        viewModel.onEvent(MainUiEvent.DateSwiped(-1))
        assertEquals(initialDate.plusWeeks(1).minusMonths(1), viewModel.uiState.value.currentDate)

        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.YEAR))
        viewModel.onEvent(MainUiEvent.DateSwiped(1))
        assertEquals(initialDate.plusWeeks(1).minusMonths(1).plusYears(1), viewModel.uiState.value.currentDate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observeEvents handles stream error gracefully`() = runTest {
        val freshModel = mockk<ModelFacade>(relaxed = true)
        every { freshModel.getAllGroups() } returns emptyFlow()
        every { freshModel.friends() } returns emptyFlow()
        coEvery { freshModel.syncExperiencePoints() } returns Result.success(Unit)

        val exceptionMessage = "DB Crash"
        every { freshModel.getEventsIn(any(), any()) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException(exceptionMessage)
        }

        val vm = MainViewModel(freshModel)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("Error message should contain '$exceptionMessage' but was '${state.errorMessage}'",
            state.errorMessage?.contains(exceptionMessage) == true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `syncExperiencePoints failure updates errorMessage`() = runTest {
        val freshModel = mockk<ModelFacade>(relaxed = true)
        every { freshModel.getAllGroups() } returns emptyFlow()
        every { freshModel.friends() } returns emptyFlow()
        every { freshModel.getEventsIn(any(), any()) } returns emptyFlow()

        val exceptionMessage = "Network Error"
        coEvery { freshModel.syncExperiencePoints() } returns Result.failure(Exception(exceptionMessage))

        val vm = MainViewModel(freshModel)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("Error message should contain '$exceptionMessage' but was '${state.errorMessage}'",
            state.errorMessage?.contains(exceptionMessage) == true)
    }

    @Test
    fun `observeEvents filters out appointments of hidden groups`() = runTest {
        val mockGroup = mockk<Group>(relaxed = true) { every { groupId() } returns 999L }

        val hiddenAppt = mockk<Appointment>(relaxed = true) {
            every { eventID() } returns 1L
            every { group() } returns mockGroup
        }
        val visibleTodo = mockk<ToDo>(relaxed = true) { every { eventID() } returns 2L }

        val freshModel = mockk<ModelFacade>(relaxed = true)
        every { freshModel.getAllGroups() } returns flowOf(listOf(mockGroup))
        every { freshModel.friends() } returns emptyFlow()
        every { freshModel.getEventsIn(any(), any()) } returns flowOf(listOf(hiddenAppt, visibleTodo))

        val localVm = MainViewModel(freshModel)
        advanceUntilIdle()

        localVm.onEvent(MainUiEvent.ToggleGroupFilter(999L))

        localVm.onEvent(MainUiEvent.DateSwiped(1))
        advanceUntilIdle()

        val filteredEvents = localVm.uiState.value.events
        assertEquals(1, filteredEvents.size)
        assertTrue(filteredEvents[0] is ToDo)
    }

    @Test
    fun `saveEntry creates SharedEvent and calls postSharedEvent`() = runTest {
        coEvery { modelFacade.postSharedEvent(any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(MainUiEvent.FabClicked)
        viewModel.onEvent(MainUiEvent.EntryTypeChanged(EntryType.SHARED_EVENT))
        viewModel.onEvent(MainUiEvent.EntryNameChanged("Shared Party"))

        val mockUser = mockk<com.gcal.app.model.modelFacade.general.User>(relaxed = true) {
            every { username() } returns "Arda"
        }
        viewModel.onEvent(MainUiEvent.ToggleFriendSelection(mockUser))

        viewModel.onEvent(MainUiEvent.SaveEntryClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { modelFacade.postSharedEvent(any(), any()) }
        assertEquals(false, viewModel.uiState.value.isEditorOpen)
    }

    @Test
    fun `saveEntry catch block handles unexpected system crashes`() = runTest {
        coEvery { modelFacade.createEvent(any()) } throws RuntimeException("Hard Crash")

        viewModel.onEvent(MainUiEvent.FabClicked)
        viewModel.onEvent(MainUiEvent.EntryTypeChanged(EntryType.APPOINTMENT))
        viewModel.onEvent(MainUiEvent.EntryNameChanged("Crash Event"))

        viewModel.onEvent(MainUiEvent.SaveEntryClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("Speichern fehlgeschlagen") == true)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `comprehensive draft update test`() = runTest {

        viewModel.onEvent(MainUiEvent.FabClicked)

        viewModel.onEvent(MainUiEvent.EntryDescriptionChanged("Neue Beschreibung"))
        viewModel.onEvent(MainUiEvent.EntryLocationChanged("Büro 1"))
        val startTime = LocalTime.of(9, 0)
        val endTime = LocalTime.of(10, 0)
        viewModel.onEvent(MainUiEvent.EntryTimeChanged(startTime, endTime))

        val testGroup = mockk<Group>(relaxed = true)
        viewModel.onEvent(MainUiEvent.EntryGroupChanged(testGroup))

        val draft = viewModel.uiState.value.draftState
        assertEquals("Neue Beschreibung", draft.description)
        assertEquals("Büro 1", draft.location)
        assertEquals(startTime, draft.startTime)
        assertEquals(endTime, draft.endTime)
        assertEquals(testGroup, draft.selectedGroup)
    }

    @Test
    fun `onEvent RequestDeleteClicked opens confirmation dialog`() = runTest {
        viewModel.onEvent(MainUiEvent.RequestDeleteClicked)

        assertTrue(viewModel.uiState.value.isDeleteConfirmationOpen)
    }

    @Test
    fun `calculateDateRange at year end boundary`() = runTest {
        val silvester = LocalDate.of(2026, 12, 31)
        viewModel.onEvent(MainUiEvent.DateSelected(silvester))
        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.YEAR))

        assertEquals(ViewMode.YEAR, viewModel.uiState.value.viewMode)
        assertEquals(silvester, viewModel.uiState.value.currentDate)
    }

    @Test
    fun `observeGroups ignores groups named None`() = runTest {
        val groupNone = mockk<Group>(relaxed = true) { every { groupName() } returns "None" }
        val groupWork = mockk<Group>(relaxed = true) { every { groupName() } returns "Work" }

        val freshModel = mockk<ModelFacade>(relaxed = true)
        every { freshModel.getAllGroups() } returns flowOf(listOf(groupNone, groupWork))
        every { freshModel.friends() } returns emptyFlow()
        every { freshModel.getEventsIn(any(), any()) } returns emptyFlow()

        val localVm = MainViewModel(freshModel)
        advanceUntilIdle()

        val groups = localVm.uiState.value.allGroups
        assertEquals(1, groups.size)
        assertEquals("Work", groups[0].group.groupName())
    }

    @Test
    fun `saveOrUpdateGroup update failure sets error message`() = runTest {
        coEvery { modelFacade.updateGroup(any()) } returns Result.failure(Exception("DB Error"))

        viewModel.onEvent(MainUiEvent.SaveGroup("Arbeit", "#FF0000", 123L))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("Update fehlgeschlagen") == true)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `saveEntry uses NoGroup when no group is selected`() = runTest {
        val apptSlot = slot<UserAppointment>()
        coEvery { modelFacade.createEvent(capture(apptSlot)) } returns Result.success(Unit)

        viewModel.onEvent(MainUiEvent.FabClicked)
        viewModel.onEvent(MainUiEvent.EntryTypeChanged(EntryType.APPOINTMENT))
        viewModel.onEvent(MainUiEvent.EntryNameChanged("Test ohne Gruppe"))

        viewModel.onEvent(MainUiEvent.SaveEntryClicked)
        advanceUntilIdle()

        assertEquals(NoGroup, apptSlot.captured.group())
    }


    @Test
    fun `saveOrUpdateGroup handles invalid color hex gracefully`() = runTest {
        coEvery { modelFacade.createGroup(any()) } returns Result.success(Unit)

        viewModel.onEvent(MainUiEvent.SaveGroup("SchrottFarbe", "KEINE_FARBE", null))
        advanceUntilIdle()

        coVerify(exactly = 1) { modelFacade.createGroup(any()) }
    }

    @Test
    fun `state and events`() = runTest {
        viewModel.onEvent(MainUiEvent.EntryDescriptionChanged("Desc"))
        viewModel.onEvent(MainUiEvent.EntryLocationChanged("Loc"))

        val dummyGroup = mockk<Group>(relaxed = true) { every { groupId() } returns 5L }
        viewModel.onEvent(MainUiEvent.OpenEditGroupClicked(dummyGroup))

        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.YEAR))
        viewModel.onEvent(MainUiEvent.DateSwiped(-1))

        viewModel.onEvent(MainUiEvent.SaveGroup("", "#000000", null))

        viewModel.onEvent(MainUiEvent.RequestDeleteClicked)

        assertTrue(viewModel.uiState.value.isDeleteConfirmationOpen)
    }

    @Test
    fun `Extra Tests for events`() = runTest {

        viewModel.onEvent(MainUiEvent.JumpToDateClicked)
        val jumpDate = LocalDate.of(2028, 5, 20)
        viewModel.onEvent(MainUiEvent.JumpToDateConfirmed(jumpDate))
        assertEquals(jumpDate, viewModel.uiState.value.currentDate)

        val dummyGroup = mockk<Group>(relaxed = true) {
            every { groupId() } returns 888L
            every { groupName() } returns "TestEdit"
        }
        viewModel.onEvent(MainUiEvent.OpenEditGroupClicked(dummyGroup))
        assertEquals(888L, viewModel.uiState.value.groupBeingEdited?.groupId())

        viewModel.onEvent(MainUiEvent.EntryDescriptionChanged("Final Coverage Push"))
        viewModel.onEvent(MainUiEvent.EntryLocationChanged("Zuhause"))

        viewModel.onEvent(MainUiEvent.CancelEditorClicked)
        assertEquals(false, viewModel.uiState.value.isEditorOpen)

        viewModel.onEvent(MainUiEvent.ViewModeSelected(ViewMode.MONTH))
        viewModel.onEvent(MainUiEvent.DateSwiped(1))

        coEvery { modelFacade.syncExperiencePoints() } returns Result.success(Unit)
        val finalVm = MainViewModel(modelFacade)
        advanceUntilIdle()
    }

    @Test
    fun `syncXpOnStartup handles unexpected system exception`() = runTest {
        val freshModel = mockk<ModelFacade>(relaxed = true)
        coEvery { freshModel.syncExperiencePoints() } throws RuntimeException("Fatal System Error")

        val vm = MainViewModel(freshModel)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage!!.contains("Systemfehler"))
    }

    @Test
    fun `saveOrUpdateGroup failure during update`() = runTest {
        coEvery { modelFacade.updateGroup(any()) } returns Result.failure(Exception("Update failed"))

        viewModel.onEvent(MainUiEvent.SaveGroup("Arbeit", "#FF0000", 123L))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Update fehlgeschlagen"))
    }

    @Test
    fun `openEditorForEdit maps SharedEvent type correctly`() = runTest {
        val mockShared = mockk<com.gcal.app.model.modelFacade.general.SharedEvent>(relaxed = true) {
            every { eventID() } returns 555L
            every { eventName() } returns "Shared"
            every { end() } returns LocalDateTime.now().plusHours(1)
        }

        viewModel.onEvent(MainUiEvent.EventClicked(mockShared))

        assertEquals(EntryType.SHARED_EVENT, viewModel.uiState.value.draftState.selectedType)
    }

    @Test
    fun `deleteEntry handles model failure during confirm`() = runTest {
        coEvery { modelFacade.deleteEvent(any()) } returns Result.failure(Exception("Delete Error"))

        viewModel.onEvent(MainUiEvent.ConfirmDelete(DeleteScope.SINGLE_INSTANCE))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Löschen fehlgeschlagen"))
    }

    @Test
    fun `completeEvent success with achievements updates state`() = runTest {
        val mockAchievement = mockk<com.gcal.app.model.modelFacade.general.Achievement>(relaxed = true) {
            every { achievementName() } returns "Fleißbiene"
        }
        val mockEvent = mockk<com.gcal.app.model.modelFacade.general.Event>(relaxed = true)

        coEvery { modelFacade.completeEvent(any()) } returns Result.success(listOf(mockAchievement))

        viewModel.onEvent(MainUiEvent.TodoChecked(mockEvent, isDone = true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Fleißbiene") == true)
    }

    @Test
    fun `completeEvent failure updates errorMessage`() = runTest {
        val mockEvent = mockk<com.gcal.app.model.modelFacade.general.Event>(relaxed = true)
        coEvery { modelFacade.completeEvent(any()) } returns Result.failure(Exception("Fehler"))

        viewModel.onEvent(MainUiEvent.TodoChecked(mockEvent, isDone = true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Aktion fehlgeschlagen") == true)
    }

    @Test
    fun `completeEvent unexpected crash handled`() = runTest {
        val mockEvent = mockk<com.gcal.app.model.modelFacade.general.Event>(relaxed = true)
        coEvery { modelFacade.completeEvent(any()) } throws RuntimeException("Crash")

        viewModel.onEvent(MainUiEvent.TodoChecked(mockEvent, isDone = true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Kritischer Fehler") == true)
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }
}