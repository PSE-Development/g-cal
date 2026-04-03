package com.gcal.app.ui.view_model.mainViewModel

import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelFacade.general.Event
import java.time.LocalDate
import java.time.LocalTime

/**
 * Defines the complete contract of user intents for the Main Screen.
 *
 * By routing all gestures, inputs, and dialog triggers through this sealed interface,
 * the ViewModel maintains absolute authority over the UI state, ensuring that
 * validation, calendar math, and data fetching are kept strictly out of the View layer.
 *
 * Architecture Note:
 * Each event carries the minimum payload required for the ViewModel to process it.
 * The View is responsible only for capturing raw user input and packaging it into
 * the appropriate event — all business logic lives in [MainViewModel.onEvent].
 */
sealed interface MainUiEvent {

    // --- Navigation & Calendar Context ---

    /**
     * Delegates the calendar layout strategy (e.g., Daily, Weekly, Monthly) to the ViewModel.
     *
     * The ViewModel needs this to adjust the scope of data fetched from the Model
     * (e.g., fetching a whole month's events vs. just one day).
     */
    data class ViewModeSelected(val mode: ViewMode) : MainUiEvent

    /**
     * Translates raw UI gestures into logical time shifts.
     *
     * The View passes an abstract direction (-1 or +1) so the ViewModel can perform
     * the actual calendar math (adding days/weeks/months based on the current ViewMode)
     * safely and accurately.
     */
    data class DateSwiped(val direction: Int) : MainUiEvent

    /**
     * Updates the single source of truth for the currently active date.
     *
     * Triggers a state recalculation and potentially a new data fetch for the selected day.
     */
    data class DateSelected(val date: LocalDate) : MainUiEvent

    // --- Overlay & Dialog Management ---

    /** Opens the group filter dialog overlay. */
    data object FilterClicked : MainUiEvent

    /** Opens the jump-to-date dialog overlay. */
    data object JumpToDateClicked : MainUiEvent

    /** Confirms a date selection from the jump-to-date dialog. */
    data class JumpToDateConfirmed(val date: LocalDate) : MainUiEvent

    /**
     * Centralized reset trigger for transient UI elements.
     *
     * Routing dialog dismissals through the ViewModel ensures that if the device rotates,
     * the recreated View doesn't accidentally reopen a dialog that was just closed.
     */
    data object CloseOverlays : MainUiEvent

    // --- Group Management ---

    /**
     * Toggles the visibility of a specific group in the calendar filter.
     *
     * Uses [Long] to match [Group.groupId()] return type from the domain model.
     */
    data class ToggleGroupFilter(val groupId: Long) : MainUiEvent

    /** Opens the create-group dialog from within the filter overlay. */
    data object OpenCreateGroupClicked : MainUiEvent

    /**
     * Initiates the editing flow for an existing group.
     *
     * We pass the [Group] so the ViewModel can extract the data and populate
     * its internal "draft" state, allowing the View's input fields to be pre-filled
     * without the View holding its own local state.
     */
    data class OpenEditGroupClicked(val group: Group) : MainUiEvent

    /**
     * Unified "Upsert" (Update/Insert) event for groups.
     *
     * If [groupId] is null, the ViewModel coordinates with the Model to create a new group.
     * If provided, it overwrites the existing one. This allows the UI to reuse the exact
     * same form for both creating and editing.
     */
    data class SaveGroup(val name: String, val colorHex: String, val groupId: Long? = null) : MainUiEvent

    // --- Editor Flow (Entry Creation/Modification) ---

    /** Opens the editor in CREATE mode via the Floating Action Button. */
    data object FabClicked : MainUiEvent

    /** Opens the editor in EDIT mode for the clicked event. */
    data class EventClicked(val event: Event) : MainUiEvent

    // --- Editor Form (Real-time Draft Updates) ---
    // The following events stream user input to the ViewModel on every change.
    // WHY: This allows the ViewModel to perform real-time business validation
    // (e.g., ensuring end-time is strictly AFTER start-time, or title is not blank)
    // and securely control whether the "Save" button is enabled or disabled.

    /** Updates the event category (ToDo, Appointment, SharedEvent). */
    data class EntryTypeChanged(val type: EntryType) : MainUiEvent

    /** Updates the event title on every keystroke. */
    data class EntryNameChanged(val name: String) : MainUiEvent

    /** Updates the event description on every keystroke. */
    data class EntryDescriptionChanged(val desc: String) : MainUiEvent

    /** Updates the event date from the date picker. */
    data class EntryDateChanged(val date: LocalDate) : MainUiEvent

    /** Updates both start and end time from the time picker. */
    data class EntryTimeChanged(val start: LocalTime, val end: LocalTime) : MainUiEvent

    /** Updates the group assignment from the group selector. */
    data class EntryGroupChanged(val group: Group) : MainUiEvent

    /** Updates the location field (Appointment/SharedEvent only). */
    data class EntryLocationChanged(val loc: String) : MainUiEvent

    /** Toggles a friend's selection in the SharedEvent participant list. */
    data class ToggleFriendSelection(val user: User) : MainUiEvent

    // --- Actions (Save, Delete, Gamification) ---

    /** Commits the current draft to the Model layer. */
    data object SaveEntryClicked : MainUiEvent

    /** Discards the current draft and closes the editor. */
    data object CancelEditorClicked : MainUiEvent

    /**
     * Elevates a destructive action's intent to the ViewModel.
     *
     * The ViewModel can intercept this to check business rules (e.g., "Is the user allowed
     * to delete this?") before instructing the View to show the actual confirmation dialog.
     */
    data object RequestDeleteClicked : MainUiEvent

    /** Confirms deletion with the specified scope (single or series). */
    data class ConfirmDelete(val scope: DeleteScope) : MainUiEvent

    /**
     * Pushes the completion state to the business logic layer.
     *
     * This is critical not just for updating the UI, but because checking off a ToDo
     * likely triggers backend syncs and the Gamification engine (awarding XP).
     */
    data class TodoChecked(val event: Event, val isDone: Boolean) : MainUiEvent
}
