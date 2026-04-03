package com.gcal.app.model

import android.util.Log
import com.gcal.app.model.modelData.data.dto.GroupDTO
import com.gcal.app.model.modelData.data.dto.PersonalUserDTO
import com.gcal.app.model.modelData.data.dto.RegularUserDTO
import com.gcal.app.model.modelData.data.dto.SharedEventRequestDTO
import com.gcal.app.model.modelData.data.dto.UserAppointmentDTO
import com.gcal.app.model.modelData.data.dto.UserSharedEventDTO
import com.gcal.app.model.modelData.data.dto.UserToDoDTO
import com.gcal.app.model.modelData.data.dto.toDomain
import com.gcal.app.model.modelData.data.system.toDTO
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClientAPITest {
    var domain = "http://193.196.39.173:8080"

    @Before
    fun mockToken() {
        mockkObject(ClientAPI.AuthState)
        mockkStatic(Log::class)
        mockkObject(LoginViewModel.Companion)

        every { Log.e(any(), any()) } answers {
            println("LOG E: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        every { Log.i(any(), any()) } answers {
            println("LOG I: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }

        coEvery { LoginViewModel.getFreshToken() } returns "fake-token"
        ClientAPI.setUserName(TestData.profile.username())
    }

    @After
    fun unmockToken() {
        unmockkObject(LoginViewModel.Companion)
        unmockkStatic(Log::class)
    }


    // Getter Tests
    @Test
    fun getActiveUserTest() = runTest {
        val user = TestData.profile
        val mockUserJson = Json.encodeToString<PersonalUserDTO>(user.toDTO())
        var capturedUrl: Url? = null
        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockUserJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)

        val result = clientAPI.getActiveUser()
        assertTrue(result is Response.Success)
        val resultUser = (result as Response.Success).data
        assertEquals(user, resultUser)
        val url = capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
        assertTrue(url.parameters.contains("userName"))
        assertEquals(user.username(), url.parameters["userName"])
    }

    @Test
    fun getFriendsTest() = runTest {
        val user = TestData.profile
        val friends = TestData.friends
        val mockFriendsJson = Json.encodeToString<List<RegularUserDTO>>(friends.map { it.toDTO() })
        var capturedUrl: Url? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockFriendsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val mockClient = testClient(mockEngine)

        val clientAPI = ClientAPI(domain, mockClient)

        val result = clientAPI.getUserFriends()

        assertTrue(result is Response.Success)
        val resultFriends = (result as Response.Success).data
        assertEquals(friends, resultFriends)
        val url = capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
        assertTrue(url.parameters.contains("userName"))
        assertEquals(user.username(), url.parameters["userName"])
    }

    @Test
    fun getLeaderboardTest() = runTest {
        val leaderboard = TestData.leaderboard
        val mockLeaderboardJson = Json.encodeToString(leaderboard.map { it.toDTO() })
        var capturedUrl: Url? = null
        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockLeaderboardJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val mockClient = testClient(mockEngine)

        val clientAPI = ClientAPI(domain, mockClient)

        val result = clientAPI.getLeaderboard()

        assertTrue(result is Response.Success)
        val resultLeaderboard = (result as Response.Success).data
        assertEquals(leaderboard, resultLeaderboard)
        capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
    }

    @Test
    fun getGroupsTest() = runTest {
        val user = TestData.profile
        val groups = TestData.groups
        val mockGroupsJson = Json.encodeToString(groups.map { it.toDTO() })
        var capturedUrl: Url? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockGroupsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val mockClient = testClient(mockEngine)

        val clientAPI = ClientAPI(domain, mockClient)

        val result = clientAPI.getUserGroups()
        (result as? Response.Error)?.e?.printStackTrace()
        assertTrue(result is Response.Success)
        val resultGroups = (result as Response.Success).data
        assertEquals(groups, resultGroups)
        val url = capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
        assertTrue(url.parameters.contains("userName"))
        assertEquals(user.username(), url.parameters["userName"])
    }

    @Test
    fun getUserAppointmentsTest() = runTest {
        val user = TestData.profile
        val appointments = TestData.appointments()
        val mockAppointmentsJson = Json.encodeToString(appointments.map { it.toDTO() })
        var capturedUrl: Url? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockAppointmentsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val mockClient = testClient(mockEngine)

        val clientAPI = ClientAPI(domain, mockClient)
        val result = clientAPI.getUserAppointments()
        (result as? Response.Error)?.e?.printStackTrace()
        assertTrue(result is Response.Success)
        val resultGroups = (result as Response.Success).data
        assertEquals(appointments, resultGroups)
        val url = capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
        assertTrue(url.parameters.contains("userName"))
        assertEquals(user.username(), url.parameters["userName"])
    }

    @Test
    fun getUserToDoTest() = runTest {
        val user = TestData.profile
        val todos = TestData.todo()
        val mockAppointmentsJson = Json.encodeToString(todos.map { it.toDTO() })
        var capturedUrl: Url? = null


        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockAppointmentsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val mockClient = testClient(mockEngine)

        val clientAPI = ClientAPI(domain, mockClient)
        val result = clientAPI.getUserToDo()
        (result as? Response.Error)?.e?.printStackTrace()
        assertTrue(result is Response.Success)
        val resultGroups = (result as Response.Success).data
        assertEquals(todos, resultGroups)
        val url = capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
        assertTrue(url.parameters.contains("userName"))
        assertEquals(user.username(), url.parameters["userName"])
    }

    @Test
    fun getUserSharedEventTest() = runTest {
        val user = TestData.profile
        val sharedEvents = TestData.sharedEvents()
        val mockAppointmentsJson = Json.encodeToString(sharedEvents.map { it.toDTO() })
        var capturedUrl: Url? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url
            respond(
                content = mockAppointmentsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val mockClient = testClient(mockEngine)

        val clientAPI = ClientAPI(domain, mockClient)
        val result = clientAPI.getUserSharedEvents()
        (result as? Response.Error)?.e?.printStackTrace()
        assertTrue(result is Response.Success)
        val resultGroups = (result as Response.Success).data
        assertEquals(sharedEvents, resultGroups)
        val url = capturedUrl ?: throw AssertionError("Request wurde nicht gesendet")
        assertTrue(url.parameters.contains("userName"))
        assertEquals(user.username(), url.parameters["userName"])
    }

    // Add Tests
    @Test
    fun addAppointmentTest() = runTest {
        val profile = TestData.profile
        val newInstance = TestData.newAppointment
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.addUserAppointments(newInstance)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentInstance = Json.decodeFromString<UserAppointmentDTO>(sentBody.text)
        assertEquals(newInstance, sentInstance.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun addToDOTest() = runTest {
        val profile = TestData.profile
        val newInstance = TestData.newToDo
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.addUserToDo(newInstance)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentInstance = Json.decodeFromString<UserToDoDTO>(sentBody.text)
        assertEquals(newInstance, sentInstance.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun addFriendTest() = runTest {
        val profile = TestData.profile
        val newInstance = TestData.newFriend
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.addUserFriend(newInstance.username())
        val sentUrl = mockEngine.requestHistory.last().url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
        assertTrue(sentUrl.parameters.contains("friendName"))
        assertEquals(newInstance.username(), sentUrl.parameters["friendName"])
    }

    @Test
    fun addGroupTest() = runTest {
        val profile = TestData.profile
        val newInstance = TestData.newGroup
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.addUserGroup(newInstance)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentInstance = Json.decodeFromString<GroupDTO>(sentBody.text)
        assertEquals(newInstance, sentInstance.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun addSharedEventTest() = runTest {
        val profile = TestData.profile
        val newInstance = TestData.newSharedEvent()
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.addSharedEvent(newInstance.eventDetails, newInstance.participants)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentInstance = Json.decodeFromString<SharedEventRequestDTO>(sentBody.text)
        assertEquals(newInstance.eventDetails, sentInstance.sharedEvent.toDomain())
        assertEquals(newInstance.participants, sentInstance.users.map { it.toDomain() })
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    //Update Tests
    @Test
    fun updateActiveUserTest() = runTest {
        val updateUser = TestData.updatedProfile
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.updateActiveUser(updateUser)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentUser = Json.decodeFromString<PersonalUserDTO>(sentBody.text)
        assertEquals(updateUser, sentUser.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(updateUser.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun updateGroupsTest() = runTest {
        val profile = TestData.updatedProfile
        val updatedGroup = TestData.updatedGroup
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.updateGroup(updatedGroup)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentUser = Json.decodeFromString<GroupDTO>(sentBody.text)
        assertEquals(updatedGroup, sentUser.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun updateAppointmentTest() = runTest {
        val profile = TestData.profile
        val updatedAppointment = TestData.updateAppointment(TestData.appointments().first())
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.updateUserAppointment(updatedAppointment)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentUser = Json.decodeFromString<UserAppointmentDTO>(sentBody.text)
        assertEquals(updatedAppointment, sentUser.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun updateToDoTest() = runTest {
        val profile = TestData.profile
        val updatedToDo = TestData.updateToDo(TestData.todo().first())
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.updateUserToDo(updatedToDo)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentTodo = Json.decodeFromString<UserToDoDTO>(sentBody.text)
        assertEquals(updatedToDo, sentTodo.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    @Test
    fun updateSharedEventTest() = runTest {
        val profile = TestData.profile
        val updatedAppointment = TestData.updateSharedEvent(TestData.sharedEvents().first())
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.updateSharedEvent(updatedAppointment)
        val sentRequest = mockEngine.requestHistory.last()
        val sentBody = sentRequest.body as io.ktor.http.content.TextContent
        val sentUser = Json.decodeFromString<UserSharedEventDTO>(sentBody.text)
        assertEquals(updatedAppointment, sentUser.toDomain())
        val sentUrl = sentRequest.url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
    }

    // Deletion Tests
    @Test
    fun deleteAppointmentTest() = runTest {
        val profile = TestData.profile
        val deletionInstance = TestData.appointments().first()
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.deleteUserAppointment(deletionInstance)
        val sentUrl = mockEngine.requestHistory.last().url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
        assertTrue(sentUrl.parameters.contains("eventID"))
        assertEquals(deletionInstance.eventID().toString(), sentUrl.parameters["eventID"])
    }

    @Test
    fun deleteToDoTest() = runTest {
        val profile = TestData.profile
        val deletionInstance = TestData.todo().first()
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.deleteUserToDo(deletionInstance)
        val sentUrl = mockEngine.requestHistory.last().url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
        assertTrue(sentUrl.parameters.contains("eventID"))
        assertEquals(deletionInstance.eventID().toString(), sentUrl.parameters["eventID"])
    }

    @Test
    fun deleteSharedEventTest() = runTest {
        val profile = TestData.profile
        val deletionInstance = TestData.sharedEvents().first()
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.deleteSharedEvent(deletionInstance)
        val sentUrl = mockEngine.requestHistory.last().url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
        assertTrue(sentUrl.parameters.contains("eventID"))
        assertEquals(deletionInstance.eventID().toString(), sentUrl.parameters["eventID"])
    }

    @Test
    fun deleteGroupTest() = runTest {
        val profile = TestData.profile
        val deletionInstance = TestData.groups.last()
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.deleteUserGroup(deletionInstance)
        val sentUrl = mockEngine.requestHistory.last().url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
        assertTrue(sentUrl.parameters.contains("groupID"))
        assertEquals(deletionInstance.groupId().toString(), sentUrl.parameters["groupID"])
    }

    @Test
    fun removeFriendTest() = runTest {
        val profile = TestData.profile
        val removeInstance = TestData.friends.last()
        val mockEngine = basicTestEngine()
        val mockClient = testClient(mockEngine)
        val clientAPI = ClientAPI(domain, mockClient)
        clientAPI.removeUserFriend(removeInstance.username())
        val sentUrl = mockEngine.requestHistory.last().url
        assertTrue(sentUrl.parameters.contains("userName"))
        assertEquals(profile.username(), sentUrl.parameters["userName"])
        assertTrue(sentUrl.parameters.contains("friendName"))
        assertEquals(removeInstance.username(), sentUrl.parameters["friendName"])
    }

    private fun testClient(testEngine: MockEngine) = HttpClient(testEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private fun basicTestEngine() = MockEngine {
        respond(
            content = "",
            status = HttpStatusCode.OK,
        )
    }
}
