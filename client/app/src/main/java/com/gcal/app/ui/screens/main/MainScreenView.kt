package com.gcal.app.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.Event
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.ToDo
import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.ui.components.GCalBottomBar
import com.gcal.app.ui.screens.main.components.*
import com.gcal.app.ui.screens.main.components.DeleteScope as ComponentDeleteScope
import com.gcal.app.ui.view_model.mainViewModel.DeleteScope as ViewModelDeleteScope
import com.gcal.app.ui.view_model.mainViewModel.EditorMode
import com.gcal.app.ui.view_model.mainViewModel.EntryDraftState
import com.gcal.app.ui.view_model.mainViewModel.EntryType
import com.gcal.app.ui.view_model.mainViewModel.MainUiEvent
import com.gcal.app.ui.view_model.mainViewModel.MainViewModel
import com.gcal.app.ui.view_model.mainViewModel.UiFilterGroup
import com.gcal.app.ui.view_model.mainViewModel.ViewMode
import java.time.LocalTime
import java.time.YearMonth
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * MainScreenView — Calendar main screen
 *
 *
 * @param viewModel The MainViewModel (provided by GCalNavHost via ViewModelFactory)
 * @param currentRoute Current navigation route for BottomBar highlighting
 * @param onNavigate Callback for bottom navigation tab switches
 * @param modifier Modifier for the screen container
 */
