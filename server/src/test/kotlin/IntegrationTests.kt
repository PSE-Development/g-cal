import io.ktor.client.HttpClient
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking

import main.kotlin.controller.USER_ID_PARAMETER
import main.kotlin.controller.EVENT_ID_PARAMETER
import main.kotlin.controller.GROUP_ID_PARAMETER
import main.kotlin.controller.FRIEND_ID_PARAMETER
import main.kotlin.controller.ROUTE_DELETE
import main.kotlin.controller.ROUTE_LOGIN
import main.kotlin.controller.TOKEN_PARAMETER
import main.kotlin.controller.USER_NAME_PARAMETER
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.RegularAccount
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.LeaderboardEntry
import main.kotlin.data.Group
import main.kotlin.data.events.Appointment
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.SharedEventRequest
import main.kotlin.data.events.ToDo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

// The jwt tokens are generated with https://www.jwt.io/ for testing purposes

// Domain used for testing locally on your own device.
const val DOMAIN = "http://localhost:8080"

const val USERNAME_1 = "test.firebase.gcal.1@gmail.com"
const val USERNAME_2 = "test.firebase.gcal.2@gmail.com"
const val TOKEN_1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6InRlc3QiLCJhZG1pbiI6dHJ1ZSwiZW1haWwiOiJ0ZXN0LmZpcmViYXNlLmdjYWwuMUBnbWFpbC5jb20iLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6MTg3MjcyODA3Nn0.k3RLMTH9AWmYmQxdT-tLoaLRsYjFt8OsXQBEKLmHjd8"
const val TOKEN_2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6InRlc3QiLCJhZG1pbiI6dHJ1ZSwiZW1haWwiOiJ0ZXN0LmZpcmViYXNlLmdjYWwuMkBnbWFpbC5jb20iLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6MTg3MjcyODA3Nn0._QXCbU8KnwxJ5p1QPgBmaHNlXESu-RYF-eOn5dmDOoQ"

class IntegrationTests {

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    val user1 = Account(USERNAME_1, "test1", ExperiencePoints(0), ExperiencePoints(0))
    val user2 = Account(USERNAME_2, "test2", ExperiencePoints(0), ExperiencePoints(0))


    // logging in both users before each test
    @BeforeEach
    fun loginUsers() {
        runBlocking {
            val res1 = client.post("$DOMAIN$ROUTE_LOGIN") {
                url {
                    parameters.append(USER_NAME_PARAMETER, user1.name)
                    parameters.append(TOKEN_PARAMETER, TOKEN_1)
                }
            }
            Assertions.assertEquals(HttpStatusCode.OK, res1.status)

            val res2 = client.post("$DOMAIN$ROUTE_LOGIN") {
                url {
                    parameters.append(USER_NAME_PARAMETER, user2.name)
                    parameters.append(TOKEN_PARAMETER, TOKEN_2)
                }
            }
            Assertions.assertEquals(HttpStatusCode.OK, res2.status)
        }
    }

    // deleting both users after each test, effectively clearing the database
    @AfterEach
    fun clearDatabase() {
        runBlocking {
            val res1 = client.delete("$DOMAIN$ROUTE_DELETE") {
                url {
                    parameters.append(USER_ID_PARAMETER, user1.username)
                    parameters.append(TOKEN_PARAMETER, TOKEN_1)
                }
            }
            Assertions.assertEquals(HttpStatusCode.OK, res1.status)

            val res2 = client.delete("$DOMAIN$ROUTE_DELETE") {
                url {
                    parameters.append(USER_ID_PARAMETER, user2.username)
                    parameters.append(TOKEN_PARAMETER, TOKEN_2)
                }
            }
            Assertions.assertEquals(HttpStatusCode.OK, res2.status)
        }
    }

    @Test
    fun testUserInsertionAndQueries() = runBlocking {
        // logs in user1
        val response1 = client.post("$DOMAIN/login") {
            url {
                parameters.append(USER_NAME_PARAMETER, user1.name)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }

        assert(response1.status == HttpStatusCode.OK)
        println("Inserted user $user1")

        // retrieves the same user again to verify that he has been added to the database
        val response2: HttpResponse = client.get("$DOMAIN/user") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        val queriedUser: Account = response2.body()
        println("Retrieved user $queriedUser")

        assert(queriedUser.username == user1.username)
    }

    @Test
    fun testUpdateName() = runBlocking {
        // update the name of user1
        val newName = "test1_changed"
        user1.name = newName
        val response1 = client.put("$DOMAIN/user") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(user1)
        }

        println(response1.bodyAsText())
        assert(response1.status == HttpStatusCode.OK)
        println("Updated user information for $user1")

        // assert that the name was indeed changed
        val response2: HttpResponse = client.get("$DOMAIN/user") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        val queriedUser: Account = response2.body()
        assert(queriedUser.name == newName)
        println("Retrieved updated user $queriedUser")
    }

