package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing the local user's personal profile and progress.
 */
@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey
    val username: String,
    val name: String,
    val xpTotal: Int,
    val xpToday: Int
)

