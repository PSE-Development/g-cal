package com.gcal.app.ui.view_model.mainViewModel

import com.gcal.app.model.modelFacade.general.Event
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelFacade.general.User
import java.time.LocalDate
import java.time.LocalTime

// --- Enums ---
// Using enums ensures mutually exclusive states, preventing the UI from
// accidentally trying to render conflicting layouts (e.g., Day and Month simultaneously).
enum class ViewMode { DAY, WEEK, MONTH, YEAR }
enum class EditorMode { CREATE, EDIT }
enum class EntryType { TODO, APPOINTMENT, SHARED_EVENT }
enum class DeleteScope { SINGLE_INSTANCE, SERIES_FUTURE }
enum class GroupDialogMode { CREATE, EDIT }

// --- Helper Classes ---

/**
 * UI-specific wrapper for the domain [Group] model.
 * The domain Model doesn't care if a group is currently "filtered out" in the UI.
 * We wrap it here so the ViewModel can toggle [isVisible] to filter the calendar
 * without mutating the actual backend data.
 */
data class UiFilterGroup(
    val group: Group,
    val isVisible: Boolean = true
)

/**
 * Acts as a temporary sandbox for user input during the Create/Edit flow.
 * * WHY A DRAFT STATE?
 * We isolate the user's edits from the actual domain [Event] until they explicitly
 * hit "Save". This allows the user to make changes and then hit "Cancel" without
 * us having to write complex rollback logic for the database/model.
 */
data class EntryDraftState(
    val id: Long = 0L,
    val selectedType: EntryType = EntryType.TODO,
    val name: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now(),
    val endTime: LocalTime = LocalTime.now().plusHours(1),
    val selectedGroup: Group? = null,
    val hasDeadline: Boolean = true,

    // Type-specific fields.
    // The View should use [selectedType] to decide whether to show these inputs.
    val location: String = "",
    val note: String = "",
    val sharedWith: List<User> = emptyList()
) {
    /**
     * The ultimate source of truth for the View's "Save" button enabled state.
     * By centralizing these business rules here, the UI remains "dumb" and never
     * has to guess if a form is ready for submission.
     */
    val isValid: Boolean
        get() {
            // Rule 1: A title is always universally required, save Entry has No-group
            val hasName = name.isNotBlank()

            // Rule 2: No groups also allowed
            val hasGroup = true

            // Rule 3: Time travel is forbidden for scheduled events. ToDos lack a strict duration.
            val isTimeValid = (selectedType == EntryType.TODO || !endTime.isBefore(startTime))

            // Rule 4: A Shared Event makes no sense without invitees.
            val isSharedValid = if (selectedType == EntryType.SHARED_EVENT) sharedWith.isNotEmpty() else true

            return hasName && hasGroup && isTimeValid && isSharedValid
        }
}

// --- Main State ---

/**
 * The single source of truth for the entire Main/Calendar Screen.
 * Exposed as a single data class so the UI always renders a cohesive snapshot,
 * avoiding race conditions (e.g., showing the editor before the draft is loaded).
 */
data class MainUiState(

    // --- Calendar View Context ---
    val viewMode: ViewMode = ViewMode.DAY,
    val currentDate: LocalDate = LocalDate.now(),

    //  (Model/UI Contract): Currently exposes raw domain [Event]s.
    // If the View requires specific formatting (e.g., combining date/time into a string),
    // we should map this to an `EventUiModel` in the future to keep the View logic-free.
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,

    // --- Filter & Groups ---
    val allGroups: List<UiFilterGroup> = emptyList(),

    // Dictates the behavior of the Group save action (Insert vs. Update).
    val groupDialogMode: GroupDialogMode = GroupDialogMode.CREATE,

    // Held in state so the View can pre-fill the edit dialog statelessly.
    // Must be null if [groupDialogMode] is CREATE.
    val groupBeingEdited: Group? = null,

    // Defines the pool of users the current user can invite to Shared Events.
    val friends: List<User> = emptyList(),

    // --- Dialog Controls (Overlays) ---
    // Managing dialog visibility in the ViewModel ensures that dialogs survive
    // Android configuration changes (like screen rotations) seamlessly.
    val isFilterDialogOpen: Boolean = false,
    val isCreateGroupDialogOpen: Boolean = false,
    val isJumpToDateDialogOpen: Boolean = false,
    val isDeleteConfirmationOpen: Boolean = false,

    // --- Editor (Bottom Sheet / Subscreen) ---
    val isEditorOpen: Boolean = false,
    val editorMode: EditorMode = EditorMode.CREATE,

    // The current snapshot of the user's unsaved form inputs.
    val draftState: EntryDraftState = EntryDraftState(),

    // --- Error handling ---
    val errorMessage: String? = null
)
