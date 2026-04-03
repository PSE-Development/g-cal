package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "base_events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0
)