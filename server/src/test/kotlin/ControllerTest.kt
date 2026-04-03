import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import main.kotlin.controller.EVENT_ID_PARAMETER
import main.kotlin.controller.FRIEND_ID_PARAMETER
import main.kotlin.controller.GROUP_ID_PARAMETER
import main.kotlin.controller.INVALID_PARAMETER
import main.kotlin.controller.ROUTE_APPOINTMENT
import main.kotlin.controller.ROUTE_FRIENDS
import main.kotlin.controller.ROUTE_GROUPS
import main.kotlin.controller.ROUTE_HEALTH
import main.kotlin.controller.ROUTE_SHARED_EVENT
import main.kotlin.controller.ROUTE_TODO
import main.kotlin.controller.ROUTE_USER
import main.kotlin.controller.RequestController
import main.kotlin.controller.RequestDispatcher
import main.kotlin.controller.TOKEN_PARAMETER
import main.kotlin.controller.USER_ID_PARAMETER
import main.kotlin.controller.USER_NAME_PARAMETER
import main.kotlin.controller.exceptions.ConnectionException
import main.kotlin.controller.main
import main.kotlin.controller.module
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import main.kotlin.data.LeaderboardEntry
import main.kotlin.data.RegularAccount
import main.kotlin.data.Result
import main.kotlin.data.config.Config
import main.kotlin.data.config.ConfigEntry
import main.kotlin.data.events.Appointment
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.SharedEventRequest
import main.kotlin.data.events.ToDo
import main.kotlin.database.DatabaseConnect
import main.kotlin.event.ChallengeManager
import main.kotlin.event.EventManager
import main.kotlin.event.GroupManager
import main.kotlin.user.UserManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import java.lang.reflect.Modifier
import java.net.ConnectException

const val TEST_URL = "0.0.0.0"
const val TEST_PORT = "8082"

@ExtendWith(SystemStubsExtension::class)
class ControllerTest : InitializeTests() {

    @SystemStub
    lateinit var variables : EnvironmentVariables
    lateinit var dispatcherMock : RequestDispatcher

    val ownClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    @BeforeEach
    fun setUp() {
        dispatcherMock = mockk<RequestDispatcher>()
    }

    // custom block to prevent redundant operations for each test
    fun testBlock (
        dispatcher: RequestDispatcher,
        testBlock: suspend (HttpClient) -> Unit
    ) = testApplication {
        application {
            module(dispatcher, Dispatchers.Default)
        }

        val testClient = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        testBlock(testClient)
    }

    // test a valid controller creation by mocking it, then starting the controller and checking the health
    // of the running sever
    @Test
    fun createValidController() = runBlocking {
        initValidEnvVariables()
        val res = RequestController.create()
        val requestController = res.getOrElse { _, err -> Assertions.fail(err) }
        requestController.start(false)

        var response = ownClient.get("http://$TEST_URL:$TEST_PORT$ROUTE_HEALTH")
        Assertions.assertEquals(HttpStatusCode.OK, response.status)
        response = ownClient.head("http://$TEST_URL:$TEST_PORT$ROUTE_HEALTH")
        Assertions.assertEquals(HttpStatusCode.OK, response.status)

        requestController.stop()
    }

    // creating an invalid controller, resulting in no running server
    @Test
    fun createInvalidController() {
        initInvalidEnvVariables()
        val res = RequestController.create()
        val requestController = res.getOrElse { _, err -> Assertions.fail(err) }

        Assertions.assertThrows(ConnectionException::class.java) {
            requestController.start(false)
        }
    }

    @Test
    fun testInvalidMain() {
        initInvalidEnvVariables()
        main()
        Assertions.assertThrows(ConnectException::class.java) {
            runBlocking {
                ownClient.get("http://$TEST_URL:$TEST_PORT$ROUTE_HEALTH")
            }
        }
    }

