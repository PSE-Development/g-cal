package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Tracks completion details for various event types.
 * Linked to Appointments, ToDos, and SharedEvents via Foreign Keys on EventEntity's.
 */
@Entity(
    tableName = "event_completions",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EventCompletionEntity(
    @PrimaryKey
    val eventId: Long,
    val completedAtTimestamp: LocalDateTime?,
    val isCompleted: Boolean
)