package com.gcal.app.ui.view_model.mainViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.NoGroup
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelFacade.*
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.Event
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.ToDo
import com.gcal.app.model.modelFacade.general.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val model: ModelFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    companion object {
        private const val TAG = "MainViewModel"
    }

    init {
        // Establishes reactive subscriptions to the Model layer immediately upon creation.
        syncXpOnStartup()
        observeEvents()
        observeGroups()
        observeFriends()
    }

    fun onEvent(event: MainUiEvent) {
        Log.d(TAG, "Event empfangen: ${event::class.simpleName} - Details: $event")
        when (event) {
            // --- Navigation ---
            is MainUiEvent.ViewModeSelected -> _uiState.update { it.copy(viewMode = event.mode) }
            is MainUiEvent.DateSelected -> _uiState.update { it.copy(currentDate = event.date, viewMode = ViewMode.DAY) }
            is MainUiEvent.DateSwiped -> handleSwipe(event.direction)
            is MainUiEvent.JumpToDateConfirmed -> _uiState.update { it.copy(currentDate = event.date, isJumpToDateDialogOpen = false) }

            // --- Dialogs ---
            is MainUiEvent.FilterClicked -> _uiState.update { it.copy(isFilterDialogOpen = true) }
            is MainUiEvent.JumpToDateClicked -> _uiState.update { it.copy(isJumpToDateDialogOpen = true) }
            is MainUiEvent.CloseOverlays -> closeAllDialogs()

            // --- Open Editor ---
            is MainUiEvent.FabClicked -> openEditorForCreate()
            is MainUiEvent.EventClicked -> openEditorForEdit(event.event)

            // --- Editor Updates ---
            is MainUiEvent.EntryTypeChanged -> updateDraft { it.copy(selectedType = event.type) }
            is MainUiEvent.EntryNameChanged -> updateDraft { it.copy(name = event.name) }
            is MainUiEvent.EntryDescriptionChanged -> updateDraft { it.copy(description = event.desc) }
            is MainUiEvent.EntryDateChanged -> updateDraft { it.copy(date = event.date) }
            is MainUiEvent.EntryTimeChanged -> updateDraft { it.copy(startTime = event.start, endTime = event.end) }
            is MainUiEvent.EntryGroupChanged -> updateDraft { it.copy(selectedGroup = event.group) }
            is MainUiEvent.EntryLocationChanged -> updateDraft { it.copy(location = event.loc) }

            // Toggles the selection state of a friend for Shared Events.
            is MainUiEvent.ToggleFriendSelection -> toggleFriendSelection(event.user)

            // --- Actions ---
            is MainUiEvent.SaveEntryClicked -> saveEntry()
            is MainUiEvent.CancelEditorClicked -> closeEditor()
            is MainUiEvent.RequestDeleteClicked -> _uiState.update { it.copy(isDeleteConfirmationOpen = true) }
            is MainUiEvent.ConfirmDelete -> deleteEntry(event.scope)
            is MainUiEvent.TodoChecked -> completeEvent(event.event)

            // --- Groups ---
            is MainUiEvent.OpenCreateGroupClicked -> {
                _uiState.update {
                    it.copy(
                        isCreateGroupDialogOpen = true,
                        groupDialogMode = GroupDialogMode.CREATE,
                        groupBeingEdited = null,
                        isFilterDialogOpen = false
                    )
                }
            }
            is MainUiEvent.OpenEditGroupClicked -> {
                _uiState.update {
                    it.copy(
                        isCreateGroupDialogOpen = true,
                        groupDialogMode = GroupDialogMode.EDIT,
                        groupBeingEdited = event.group,
                        isFilterDialogOpen = false
                    )
                }
            }
            is MainUiEvent.SaveGroup -> saveOrUpdateGroup(event.name, event.colorHex, event.groupId)
            is MainUiEvent.ToggleGroupFilter -> toggleFilter(event.groupId)
        }
    }

    private fun syncXpOnStartup() {
        viewModelScope.launch {
            try {
                // (try-catch)
                model.syncExperiencePoints().fold(
                    onSuccess = { println("XP erfolgreich synchronisiert.") },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = "XP-Synchronisierung fehlgeschlagen: ${error.message}") }
                    }
                )
            } catch (e: Throwable) {
                _uiState.update { it.copy(errorMessage = "Systemfehler beim XP-Synchronisieren: ${e.message}") }
            }
        }
    }

    /**
     * Core reactive pipeline for the Calendar View.
     * Reacts automatically to any changes in the selected date or view mode.
     * Using `flatMapLatest` guarantees that if the user swipes rapidly, obsolete
     * network/database requests are cancelled, and only the latest date range is fetched.
     */
    private fun observeEvents() {
        viewModelScope.launch {
            try {
                uiState
                    .map { state -> calculateDateRange(state.currentDate, state.viewMode) }
                    .distinctUntilChanged()
                    .flatMapLatest { (start, end) ->
                        _uiState.update { it.copy(isLoading = true) }
                        Log.d(TAG, "Frage Model nach Events von $start bis $end...") // <--- DEBUG
                        model.getEventsIn(start, end)
                    }
                    .collect { events ->
                        // --- DEBUGGING START ---
                        Log.d(TAG, "=== FLOW VOM MODEL EMPFANGEN ===")
                        Log.d(TAG, "Gesamtanzahl Events: ${events.size}")

                        val sharedEvents = events.filterIsInstance<SharedEvent>()
                        val appointmentsOnly = events.filterIsInstance<Appointment>().filter { it !is SharedEvent }
                        val todos = events.filterIsInstance<ToDo>()

                        Log.d(TAG, "Davon sind To-Dos: ${todos.size}")
                        todos.forEach { Log.d(TAG, "-> [TO-DO] '${it.eventName()}'") }

                        Log.d(TAG, "Davon sind Shared-Events: ${sharedEvents.size}")
                        sharedEvents.forEach { Log.d(TAG, "-> [SHARED] '${it.eventName()}'") }

                        Log.d(TAG, "Davon sind normale Termine: ${appointmentsOnly.size}")
                        appointmentsOnly.forEach { Log.d(TAG, "-> [TERMIN] '${it.eventName()}'") }
                        // --- DEBUGGING END ---

                        _uiState.update { state ->
                            val hiddenGroupIds = state.allGroups //List of groups
                                .filter { !it.isVisible } // Filter only visible groups
                                .map { it.group.groupId() } // extract the id of those groups

                            val filteredEvents = events.filter { event ->
                                when (event) {
                                    is Appointment -> {
                                        val gid = event.group().groupId()
                                        if (gid == 0L) true else gid !in hiddenGroupIds
                                    }
                                    else -> {
                                        true
                                    }
                                }
                            }
                            state.copy(isLoading = false, events = filteredEvents, errorMessage = null)
                        }
                    }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Fehler beim Laden: ${e.message}")
                }
            }
        }
    }

    /**
     * Streams available groups from the Model and wraps them in UI-specific state objects.
     */
    private fun observeGroups() {
        viewModelScope.launch {
            try {
                model.getAllGroups().collect { groups ->
                    _uiState.update { state ->
                        val existingVisibility = state.allGroups.associate { it.group.groupId() to it.isVisible } // Which groups are visible
                        val updatedGroups = groups
                            .filter { it.groupName() != "None" }
                            .map { group ->
                            UiFilterGroup(
                                group = group,
                                isVisible = existingVisibility[group.groupId()] ?: true // If not in existing, show
                            )
                        }
                        state.copy(allGroups = updatedGroups, errorMessage = null)
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(errorMessage = "Gruppen konnten nicht geladen werden: ${e.message}") }
            }
        }
    }

    private fun observeFriends() {
        viewModelScope.launch {
            try {
                model.friends().collect { users ->
                    _uiState.update { it.copy(friends = users, errorMessage = null) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(errorMessage = "Freundesliste konnte nicht geladen werden: ${e.message}") }
            }
        }
    }

    /**
     * Manages the logic for selecting/deselecting friends in the Shared Event editor.
     */
    private fun toggleFriendSelection(user: User) {
        updateDraft { draft ->
            val currentList = draft.sharedWith.toMutableList()
            // Identify duplicates via username to safely toggle the selection state.
            val exists = currentList.any { it.username() == user.username() }

            if (exists) {
                currentList.removeIf { it.username() == user.username() }
            } else {
                currentList.add(user)
            }
            draft.copy(sharedWith = currentList)
        }
    }



    private fun calculateDateRange(date: LocalDate, mode: ViewMode): Pair<LocalDateTime, LocalDateTime> {
        val startOfDay = date.atStartOfDay()
        return when(mode) {
            ViewMode.DAY -> Pair(startOfDay, startOfDay.plusDays(1))
            ViewMode.WEEK -> {
                // Assumption: Week starts on Monday
                val startOfWeek = startOfDay.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                Pair(startOfWeek, startOfWeek.plusWeeks(1))
            }
            ViewMode.MONTH -> {
                val startOfMonth = startOfDay.withDayOfMonth(1)
                Pair(startOfMonth, startOfMonth.plusMonths(1))
            }
            ViewMode.YEAR -> {
                val startOfYear = startOfDay.withDayOfYear(1)
                Pair(startOfYear, startOfYear.plusYears(1))
            }
        }
    }

    private fun handleSwipe(direction: Int) {
        // direction: -1 = Back, 1 = Forward
        _uiState.update { state ->
            val newDate = when (state.viewMode) {
                ViewMode.DAY -> state.currentDate.plusDays(direction.toLong())
                ViewMode.WEEK -> state.currentDate.plusWeeks(direction.toLong())
                ViewMode.MONTH -> state.currentDate.plusMonths(direction.toLong())
                ViewMode.YEAR -> state.currentDate.plusYears(direction.toLong())
            }
            state.copy(currentDate = newDate)
        }
    }

    // --- Editor Logic ---

    private fun openEditorForCreate() {
        _uiState.update {
            it.copy(
                isEditorOpen = true,
                editorMode = EditorMode.CREATE,
                draftState = EntryDraftState(date = it.currentDate)
            )
        }
    }

    private fun openEditorForEdit(event: Event) {
        // Mapping: Model -> Draft State
        val type = when (event) {
            is ToDo -> EntryType.TODO
            is SharedEvent -> EntryType.SHARED_EVENT
            else -> EntryType.APPOINTMENT
        }

        // Safely extract properties depending on the specific Event subclass.
        val safeDate = if (event is Appointment) event.start().toLocalDate() else event.end()!!.toLocalDate()
        val safeStartTime = if (event is Appointment) event.start().toLocalTime() else LocalTime.now()
        val safeEndTime = event.end()!!.toLocalTime() // all events have end()
        val safeGroup = if (event is Appointment) event.group() else null

        val draft = EntryDraftState(
            id = event.eventID(),
            selectedType = type,
            name = event.eventName(),
            description = event.description(),
            date = safeDate,
            startTime = safeStartTime,
            endTime = safeEndTime,
            selectedGroup = safeGroup,
            location = "",
        )

        _uiState.update {
            it.copy(
                isEditorOpen = true,
                editorMode = EditorMode.EDIT,
                draftState = draft
            )
        }
    }

    private fun updateDraft(transform: (EntryDraftState) -> EntryDraftState) {
        _uiState.update { it.copy(draftState = transform(it.draftState)) }
    }

    private fun saveEntry() {
        val draft = _uiState.value.draftState
        //Debug
        Log.d(TAG, "SaveEntry aufgerufen. Aktueller Draft: Typ=${draft.selectedType}, Name='${draft.name}', Valid=${draft.isValid}")
        // Finaler Check: Speichern abbrechen, falls das Formular ungültig ist
        if (!draft.isValid) {
            Log.d(TAG, "SaveEntry abgebrochen! Draft ist ungültig. (Fehlt Name? Falsche Zeit? Keine Freunde bei SharedEvent?)")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Log.d(TAG, "Ladezustand aktiviert. Baue Model-Payload")

            try {
                val completion = EventCD(eventId = draft.id, isCompleted = false, completionTime = null)
                val startDateTime = LocalDateTime.of(draft.date, draft.startTime)
                val endDateTime = LocalDateTime.of(draft.date, draft.endTime)

                val groupToUse = draft.selectedGroup ?: NoGroup
                val xpToUse = XP.from(0)

                when (draft.selectedType) {
                    EntryType.TODO -> {
                        val finalDeadline: LocalDateTime? = if (draft.hasDeadline) {
                            endDateTime
                        } else {
                            null
                        }
                        val todo = UserToDo(
                            id = draft.id, name = draft.name, description = draft.description,
                            deadline = finalDeadline, xpValue = xpToUse, detail = completion
                        )
                        Log.d(TAG, "Sende To-Do an Model: $todo (Mode: ${_uiState.value.editorMode})")
                        if (_uiState.value.editorMode == EditorMode.CREATE) model.createEvent(todo) else model.updateEvent(todo)
                    }
                    EntryType.APPOINTMENT -> {
                        val appt = UserAppointment(
                            id = draft.id, name = draft.name, description = draft.description,
                            startAt = startDateTime, endAt = endDateTime, group = groupToUse, xpValue = xpToUse, detail = completion
                        )
                        Log.d(TAG, "Sende Appointment an Model: $appt (Mode: ${_uiState.value.editorMode})")
                        if (_uiState.value.editorMode == EditorMode.CREATE) model.createEvent(appt) else model.updateEvent(appt)
                    }
                    EntryType.SHARED_EVENT -> {
                        val shared = UserSharedEvent(
                            id = draft.id, name = draft.name, description = draft.description,
                            startAt = startDateTime, endAt = endDateTime, group = groupToUse, xpValue = xpToUse, detail = completion
                        )
                        Log.d(TAG, "Sende SharedEvent an Model: $shared mit ${draft.sharedWith.size} Freunden (Mode: ${_uiState.value.editorMode})")

                        // Friends are required for shared events!!!
                        if (_uiState.value.editorMode == EditorMode.CREATE) {
                            model.postSharedEvent(shared, draft.sharedWith)
                        } else {
                            model.updateEvent(shared)
                        }
                    }
                }
                Log.d(TAG, "Model-Aufruf abgeschlossen. Schließe Editor.")

                closeEditor()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Throwable) {
                Log.d(TAG, "EXCEPTION in saveEntry: ${e.message}")
                e.printStackTrace()
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Speichern fehlgeschlagen: ${e.message}")
                }
            }
        }
    }

    private fun deleteEntry(scope: DeleteScope) {
        val draft = _uiState.value.draftState
        Log.d(TAG, "deleteEntry aufgerufen. ID: ${draft.id}, Scope: $scope")

        // Dummy-Event, The model only needs the id to remove it
        val dummyEvent: Event = when (draft.selectedType) {
            EntryType.TODO -> UserToDo(
                id = draft.id,
                name = "",
                description = "",
                deadline = LocalDateTime.now(),
                xpValue = XP.from(0),
                detail = EventCD(draft.id, false, null)
            )
            EntryType.APPOINTMENT -> UserAppointment(
                id = draft.id,
                name = "",
                description = "",
                startAt = LocalDateTime.now(),
                endAt = LocalDateTime.now(),
                group = NoGroup,
                xpValue = XP.from(0),
                detail = EventCD(draft.id, false, null)
            )
            else -> UserSharedEvent(
                id = draft.id,
                name = "",
                description = "",
                startAt = LocalDateTime.now(),
                endAt = LocalDateTime.now(),
                group = NoGroup,
                xpValue = XP.from(0),
                detail = EventCD(draft.id, false, null)
            )
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Sende Delete-Request an Model für Typ: ${draft.selectedType}")
                model.deleteEvent(dummyEvent).fold(
                    onSuccess = {
                        Log.d(TAG, "Delete erfolgreich. Schließe Editor.")
                        closeEditor() },
                    onFailure = { e ->
                        Log.d(TAG, "Delete vom Model abgelehnt: ${e.message}")
                        _uiState.update { it.copy(errorMessage = "Löschen fehlgeschlagen: ${e.message}") }
                    }
                )
            } catch (e: Throwable) {
                Log.d(TAG, "EXCEPTION in deleteEntry: ${e.message}")
                _uiState.update {
                    it.copy(errorMessage = "Kritischer Fehler beim Löschen: ${e.message}")
                }
            }
        }
    }

    // --- Actions ---

    /**
     * Bridges the UI action of checking a  with the Gamification engine.
     * Completing an event triggers the Model layer to potentially award XP or achievements.
     */
    private fun completeEvent(event: Event) {
        Log.d(TAG, "completeEvent aufgerufen für Event-ID: ${event.eventID()}")
        viewModelScope.launch {
            try {
                model.completeEvent(event).fold(
                    onSuccess = { achievements ->
                        Log.d(TAG, "Model meldet Erfolg! Neue Achievements: ${achievements?.size ?: 0}")
                        if (!achievements.isNullOrEmpty()) {
                            val msg = "Erfolg! ${achievements.first().achievementName()} freigeschaltet!"
                            _uiState.update { it.copy(errorMessage = msg) }
                        }
                    },
                    onFailure = { e ->
                        Log.d(TAG, "Model meldet Failure bei completeEvent: ${e.message}")
                        _uiState.update { it.copy(errorMessage = "Aktion fehlgeschlagen: ${e.message}") }
                    }
                )
            } catch (e: Throwable) {
                Log.d(TAG, "EXCEPTION in completeEvent: ${e.message}")
                _uiState.update {
                    it.copy(errorMessage = "Kritischer Fehler bei Aktion: ${e.message}")
                }
            }
        }
    }

    private fun saveOrUpdateGroup(name: String, colorHex: String, groupId: Long?) {
        Log.d(TAG, "saveOrUpdateGroup aufgerufen. Name: '$name', Color: '$colorHex', GroupID: $groupId")
        //  Lokale Validierung: Kein leerer Name erlaubt
        if (name.isBlank()) {
            Log.d(TAG, "Speichern abgebrochen: Gruppenname ist leer.")
            _uiState.update { it.copy(errorMessage = "Der Gruppenname darf nicht leer sein.") }
            return
        }
        viewModelScope.launch {
            // UI blockieren und alte Fehler löschen
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val colorInt = try {
                    android.graphics.Color.parseColor(colorHex)
                } catch (e: Exception) {
                    Log.d(TAG, "Ungültiger Farbcode '$colorHex', falle auf Weiß (0xFFFFFF) zurück.")
                    0xFFFFFF
                }

                if (groupId == null) {
                    val newGroup = Group.asUserGroup(id = System.currentTimeMillis(), name = name, color = colorInt)
                    Log.d(TAG, "Sende neue Gruppe an Model: $newGroup")
                    model.createGroup(newGroup).fold(
                        onSuccess = {
                            Log.d(TAG, "Gruppe erstellt. Schließe Dialog.")
                            closeAllDialogs()
                        },
                        onFailure = { e ->
                            Log.d(TAG, "Gruppe erstellung fehlgeschlagen: ${e.message}")
                            _uiState.update { it.copy(errorMessage = "Update fehlgeschlagen: ${e.message}") }
                        }
                    )
                } else {
                    val updatedGroup = Group.asUserGroup(id = groupId, name = name, color = colorInt)
                    Log.d(TAG, "Sende Gruppen-Update an Model: $updatedGroup")
                    model.updateGroup(updatedGroup).fold(
                        onSuccess = {
                            Log.d(TAG, "Update erfolgreich. Schließe Dialog.")
                            closeAllDialogs()
                        },
                        onFailure = { e ->
                            Log.d(TAG, "Update vom Model abgelehnt: ${e.message}")
                            _uiState.update { it.copy(errorMessage = "Update fehlgeschlagen: ${e.message}") }
                        }
                    )
                }
            } catch (e: Throwable) {
                Log.d(TAG, "EXCEPTION in saveOrUpdateGroup: ${e.message}")
                _uiState.update {
                    it.copy(errorMessage = "Gruppe speichern fehlgeschlagen: ${e.message}")
                }
            } finally {
                //turn Loading off
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    // Helpers

    private fun closeEditor() {
        _uiState.update { it.copy(isEditorOpen = false, isDeleteConfirmationOpen = false) }
    }

    private fun closeAllDialogs() {
        _uiState.update {
            it.copy(
                isFilterDialogOpen = false,
                isCreateGroupDialogOpen = false,
                isJumpToDateDialogOpen = false,
                isDeleteConfirmationOpen = false,
                groupBeingEdited = null
            )
        }
    }

    private fun toggleFilter(groupId: Long) {
        _uiState.update { state ->
            val updatedGroups = state.allGroups.map {
                if (it.group.groupId() == groupId) it.copy(isVisible = !it.isVisible) else it
            }
            state.copy(allGroups = updatedGroups)
        }
    }
}