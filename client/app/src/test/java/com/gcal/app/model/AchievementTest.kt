package com.gcal.app.model

import com.gcal.app.model.localData.AchievementDao
import com.gcal.app.model.localData.entity.AchievementEntity
import com.gcal.app.model.modelData.data.achievements.AchievementEvaluator
import com.gcal.app.model.modelData.data.achievements.AchievementType
import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.repo.AchievementHandler
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelData.repo.UserRepo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate


class AchievementTest {
    val userRepo = mockk<UserRepo>()
    val eventRepo = mockk<EventRepo>()

    val achievementDao = mockk<AchievementDao>()

    val achievementEvaluator = mockk<AchievementEvaluator>()


    // AchievementHandler
    @Test
    fun checkAchievementsTest() = runTest {
        val achievements = listOf(
            AchievementEntity("Früher Vogel", false, null),
            AchievementEntity("Nachteule", false, null)
        )
        val capturedAchievements = mutableListOf<AchievementEntity>()
        every { achievementDao.observeAllAchievements() } returns MutableStateFlow(achievements)
        coEvery { achievementEvaluator.isCriteriaMet(AchievementType.EARLY_BIRD) } returns false
        coEvery { achievementEvaluator.isCriteriaMet(AchievementType.NIGHT_OWL) } returns true
        coEvery { achievementDao.updateAchievement(capture(capturedAchievements)) } returns Unit

        val handler = AchievementHandler(userRepo, eventRepo, achievementDao, achievementEvaluator)
        val result = handler.checkAchievements()
        assert(result.map { it.type } == capturedAchievements.map { it.toDomainModel().type })
        assert(capturedAchievements.map { it.toDomainModel().type } == listOf(AchievementType.NIGHT_OWL))
    }

    // Achievement Evaluator
    @Test
    fun earlyBirdCriteriaTest() = runTest {
        val today = LocalDate.now()
        val completedToDo = listOf(
            UserToDo(
                10, "testToDo1", "ab", today.atTime(20, 0), XP.from(0),
                EventCD(10, true, today.atTime(5,30))
            ),
            UserToDo(
                20, "testToDo2", "mn", null, XP.from(0),
                EventCD(20, true, today.atTime(7,11))
            ),
            UserToDo(
                30, "testToDo3", "mn", null, XP.from(0),
                EventCD(30, true, today.atTime(9,13))
            )
        )
        every {
            eventRepo.getCompletedToDoIn(
                today.atTime(5, 0),
                today.atTime(10, 0)
            )
        } returns MutableStateFlow(completedToDo)

        val evaluator = AchievementEvaluator(userRepo, eventRepo)
        val result = evaluator.isCriteriaMet(AchievementType.EARLY_BIRD)
        assert(result)
    }

    @Test
    fun nightOwlCriteriaTest() = runTest {
        val today = LocalDate.now()
        val completedToDo = listOf(
            UserToDo(
                10, "testToDo1", "ab", today.atTime(20, 0), XP.from(0),
                EventCD(10, true, today.atTime(0,30))
            ),
            UserToDo(
                20, "testToDo2", "mn", null, XP.from(0),
                EventCD(20, true, today.atTime(2,11))
            ),
            UserToDo(
                30, "testToDo3", "mn", null, XP.from(0),
                EventCD(30, true, today.atTime(3,13))
            )
        )
        every {
            eventRepo.getCompletedToDoIn(
                today.atTime(0, 0),
                today.atTime(5, 0)
            )
        } returns MutableStateFlow(completedToDo)

        val evaluator = AchievementEvaluator(userRepo, eventRepo)
        val result = evaluator.isCriteriaMet(AchievementType.NIGHT_OWL)
        assert(result)
    }
}
