package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Database entity representing a user achievement record.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val name: String,
    val isCompleted: Boolean,
    val completedAtTimestamp: LocalDateTime?
)