@Composable
fun MainScreenView(
    viewModel: MainViewModel,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe state from ViewModel — recomposes automatically on state changes
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Map domain Event objects to UI-friendly CalendarEvent objects.

    val uiEvents = remember(uiState.events) {
        uiState.events.mapNotNull { it.toCalendarEvent() }
    }

    // Map ViewModel's UiFilterGroup list to UI-friendly CalendarGroup objects
    val uiGroups = remember(uiState.allGroups) {
        uiState.allGroups.map { it.toCalendarGroup() }
    }


    val friends = remember(uiState.friends) {
        uiState.friends.map { user ->
            Friend(
                id = user.username(),
                username = user.username(),
                displayName = user.username()
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                // ========== TOP BAR ==========
                CalendarTopBar(
                    activeMode = uiState.viewMode.toUiViewMode(),
                    onViewModeSelected = { uiMode ->
                        viewModel.onEvent(MainUiEvent.ViewModeSelected(uiMode.toViewModelMode()))
                    },
                    onFilterClicked = {
                        viewModel.onEvent(MainUiEvent.FilterClicked)
                    },
                    onJumpToDateClicked = {
                        viewModel.onEvent(MainUiEvent.JumpToDateClicked)
                    }
                )

                // ========== HEADER ==========
                CalendarHeader(
                    currentDate = uiState.currentDate,
                    currentMonth = YearMonth.from(uiState.currentDate),
                    currentYear = uiState.currentDate.year,
                    viewMode = uiState.viewMode.toUiViewMode()
                )
            }
        },
        bottomBar = {
            GCalBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.onEvent(MainUiEvent.FabClicked)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.event_new)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {

                val filteredEvents = getFilteredEvents(uiEvents, uiGroups)
                val currentYearMonth = YearMonth.from(uiState.currentDate)

                when (uiState.viewMode) {
                    ViewMode.DAY -> {
                        DayViewContent(
                            currentDate = uiState.currentDate,
                            events = filteredEvents,
                            onDateChanged = { newDate ->
                                viewModel.onEvent(MainUiEvent.DateSelected(newDate))
                            },
                            onEventClicked = { calendarEvent ->
                                // Match the UI CalendarEvent back to its domain Event by Long ID
                                uiState.events.find { it.eventID() == calendarEvent.id }?.let { event ->
                                    viewModel.onEvent(MainUiEvent.EventClicked(event))
                                }
                            },
                            onEventChecked = { calendarEvent, isChecked ->
                                uiState.events.find { it.eventID() == calendarEvent.id }?.let { event ->
                                    if (isChecked) {
                                        viewModel.onEvent(MainUiEvent.TodoChecked(event, true))
                                    }
                                }
                            }
                        )
                    }
                    ViewMode.WEEK -> {
                        WeekViewContent(
                            currentDate = uiState.currentDate,
                            events = filteredEvents,
                            onDateChanged = { newDate ->
                                viewModel.onEvent(MainUiEvent.DateSwiped(
                                    if (newDate.isAfter(uiState.currentDate)) 1 else -1
                                ))
                            },
                            onDaySelected = { selectedDate ->
                                viewModel.onEvent(MainUiEvent.DateSelected(selectedDate))
                            },
                            onEventClicked = { calendarEvent ->
                                uiState.events.find { it.eventID() == calendarEvent.id }?.let { event ->
                                    viewModel.onEvent(MainUiEvent.EventClicked(event))
                                }
                            }
                        )
                    }
                    ViewMode.MONTH -> {
                        MonthViewContent(
                            currentMonth = currentYearMonth,
                            events = filteredEvents,
                            onMonthChanged = { newMonth ->
                                val direction = if (newMonth.isAfter(currentYearMonth)) 1 else -1
                                viewModel.onEvent(MainUiEvent.DateSwiped(direction))
                            },
                            onDaySelected = { selectedDate ->
                                viewModel.onEvent(MainUiEvent.DateSelected(selectedDate))
                            }
                        )
                    }
                    ViewMode.YEAR -> {
                        YearViewContent(
                            currentYear = uiState.currentDate.year,
                            currentMonth = currentYearMonth,
                            events = filteredEvents,
                            onYearChanged = { newYear ->
                                val direction = if (newYear > uiState.currentDate.year) 1 else -1
                                viewModel.onEvent(MainUiEvent.DateSwiped(direction))
                            },
                            onMonthSelected = { selectedMonth ->
                                viewModel.onEvent(MainUiEvent.DateSelected(selectedMonth.atDay(1)))
                            }
                        )
                    }
                }
            }
        }
    }

    // Jump to Date Dialog
    if (uiState.isJumpToDateDialogOpen) {
        JumpToDateDialog(
            currentDate = uiState.currentDate,
            onConfirmDate = { selectedDate ->
                viewModel.onEvent(MainUiEvent.JumpToDateConfirmed(selectedDate))
            },
            onDismiss = {
                viewModel.onEvent(MainUiEvent.CloseOverlays)
            }
        )
    }

    // Filter Dialog
    if (uiState.isFilterDialogOpen) {
        FilterDialog(
            groups = uiGroups,
            onToggleGroup = { groupId ->
                viewModel.onEvent(MainUiEvent.ToggleGroupFilter(groupId))
            },
            onCreateGroup = { name, color ->
                val colorHex = String.format("#%06X", 0xFFFFFF and color.toInt())
                viewModel.onEvent(MainUiEvent.SaveGroup(name, colorHex))
            },
            onDismiss = {
                viewModel.onEvent(MainUiEvent.CloseOverlays)
            }
        )
    }

    // Create Entry Sheet
    if (uiState.isEditorOpen && uiState.editorMode == EditorMode.CREATE) {
        CreateEntrySheetWithViewModel(
            draftState = uiState.draftState,
            groups = uiGroups.filter { it.isVisible },
            domainGroups = uiState.allGroups.map { it.group },
            domainFriends = uiState.friends,
            onEvent = { event -> viewModel.onEvent(event) },
            onDismiss = {
                viewModel.onEvent(MainUiEvent.CancelEditorClicked)
            }
        )
    }

    // Event Editor Dialog
    if (uiState.isEditorOpen && uiState.editorMode == EditorMode.EDIT) {
        EventEditorDialogWithViewModel(
            draftState = uiState.draftState,
            groups = uiGroups.filter { it.isVisible },
            friends = friends,
            domainGroups = uiState.allGroups.map { it.group },
            domainFriends = uiState.friends,
            onEvent = { event -> viewModel.onEvent(event) },
            onDismiss = {
                viewModel.onEvent(MainUiEvent.CancelEditorClicked)
            }
        )
    }
}



/**
 * Maps a domain [Event] to the UI-friendly [CalendarEvent].
 *
 * Uses Kotlin smart casts to extract type-specific fields (e.g., start time for Appointment,
 * group for SharedEvent). Returns null for unknown Event subtypes as a safety measure.
 *
 * All ID fields remain as [Long] to match the domain model — no String conversion needed.
 */
private fun Event.toCalendarEvent(): CalendarEvent? {
    return when (this) {
        is Appointment -> CalendarEvent(
            id = eventID(),
            title = eventName(),
            description = description(),
            date = start().toLocalDate(),
            startTime = start().toLocalTime().toString().take(5),
            endTime = end()!!.toLocalTime().toString().take(5),
            groupId = group().groupId(),
            groupName = group().groupName(),
            groupColor = group().groupColour(),
            xpValue = experiencePoints().value(),
            eventType = if (this is SharedEvent) UiEntryType.SHARED_EVENT else UiEntryType.TERMIN,
            repeatOption = UiRepeatOption.NONE,
            location = "",
            isCompleted = checkCompletion().completed()
        )
        is ToDo -> CalendarEvent(
            id = eventID(),
            title = eventName(),
            description = description(),
            date = end()!!.toLocalDate(),
            xpValue = experiencePoints().value(),
            eventType = UiEntryType.TODO,
            repeatOption = UiRepeatOption.NONE,
            isCompleted = checkCompletion().completed()
        )
        else -> null
    }
}

