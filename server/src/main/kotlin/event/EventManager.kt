package main.kotlin.event

import io.ktor.http.HttpStatusCode
import main.kotlin.data.Result
import main.kotlin.data.Group
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.events.ToDo
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.Appointment
import main.kotlin.database.Accounts
import main.kotlin.database.Events
import main.kotlin.database.Groups
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.entity.any
import org.ktorm.entity.filter
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList

/**
 * This class manages the events of the calendar by providing the functionality for multiple operations, acting as
 * an interface to the database as part of the persistence layer.
 * The updating/deleting operations are considered successful even if no entry gets deleted/updated, as long as no
 * error occurs, which results in returning [Result.Success].
 * Uses an [ResourceAuthorizer] to validate user access to events.
 * Implements [EventManagement].
 */

const val ID_NUMBER_RANGE = 100000

class EventManager (db : Database) : EventManagement {
    private val database = db
    private val authorizer = ResourceAuthorizer(db)


    /**
     * Creates a new appointment for a given user if the appointment did not already exist.
     * Invalid if the user has no access to the appointments group.
     * @param username the name of the acting user
     * @param appointment the created appointment
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun createAppointment(username : String, appointment : Appointment) : Result<Unit> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound,"User not found")
        if (eventExists(username, appointment.eventID)) return Result.Failure(HttpStatusCode.Forbidden, ERROR_MESSAGE)

        val authResult = authorizer.authorizeGroup(username, appointment.group.groupID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.insert(Events) {
                set(it.eventID, appointment.eventID)
                set(it.eventType, EventType.APPOINTMENT_TYPE.name)
                set(it.name, appointment.name)
                set(it.description, appointment.description)
                set(it.end, appointment.end)
                set(it.experiencePoints, appointment.experiencePoints.value)
                set(it.user, username)
                set(it.completed, appointment.completed.isCompleted)
                set(it.completionDate, appointment.completed.completionTime)
                set(it.groupID, appointment.group.groupID)
                set(it.start, appointment.start)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Creates a new to-Do for a given user if the to-Do did not already exist.
     * @param username the name of the acting user
     * @param toDo the created to-Do
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun createToDo(username : String, toDo : ToDo) : Result<Unit> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")
        if (eventExists(username, toDo.eventID)) return Result.Failure(HttpStatusCode.Forbidden, ERROR_MESSAGE)

        return try {
            database.insert(Events) {
                set(it.eventID, toDo.eventID)
                set(it.eventType, EventType.TODO_TYPE.name)
                set(it.name, toDo.name)
                set(it.description, toDo.description)
                set(it.end, toDo.end)
                set(it.experiencePoints, toDo.experiencePoints.value)
                set(it.user, username)
                set(it.completed, toDo.completed.isCompleted)
                set(it.completionDate, toDo.completed.completionTime)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Creates a new shared event for a given user if the shared event did not already exist.
     * Invalid if the user has no access to the shared events group.
     * @param username the name of the acting user
     * @param sharedEvent the created shared event
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun createSharedEvent(username : String, sharedEvent: SharedEvent) : Result<Unit> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")
        if (eventExists(username, sharedEvent.eventID)) return Result.Failure(HttpStatusCode.Forbidden, ERROR_MESSAGE)

        val authResult = authorizer.authorizeGroup(username, sharedEvent.group.groupID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.insert(Events) {
                set(it.eventID, sharedEvent.eventID)
                set(it.eventType, EventType.SHARED_EVENT_TYPE.name)
                set(it.name, sharedEvent.name)
                set(it.description, sharedEvent.description)
                set(it.end, sharedEvent.end)
                set(it.experiencePoints, sharedEvent.experiencePoints.value)
                set(it.user, username)
                set(it.completed, sharedEvent.completed.isCompleted)
                set(it.completionDate, sharedEvent.completed.completionTime)
                set(it.groupID, sharedEvent.group.groupID)
                set(it.start, sharedEvent.start)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns a List of appointments for a given user.
     * @param username the name of the querying user
     * @return [Result.Success] of a list of [Appointment] if successful, [Result.Failure] otherwise
     */
    override fun getAppointments(username : String) : Result<List<Appointment>> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")

