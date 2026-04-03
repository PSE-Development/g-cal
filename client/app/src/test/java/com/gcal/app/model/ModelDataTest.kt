package com.gcal.app.model

import android.util.Log
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.modelData.ModelData
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.repo.AchievementHandler
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelData.repo.UserRepo
import com.gcal.app.model.modelFacade.RequestAPI
import com.gcal.app.model.modelFacade.Response
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ModelDataTest {
    val database = mockk<LocalData>()
    val api = mockk<RequestAPI>()
    val eventRepo = mockk<EventRepo>()
    val userRepo = mockk<UserRepo>()
    val achievementHandler = mockk<AchievementHandler>()

    @Before
    fun mockLogging(){
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } answers {
            println("LOG E: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        every { Log.i(any(), any()) } answers {
            println("LOG I: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
    }

    @After
    fun unmockLogging(){
        unmockkStatic(Log::class)
    }

    @Test
    fun completeAppointmentTest() = runTest {
        val appointment = TestData.completableAppointment()
        val captureAppointment = mutableListOf<UserAppointment>()
        val captureProfile = mutableListOf<PersonalUser>()

        coEvery { userRepo.getCurrentUser() } returns MutableStateFlow(TestData.profile)
        coEvery { eventRepo.updateAppointment(capture(captureAppointment)) } returns Response.Success(
            Unit
        )
        coEvery { userRepo.updateCurrentUser(capture(captureProfile)) } returns Response.Success(
            Unit
        )
        coEvery { achievementHandler.checkAchievements() } returns listOf()

        val modelData = ModelData(api, database, userRepo, eventRepo, achievementHandler)
        val result = modelData.completeEvent(appointment)
        assert(result.isSuccess)

        val completedAppointment = captureAppointment.first()
        assert(completedAppointment.eventID() == appointment.eventID())
        assert(completedAppointment.checkCompletion().completed())
        val updatedProfile = captureProfile.first()
        assert(updatedProfile.username() == TestData.profile.username())
        assert(
            updatedProfile.experiencePoints().value() == TestData.profile.experiencePoints()
                .value() + appointment.experiencePoints().value()
        )
        assert(
            updatedProfile.dailyProgress().value() == TestData.profile.dailyProgress()
                .value() + appointment.experiencePoints().value()
        )
    }

    @Test
    fun completeToDoTest() = runTest {
        val todo = TestData.completableTodo()
        val capturedTodo = mutableListOf<UserToDo>()
        val captureProfile = mutableListOf<PersonalUser>()

        coEvery { userRepo.getCurrentUser() } returns MutableStateFlow(TestData.profile)
        coEvery { eventRepo.getAppointmentsIn(any(), any()) } returns MutableStateFlow(TestData.appointmentsToday())
        coEvery { eventRepo.updateToDo(capture(capturedTodo)) } returns Response.Success(
            Unit
        )
        coEvery { userRepo.updateCurrentUser(capture(captureProfile)) } returns Response.Success(
            Unit
        )
        coEvery { achievementHandler.checkAchievements() } returns listOf()

        val modelData = ModelData(api, database, userRepo, eventRepo, achievementHandler)
        val result = modelData.completeEvent(todo)

        assert(result.isSuccess)
        val completedToDo = capturedTodo.first()
        assert(completedToDo.eventID() == todo.eventID())
        assert(completedToDo.checkCompletion().completed())
        val updatedProfile = captureProfile.first()
        assert(updatedProfile.username() == TestData.profile.username())
        assert(
            updatedProfile.experiencePoints().value() == TestData.profile.experiencePoints()
                .value() + completedToDo.experiencePoints().value()
        )
        assert(
            updatedProfile.dailyProgress().value() == TestData.profile.dailyProgress()
                .value() + completedToDo.experiencePoints().value()
        )
    }

    @Test
    fun completeSharedEventTest() = runTest {
        val sharedEvent = TestData.completableSharedEvent()
        val capturedSharedEvents = mutableListOf<UserSharedEvent>()
        val captureProfile = mutableListOf<PersonalUser>()

        coEvery { userRepo.getCurrentUser() } returns MutableStateFlow(TestData.profile)
        coEvery { eventRepo.updateSharedEvent(capture(capturedSharedEvents)) } returns Response.Success(
            Unit
        )
        coEvery { userRepo.updateCurrentUser(capture(captureProfile)) } returns Response.Success(
            Unit
        )
        coEvery { achievementHandler.checkAchievements() } returns listOf()

        val modelData = ModelData(api, database, userRepo, eventRepo, achievementHandler)
        val result = modelData.completeEvent(sharedEvent)

        assert(result.isSuccess)
        val completedSharedEvent = capturedSharedEvents.first()
        assert(completedSharedEvent.eventID() == sharedEvent.eventID())
        assert(completedSharedEvent.checkCompletion().completed())
        val updatedProfile = captureProfile.first()
        assert(updatedProfile.username() == TestData.profile.username())
        assert(
            updatedProfile.experiencePoints().value() == TestData.profile.experiencePoints()
                .value() + sharedEvent.experiencePoints().value()
        )
        assert(
            updatedProfile.dailyProgress().value() == TestData.profile.dailyProgress()
                .value() + sharedEvent.experiencePoints().value()
        )
    }
}