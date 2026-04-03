package main.kotlin.controller

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import main.kotlin.controller.exceptions.ConnectionException
import main.kotlin.data.events.Appointment
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.SharedEventRequest
import main.kotlin.data.events.ToDo
import main.kotlin.event.GroupManager
import main.kotlin.event.ChallengeManager
import main.kotlin.event.EventManager
import main.kotlin.data.Result
import main.kotlin.data.Account
import main.kotlin.data.RegularAccount
import main.kotlin.data.Group
import main.kotlin.user.UserManager
import main.kotlin.data.config.Config
import main.kotlin.data.config.ConfigEntry
import main.kotlin.database.DatabaseConnect
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.days


const val INVALID_PARAMETER = "Invalid parameter given"
const val INVALID_BODY = "Invalid body"
const val NO_LOGIN = "Not logged in"
const val NO_AUTH = "Unauthorized for this action"

const val USER_ID_PARAMETER = "userName"
const val USER_NAME_PARAMETER = "name"
const val FRIEND_ID_PARAMETER = "friendName"
const val EVENT_ID_PARAMETER = "eventID"
const val GROUP_ID_PARAMETER = "groupID"
const val TOKEN_PARAMETER = "idToken"

const val ROUTE_USER = "/user"
const val ROUTE_LEADERBOARD = "/leaderboard"
const val ROUTE_APPOINTMENT = "/appointments"
const val ROUTE_TODO = "/toDos"
const val ROUTE_SHARED_EVENT = "/shared-events"
const val ROUTE_FRIENDS = "/friends"
const val ROUTE_GROUPS = "/groups"
const val ROUTE_LOGIN = "/login"
const val ROUTE_HEALTH = "/health"

const val ROUTE_DELETE = "/delete"

const val GENERAL_ERROR_MESSAGE = "Could not %s because of: %s."
const val SERIALIZATION_ERROR = "Could not deserialize body because of: \"%s\"."
const val CONFIG_ERROR = "Could not read \"%s\"."

val RECEIVE_DTO_MESSAGE = listOf("Trying to receive DTO...", "Received DTO.")
val GLOBAL_CHALLENGE_PERIOD = 7.days
val logger : org.slf4j.Logger = LoggerFactory.getLogger(RequestController::class.java)


/**
 * This file is the entry point for the server project of g-cal.
 * It provides the main method, which is executed with the jar-File, configuring the Ktor Server.
 * The method [configureRouting] defines all the REST Routes the server offers for client requests.
 */


fun main() {
    try {
        RequestController.create()
            .onSuccess { controller -> controller.start(true) }
            .onFailure { _, err -> logger.error(err) }
    } catch (e : ConnectionException) {
        logger.error(GENERAL_ERROR_MESSAGE.format("connect to server", e.message))
    }
}

fun Application.module(dispatcher : RequestDispatcher, coroutineDispatcher: CoroutineDispatcher) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(StatusPages) {
        exception<SerializationException> { call, e ->
            logger.error(SERIALIZATION_ERROR.format(e.message))
            call.respond(HttpStatusCode.BadRequest, SERIALIZATION_ERROR.format(e.message))
        }
        exception<ContentTransformationException> { call, e ->
            logger.error(SERIALIZATION_ERROR.format(e.message))
            call.respond(HttpStatusCode.BadRequest, SERIALIZATION_ERROR.format(e.message))
        }
    }
    launch(coroutineDispatcher) {
        while (isActive) {
            try {
                val res = dispatcher.createGlobalChallenge()
                res.onFailure { _, err -> logger.error("Could not create global challenge because of $err") }
                res.onSuccess { logger.info("Created global challenge.") }
                delay(GLOBAL_CHALLENGE_PERIOD)
            } catch (e : Exception) {
                logger.error(GENERAL_ERROR_MESSAGE.format("create global challenge", e.message))
                delay(GLOBAL_CHALLENGE_PERIOD)
            }
        }
    }
    configureRouting(dispatcher)
}

