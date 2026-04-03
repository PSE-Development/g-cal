package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Database entity representing a task or 'To-Do' item.
 */
@Entity(tableName = "todos",
        foreignKeys = [
            ForeignKey(
                entity = EventEntity::class,
                parentColumns = ["eventId"],
                childColumns = ["eventId"],
                onDelete = ForeignKey.CASCADE
            )
        ]
    )
data class ToDoEntity(
    @PrimaryKey
    val eventId: Long,
    val name: String,
    val description: String,
    val deadlineTimestamp: LocalDateTime?,
    val xpValue: Int
)

