package com.gcal.app.model.modelData.data.achievements

import com.gcal.app.model.modelData.data.system.LocalDateTimeSerializer
import com.gcal.app.model.modelFacade.general.Achievement
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Data class representing a user's completion status for a specific achievement type.
 */
@Serializable
data class UserAchievement(
    val type: AchievementType,
    private val completed: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    private val achievedAt: LocalDateTime?
) : Achievement {
    override fun achievementName() = type.achievementName
    override fun achievementDescription() = type.description
    override fun isCompleted() = completed
    override fun completionDate() = achievedAt
}