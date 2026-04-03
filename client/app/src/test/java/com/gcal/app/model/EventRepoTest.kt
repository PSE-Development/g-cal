package com.gcal.app.model

import android.util.Log
import com.gcal.app.model.localData.EventDao
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.localData.relation.AppointmentRelation
import com.gcal.app.model.localData.relation.SharedEventRelation
import com.gcal.app.model.localData.relation.ToDoRelation
import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.NoGroup
import com.gcal.app.model.modelData.data.system.NoXp
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserGroup
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.ToDo
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepoTest {
    private val apiMock = mockk<ClientAPI>()
    private val eventDao = mockk<EventDao>()

    private val today = LocalDateTime.now().toLocalDate()

    private val dbMock = mockk<LocalData> {
        every { eventDao() } returns eventDao
    }

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
    fun getAppointmentsInTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val appointment = UserAppointment(
            0, "test", "test", today.atTime(10, 0), today.atTime(12, 0), NoGroup, XP.from(10),
            EventCD(0, false, null)
        )
        val appointmentRelation = AppointmentRelation(
            appointment.asDatabaseEntity(),
            appointment.checkCompletion().asDatabaseEntity(),
            appointment.group().asDatabaseEntity()
        )

        every { eventDao.observeAppointments(any(), any()) } returns MutableStateFlow(
            listOf(
                appointmentRelation
            )
        )
        coEvery { apiMock.getUserAppointments() } returns Response.Success(listOf(appointment))

        val results = mutableListOf<List<Appointment>>()
        val job = launch(testDispatcher) {
            repository.getAppointmentsIn(LocalDateTime.now(), LocalDateTime.now().plusDays(1))
                .collect {
                    results.add(it)
                }
        }

        advanceUntilIdle()

        assert(results.isNotEmpty())
        val firstResult = results.first()
        assertEquals(1, firstResult.size)
        assertEquals(appointment, firstResult.first())
        coVerify { apiMock.getUserAppointments() }

        job.cancel()
    }

    @Test
    fun getToDoInTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val todo = TestData.todo()
        val todoRelation = todo.map {
            ToDoRelation(
                it.asDatabaseEntity(),
                it.checkCompletion().asDatabaseEntity()
            )
        }

        every { eventDao.observeToDos(any(), any()) } returns MutableStateFlow(todoRelation)
        coEvery { apiMock.getUserToDo() } returns Response.Success(todo)

        val results = mutableListOf<List<ToDo>>()
        val job = launch(testDispatcher) {
            repository.getToDoIn(LocalDateTime.now(), LocalDateTime.now().plusDays(2))
                .collect {
                    results.add(it)
                }
        }

        advanceUntilIdle()

        assert(results.isNotEmpty())
        val firstResult = results.first()
        assertEquals(2, firstResult.size)
        assertEquals(todo, firstResult)
        coVerify { apiMock.getUserToDo() }

        job.cancel()
    }

    @Test
    fun getSharedEventInTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val sharedEvents = TestData.sharedEvents()
        val sharedEventRelations = sharedEvents.map {
            SharedEventRelation(
                it.asDatabaseEntity(),
                it.checkCompletion().asDatabaseEntity(),
                it.group().asDatabaseEntity()
            )
        }

        every { eventDao.observeSharedEvents(any(), any()) } returns MutableStateFlow(sharedEventRelations)
        coEvery {eventDao.upsertSharedEvents(any())} just Runs
        coEvery {eventDao.updateGroups(any())} just Runs
        coEvery {eventDao.updateCompletionDetails(any())} just Runs
        coEvery { apiMock.getUserSharedEvents() } returns Response.Success(sharedEvents)

        val results = mutableListOf<List<SharedEvent>>()
        val job = launch(testDispatcher) {
            repository.getSharedEventsIn(LocalDateTime.now(), LocalDateTime.now().plusDays(2))
                .collect {
                    results.add(it)
                }
        }

        advanceUntilIdle()

        assert(results.isNotEmpty())
        val firstResult = results.first()
        assertEquals(2, firstResult.size)
        assertEquals(sharedEvents, firstResult)
        coVerify { apiMock.getUserSharedEvents() }

        job.cancel()
    }

    @Test
    fun getGroupsTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val groups = TestData.groups
        val groupEntities = groups.map { it.asDatabaseEntity() }

        every { eventDao.observeGroups() } returns MutableStateFlow(groupEntities)
        coEvery { apiMock.getUserGroups() } returns Response.Success(groups)

        val results = mutableListOf<List<Group>>()
        val job = launch(testDispatcher) {
            repository.getGroups()
                .collect {
                    results.add(it)
                }
        }

        advanceUntilIdle()

        assert(results.isNotEmpty())
        val firstResult = results.first()
        assertEquals(2, firstResult.size)
        assertEquals(TestData.groups, firstResult)
        coVerify { apiMock.getUserGroups() }

        job.cancel()
    }

    // Create Tests
    @Test
    fun createAppointmentFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val appointment = TestData.newAppointment
        val generatedId = 89235468346546L

        coEvery { dbMock.eventDao().insertNewAppointment(any()) } returns generatedId
        coEvery { apiMock.addUserAppointments(any()) } returns Response.Error(Exception("Network Down"))
        coEvery { dbMock.eventDao().deleteAppointment(any()) } just Runs

        val result = repository.createAppointment(appointment)

        assert(result is Response.Error)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1) { dao.insertNewAppointment(any()) }
        coVerify(exactly = 1) { apiMock.addUserAppointments(any()) }
        coVerify(exactly = 1) { dao.deleteAppointment(any()) }
    }

    @Test
    fun createAppointmentSuccess() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val appointment = TestData.newAppointment
        val generatedId = 89235468346546L
        val expectedAppointment = UserAppointment(
            generatedId,
            appointment.eventName(),
            appointment.description(),
            appointment.start(),
            appointment.end(),
            appointment.group(),
            NoXp,
            EventCD(generatedId, false, null)
        )

        coEvery { dbMock.eventDao().insertNewAppointment(any()) } returns generatedId
        coEvery { apiMock.addUserAppointments(any()) } returns Response.Success(Unit, 201)

        val result = repository.createAppointment(appointment)

        assert(result is Response.Success)
        val dao = dbMock.eventDao()
        coVerify { dao.insertNewAppointment(any()) }
        coVerify { apiMock.addUserAppointments(expectedAppointment) }
        coVerify(exactly = 0) { dao.deleteAppointment(any()) }
    }

    @Test
    fun createToDoFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val todo = TestData.newToDo
        val generatedId = 83456283695L

        coEvery { dbMock.eventDao().insertNewToDo(any()) } returns generatedId
        coEvery { apiMock.addUserToDo(any()) } returns Response.Error(Exception("Network Down"))
        coEvery { dbMock.eventDao().deleteToDo(any()) } just Runs

        val result = repository.createTodo(todo)

        assert(result is Response.Error)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1) { dao.insertNewToDo(any()) }
        coVerify(exactly = 1) { apiMock.addUserToDo(any()) }
        coVerify(exactly = 1) { dao.deleteToDo(any()) }
    }

    @Test
    fun createToDoSuccess() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val todo = TestData.newToDo
        val generatedId = 83456283695L
        val expectedToDo = UserToDo(
            generatedId,
            todo.eventName(),
            todo.description(),
            todo.end(),
            NoXp,
            EventCD(generatedId, false, null)
        )

        coEvery { dbMock.eventDao().insertNewToDo(any()) } returns generatedId
        coEvery { apiMock.addUserToDo(any()) } returns Response.Success(Unit, 201)

        val result = repository.createTodo(todo)

        assert(result is Response.Success)
        val dao = dbMock.eventDao()
        coVerify { dao.insertNewToDo(any()) }
        coVerify { apiMock.addUserToDo(expectedToDo) }
        coVerify(exactly = 0) { dao.deleteToDo(any()) }
    }

    @Test
    fun createGroupFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val group = TestData.newGroup
        val generatedId = 324726824L

        coEvery { dbMock.eventDao().insertGroup(any()) } returns generatedId
        coEvery { apiMock.addUserGroup(any()) } returns Response.Error(Exception("Network Down"))
        coEvery { dbMock.eventDao().deleteGroup(any()) } just Runs

        val result = repository.createGroup(group)

        assert(result is Response.Error)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1) { dao.insertGroup(any()) }
        coVerify(exactly = 1) { apiMock.addUserGroup(any()) }
        coVerify(exactly = 1) { dao.deleteGroup(any()) }
    }

    @Test
    fun createGroupSuccess() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val group = TestData.newGroup
        val generatedId = 324726824L
        val expectedUserGroup = UserGroup(generatedId, group.groupName(), group.groupColour())

        coEvery { dbMock.eventDao().insertGroup(any()) } returns generatedId
        coEvery { apiMock.addUserGroup(expectedUserGroup) } returns Response.Success(Unit, 201)

        val result = repository.createGroup(group)

        assert(result is Response.Success)
        val dao = dbMock.eventDao()
        coVerify { dbMock.eventDao().insertGroup(any()) }
        coVerify { apiMock.addUserGroup(expectedUserGroup) }
        coVerify(exactly = 0) { dao.deleteGroup(any()) }
    }

    @Test
    fun createSharedEventTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val sharedEvent = TestData.newSharedEvent()

        coEvery { apiMock.addSharedEvent(any(), any()) } returns Response.Success(Unit, 201)

        val response =
            repository.createSharedEvent(sharedEvent.eventDetails, sharedEvent.participants)

        assert(response is Response.Success)
    }

    // Update Tests

    @Test
    fun updateAppointmentTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val appointment = TestData.appointments().first()

        coEvery { apiMock.updateUserAppointment(any()) } returns Response.Success(Unit, 200)
        coEvery { dbMock.eventDao().updateAppointment(any()) } just Runs
        coEvery { dbMock.eventDao().updateCompletionDetail(any()) } just Runs

        val response = repository.updateAppointment(appointment)

        assert(response is Response.Success)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1) { apiMock.updateUserAppointment(any()) }
        coVerify(exactly = 1) { dao.updateAppointment(any()) }
        coVerify(exactly = 1) { dao.updateCompletionDetail(any()) }
    }

    @Test
    fun updateToDoTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val todo = TestData.todo().first()

        coEvery { apiMock.updateUserToDo(any()) } returns Response.Success(Unit, 200)
        coEvery { dbMock.eventDao().updateToDo(any()) } just Runs
        coEvery { dbMock.eventDao().updateCompletionDetail(any()) } just Runs

        val response = repository.updateToDo(todo)

        assert(response is Response.Success)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1) { apiMock.updateUserToDo(any()) }
        coVerify(exactly = 1) { dao.updateToDo(any()) }
        coVerify(exactly = 1) { dao.updateCompletionDetail(any()) }
    }

    @Test
    fun updateSharedEventTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val sharedEvent = TestData.sharedEvents().first()

        coEvery { apiMock.updateSharedEvent(any()) } returns Response.Success(Unit, 200)
        coEvery { dbMock.eventDao().updateSharedEvent(any()) } just Runs
        coEvery { dbMock.eventDao().updateCompletionDetail(any()) } just Runs

        val response = repository.updateSharedEvent(sharedEvent)

        assert(response is Response.Success)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1) { apiMock.updateSharedEvent(any()) }
        coVerify(exactly = 1) { dao.updateSharedEvent(any()) }
        coVerify(exactly = 1) { dao.updateCompletionDetail(any()) }
    }

    @Test
    fun updateGroupTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val appointment = TestData.groups.first()

        coEvery { apiMock.updateGroup(any()) } returns Response.Success(Unit, 200)
        coEvery { dbMock.eventDao().updateGroup(any()) } just Runs

        val response = repository.updateGroup(
            UserGroup(
                appointment.groupId(),
                appointment.groupName(),
                appointment.groupColour()
            )
        )
        assert(response is Response.Success)
        val dao = dbMock.eventDao()
        coVerify(exactly = 1)
        { apiMock.updateGroup(any()) }
        coVerify(exactly = 1)
        { dao.updateGroup(any()) }
    }

    // Delete Tests

    @Test
    fun deleteAppointmentTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val appointment = TestData.appointments().first()

        coEvery { apiMock.deleteUserAppointment(any()) } returns Response.Success(Unit, 200)
        coEvery { dbMock.eventDao().deleteAppointment(any()) } just Runs

        val result = repository.deleteAppointment(appointment)

        assert(result is Response.Success)
        coVerify { apiMock.deleteUserAppointment(appointment) }
        coVerify { dbMock.eventDao().deleteAppointment(appointment.asDatabaseEntity()) }
    }

    @Test
    fun deleteToDoTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = EventRepo(apiMock, dbMock, this, testDispatcher)

        val todo = TestData.todo().first()

        coEvery { apiMock.deleteUserToDo(any()) } returns Response.Success(Unit, 200)
        coEvery { dbMock.eventDao().deleteToDo(any()) } just Runs

        val result = repository.deleteToDo(todo)

        assert(result is Response.Success)
        coVerify { apiMock.deleteUserToDo(todo) }
        coVerify { dbMock.eventDao().deleteToDo(todo.asDatabaseEntity()) }
    }
}
