package com.gcal.app.model.localData.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Database entity representing events shared across multiple users.
 */
@Entity(
    tableName = "shared_events",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_DEFAULT
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class SharedEventEntity(
    @PrimaryKey
    val eventId: Long,
    val name: String,
    val description: String,
    val groupId: Long?,
    val startTimeTimestamp: LocalDateTime,
    val endTimeTimestamp: LocalDateTime,
    val xpValue: Int
)


