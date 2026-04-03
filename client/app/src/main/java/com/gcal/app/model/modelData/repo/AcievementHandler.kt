package com.gcal.app.model.modelData.repo

import com.gcal.app.model.localData.AchievementDao
import com.gcal.app.model.asCompletedDatabaseEntity
import com.gcal.app.model.toDomainModel
import com.gcal.app.model.modelData.data.achievements.AchievementEvaluator
import com.gcal.app.model.modelData.data.achievements.UserAchievement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class AchievementHandler(
    userRepo: UserRepo,
    eventRepo: EventRepo,
    val dao: AchievementDao,
    val evaluator: AchievementEvaluator = AchievementEvaluator(
        userRepo,
        eventRepo
    )
) {

    /**
     * Evaluates all uncompleted achievements and returns those that have just been earned.
     */
    suspend fun checkAchievements(): List<UserAchievement> {
        val currentAchievements = getAllAchievements()
        val achievements = currentAchievements
            .filter { !it.isCompleted() }
            .filter { achievement -> evaluator.isCriteriaMet(achievement.type) }
        for (achievement in achievements) {
            dao.updateAchievement(achievement.asCompletedDatabaseEntity())
        }
        return achievements
    }

    suspend fun getCompletedAchievementsIn(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<UserAchievement> {
        return dao.getCompletedAchievementsIn(start, end)
            .map { entity -> entity.toDomainModel() }
    }

    suspend fun getAllAchievements(): List<UserAchievement> {
        return dao.observeAllAchievements()
            .map { list -> list.map { it.toDomainModel() } }
            .first()
    }
}