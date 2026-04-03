package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import java.time.LocalDateTime

/**
 * Database entity for appointments. If the associated group is deleted,
 * the [groupId] is reset to null (representing 'No Group').
 */
@Entity(
    tableName = "appointments",
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
        )],
)
data class AppointmentEntity(
    @PrimaryKey
    val eventId: Long,
    val name: String,
    val description: String,
    val groupId: Long?,
    val startTimeTimestamp: LocalDateTime,
    val endTimeTimestamp: LocalDateTime,
    val xpValue: Int
)
