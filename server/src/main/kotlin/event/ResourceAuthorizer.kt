package main.kotlin.event

import io.ktor.http.HttpStatusCode
import org.ktorm.database.Database
import main.kotlin.data.Result
import org.ktorm.entity.sequenceOf
import main.kotlin.database.Events
import main.kotlin.database.Groups
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.any


const val ERROR_MESSAGE = "Invalid operation: %s"
const val NO_ACCESS = "User has no access to this object"

/**
 * Offers an implementation of [Authorizer] to allow for authorization when a user tries to access a group or event.
 * Implements [Authorizer].
 */
class ResourceAuthorizer(db : Database) : Authorizer {
    private val database = db

    /**
     * Authorizes the access of a user to a given event. If there is an event with the user and eventID, it means the
     * user has access to the event.
     * @param username the name of the user trying to access
     * @param eventID the ID of the event to be accessed
     * @return [Result.Success] of true if the access is valid, false if not, [Result.Failure] if an error has occurred
     */
    override fun authorizeEvent(username: String, eventID: Long): Result<Boolean> {
        return try {
            val exists = database.sequenceOf(Events)
                .any { (it.eventID eq eventID) and (it.user eq username) }
            Result.Success(exists)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.Forbidden, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Authorizes the access of a user to a given group. If there is a group with the user and groupID, it means the
     * user has access to the group.
     * @param username the name of the user trying to access
     * @param groupID the ID of the group to be accessed
     * @return [Result.Success] of true if the access is valid, false if not, [Result.Failure] if an error has occurred
     */
    override fun authorizeGroup(username: String, groupID: Long): Result<Boolean> {
        return try {
            val exists = database.sequenceOf(Groups)
                .any { (it.groupID eq groupID) and (it.user eq username) }
            Result.Success(exists)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.Forbidden, ERROR_MESSAGE.format(e.message))
        }
    }
}
