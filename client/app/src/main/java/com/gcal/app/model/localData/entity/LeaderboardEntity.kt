package com.gcal.app.model.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a user's rank and performance on the leaderboard.
 */
@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey
    val rank: Int,
    val username: String,
    val name: String,
    val xpTotal: Int
)