/**
 * Maps a ViewModel [UiFilterGroup] to the UI-friendly [CalendarGroup].
 *
 * Color is kept as [Int] (Android ColorInt) to match [Group.groupColour()].
 */
private fun UiFilterGroup.toCalendarGroup(): CalendarGroup {
    return CalendarGroup(
        id = group.groupId(),
        name = group.groupName(),
        color = group.groupColour(),
        isVisible = isVisible
    )
}

/**
 * Maps ViewModel [ViewMode] to UI [UiViewMode] for the CalendarTopBar.
 */
private fun ViewMode.toUiViewMode(): UiViewMode {
    return when (this) {
        ViewMode.DAY -> UiViewMode.DAY
        ViewMode.WEEK -> UiViewMode.WEEK
        ViewMode.MONTH -> UiViewMode.MONTH
        ViewMode.YEAR -> UiViewMode.YEAR
    }
}

/**
 * Maps UI [UiViewMode] back to ViewModel [ViewMode] for event dispatch.
 */
private fun UiViewMode.toViewModelMode(): ViewMode {
    return when (this) {
        UiViewMode.DAY -> ViewMode.DAY
        UiViewMode.WEEK -> ViewMode.WEEK
        UiViewMode.MONTH -> ViewMode.MONTH
        UiViewMode.YEAR -> ViewMode.YEAR
    }
}

/**
 * Maps ViewModel [EntryType] to UI [UiEntryType] for the editor form.
 */
private fun EntryType.toUiEntryType(): UiEntryType {
    return when (this) {
        EntryType.TODO -> UiEntryType.TODO
        EntryType.APPOINTMENT -> UiEntryType.TERMIN
        EntryType.SHARED_EVENT -> UiEntryType.SHARED_EVENT
    }
}

/**
 * Maps UI [UiEntryType] back to ViewModel [EntryType] for event dispatch.
 *
 * Used by the bridge functions when translating the editor's output (CalendarEvent)
 * back into MainUiEvent.EntryTypeChanged for the ViewModel.
 */
private fun UiEntryType.toViewModelEntryType(): EntryType {
    return when (this) {
        UiEntryType.TODO -> EntryType.TODO
        UiEntryType.TERMIN -> EntryType.APPOINTMENT
        UiEntryType.SHARED_EVENT -> EntryType.SHARED_EVENT
    }
}

/**
 * Filters calendar events by group visibility.
 *
 * Events without a group (e.g., ToDo items) are always visible.
 * Events with a group are only shown if their group's [CalendarGroup.isVisible] is true.
 */
private fun getFilteredEvents(
    events: List<CalendarEvent>,
    groups: List<CalendarGroup>
): List<CalendarEvent> {
    val hiddenGroupIds = groups.filter { !it.isVisible }.map { it.id }.toSet()
    return events.filter { event ->
        event.groupId == null || event.groupId !in hiddenGroupIds
    }
}



/**
 * CreateEntrySheet wrapper for ViewModel integration.
 *
 * Bridges the gap between the component's callback-based API and the ViewModel's
 * event-driven architecture. Translates the saved CalendarEvent back into
 * individual MainUiEvent updates before triggering the save.
 *

 */
