package com.gcal.app.model

import android.util.Log
import com.gcal.app.model.modelData.XpDistributor
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelData.repo.UserRepo
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.model.modelFacade.general.Event
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class XpDistributorTest {
    private val userRepo = mockk<UserRepo>()
    private val eventRepo = mockk<EventRepo>()

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
    fun testCalculateAppointments() = runTest {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val endOfDay = now.toLocalDate().atTime(LocalTime.MAX)
        val tomorrow = now.plusDays(1).toLocalDate()
        val startOfTomorrow = tomorrow.atStartOfDay()
        val endOfTomorrow = tomorrow.atTime(LocalTime.MAX)

        val todayFlow = MutableStateFlow(TestData.appointmentsToday())
        val tomorrowFlow = MutableStateFlow(TestData.appointmentsTomorrow())
        val sharedEventsFlow = MutableStateFlow(TestData.sharedEvents())
        val response = Response.Success(Unit)

        val capturedAppointments =
            mutableListOf<UserAppointment>()
        val capturedSharedEvents =
            mutableListOf<UserSharedEvent>()

        every { userRepo.getCurrentUser() } returns MutableStateFlow(TestData.profile)
        coEvery { eventRepo.updateAppointment(capture(capturedAppointments)) } returns response
        every { eventRepo.getAppointmentsIn(startOfDay, endOfDay) } returns todayFlow
        every { eventRepo.getAppointmentsIn(startOfTomorrow, endOfTomorrow) } returns tomorrowFlow
        every { eventRepo.getSharedEventsIn(any(), any()) } returns sharedEventsFlow
        coEvery { eventRepo.updateSharedEvent(capture(capturedSharedEvents)) } returns response

        val distributor = XpDistributor(userRepo, eventRepo)
        distributor.distributeXp()

        assertEquals(TestData.appointmentsTomorrow().size, capturedAppointments.size)
        assertEquals(tomorrowFlow.first().first().eventID(), capturedAppointments.first().eventID())
        assertEquals(tomorrowFlow.first().last().eventID(), capturedAppointments.last().eventID())
        for (event: Event in capturedAppointments + capturedSharedEvents) {
            assert(event.experiencePoints().value() != 0)
        }
        val totalXp = capturedAppointments.sumOf { it.experiencePoints().value() }
        assert(totalXp <= 128)
    }

    @Test
    fun testCalculateAppointmentsToday() = runTest {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val endOfDay = now.toLocalDate().atTime(LocalTime.MAX)
        val tomorrow = now.plusDays(1).toLocalDate()
        val startOfTomorrow = tomorrow.atStartOfDay()
        val endOfTomorrow = tomorrow.atTime(LocalTime.MAX)

        val todayFlow = MutableStateFlow(TestData.bareAppointmentsToday())
        val tomorrowFlow = MutableStateFlow(TestData.appointmentsTomorrow())
        val sharedEventsFlow = MutableStateFlow(TestData.sharedEvents())
        val response = Response.Success(Unit)

        val capturedAppointments =
            mutableListOf<UserAppointment>()
        val capturedSharedEvents =
            mutableListOf<UserSharedEvent>()

        every { userRepo.getCurrentUser() } returns MutableStateFlow(TestData.profile)
        coEvery { eventRepo.updateAppointment(capture(capturedAppointments)) } returns response
        every { eventRepo.getAppointmentsIn(startOfDay, endOfDay) } returns todayFlow
        every { eventRepo.getAppointmentsIn(startOfTomorrow, endOfTomorrow) } returns tomorrowFlow
        every { eventRepo.getSharedEventsIn(any(), any()) } returns sharedEventsFlow
        coEvery { eventRepo.updateSharedEvent(capture(capturedSharedEvents)) } returns response

        val distributor = XpDistributor(userRepo, eventRepo)
        distributor.distributeXp()

        assertEquals(TestData.appointmentsTomorrow().size+ TestData.bareAppointmentsToday().size, capturedAppointments.size)
        for (event: Event in capturedAppointments + capturedSharedEvents) {
            assert(event.experiencePoints().value() != 0)
        }
        val totalXp = capturedAppointments.sumOf { it.experiencePoints().value() }
        assert(totalXp <= 128)
    }

    @Test
    fun testCalculateTodo() = runTest {
        val user = TestData.profile
        val todo = TestData.todo().first()
        every { userRepo.getCurrentUser() } returns MutableStateFlow(user)
        coEvery { eventRepo.getAppointmentsIn(any(),any()) } returns MutableStateFlow(emptyList())
        val distributor = XpDistributor(userRepo, eventRepo)
        val result = distributor.calculateTodo(todo)
        assert(result.experiencePoints().value() != 0)
        assert(result.experiencePoints().value() <= 0.25 * user.dailyXp.value())
    }
}