    // asserts that the constructor is private and creates an instance
    @Test
    fun assertSingletonBehaviour() {
        val constructor = RequestController::class.java.getDeclaredConstructor(
            Config::class.java,
            RequestDispatcher::class.java
        )
        Assertions.assertTrue(Modifier.isPrivate(constructor.modifiers))

        initValidEnvVariables()
        val config = Config.create().getOrElse { _, err -> Assertions.fail(err) }
        val db = DatabaseConnect.connectDatabase(config).getOrElse { _, err -> Assertions.fail(err) }

        constructor.isAccessible = true
        val instance = constructor.newInstance(config, RequestDispatcher(
            UserManager(db),
            EventManager(db),
            GroupManager(db),
            ChallengeManager(db)
            ))
        Assertions.assertNotNull(instance)
        Assertions.assertTrue(instance is RequestController)
    }

    @Test
    fun invalidTokenLogin() = testBlock(dispatcherMock) { client ->
        val response = client.post("/login") {
            url {
                parameters.append(USER_NAME_PARAMETER, "name")
            }
        }

        Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
        Assertions.assertEquals(INVALID_PARAMETER, response.bodyAsText())
    }

    @Test
    fun invalidNameLogin() = testBlock(dispatcherMock) { client ->
        val response = client.post("/login") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }

        Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
        Assertions.assertEquals(INVALID_PARAMETER, response.bodyAsText())
    }

    @Test
    fun invalidUserRetrieval() = testBlock(dispatcherMock) { client ->
        every { dispatcherMock.loginUser(TOKEN_1, USERNAME_1) } returns
                Result.Failure(HttpStatusCode.BadRequest, "error")

        val response = client.post("/login") {
            url {
                parameters.append(USER_NAME_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }

        Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun validLogin() = testBlock(dispatcherMock) { client ->
        every { dispatcherMock.loginUser(TOKEN_1, USERNAME_1) } returns
                Result.Success(Account(USERNAME_1, "name",
            ExperiencePoints(0), ExperiencePoints(0)))

        val response = client.post("/login") {
            url {
                parameters.append(USER_NAME_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }

        Assertions.assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testValidLeaderboard() = testBlock(dispatcherMock) { client ->
        every { dispatcherMock.getLeaderboard() } returns Result.Success(listOf(
            LeaderboardEntry(1, "USERNAME_1", "name", ExperiencePoints(0))))

        val response = client.get("/leaderboard")

        Assertions.assertEquals(HttpStatusCode.OK, response.status)
        Assertions.assertFalse(Json.decodeFromString<List<LeaderboardEntry>>(response.bodyAsText()).isEmpty())
        Assertions.assertEquals("USERNAME_1",
            Json.decodeFromString<List<LeaderboardEntry>>(response.bodyAsText()).first().username)
    }

    @Test
    fun testInvalidLeaderboard() = testBlock(dispatcherMock) { client ->
        every { dispatcherMock.getLeaderboard() } returns
                Result.Failure(HttpStatusCode.BadRequest, "error")

        val response = client.get("/leaderboard")

        Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testUserInformation() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every { dispatcherMock.getUserInfo(USERNAME_1) } returns Result.Success(account)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val response = client.get("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        println(response.bodyAsText())
        Assertions.assertEquals(HttpStatusCode.OK, response.status)
        Assertions.assertEquals(USERNAME_1,
            Json.decodeFromString<Account>(response.bodyAsText()).username)
    }

    @Test
    fun invalidUserInformation() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every { dispatcherMock.getUserInfo(USERNAME_1) } returns
                Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.get("/user") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.get("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.get("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validUserUpdate() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "changedName", ExperiencePoints(0),
            ExperiencePoints(0))
        every { dispatcherMock.setUserInfo(USERNAME_1, account) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.put("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(account)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidUserUpdate() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every { dispatcherMock.setUserInfo(USERNAME_1, account) } returns
                Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.put("/user") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.put("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.put("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.put("/user") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(account)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validDeleteUser() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every { dispatcherMock.deleteUserInfo(USERNAME_1) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.delete("/delete") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidDeleteUser() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every { dispatcherMock.deleteUserInfo(USERNAME_1) } returns
                Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.delete("/delete") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.delete("/delete") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.delete("/delete") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validAppointmentCreation() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val appointment = generateAppointment()
        every { dispatcherMock.createAppointment(USERNAME_1, appointment) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.post("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidAppointmentCreation() = testBlock(dispatcherMock) { client ->
        val appointment = generateAppointment()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.createAppointment(
                USERNAME_1,
                appointment
            )
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.post("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.post("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.post("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.post("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validToDoCreation() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val toDo = generateToDo()
        every { dispatcherMock.createToDo(USERNAME_1, toDo) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.post("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(toDo)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidToDoCreation() = testBlock(dispatcherMock) { client ->
        val toDo = generateToDo()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.createToDo(USERNAME_1, toDo)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.post("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.post("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.post("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.post("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(toDo)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validSharedEventCreation() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val sharedEvent = generateSharedEvent()
        val user2 = RegularAccount("USERNAME_2", "name", ExperiencePoints(0))
        val userList = listOf(user2)
        every { dispatcherMock.createSharedEventAndShare(USERNAME_1, sharedEvent, userList) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.post("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(SharedEventRequest(sharedEvent, userList))
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidSharedEventCreation() = testBlock(dispatcherMock) { client ->
        val sharedEvent = generateSharedEvent()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val user2 = RegularAccount("USERNAME_2", "name", ExperiencePoints(0))
        val userList = listOf(user2)
        every {
            dispatcherMock.createSharedEventAndShare(
                USERNAME_1,
                sharedEvent,
                userList
            )
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.post("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.post("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.post("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.post("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(SharedEventRequest(sharedEvent, userList))
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validGroupCreation() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val group = generateGroup()
        every { dispatcherMock.createGroup(USERNAME_1, group) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.post("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidGroupCreation() = testBlock(dispatcherMock) { client ->
        val group = generateGroup()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.createGroup(USERNAME_1, group)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.post("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.post("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.post("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.post("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validAppointmentUpdate() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val appointment = generateAppointment()
        every { dispatcherMock.updateAppointment(USERNAME_1, appointment) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.put("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidAppointmentUpdate() = testBlock(dispatcherMock) { client ->
        val appointment = generateAppointment()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.updateAppointment(
                USERNAME_1,
                appointment
            )
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.put("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.put("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.put("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.put("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(appointment)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validToDoUpdate() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val toDo = generateToDo()
        every { dispatcherMock.updateToDo(USERNAME_1, toDo) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.put("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(toDo)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidToDoUpdate() = testBlock(dispatcherMock) { client ->
        val toDo = generateToDo()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.updateToDo(USERNAME_1, toDo)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.put("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.put("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.put("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.put("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(toDo)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validSharedEventUpdate() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val sharedEvent = generateSharedEvent()
        every { dispatcherMock.updateSharedEvent(USERNAME_1, sharedEvent) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.put("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(sharedEvent)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidSharedEventUpdate() = testBlock(dispatcherMock) { client ->
        val sharedEvent = generateSharedEvent()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.updateSharedEvent(USERNAME_1, sharedEvent)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.put("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.put("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.put("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.put("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(sharedEvent)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validGroupUpdate() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val group = generateGroup()
        every { dispatcherMock.updateGroup(USERNAME_1, group) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.put("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidGroupUpdate() = testBlock(dispatcherMock) { client ->
        val group = generateGroup()
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.updateGroup(USERNAME_1, group)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.put("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.put("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.put("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(RegularAccount(USERNAME_1, "name", ExperiencePoints(0)))
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.put("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
            contentType(ContentType.Application.Json)
            setBody(group)
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validAppointmentDeletion() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val eventID = 5L
        every { dispatcherMock.deleteAppointment(USERNAME_1, eventID) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.delete("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidAppointmentDeletion() = testBlock(dispatcherMock) { client ->
        val eventID = 5L
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.deleteAppointment(USERNAME_1, eventID)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.delete("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.delete("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.delete("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validToDoDeletion() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val eventID = 5L
        every { dispatcherMock.deleteToDo(USERNAME_1, eventID) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.delete("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidToDoDeletion() = testBlock(dispatcherMock) { client ->
        val eventID = 5L
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        every {
            dispatcherMock.deleteToDo(USERNAME_1, eventID)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.delete("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.delete("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.delete("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validSharedEventDeletion() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0),
            ExperiencePoints(0))
        val eventID = 5L
        every { dispatcherMock.deleteSharedEvent(USERNAME_1, eventID) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.delete("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidSharedEventDeletion() = testBlock(dispatcherMock) { client ->
        val eventID = 5L
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.deleteSharedEvent(USERNAME_1, eventID)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.delete("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.delete("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.delete("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(EVENT_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validGroupDeletion() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        val eventID = 5L
        every { dispatcherMock.deleteGroup(USERNAME_1, eventID) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.delete("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(GROUP_ID_PARAMETER, eventID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidGroupDeletion() = testBlock(dispatcherMock) { client ->
        val groupID = 5L
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.deleteGroup(USERNAME_1, groupID)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.delete("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.delete("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.delete("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
                parameters.append(GROUP_ID_PARAMETER, groupID.toString())
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validAppointmentRetrieval() = testBlock(dispatcherMock) { client ->
        val serverList = listOf(generateAppointment(), generateAppointment())
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every { dispatcherMock.getAppointments(USERNAME_1) } returns Result.Success(serverList)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.get("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
        val appointmentList = Json.decodeFromString<List<Appointment>>(res.bodyAsText())
        Assertions.assertEquals(serverList.first().eventID, appointmentList.first().eventID)
        Assertions.assertEquals(serverList.last().eventID, appointmentList.last().eventID)
    }

    @Test
    fun invalidAppointmentRetrieval() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.getAppointments(USERNAME_1)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.get("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.get("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.get("$ROUTE_USER$ROUTE_APPOINTMENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validToDoRetrieval() = testBlock(dispatcherMock) { client ->
        val serverList = listOf(generateToDo(), generateToDo())
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every { dispatcherMock.getToDos(USERNAME_1) } returns Result.Success(serverList)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.get("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
        val toDoList = Json.decodeFromString<List<ToDo>>(res.bodyAsText())
        Assertions.assertEquals(serverList.first().eventID, toDoList.first().eventID)
        Assertions.assertEquals(serverList.last().eventID, toDoList.last().eventID)
    }

    @Test
    fun invalidToDoRetrieval() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.getToDos(USERNAME_1)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.get("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.get("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.get("$ROUTE_USER$ROUTE_TODO") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validSharedEventRetrieval() = testBlock(dispatcherMock) { client ->
        val serverList = listOf(generateSharedEvent(), generateSharedEvent())
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every { dispatcherMock.getSharedEvents(USERNAME_1) } returns Result.Success(serverList)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.get("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
        val sharedEventList = Json.decodeFromString<List<SharedEvent>>(res.bodyAsText())
        Assertions.assertEquals(serverList.first().eventID, sharedEventList.first().eventID)
        Assertions.assertEquals(serverList.last().eventID, sharedEventList.last().eventID)
    }

    @Test
    fun invalidSharedEventRetrieval() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.getSharedEvents(USERNAME_1)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.get("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.get("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.get("$ROUTE_USER$ROUTE_SHARED_EVENT") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validGroupRetrieval() = testBlock(dispatcherMock) { client ->
        val serverList = listOf(generateGroup(), generateGroup())
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every { dispatcherMock.getGroups(USERNAME_1) } returns Result.Success(serverList)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res = client.get("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
        val groupList = Json.decodeFromString<List<Group>>(res.bodyAsText())
        Assertions.assertEquals(serverList.first().groupID, groupList.first().groupID)
        Assertions.assertEquals(serverList.last().groupID, groupList.last().groupID)
    }

    @Test
    fun invalidGroupRetrieval() = testBlock(dispatcherMock) { client ->
        val account = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.getGroups(USERNAME_1)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(account)

        val res1 = client.get("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.get("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        val res3 = client.get("$ROUTE_USER$ROUTE_GROUPS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }

    @Test
    fun validFriendAdding() = testBlock(dispatcherMock) { client ->
        val user1 = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        val user2 = Account(USERNAME_2, "name", ExperiencePoints(0), ExperiencePoints(0))
        every { dispatcherMock.addFriend(USERNAME_1, USERNAME_2) } returns Result.Success(user2)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(user1)

        val res = client.post("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
        Assertions.assertEquals(USERNAME_2, Json.decodeFromString<RegularAccount>(res.bodyAsText()).username)
    }

    @Test
    fun invalidFriendAdding() = testBlock(dispatcherMock) { client ->
        val user1 = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.addFriend(USERNAME_1, USERNAME_2)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(user1)

        val res1 = client.post("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.post("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        Assertions.assertEquals(INVALID_PARAMETER, res2.bodyAsText())
        val res3 = client.post("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.post("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validFriendRemoval() = testBlock(dispatcherMock) { client ->
        val user1 = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every { dispatcherMock.removeFriend(USERNAME_1, USERNAME_2) } returns Result.Success(Unit)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(user1)

        val res = client.delete("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun invalidFriendRemoval() = testBlock(dispatcherMock) { client ->
        val user1 = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.removeFriend(USERNAME_1, USERNAME_2)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(user1)

        val res1 = client.delete("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        Assertions.assertEquals(INVALID_PARAMETER, res1.bodyAsText())
        val res2 = client.delete("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        Assertions.assertEquals(INVALID_PARAMETER, res2.bodyAsText())
        val res3 = client.delete("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res3.status)
        val res4 = client.delete("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res4.status)
    }

    @Test
    fun validFriendRetrieval() = testBlock(dispatcherMock) { client ->
        val user1 = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        val user2 = Account(USERNAME_2, "name", ExperiencePoints(0), ExperiencePoints(0))
        val serverList = listOf(user2)
        every { dispatcherMock.getFriends(USERNAME_1) } returns Result.Success(serverList)
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(user1)

        val res = client.get("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.OK, res.status)
        Assertions.assertEquals(USERNAME_2,
            Json.decodeFromString<List<RegularAccount>>(res.bodyAsText()).first().username)
    }

    @Test
    fun invalidFriendRetrieval() = testBlock(dispatcherMock) { client ->
        val user1 = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        every {
            dispatcherMock.getFriends(USERNAME_1)
        } returns Result.Failure(HttpStatusCode.Forbidden, "invalid")
        every { dispatcherMock.authenticateUser(TOKEN_1) } returns Result.Success(user1)

        val res1 = client.get("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(FRIEND_ID_PARAMETER, USERNAME_2)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res1.status)
        val res2 = client.get("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.BadRequest, res2.status)
        Assertions.assertEquals(INVALID_PARAMETER, res2.bodyAsText())
        val res3 = client.get("$ROUTE_USER$ROUTE_FRIENDS") {
            url {
                parameters.append(USER_ID_PARAMETER, USERNAME_1)
                parameters.append(TOKEN_PARAMETER, TOKEN_1)
            }
        }
        Assertions.assertEquals(HttpStatusCode.Forbidden, res3.status)
    }





    private fun initValidEnvVariables() {
        val dbUser = "sa"
        val dbPassword = ""
        val dbDriver = "org.h2.Driver"
        val dbUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=runscript from 'classpath:init.sql'"
        val serverUrl = TEST_URL
        val serverPort = TEST_PORT

        variables.set(ConfigEntry.DB_USER.name, dbUser)
        variables.set(ConfigEntry.DB_PASSWORD.name, dbPassword)
        variables.set(ConfigEntry.DB_DRIVER.name, dbDriver)
        variables.set(ConfigEntry.DB_URL.name, dbUrl)
        variables.set(ConfigEntry.SERVER_URL.name, serverUrl)
        variables.set(ConfigEntry.SERVER_PORT.name, serverPort)
    }

    private fun initInvalidEnvVariables() {
        val dbUser = "sa"
        val dbPassword = ""
        val dbDriver = "org.h2.Driver"
        val dbUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=runscript from 'classpath:init.sql'"
        val serverUrl = "sd"
        val serverPort = "8083"

        variables.set(ConfigEntry.SERVER_URL.name, serverUrl)
        variables.set(ConfigEntry.DB_USER.name, dbUser)
        variables.set(ConfigEntry.DB_PASSWORD.name, dbPassword)
        variables.set(ConfigEntry.DB_DRIVER.name, dbDriver)
        variables.set(ConfigEntry.DB_URL.name, dbUrl)
        variables.set(ConfigEntry.SERVER_URL.name, serverUrl)
        variables.set(ConfigEntry.SERVER_PORT.name, serverPort)
    }
}
