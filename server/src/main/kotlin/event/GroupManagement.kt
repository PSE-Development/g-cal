package main.kotlin.event

import main.kotlin.data.Result
import main.kotlin.data.Group

/**
 * Interface for managing the groups by database access.
 */
interface GroupManagement {

    /**
     * Returns the groups of a user.
     */
    fun getGroups(username : String) : Result<List<Group>>

    /**
     * Creates a new group for a user.
     */
    fun createGroup(username : String, group : Group) : Result<Unit>

    /**
     * Updates an existing group for a user.
     */
    fun updateGroup(username : String, updatedGroup : Group) : Result<Unit>

    /**
     * Deletes an existing group for a user.
     */
    fun deleteGroup(username : String, groupID : Long) : Result<Unit>
}