@Composable
private fun CreateEntrySheetWithViewModel(
    draftState: EntryDraftState,
    groups: List<CalendarGroup>,
    domainGroups: List<Group>,
    domainFriends: List<User>,
    onEvent: (MainUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    CreateEntrySheet(
        initialDate = draftState.date,
        groups = groups,
        friends = domainFriends.map { user ->
            Friend(
                id = user.username(),
                username = user.username(),
                displayName = user.username()
            )
        },
        onSave = { newEvent ->
            //  Entry type — determines how the ViewModel constructs the domain object
            onEvent(MainUiEvent.EntryTypeChanged(newEvent.eventType.toViewModelEntryType()))

            // Basic fields
            onEvent(MainUiEvent.EntryNameChanged(newEvent.title))
            onEvent(MainUiEvent.EntryDescriptionChanged(newEvent.description))
            onEvent(MainUiEvent.EntryDateChanged(newEvent.date))

            //  Time — parse from String back to LocalTime
            val startTime = LocalTime.parse(newEvent.startTime.ifEmpty { "09:00" })
            val endTime = LocalTime.parse(newEvent.endTime.ifEmpty { "10:00" })
            onEvent(MainUiEvent.EntryTimeChanged(startTime, endTime))

            // Group — reverse-lookup domain Group by matching groupId
            newEvent.groupId?.let { gId ->
                domainGroups.find { it.groupId() == gId }?.let { group ->
                    onEvent(MainUiEvent.EntryGroupChanged(group))
                }
            }

            // Location (Appointment/SharedEvent only)
            if (newEvent.location.isNotBlank()) {
                onEvent(MainUiEvent.EntryLocationChanged(newEvent.location))
            }

            // Shared participants — reverse-lookup domain User by username
            newEvent.sharedWith.forEach { friend ->
                domainFriends.find { it.username() == friend.username }?.let { user ->
                    onEvent(MainUiEvent.ToggleFriendSelection(user))
                }
            }

            // Commit the draft to the Model layer
            onEvent(MainUiEvent.SaveEntryClicked)
        },
        onDismiss = onDismiss
    )
}

/**
 * EventEditorDialog wrapper for ViewModel integration.
 *
 * Converts the ViewModel's [EntryDraftState] into a [CalendarEvent] for the editor component,
 * and translates the editor's output back into ViewModel events.
 *
 */
@Composable
private fun EventEditorDialogWithViewModel(
    draftState: EntryDraftState,
    groups: List<CalendarGroup>,
    friends: List<Friend>,
    domainGroups: List<Group>,
    domainFriends: List<User>,
    onEvent: (MainUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val event = CalendarEvent(
        id = draftState.id,
        title = draftState.name,
        description = draftState.description,
        date = draftState.date,
        startTime = draftState.startTime.toString().take(5),
        endTime = draftState.endTime.toString().take(5),
        groupId = draftState.selectedGroup?.groupId(),
        groupName = draftState.selectedGroup?.groupName() ?: "",
        groupColor = draftState.selectedGroup?.groupColour() ?: 0,
        xpValue = 0,
        eventType = draftState.selectedType.toUiEntryType(),
        location = draftState.location,
        sharedWith = draftState.sharedWith.map { user ->
            Friend(
                id = user.username(),
                username = user.username(),
                displayName = user.username()
            )
        }
    )

    EventEditorDialog(
        event = event,
        groups = groups,
        friends = friends,
        isRecurringSeries = false,
        onSave = { updatedEvent ->
            // Entry type
            onEvent(MainUiEvent.EntryTypeChanged(updatedEvent.eventType.toViewModelEntryType()))

            //  Basic fields
            onEvent(MainUiEvent.EntryNameChanged(updatedEvent.title))
            onEvent(MainUiEvent.EntryDescriptionChanged(updatedEvent.description))
            onEvent(MainUiEvent.EntryDateChanged(updatedEvent.date))

            //  Time
            val startTime = LocalTime.parse(updatedEvent.startTime.ifEmpty { "09:00" })
            val endTime = LocalTime.parse(updatedEvent.endTime.ifEmpty { "10:00" })
            onEvent(MainUiEvent.EntryTimeChanged(startTime, endTime))

            //  Group — reverse-lookup domain Group by matching groupId
            updatedEvent.groupId?.let { gId ->
                domainGroups.find { it.groupId() == gId }?.let { group ->
                    onEvent(MainUiEvent.EntryGroupChanged(group))
                }
            }

            //  Location
            onEvent(MainUiEvent.EntryLocationChanged(updatedEvent.location))

            // Shared participants

            draftState.sharedWith.forEach { user ->
                onEvent(MainUiEvent.ToggleFriendSelection(user)) // un-toggle old
            }
            updatedEvent.sharedWith.forEach { friend ->
                domainFriends.find { it.username() == friend.username }?.let { user ->
                    onEvent(MainUiEvent.ToggleFriendSelection(user)) // toggle new
                }
            }


            onEvent(MainUiEvent.SaveEntryClicked)
        },
        onDelete = { compDeleteScope: ComponentDeleteScope ->

            val vmScope: ViewModelDeleteScope = when (compDeleteScope) {
                ComponentDeleteScope.SINGLE_INSTANCE -> ViewModelDeleteScope.SINGLE_INSTANCE
                ComponentDeleteScope.SERIES_FUTURE -> ViewModelDeleteScope.SERIES_FUTURE
            }
            onEvent(MainUiEvent.ConfirmDelete(vmScope))
        },
        onDismiss = onDismiss
    )
}