/**
 * The class [RequestController] offers a static method [RequestController.create] for creating a controller object
 * while also configuring the database and the [RequestDispatcher].
 * Implements [Controller].
 */

class RequestController private constructor(
    private val config : Config,
    private val dispatcher: RequestDispatcher
): Controller {

    private var engine: ApplicationEngine? = null

    companion object {

        /**
         * Creates a RequestController object while reading environment variables and initializing the database.
         * @return [Result.Success] of the [RequestController] if successful, [Result.Failure] otherwise
         */
        fun create(): Result<RequestController> {
            return try {
                val config = Config.create().getOrElse { status, err ->
                    return Result.Failure(status, "Config Error: $err") }

                val database = DatabaseConnect.connectDatabase(config).getOrElse { status, err ->
                    return Result.Failure(status, "DB Error: $err" )
                }

                val dispatcher = RequestDispatcher(
                    UserManager(database),
                    EventManager(database),
                    GroupManager(database),
                    ChallengeManager(database)
                )

                Result.Success(RequestController(config, dispatcher))
            } catch (e: Exception) {
                Result.Failure(HttpStatusCode.BadRequest, "Unexpected Error: ${e.message}")
            }
        }
    }

    /**
     * Starts the server with the values retrieved from the [Config] object.
     * Overrides [Controller.start].
     * @param waitForServer states whether to run a blocking server or not (for tests)
     * @throws [ConnectionException] if the config contains invalid values
     */
    override fun start(waitForServer : Boolean) {
        if (engine != null) return

        val port = config.getValue(ConfigEntry.SERVER_PORT).getOrElse { _, _ ->
            throw ConnectionException(CONFIG_ERROR.format(ConfigEntry.SERVER_PORT.identifier)) }
            .toIntOrNull()
            ?: throw ConnectionException(CONFIG_ERROR.format(ConfigEntry.SERVER_PORT.identifier))

        val url = config.getValue(ConfigEntry.SERVER_URL).getOrElse { _, _ ->
            throw ConnectionException(CONFIG_ERROR.format(ConfigEntry.SERVER_URL.identifier))
        }

        logger.info("Starting server on url $url with port $port.")
        try {
            engine = embeddedServer(
                Netty,
                port = port,
                host = url,
                module = { module(dispatcher, Dispatchers.IO)}
            ).start(wait = waitForServer)
        } catch (e : Exception) {
            logger.error("Could not connect to server because of {}", e.message)
            throw ConnectionException("Invalid server configuration")
        }
    }

    /**
     * Stops the running server and resets its information.
     * Overrides [Controller.stop].
     */
    override fun stop() {
        engine?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        engine = null
    }
}

private fun validateUser(username: String, idToken: String, dispatcher: RequestDispatcher): Boolean {
    dispatcher.authenticateUser(idToken)
        .onSuccess {
            account -> return account.username == username
        }
    return false
}

private fun mapAccountToRegularAccount(account : Account) : RegularAccount{
    return RegularAccount(account.username, account.name, account.experiencePoints)
}

