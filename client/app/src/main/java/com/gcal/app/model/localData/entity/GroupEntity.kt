package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a group category for events and appointments within the local database.
 */
@Entity(tableName = "event_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val colour: Int
)