    @Test
    fun testLeaderboard() = runBlocking {
        // retrieve the leaderboard list and assert that the list is sorted in descending order by experience points
        val response = client.get("$DOMAIN/leaderboard")

        assert(response.status == HttpStatusCode.OK)
        val list: List<LeaderboardEntry> = response.body()
        val sortedList: List<LeaderboardEntry> = list.sortedByDescending { it.experiencePoints.value }

        assert(list == sortedList)
        println("Retrieved following leaderboard:")
        list.forEach { println(it) }
    }

    @Test
    fun testFriendList() = runBlocking {
        // logs in user2
        val response1 = client.post("$DOMAIN/login") {
            url {
                parameters.append(USER_NAME_PARAMETER, user2.name)
                parameters.append(TOKEN_PARAMETER, TOKEN_2)
            }
        }
        assert(response1.status == HttpStatusCode.OK)

        // user1 adds user2 as a friend
        val response2 = client.post("$DOMAIN/user/friends") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(FRIEND_ID_PARAMETER, user2.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        val friend = response2.body<RegularAccount>()
        assert(friend.username == user2.username)

        // assert that the friend list of user1 contains user1
        val response3 = client.get("$DOMAIN/user/friends") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response3.status == HttpStatusCode.OK)

        val userList: List<RegularAccount> = response3.body()
        println("Retrieved friend request list $userList for $user1")
        assert(userList.any { it.username == user2.username })

        // user1 removes user2 as a friend
        val response4 = client.delete("$DOMAIN/user/friends") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(FRIEND_ID_PARAMETER, user2.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response4.status == HttpStatusCode.OK)
        println("$user1 removed $user2 as a friend")
        val response5 = client.get("$DOMAIN/user/friends") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response5.status == HttpStatusCode.OK)
        assert(response5.body<List<RegularAccount>>().isEmpty())
    }

    @Test
    fun testGroups() = runBlocking {
        // add a group for the user
        val groupID = (0..1000).random().toLong()
        val group = Group(groupID, "group", 1)
        val response1 = client.post("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        assert(response1.status == HttpStatusCode.OK)
        println("Added group $group for $user1")

        // returns the groups of the user and asserts that the list contains the added group
        val response2 = client.get("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        val groupList1 = response2.body<List<Group>>()
        assert(groupList1.any { it.groupID == groupID})
        assert(groupList1.size == 2)
        println("Retrieved following group list:")
        groupList1.forEach { println(it)}

        // updates the name of already existing group
        val updatedName = "updatedGroup"
        group.name = updatedName
        val response3 = client.put("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        assert(response3.status == HttpStatusCode.OK)
        println("Updated group with id $groupID to $group")

        // asserts that a group with the same id and new name exists
        val response4 = client.get("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response4.status == HttpStatusCode.OK)
        val groupList2 = response4.body<List<Group>>()
        assert(groupList2.any { it.groupID == groupID && it.name == updatedName})

        // delete the group from the database
        val response5 = client.delete("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(GROUP_ID_PARAMETER, groupID.toString())
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response5.status == HttpStatusCode.OK)
        println("Deleted group $group for $user1")

        // assert that the group was indeed deleted
        val response6 = client.get("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response6.status == HttpStatusCode.OK)
        val groupList3 = response6.body<List<Group>>()
        assert(groupList3.none { it.groupID == groupID })
        assert(groupList3.size == 1)
    }

    @Test
    fun testGroupAccess() = runBlocking {
        // adds a group for the user
        val groupID = (0..1000).random().toLong()
        val group = Group(groupID, "group", 1)
        val response1 = client.post("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        assert(response1.status == HttpStatusCode.OK)
        println("Added group $group for $user1")

        // user2 tries to delete group of user1
        val response2 = client.delete("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user2.username)
                parameters.append(GROUP_ID_PARAMETER, group.groupID.toString())
                parameters.append(TOKEN_PARAMETER, TOKEN_2)
            }
        }
        assert(response2.status == HttpStatusCode.Forbidden)

        // asserts that the group still exists for user1
        val response3 = client.get("$DOMAIN/user/groups") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response3.status == HttpStatusCode.OK)
        val groupList2 = response3.body<List<Group>>()
        println("Retrieved following group list:")
        groupList2.forEach { print(it) }
        assert(groupList2.any { it.groupID == groupID })
        assert(groupList2.size == 2)
    }

    @Test
    fun testAppointments() = runBlocking {
        // adds an appointment for the user
        val group = Group(0, "None", 0)
        val appointment = createTestAppointment(group)

        val response1 = client.post("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        println(response1.bodyAsText())
        assert(response1.status == HttpStatusCode.OK)
        println("Added event $appointment for $user1")

        // retrieves the list of appointments and asserts that the list contains the added appointment
        val response2 = client.get("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        val eventList1 = response2.body<List<Appointment>>()
        println("Retrieved following appointment list:")
        eventList1.forEach { println(it) }
        assert(eventList1.isNotEmpty())
        assert(eventList1.any { it.eventID == appointment.eventID })

        // updates the name of the appointment
        val updatedName = "School"
        appointment.name = updatedName
        val response3 = client.put("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        assert(response3.status == HttpStatusCode.OK)
        println("Updated event with ID ${appointment.eventID} to $appointment")

        // asserts that the list of appointments contains the updated appointment
        val response4 = client.get("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response4.status == HttpStatusCode.OK)
        val eventList2 = response4.body<List<Appointment>>()
        println("Retrieved following appointment list:")
        eventList2.forEach { println(it) }
        assert(eventList2.any { it.eventID == appointment.eventID && it.name == updatedName})

        // deletes the appointment of the user
        val response5 = client.delete("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, appointment.eventID.toString())
            }
        }
        assert(response5.status == HttpStatusCode.OK)

        // asserts that the appointment no longer exists in the appointment list
        val response6 = client.get("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response6.status == HttpStatusCode.OK)
        val eventList3 = response6.body<List<Appointment>>()
        println("Deleted $appointment for $user1")
        println("Retrieved following appointment list:")
        eventList3.forEach { println(it) }
        assert(eventList3.none { it.eventID == appointment.eventID })
        assert(eventList3.isEmpty())
    }

    @Test
    fun testDoDos() = runBlocking {
        // adds a to-Do for the user
        val toDo = createTestToDo()

        val response1 = client.post("$DOMAIN/user/toDos") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(toDo)
        }
        println(response1.bodyAsText())
        assert(response1.status == HttpStatusCode.OK)
        println("Added event $toDo for $user1")

        // retrieves the list of to-Dos and asserts that the list contains the added to-Do
        val response2 = client.get("$DOMAIN/user/toDos") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        val eventList1 = response2.body<List<ToDo>>()
        println("Retrieved following to-Do list:")
        eventList1.forEach { println(it) }
        assert(eventList1.isNotEmpty())
        assert(eventList1.any { it.eventID == toDo.eventID })

        // updates the name of the to-Do
        val updatedName = "School"
        toDo.name = updatedName
        val response3 = client.put("$DOMAIN/user/toDos") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(toDo)
        }
        assert(response3.status == HttpStatusCode.OK)
        println("Updated event with ID ${toDo.eventID} to $toDo")

        // asserts that the list of to-Dos contains the updated to-Do
        val response4 = client.get("$DOMAIN/user/toDos") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response4.status == HttpStatusCode.OK)
        val eventList2 = response4.body<List<ToDo>>()
        println("Retrieved following to-Do list:")
        eventList2.forEach { println(it) }
        assert(eventList2.any { it.eventID == toDo.eventID && it.name == updatedName})

        // deletes the to-Do of the user
        val response5 = client.delete("$DOMAIN/user/toDos") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, toDo.eventID.toString())
            }
        }
        assert(response5.status == HttpStatusCode.OK)

        // asserts that the to-Do no longer exists in the to-Do list
        val response6 = client.get("$DOMAIN/user/toDos") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response6.status == HttpStatusCode.OK)
        val eventList3 = response6.body<List<ToDo>>()
        println("Deleted $toDo for $user1")
        println("Retrieved following to-Do list:")
        eventList3.forEach { println(it) }
        assert(eventList3.none { it.eventID == toDo.eventID })
        assert(eventList3.isEmpty())
    }

    @Test
    fun testAppointmentAccess() = runBlocking {
        // adds an event for the user
        val group = Group(0, "None", 0)
        val appointment = createTestAppointment(group)

        val response1 = client.post("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        assert(response1.status == HttpStatusCode.OK)
        println("Added event $appointment for $user1")

        // user2 tries to delete event of user1
        val response2 = client.delete("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user2.username)
                parameters.append(EVENT_ID_PARAMETER, appointment.eventID.toString())
                parameters.append(TOKEN_PARAMETER, TOKEN_2)
            }
        }
        assert(response2.status == HttpStatusCode.Forbidden)

        // asserts that the appointment still exists for user1
        val response3 = client.get("$DOMAIN/user/appointments") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response3.status == HttpStatusCode.OK)
        val appointmentList = response3.body<List<Appointment>>()
        println("Retrieved following appointment list:")
        appointmentList.forEach { print(it) }
        assert(appointmentList.any { it.eventID == appointment.eventID })
        assert(appointmentList.size == 1)
    }

    @Test
    fun testSharedEvents() = runBlocking {
        // user list should not contain the acting user
        val userList = mutableListOf(RegularAccount(user2.username, user2.name, user2.experiencePoints))
        val group = Group(0, "None", 0)
        val sharedEvent = createTestSharedEvent(group)
        val sharedEventRequest = SharedEventRequest(sharedEvent, userList)

        // assert that sharing events with non friends is invalid
        val response1 = client.post("$DOMAIN/user/shared-events") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(sharedEventRequest)
        }
        assert(response1.status == HttpStatusCode.Forbidden)

        // add the friendship between both users
        val response2 = client.post("$DOMAIN/user/friends") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(FRIEND_ID_PARAMETER, user2.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(response2.status == HttpStatusCode.OK)
        assert(response2.body<RegularAccount>().username == user2.username)


        // user1 again shares the event with the other user
        val response4 = client.post("$DOMAIN/user/shared-events") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(sharedEventRequest)
        }
        assert(response4.status == HttpStatusCode.OK)

        // inserting user1 for easier testing
        userList.add(RegularAccount(user1.username, "", user1.experiencePoints))
        // assert that each user has the shared event
        var assignedEventID: Long
        val res1 = client.get("$DOMAIN/user/shared-events") {
            url {
                parameters.append(USER_ID_PARAMETER, user1.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        assert(res1.status == HttpStatusCode.OK)
        val sharedEventList1 = res1.body<List<SharedEvent>>()
        println("Retrieved shared event list for $user1")
        sharedEventList1.forEach { println(it) }
        assert(sharedEventList1.any { it.name == sharedEvent.name })

        val res2 = client.get("$DOMAIN/user/shared-events") {
            url {
                parameters.append(USER_ID_PARAMETER, user2.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_2)
            }
        }
        assert(res2.status == HttpStatusCode.OK)
        val sharedEventList2 = res2.body<List<SharedEvent>>()
        println("Retrieved shared event list for $user1")
        sharedEventList2.forEach { println(it) }
        assert(sharedEventList2.any { it.name == sharedEvent.name })
        assignedEventID = sharedEventList2.firstOrNull()!!.eventID

        // updates the shared event with a new name and completion detail
        val oldName = sharedEvent.name
        val updatedName = "Schule"
        val updatedEvent = sharedEvent.copy(
            eventID = assignedEventID,
            name = updatedName,
            completed = EventCompletionDetail(sharedEvent.eventID, true, LocalDateTime.now())
        )
        val response5 = client.put("$DOMAIN/user/shared-events") {
            url {
                parameters.append(USER_ID_PARAMETER, user2.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_2)
            }
            contentType(ContentType.Application.Json)
            setBody(updatedEvent)
        }
        assert(response5.status == HttpStatusCode.OK)

        // assert that only the completion detail has changed since other attributes are not changeable
        val response6 = client.get("$DOMAIN/user/shared-events") {
            url {
                parameters.append(USER_ID_PARAMETER, user2.username)
                parameters.append(TOKEN_PARAMETER, TOKEN_2)
            }
        }
        assert(response6.status == HttpStatusCode.OK)
        val sharedEventList = response6.body<List<SharedEvent>>()
        assert(sharedEventList.any {
            it.eventID == updatedEvent.eventID && it.name == oldName && it.completed.isCompleted
        })
    }

    /*
        Private methods for generating test appointments and shared events.
     */
    private fun createTestAppointment(group : Group) : Appointment {
        val eventID = (0..1000).random().toLong()
        val appointment = Appointment(eventID, "Meeting", "Besprechung der Aufgaben",
            LocalDateTime.of(2026, 2, 10, 14, 0),
            LocalDateTime.of(2026, 2, 10, 15, 30),
            group, ExperiencePoints(50), EventCompletionDetail(eventID, false, null))
        return appointment
    }

    private fun createTestToDo() : ToDo {
        val eventID = (0..1000).random().toLong()
        val toDo = ToDo(eventID, "homework", "Do your homework",
            LocalDateTime.of(2026, 2, 10, 15, 30),
            ExperiencePoints(150), EventCompletionDetail(eventID, false, null))
        return toDo
    }


    private fun createTestSharedEvent(group : Group) : SharedEvent {
        val eventID = (0..1000).random().toLong()
        val sharedEvent = SharedEvent(eventID, "Uni", "PSE",
            LocalDateTime.of(2026, 2, 11, 13, 0),
            LocalDateTime.of(2026, 2, 11, 16, 30),
            group, ExperiencePoints(200), EventCompletionDetail(eventID, false, null))
        return sharedEvent
    }
}
