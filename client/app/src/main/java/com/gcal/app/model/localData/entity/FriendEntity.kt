package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a friend of the user.
 */
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    val username: String,
    val name: String,
    val xpTotal: Int
)
