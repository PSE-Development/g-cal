import io.mockk.every
import io.mockk.mockk
import main.kotlin.data.Account
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import main.kotlin.data.Result
import main.kotlin.data.events.Event
import main.kotlin.database.Events
import main.kotlin.event.EventManager
import main.kotlin.event.EventType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import java.time.LocalDateTime

class EventManagerTest : InitializeTests() {

    // creates an appointment, asserting that the user has to exists and the appointment cannot be added twice
    @Test
    fun createAppointmentTest() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))

        val appointment = generateAppointment()
        val res1 = eventManager.createAppointment(user.username, appointment)
        Assertions.assertTrue(res1 is Result.Failure)

        userAuthenticator.addUser(user)
        val res2 = eventManager.createAppointment(user.username, appointment)
        Assertions.assertTrue(res2 is Result.Success)
        val res3 = eventManager.createAppointment(user.username, appointment)
        Assertions.assertTrue(res3 is Result.Failure)

        val res4 = eventManager.getAppointments(user.username)
        val eventList = res4.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(eventList.any { it.eventID == appointment.eventID } )
    }

    // creates a to-Do, asserting that the user has to exists and the to-Do cannot be added twice
    @Test
    fun createToDoTest() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))

        val toDo = generateToDo()
        val res1 = eventManager.createToDo(user.username, toDo)
        Assertions.assertTrue(res1 is Result.Failure)

        userAuthenticator.addUser(user)
        val res2 = eventManager.createToDo(user.username, toDo)
        Assertions.assertTrue(res2 is Result.Success)
        val res3 = eventManager.createToDo(user.username, toDo)
        Assertions.assertTrue(res3 is Result.Failure)

        val res4 = eventManager.getToDos(user.username)
        val eventList = res4.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(eventList.any { it.eventID == toDo.eventID })
    }

    // creates a shared event, asserting that the user has to exists and the shared event cannot be added twice
    @Test
    fun createSharedEventTets() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))

        val sharedEvent = generateSharedEvent()
        val res1 = eventManager.createSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res1 is Result.Failure)

        userAuthenticator.addUser(user)
        val res2 = eventManager.createSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res2 is Result.Success)
        val res3 = eventManager.createSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res3 is Result.Failure)

        val res4 = eventManager.getSharedEvents(user.username)
        val eventList = res4.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(eventList.any { it.eventID == sharedEvent.eventID })
    }

    // test an appointment creation where the user has no access to the group -> error
    @Test
    fun createAppointmentNoGroupAccess() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        val appointment = generateAppointment()
        appointment.group = Group(5, "test", 0)
        userAuthenticator.addUser(user)

        val res = eventManager.createAppointment(user.username, appointment)
        Assertions.assertTrue(res is Result.Failure)
    }

    // test a shared event creation where the user has no access to the group -> error
    @Test
    fun createSharedEventNoGroupAccess() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        val sharedEvent = generateSharedEvent()
        sharedEvent.group = Group(5, "test", 0)
        userAuthenticator.addUser(user)

        val res = eventManager.createSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun gettingEventsForInvalidUser() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        val res1 = eventManager.getAppointments(user.username)
        Assertions.assertTrue(res1 is Result.Failure)
        val res2 = eventManager.getAppointments(user.username)
        Assertions.assertTrue(res2 is Result.Failure)
        val res3 = eventManager.getAppointments(user.username)
        Assertions.assertTrue(res3 is Result.Failure)
    }

    // appointments and shared events have to save start attribute in the database, otherwise they will fail when retrieving them
    @Test
    fun testEventsWithStartAttribute() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val appointment = generateAppointment()
        database.insert(Events) {
            set(it.eventID, appointment.eventID)
            set(it.eventType, EventType.APPOINTMENT_TYPE.name)
            set(it.name, appointment.name)
            set(it.description, appointment.description)
            set(it.end, appointment.end)
            set(it.experiencePoints, appointment.experiencePoints.value)
            set(it.user, user.username)
            set(it.completed, appointment.completed.isCompleted)
            set(it.completionDate, appointment.completed.completionTime)
            set(it.groupID, appointment.group.groupID)
            set(it.start, null)
        }

        val sharedEvent = generateSharedEvent()
        database.insert(Events) {
            set(it.eventID, sharedEvent.eventID)
            set(it.eventType, EventType.SHARED_EVENT_TYPE.name)
            set(it.name, sharedEvent.name)
            set(it.description, sharedEvent.description)
            set(it.end, sharedEvent.end)
            set(it.experiencePoints, sharedEvent.experiencePoints.value)
            set(it.user, user.username)
            set(it.completed, sharedEvent.completed.isCompleted)
            set(it.completionDate, sharedEvent.completed.completionTime)
            set(it.groupID, sharedEvent.group.groupID)
            set(it.start, null)
        }

        val res1 = eventManager.getAppointments(user.username)
        Assertions.assertTrue(res1 is Result.Failure)
        val res2 = eventManager.getSharedEvents(user.username)
        Assertions.assertTrue(res2 is Result.Failure)
    }

    @Test
    fun testUpdateAppointment() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val appointment = generateAppointment()
        eventManager.createAppointment(user.username, appointment)

        val copyAppointment = appointment.copy(eventID = 6L)
        val res1 = eventManager.updateAppointment(user.username, copyAppointment)
        Assertions.assertTrue(res1 is Result.Failure)

        appointment.name = "changed name"
        appointment.group = Group(5L ,"test", 0)
        val res2 = eventManager.updateAppointment(user.username, appointment)
        Assertions.assertTrue(res2 is Result.Failure)

        appointment.group = Group(0, "None", 0)
        val res3 = eventManager.updateAppointment(user.username, appointment)
        Assertions.assertTrue(res3 is Result.Success)
    }

    @Test
    fun testUpdateToDo() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val toDo = generateToDo()
        eventManager.createToDo(user.username, toDo)

        val copyToDo = toDo.copy(eventID = 6L)
        val res1 = eventManager.updateToDo(user.username, copyToDo)
        Assertions.assertTrue(res1 is Result.Failure)

        val res2 = eventManager.updateToDo(user.username, toDo)
        Assertions.assertTrue(res2 is Result.Success)
    }

    @Test
    fun testUpdateSharedEvent() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val sharedEvent = generateSharedEvent()
        val oldName = sharedEvent.name
            eventManager.createSharedEvent(user.username, sharedEvent)

        val copySharedEvent = sharedEvent.copy(eventID = 6L)
        val res1 = eventManager.updateSharedEvent(user.username, copySharedEvent)
        Assertions.assertTrue(res1 is Result.Failure)

        sharedEvent.name = "changed name"
        sharedEvent.group = Group(5L ,"test", 0)
        val res2 = eventManager.updateSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res2 is Result.Failure)

        sharedEvent.group = Group(0, "None", 0)
        sharedEvent.name = oldName
        sharedEvent.completed = EventCompletionDetail(sharedEvent.eventID,true, LocalDateTime.now())
        val res3 = eventManager.updateSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res3 is Result.Success)
    }

    @Test
    fun testDeleteAppointment() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val appointment = generateAppointment()
        eventManager.createAppointment(user.username, appointment)

        val copyAppointment = appointment.copy(eventID = 6L)
        val res1 = eventManager.deleteAppointment(user.username, copyAppointment.eventID)
        Assertions.assertTrue(res1 is Result.Failure)

        val res3 = eventManager.deleteAppointment(user.username, appointment.eventID)
        Assertions.assertTrue(res3 is Result.Success)

        Assertions.assertTrue(eventManager.getAppointments(user.username).getOrElse { _, err -> Assertions.fail(err) }.isEmpty())
    }

    @Test
    fun testDeleteToDo() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val toDo = generateToDo()
        eventManager.createToDo(user.username, toDo)

        val copyToDo = toDo.copy(eventID = 6L)
        val res1 = eventManager.deleteToDo(user.username, copyToDo.eventID)
        Assertions.assertTrue(res1 is Result.Failure)

        val res3 = eventManager.deleteToDo(user.username, toDo.eventID)
        Assertions.assertTrue(res3 is Result.Success)

        Assertions.assertTrue(eventManager.getToDos(user.username).getOrElse { _, err -> Assertions.fail(err) }.isEmpty())
    }

    @Test
    fun testDeleteSharedEvent() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val sharedEvent = generateSharedEvent()
        eventManager.createSharedEvent(user.username, sharedEvent)

        val copySharedEvent = sharedEvent.copy(eventID = 6L)
        val res1 = eventManager.deleteSharedEvent(user.username, copySharedEvent.eventID)
        Assertions.assertTrue(res1 is Result.Failure)

        val res3 = eventManager.deleteSharedEvent(user.username, sharedEvent.eventID)
        Assertions.assertTrue(res3 is Result.Success)

        Assertions.assertTrue(eventManager.getSharedEvents(user.username).getOrElse { _, err -> Assertions.fail(err) }.isEmpty())
    }

    @Test
    fun testInvalidEventManagerDB() {
        val db = mockk<Database>()
        every { db.dialect } throws RuntimeException("DB error")
        every { db.executeQuery(any()) } throws RuntimeException("DB Error")
        val manager = EventManager(db)

        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))

        val appointment = generateAppointment()
        val toDo = generateToDo()
        val sharedEvent = generateSharedEvent()

        val res4 = manager.getAppointments(user.username)
        Assertions.assertTrue(res4 is Result.Failure)
        val res5 = manager.getToDos(user.username)
        Assertions.assertTrue(res5 is Result.Failure)
        val res6 = manager.getSharedEvents(user.username)
        Assertions.assertTrue(res6 is Result.Failure)

        val res7 = manager.createAppointment(user.username, appointment)
        Assertions.assertTrue(res7 is Result.Failure)
        val res8 = manager.createToDo(user.username, toDo)
        Assertions.assertTrue(res8 is Result.Failure)
        val res9 = manager.createSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res9 is Result.Failure)

        val res10 = manager.updateAppointment(user.username, appointment)
        Assertions.assertTrue(res10 is Result.Failure)
        val res11 = manager.updateToDo(user.username, toDo)
        Assertions.assertTrue(res11 is Result.Failure)
        val res12 = manager.updateSharedEvent(user.username, sharedEvent)
        Assertions.assertTrue(res12 is Result.Failure)

        val res13 = manager.deleteAppointment(user.username, appointment.eventID)
        Assertions.assertTrue(res13 is Result.Failure)
        val res14 = manager.deleteToDo(user.username, toDo.eventID)
        Assertions.assertTrue(res14 is Result.Failure)
        val res15 = manager.deleteSharedEvent(user.username, sharedEvent.eventID)
        Assertions.assertTrue(res15 is Result.Failure)
    }


    @Test
    fun testUniqueEventID() {
        val user1 = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        val user2 = Account("USERNAME_2", "name", ExperiencePoints(0), ExperiencePoints(0))
        val user3 = Account("username3", "name", ExperiencePoints(0), ExperiencePoints(0))
        val user4 = Account("username4", "name", ExperiencePoints(0), ExperiencePoints(0))
        val userList = listOf(user1, user2, user3, user4)
        userList.forEach { user ->
            userAuthenticator.addUser(user)
            eventManager.createAppointment(user.username, generateAppointment())
            eventManager.createAppointment(user.username, generateAppointment())
            eventManager.createAppointment(user.username, generateAppointment())
            eventManager.createToDo(user.username, generateToDo())
            eventManager.createToDo(user.username, generateToDo())
            eventManager.createToDo(user.username, generateToDo())
            eventManager.createSharedEvent(user.username, generateSharedEvent())
        }

        val eventList = mutableListOf<Event>()
        for (user in userList) {
            eventList.addAll(eventManager.getAppointments(user.username).getOrElse { _, err -> Assertions.fail(err) })
            eventList.addAll(eventManager.getToDos(user.username).getOrElse { _, err -> Assertions.fail(err) })
            eventList.addAll(eventManager.getSharedEvents(user.username).getOrElse { _, err -> Assertions.fail(err) })
        }

        val uniqueEventID = eventManager.generateUniqueEventID()
        Assertions.assertFalse(eventList.map { it.eventID }.contains(uniqueEventID))
    }
}
