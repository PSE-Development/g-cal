package com.gcal.app.ui.screens.main

import java.time.LocalDate
import androidx.annotation.StringRes
import com.gcal.app.R


/**
 * Calendar view mode (Day, Week, Month, Year).
 *
 * Provides user-facing labels for each calendar layout option.
 */
enum class UiViewMode(@StringRes val labelResId: Int) {
    DAY(R.string.view_tag),
    WEEK(R.string.view_woche),
    MONTH(R.string.view_monat),
    YEAR(R.string.view_jahr)
}

/**
 * Entry type for creating calendar entries.
 */
enum class UiEntryType(@StringRes val labelResId: Int) {
    TODO(R.string.event_todo),
    TERMIN(R.string.event_termin),
    SHARED_EVENT(R.string.event_shared)
}

/**
 * Repeat options for recurring events.
 *
 * Note: The current ModelFacade does not support recurrence natively.
 * This enum is defined for future extensibility and is currently
 * rendered in the UI as a disabled/hidden option.
 */
enum class UiRepeatOption(@StringRes val labelResId: Int) {
    NONE(R.string.group_none),
}


/**
 * CalendarEvent — UI representation of a domain Event.
 *
 *
 */
data class CalendarEvent(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: String = "",
    val endTime: String = "",
    val groupId: Long? = null,
    val groupName: String = "",
    val groupColor: Int = 0,
    val xpValue: Int = 0,       // Read-only — set by model, displayed on EventCard
    val eventType: UiEntryType = UiEntryType.TODO,
    val repeatOption: UiRepeatOption = UiRepeatOption.NONE,
    val location: String = "",
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val sharedWith: List<Friend> = emptyList()
    // minimumXp REMOVED — XP is determined by backend, not user input
)

/**
 * CalendarGroup — UI representation of a domain Group.
 *
 * The [isVisible] flag controls whether events in this group appear in the calendar view.
 */
data class CalendarGroup(
    val id: Long,
    val name: String,
    val color: Int,
    val isVisible: Boolean = true
)

/**
 * Friend — UI representation of a friend user.
 *
 * Used in the SharedEvent creation flow to select participants.
 */
data class Friend(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null
)