        return try {
            val list = database
                .from(Events)
                .leftJoin(Groups, on = (Events.groupID eq Groups.groupID) and (Events.user eq Groups.user))
                .select()
                .where { (Events.user eq username) and (Events.eventType eq EventType.APPOINTMENT_TYPE.name) }
                .map { row ->
                    val event = Events.createEntity(row)
                    val group = Groups.createEntity(row)
                    val start = event.start
                        ?: throw IllegalArgumentException("Attribute 'start' is missing for event ${event.eventID}")

                    Appointment(event.eventID, event.name, event.description, start, event.end,
                        Group(group.groupID, group.name, group.colour), ExperiencePoints(event.experiencePoints),
                        EventCompletionDetail(event.eventID, event.completed, event.completionDate)
                    )
                }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns a List of to-Dos for a given user.
     * @param username the name of the querying user
     * @return [Result.Success] of a list of [ToDo] if successful, [Result.Failure] otherwise
     */
    override fun getToDos(username : String) : Result<List<ToDo>> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound,"User not found")

        return try {
            val list = database.sequenceOf(Events)
                .filter { (it.user eq username) and (it.eventType eq EventType.TODO_TYPE.name) }
                .toList()
                .map { entity ->
                    ToDo(entity.eventID, entity.name, entity.description, entity.end,
                        ExperiencePoints(entity.experiencePoints),
                        EventCompletionDetail(entity.eventID, entity.completed, entity.completionDate))
                }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns a List of shared events for a given user.
     * @param username the name of the querying user
     * @return [Result.Success] of a list of [SharedEvent] if successful, [Result.Failure] otherwise
     */
    override fun getSharedEvents(username : String) : Result<List<SharedEvent>> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")

        return try {
            val list = database
                .from(Events)
                .leftJoin(Groups, on = (Events.groupID eq Groups.groupID) and (Events.user eq Groups.user))
                .select()
                .where { (Events.user eq username) and (Events.eventType eq EventType.SHARED_EVENT_TYPE.name) }
                .map { row ->
                    val event = Events.createEntity(row)
                    val group = Groups.createEntity(row)
                    val start = event.start
                        ?: throw IllegalArgumentException("Attribute 'start' is missing for event ${event.eventID}")

                    SharedEvent(event.eventID, event.name, event.description, start, event.end,
                        Group(group.groupID, group.name, group.colour), ExperiencePoints(event.experiencePoints),
                        EventCompletionDetail(event.eventID, event.completed, event.completionDate)
                    )
                }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }


    /**
     * Updates an existing appointment for a given user by replacing the appointment of the user with the same eventID.
     * Invalid if the user has no access to the old eventID or the group of the updated appointment.
     * @param username the name of the acting user
     * @param updatedAppointment the updated appointment
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun updateAppointment(username : String, updatedAppointment: Appointment) : Result<Unit> {
        val authResultEvent = authorizer.authorizeEvent(username, updatedAppointment.eventID)
        if (authResultEvent is Result.Failure || (authResultEvent is Result.Success && !authResultEvent.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }
        val authResultGroup = authorizer.authorizeGroup(username, updatedAppointment.group.groupID)
        if (authResultGroup is Result.Failure || (authResultGroup is Result.Success && !authResultGroup.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.useTransaction {
                val event = database.sequenceOf(Events)
                    .firstOrNull {
                        (it.eventID eq updatedAppointment.eventID) and
                                (it.eventType eq EventType.APPOINTMENT_TYPE.name) and (it.user eq username)
                    } ?: throw IllegalArgumentException("Appointment with id ${updatedAppointment.eventID} does not exist")

                val group = database.sequenceOf(Groups)
                    .firstOrNull { (it.groupID eq updatedAppointment.group.groupID) and (it.user eq username) }
                        ?: throw IllegalArgumentException(
                            "Group with id ${updatedAppointment.group.groupID} does not exist")

                event.name = updatedAppointment.name
                event.description = updatedAppointment.description
                event.end = updatedAppointment.end
                event.experiencePoints = updatedAppointment.experiencePoints.value
                event.completed = updatedAppointment.completed.isCompleted
                event.completionDate  = updatedAppointment.completed.completionTime
                event.groupID = group.groupID
                event.start = updatedAppointment.start
                event.flushChanges()
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Updates an existing to-Do for a given user by replacing the to-Do of the user with the same eventID.
     * Invalid if the user has no access to the old eventID.
     * @param username the name of the acting user
     * @param updatedToDo the updated to-Do
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun updateToDo(username : String, updatedToDo: ToDo) : Result<Unit> {
        val authResult = authorizer.authorizeEvent(username, updatedToDo.eventID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.useTransaction {
                val event = database.sequenceOf(Events)
                    .firstOrNull {
                        (it.eventID eq updatedToDo.eventID) and
                                (it.eventType eq EventType.TODO_TYPE.name) and (it.user eq username)
                    } ?: throw IllegalArgumentException("To-Do with id ${updatedToDo.eventID} does not exist")

                event.name = updatedToDo.name
                event.description = updatedToDo.description
                event.end = updatedToDo.end
                event.experiencePoints = updatedToDo.experiencePoints.value
                event.completed = updatedToDo.completed.isCompleted
                event.completionDate  = updatedToDo.completed.completionTime
                event.flushChanges()
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Updates an existing shared event for a given user by replacing the shared event of the user with the same
     * eventID. Only update allowed is updating the completion detail, i.e. the completed flag and the completion date.
     * Invalid if the user has no access to the old eventID or the group of the updated shared event.
     * @param username the name of the acting user
     * @param updatedSharedEvent the updated shared event
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun updateSharedEvent(username : String, updatedSharedEvent: SharedEvent) : Result<Unit> {
        val authResultEvent = authorizer.authorizeEvent(username, updatedSharedEvent.eventID)
        if (authResultEvent is Result.Failure || (authResultEvent is Result.Success && !authResultEvent.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }
        val authResultGroup = authorizer.authorizeGroup(username, updatedSharedEvent.group.groupID)
        if (authResultGroup is Result.Failure || (authResultGroup is Result.Success && !authResultGroup.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.useTransaction {
                val event = database.sequenceOf(Events)
                    .firstOrNull {
                        (it.eventID eq updatedSharedEvent.eventID) and
                                (it.eventType eq EventType.SHARED_EVENT_TYPE.name) and (it.user eq username)
                    } ?: throw IllegalArgumentException(
                        "Shared event with id ${updatedSharedEvent.eventID} does not exist")

                event.completed = updatedSharedEvent.completed.isCompleted
                event.completionDate  = updatedSharedEvent.completed.completionTime
                event.flushChanges()
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Deletes an existing appointment for a given user.
     * Only deleting if the user has access to the appointment with the eventID.
     * @param username the name of the acting user
     * @param eventID the id of the appointment to be deleted
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun deleteAppointment(username: String, eventID: Long): Result<Unit> {
        val authResult = authorizer.authorizeEvent(username, eventID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.delete(Events) {
                (it.eventID eq eventID) and (it.user eq username) and (it.eventType eq EventType.APPOINTMENT_TYPE.name)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Deletes an existing to-Do for a given user.
     * Only deleting if the user has access to the to-Do with the eventID.
     * @param username the name of the acting user
     * @param eventID the id of the to-Do to be deleted
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun deleteToDo(username: String, eventID: Long): Result<Unit> {
        val authResult = authorizer.authorizeEvent(username, eventID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.delete(Events) {
                (it.eventID eq eventID) and (it.user eq username) and (it.eventType eq EventType.TODO_TYPE.name)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Deletes an existing shared event for a given user, but only his version.
     * Only deleting if the user has access to the shared event with the eventID.
     * @param username the name of the acting user
     * @param eventID the id of the shared event to be deleted
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun deleteSharedEvent(username: String, eventID: Long): Result<Unit> {
        val authResult = authorizer.authorizeEvent(username, eventID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.delete(Events) {
                (it.eventID eq eventID) and (it.user eq username) and (it.eventType eq EventType.SHARED_EVENT_TYPE.name)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Generates a unique eventID which is not used by any user.
     * @return the unique eventID
     */
    override fun generateUniqueEventID(): Long {
        var eventID : Long
        do {
            eventID = (0..ID_NUMBER_RANGE).random().toLong()
        } while (database.sequenceOf(Events).any { it.eventID eq eventID})
        return eventID
    }

    private fun userExists(username : String) : Boolean {
        return try {
            database.sequenceOf(Accounts)
                .any { it.username eq username }
        } catch (_ : Exception) {
            false
        }
    }

    private fun eventExists(username : String, eventID : Long) : Boolean {
        return try {
            database.sequenceOf(Events)
                .any { (it.eventID eq eventID) and (it.user eq username) }
        } catch (_ : Exception) {
            false
        }
    }
}
