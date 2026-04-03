package main.kotlin.event

import io.ktor.http.HttpStatusCode
import main.kotlin.data.Result
import main.kotlin.data.Group
import main.kotlin.database.Accounts
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.any
import org.ktorm.entity.sequenceOf
import main.kotlin.database.Groups
import main.kotlin.database.Events
import main.kotlin.user.NO_GROUP_ID
import org.ktorm.dsl.and
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.filter

import org.ktorm.dsl.delete
import org.ktorm.entity.toList

/**
 * This class manages the groups of the calendar by providing the functionality for multiple operations, acting as
 * an interface to the database as part of the persistence layer.
 * The updating/deleting operations are considered successful even if no entry gets deleted/updated, as long as no
 * error occurs, which results in returning [Result.Success].
 * Uses an [ResourceAuthorizer] to validate user access to groups.
 * Implements [GroupManagement].
 */

class GroupManager (db : Database): GroupManagement {
    private val database = db
    private val authorizer = ResourceAuthorizer(db)

    /**
     * Returns a List of Groups for a given user.
     * @param username the name of the querying user
     * @return [Result.Success] of a list of [Group] if successful, [Result.Failure] otherwise
     */
    override fun getGroups(username : String) : Result<List<Group>> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")

        return try {
            val list = database.sequenceOf(Groups)
                .filter { it.user eq username}
                .toList()
                .map { Group(it.groupID, it.name, it.colour) }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Creates a new group for a given user if the group did not already exist.
     * @param username the name of the acting user
     * @param group the created group
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun createGroup(username : String, group : Group) : Result<Unit> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")
        if (groupExists(username, group.groupID))
            return Result.Failure(HttpStatusCode.Forbidden, "Group already exists")

        return try {
            database.insert(Groups) {
                set(it.groupID, group.groupID)
                set(it.name, group.name)
                set(it.colour, group.colour)
                set(it.user, username)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Updates an existing group for a given user by replacing the group of the user with the same groupID.
     * Updating the NoGroup with ID "0" is not allowed. Only updating if the user has access to the group
     * with the groupID.
     * @param username the name of the acting user
     * @param updatedGroup the updated group
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun updateGroup(username : String, updatedGroup : Group) : Result<Unit> {
        if (updatedGroup.groupID == 0L) return Result.Failure(HttpStatusCode.Forbidden, "Group is reserved")
        val authResult = authorizer.authorizeGroup(username, updatedGroup.groupID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.update(Groups) {
                set(it.name, updatedGroup.name)
                set(it.colour, updatedGroup.colour)
                where { (it.groupID eq updatedGroup.groupID) and (it.user eq username) }
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Deletes an existing group for a given user. Deleting the NoGroup with ID "0" is not allowed.
     * Only deleting if the user has access to the group with the groupID.
     * Sets all the events of the user that reference the groupID to the NoGroup.
     * @param username the name of the acting user
     * @param groupID the id of the group to be deleted
     * @param [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun deleteGroup(username: String, groupID: Long): Result<Unit> {
        if (groupID == NO_GROUP_ID) return Result.Failure(HttpStatusCode.Forbidden, "Group is reserved")
        val authResult = authorizer.authorizeGroup(username, groupID)
        if (authResult is Result.Failure || (authResult is Result.Success && !authResult.value)) {
            return Result.Failure(HttpStatusCode.Forbidden, NO_ACCESS)
        }

        return try {
            database.useTransaction {
                database.update(Events) {
                    set(it.groupID, NO_GROUP_ID)
                    where { (it.groupID eq groupID) and (it.user eq username) }
                }

                database.delete(Groups) {
                    (it.groupID eq groupID) and (it.user eq username)
                }
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    private fun userExists(username : String) : Boolean {
        return try {
            database.sequenceOf(Accounts)
                .any { it.username eq username }
        } catch (_ : Exception) {
            false
        }
    }

    private fun groupExists(username : String, groupID : Long) : Boolean {
        return try {
            database.sequenceOf(Groups)
                .any { (it.user eq username) and (it.groupID eq groupID) }
        } catch (_ : Exception) {
            false
        }
    }
}
