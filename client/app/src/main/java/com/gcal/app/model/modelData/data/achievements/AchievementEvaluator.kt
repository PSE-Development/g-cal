package com.gcal.app.model.modelData.data.achievements

import com.gcal.app.model.modelData.ModelData
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelData.repo.UserRepo
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Evaluates user activities against achievement criteria.
 */
class AchievementEvaluator(private val userRepo: UserRepo, private val eventRepo: EventRepo) {

    /**
     * Determines if the conditions for a specific achievement type are fulfilled.
     * @param type The achievement category to evaluate.
     * @return True if the user has met the requirements.
     */
    suspend fun isCriteriaMet(type: AchievementType): Boolean {
        return when (type) {
            AchievementType.EARLY_BIRD -> hasCompletedTasksInRange(LocalTime.of(5, 0), LocalTime.of(10, 0))
            AchievementType.NIGHT_OWL -> hasCompletedTasksInRange(LocalTime.of(0, 0), LocalTime.of(5, 0))
        }
    }

    /**
     * Checks if at least three to do were completed within a specific time window on the current day.
     */
    private suspend fun hasCompletedTasksInRange(start: LocalTime, end: LocalTime): Boolean {
        val today = LocalDate.now()
        val tasks = eventRepo.getCompletedToDoIn(
            today.atTime(start),
            today.atTime(end)
        ).first()

        return tasks.size >= 3
    }
}