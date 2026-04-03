package com.gcal.app.model.localData

import androidx.room.*
import com.gcal.app.model.localData.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for managing user achievements.
 */
@Dao
interface AchievementDao {

    /**
     * Observes all achievements stored in the database.
     */
    @Query("SELECT * FROM achievements")
    fun observeAllAchievements(): Flow<List<AchievementEntity>>

    /**
     * Retrieves all achievements that have been marked as completed.
     */
    @Query("SELECT * FROM achievements WHERE isCompleted = 1")
    suspend fun getCompletedAchievements(): List<AchievementEntity>

    /**
     * Retrieves achievements completed within a specific timeframe.
     */
    @Query("""
        SELECT * FROM achievements 
        WHERE isCompleted = 1 
        AND completedAtTimestamp BETWEEN :start AND :end
    """)
    suspend fun getCompletedAchievementsIn(start: LocalDateTime, end: LocalDateTime): List<AchievementEntity>

    /**
     * Updates an achievement.
     */
    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    /**
     * Bulk Update for achievements..
     */
    @Update
    suspend fun updateAchievements(achievements: List<AchievementEntity>)
}