private fun Application.configureRouting(dispatcher : RequestDispatcher) {
    routing {

        /**
         * Route for Login.
         * /login
         */
        route(ROUTE_LOGIN) {
            post {
                val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                    ?: run {
                        logger.error(GENERAL_ERROR_MESSAGE.format("receive idToken", "token is null"))
                        return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    }
                val name = call.request.queryParameters[USER_NAME_PARAMETER]
                    ?: run {
                        logger.error(GENERAL_ERROR_MESSAGE.format("receive name", "name is null"))
                        return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    }
                dispatcher.loginUser(idToken, name)
                    .onSuccess { user ->
                        logger.info("User ${user.username} signed in.")
                        call.respond(HttpStatusCode.OK, user)
                    }
                    .onFailure { status, err ->
                        logger.error(GENERAL_ERROR_MESSAGE.format("login user", err))
                        call.respond(status, err)
                    }
            }
        }

        /**
         * Route for account deletion.
         * /delete
         */
        route(ROUTE_DELETE) {
            delete {
                val username = call.request.queryParameters[USER_ID_PARAMETER]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                if (!validateUser(username, idToken, dispatcher)) {
                    return@delete call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                }
                dispatcher.deleteUserInfo(username)
                    .onSuccess {
                        logger.info("Deleted user $username.")
                        call.respond(HttpStatusCode.OK) }
                    .onFailure { status, err ->
                        logger.error(GENERAL_ERROR_MESSAGE.format(
                            "delete user information for user $username", err))
                        call.respond(status, err) }
            }
        }

        /**
         * Route for getting the leaderboard of the users.
         * /leaderboard
         */
        route(ROUTE_LEADERBOARD) {
            get {
                dispatcher.getLeaderboard()
                    .onSuccess { entries ->
                        logger.info("Retrieved leaderboard.")
                        call.respond(HttpStatusCode.OK, entries)}
                    .onFailure { status, err ->
                        logger.error(GENERAL_ERROR_MESSAGE.format("retrieve leaderboard", err))
                        call.respond(status, err) }
            }
        }

        /**
         * Provides the route /user as a route to get and update user information.
         * Acts as a prefix for sub routes.
         */
        route(ROUTE_USER) {
            get {
                val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                val username = call.request.queryParameters[USER_ID_PARAMETER]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                if (!validateUser(username, idToken, dispatcher)) {
                    return@get call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                }
                dispatcher.getUserInfo(username)
                    .onSuccess { user ->
                        logger.info("Retrieved user information for user ${user.username}")
                        call.respond(HttpStatusCode.OK, user) }
                    .onFailure { status, err ->
                        logger.error(GENERAL_ERROR_MESSAGE.format("user information for $username", err))
                        call.respond(status, err) }
            }
            put {
                val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                val username = call.request.queryParameters[USER_ID_PARAMETER]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                val updatedUser = call.receiveNullable<Account>()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                logger.debug("Received name {} and DTO {}", username, updatedUser)
                if (!validateUser(username, idToken, dispatcher)) {
                    return@put call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                }
                dispatcher.setUserInfo(username, updatedUser)
                    .onSuccess {
                        logger.info("Updated user information for user $username.")
                        call.respond(HttpStatusCode.OK) }
                    .onFailure { status, err ->
                        logger.error(GENERAL_ERROR_MESSAGE.format("update user information for user $username", err))
                        call.respond(status, err) }
            }

            /**
             *  Route for Appointments.
             *  /user/appointments
             */
            route(ROUTE_APPOINTMENT) {
                get {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@get call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    dispatcher.getAppointments(username)
                        .onSuccess { appointments ->
                            logger.info("User $username retrieved appointment list of size ${appointments.size}.")
                            call.respond(HttpStatusCode.OK, appointments) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "retrieve appointment list for user $username", err))
                            call.respond(status, err) }
                }
                post {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@post call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.first())
                    var appointment : Appointment
                    try {
                        appointment = call.receiveNullable<Appointment>()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    } catch (e : Exception) {
                        val errorMessage = GENERAL_ERROR_MESSAGE.format("deserialize appointment", e.message)
                        logger.error(errorMessage)
                        return@post call.respond(HttpStatusCode.BadRequest, errorMessage)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.last())
                    dispatcher.createAppointment(username, appointment)
                        .onSuccess {
                            logger.info("Created appointment with id ${appointment.eventID} for user $username.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "create appointment with id ${appointment.eventID} for user $username", err))
                            call.respond(status, err) }
                }
                put {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@put call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val updatedAppointment = call.receiveNullable<Appointment>()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    dispatcher.updateAppointment(username, updatedAppointment)
                        .onSuccess {
                            logger.info("User $username updated appointment with id ${updatedAppointment.eventID}.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "update appointment with id ${updatedAppointment.eventID} for user $username", err))
                            call.respond(status, err) }
                }
                delete {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@delete call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val eventID = call.request.queryParameters[EVENT_ID_PARAMETER]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    dispatcher.deleteAppointment(username, eventID)
                        .onSuccess {
                            logger.info("User $username deleted appointment with id $eventID.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "create delete with id $eventID for user $username", err))
                            call.respond(status, err) }
                }
            }

            /**
             * Route for ToDos.
             * /user/toDos
             */
            route(ROUTE_TODO) {
                get {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        logger.info("User invalid.")
                        return@get call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    dispatcher.getToDos(username)
                        .onSuccess { toDos ->
                            logger.info("User $username retrieved to-do list of size ${toDos.size}.")
                            call.respond(HttpStatusCode.OK, toDos) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "retrieve to-do list for user $username", err))
                            call.respond(status, err) }
                }
                post {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@post call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.first())
                    var toDo : ToDo
                    try {
                        toDo = call.receiveNullable<ToDo>()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    } catch (e : Exception) {
                        val errorMessage = GENERAL_ERROR_MESSAGE.format("deserialize to-do", e.message)
                        logger.error(errorMessage)
                        return@post call.respond(HttpStatusCode.BadRequest, errorMessage)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.last())
                    dispatcher.createToDo(username, toDo)
                        .onSuccess {
                            logger.info("Created to-do with id ${toDo.eventID} for user $username.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "create to-do with id ${toDo.eventID} for user $username", err))
                            call.respond(status, err) }
                }
                put {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@put call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val updatedToDo = call.receiveNullable<ToDo>()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    dispatcher.updateToDo(username, updatedToDo)
                        .onSuccess {
                            logger.info("User $username updated to-do with id ${updatedToDo.eventID}.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "update to-do with id ${updatedToDo.eventID} for user $username", err))
                            call.respond(status, err) }
                }
                delete {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@delete call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val eventID = call.request.queryParameters[EVENT_ID_PARAMETER]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    dispatcher.deleteToDo(username, eventID)
                        .onSuccess {
                            logger.info("User $username deleted to-do with id $eventID.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "delete to-do with id $eventID for user $username", err))
                            call.respond(status, err) }
                }
            }

            /**
             * Route for Shared Events.
             * /user/shared-events
             */
            route(ROUTE_SHARED_EVENT) {
                get {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@get call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    dispatcher.getSharedEvents(username)
                        .onSuccess { sharedEvents ->
                            logger.info("User $username retrieved shared event list of size ${sharedEvents.size}.")
                            call.respond(HttpStatusCode.OK, sharedEvents) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "retrieve shared event list for user $username", err))
                            call.respond(status, err) }
                }
                post {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@post call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.first())
                    var sharedEventRequest : SharedEventRequest
                    try {
                        sharedEventRequest = call.receiveNullable<SharedEventRequest>()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    } catch (e : Exception) {
                        val errorMessage = GENERAL_ERROR_MESSAGE.format("deserialize shared event", e.message)
                        logger.error(errorMessage)
                        return@post call.respond(HttpStatusCode.BadRequest, errorMessage)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.last())
                    dispatcher.createSharedEventAndShare(username,
                        sharedEventRequest.sharedEvent, sharedEventRequest.users)
                        .onSuccess {
                            logger.info("User $username shared event with id" +
                                    " ${sharedEventRequest.sharedEvent.eventID}.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "create shared event with id ${sharedEventRequest.sharedEvent.eventID} " +
                                        "for user $username", err))
                            call.respond(status, err) }
                }
                put {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@put call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val updatedSharedEvent = call.receiveNullable<SharedEvent>()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    dispatcher.updateSharedEvent(username, updatedSharedEvent)
                        .onSuccess {
                            logger.info("User $username updated shared event with id ${updatedSharedEvent.eventID}.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "update shared event with id ${updatedSharedEvent.eventID} for user $username", err))
                            call.respond(status, err) }
                }
                delete {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@delete call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val eventID = call.request.queryParameters[EVENT_ID_PARAMETER]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    dispatcher.deleteSharedEvent(username, eventID)
                        .onSuccess {
                            logger.info("User $username deleted shared event with id $eventID.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "delete shared event with id $eventID for user $username", err))
                            call.respond(status, err) }
                }
            }


            /**
             * Route for Groups.
             * /user/groups
             */
            route(ROUTE_GROUPS) {
                get {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@get call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    dispatcher.getGroups(username)
                        .onSuccess { groups ->
                            logger.info("User $username retrieved group list of size ${groups.size}.")
                            call.respond(HttpStatusCode.OK, groups) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "retrieve group list for user $username", err))
                            call.respond(status, err) }
                }
                post {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@post call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.first())
                    var group : Group
                    try {
                        group = call.receiveNullable<Group>()
                            ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    } catch (e : Exception) {
                        val errorMessage = GENERAL_ERROR_MESSAGE.format("deserialize group", e.message)
                        logger.error(errorMessage)
                        return@post call.respond(HttpStatusCode.BadRequest, errorMessage)
                    }
                    logger.debug(RECEIVE_DTO_MESSAGE.last())
                    dispatcher.createGroup(username, group)
                        .onSuccess {
                            logger.info("Created group with id ${group.groupID} for user $username.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "create group with id ${group.groupID} for user $username", err))
                            call.respond(status, err) }
                }
                put {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@put call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val updatedGroup = call.receiveNullable<Group>()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, INVALID_BODY)
                    dispatcher.updateGroup(username, updatedGroup)
                        .onSuccess {
                            logger.info("User $username updated group with id ${updatedGroup.groupID}.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "update group with id ${updatedGroup.groupID} for user $username", err))
                            call.respond(status, err) }
                }
                delete {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@delete call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val groupID = call.request.queryParameters[GROUP_ID_PARAMETER]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    dispatcher.deleteGroup(username, groupID)
                        .onSuccess {
                            logger.info("User $username deleted group with id $groupID.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "delete group with id $groupID for user $username", err))
                            call.respond(status, err) }
                }
            }


            /**
             * Route for friends.
             * /user/friends
             */
            route(ROUTE_FRIENDS) {
                get {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val username = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(username, idToken, dispatcher)) {
                        return@get call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    dispatcher.getFriends(username)
                        .onSuccess { friends ->
                            logger.info("User $username retrieved friend list of size ${friends.size}.")
                            call.respond(HttpStatusCode.OK,
                            friends.map { account -> mapAccountToRegularAccount(account) }) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "retrieve friend list for user $username", err))
                            call.respond(status, err) }
                }
                post {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val actingUsername = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(actingUsername, idToken, dispatcher)) {
                        return@post call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val friend = call.request.queryParameters[FRIEND_ID_PARAMETER]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    dispatcher.addFriend(actingUsername, friend)
                        .onSuccess { friendAccount ->
                            logger.info("User $actingUsername added $friend as a friend.")
                            call.respond(HttpStatusCode.OK, mapAccountToRegularAccount(friendAccount)) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "add user $friend as a friend for user $actingUsername", err))
                            call.respond(status, err) }
                }
                delete {
                    val idToken = call.request.queryParameters[TOKEN_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, NO_LOGIN)
                    val actingUsername = call.request.queryParameters[USER_ID_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    if (!validateUser(actingUsername, idToken, dispatcher)) {
                        return@delete call.respond(HttpStatusCode.BadRequest, NO_AUTH)
                    }
                    val friend = call.request.queryParameters[FRIEND_ID_PARAMETER]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, INVALID_PARAMETER)
                    dispatcher.removeFriend(actingUsername, friend)
                        .onSuccess {
                            logger.info("User $actingUsername deleted $friend as a friend.")
                            call.respond(HttpStatusCode.OK) }
                        .onFailure { status, err ->
                            logger.error(GENERAL_ERROR_MESSAGE.format(
                                "remove user $friend as a friend for $actingUsername", err))
                            call.respond(status, err) }
                }
            }
        }

        /**
         * Route for checking server status.
         * /health
         */
        route(ROUTE_HEALTH) {
            head {
                call.respond(HttpStatusCode.OK)
            }
            get {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
