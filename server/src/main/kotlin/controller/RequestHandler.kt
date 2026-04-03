package main.kotlin.controller

import main.kotlin.data.Account
import main.kotlin.data.Result
import main.kotlin.data.RegularAccount
import main.kotlin.data.LeaderboardEntry
import main.kotlin.data.Group
import main.kotlin.data.events.ToDo
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.Appointment

/**
 * This interface provides a number of methods to be used by the [Controller] interface and acts as an intermediate
 * dispatching layer between the Controller Layer, which translates the API calls, and the persistence layer with
 * database access.
 */
interface RequestHandler {

    /**
     * Performs a login for a user by validating the token.
     */
    fun loginUser(token : String, name : String) : Result<Account>

    /**
     * Authenticates the token of a user.
     */
    fun authenticateUser(token : String) : Result<Account>

    /**
     * Adds a user to the database.
     */
    fun addUser(user : Account) : Result<Unit>

    /**
     * Gets user information.
     */
    fun getUserInfo(username: String) : Result<Account>

    /**
     * Updates user information.
     */
    fun setUserInfo(username: String, updatedUser : Account) : Result<Unit>

    /**
     * Deletes user information
     */
    fun deleteUserInfo(username : String) : Result<Unit>

    /**
     * Creates a group for a user.
     */
    fun createGroup(username : String, group : Group) : Result<Unit>

    /**
     * Gets the groups of a user.
     */
    fun getGroups(username : String) : Result<List<Group>>

    /**
     * Updates a group of a user.
     */
    fun updateGroup(username : String, updatedGroup : Group) : Result<Unit>

    /**
     * Deletes a group of a user.
     */
    fun deleteGroup(username : String, groupID: Long) : Result<Unit>

    /**
     * Creates an appointment for a user.
     */
    fun createAppointment(username : String, appointment: Appointment) : Result<Unit>

    /**
     * Creates a to-Do for a user.
     */
    fun createToDo(username : String, toDo : ToDo) : Result<Unit>

    /**
     * Creates a shared event for a list of users.
     */
    fun createSharedEventAndShare(username : String, sharedEvent: SharedEvent,
                                  userList : List<RegularAccount>) : Result<Unit>

    /**
     * Gets the appointments of a user.
     */
    fun getAppointments(username : String) : Result<List<Appointment>>

    /**
     * Gets the to-Dos of a user.
     */
    fun getToDos(username : String) : Result<List<ToDo>>

    /**
     * Gets the shared events of a user.
     */
    fun getSharedEvents(username : String) : Result<List<SharedEvent>>

    /**
     * Updates an appointment of a user.
     */
    fun updateAppointment(username : String, updatedAppointment : Appointment) : Result<Unit>

    /**
     * Updates a to-Do of a user.
     */

    fun updateToDo(username : String, updatedToDo : ToDo) : Result<Unit>

    /**
     * Updates a shared event of a user.
     */
    fun updateSharedEvent(username : String, updatedSharedEvent : SharedEvent) : Result<Unit>

    /**
     * Deletes an appointment of a user.
     */
    fun deleteAppointment(username : String, eventID : Long) : Result<Unit>

    /**
     * Deletes a to-Do of a user.
     */
    fun deleteToDo(username : String, eventID: Long) : Result<Unit>

    /**
     * Deletes a shared event of a user.
     */
    fun deleteSharedEvent(username: String, eventID: Long): Result<Unit>

    /**
     * Adds another user as a friend.
     */
    fun addFriend(actingUserName : String, friendName : String) : Result<Account>

    /**
     * Removes another user as a friend.
     */
    fun removeFriend(actingUserName : String, friendName : String) : Result<Unit>

    /**
     * Gets the friends of a user.
     */
    fun getFriends(username : String) : Result<List<Account>>

    /**
     * Returns a List of [LeaderboardEntry]s, ordering the users of the database by some metric.
     */
    fun getLeaderboard() : Result<List<LeaderboardEntry>>

    /**
     * Creates a global challenge as a
     */
    fun createGlobalChallenge() : Result<Unit>
}
