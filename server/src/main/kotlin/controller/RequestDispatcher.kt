package main.kotlin.controller

import io.ktor.http.HttpStatusCode
import main.kotlin.data.Account
import main.kotlin.data.Result
import main.kotlin.data.RegularAccount
import main.kotlin.data.LeaderboardEntry
import main.kotlin.data.Group
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.events.ToDo
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.Appointment
import main.kotlin.event.ChallengeManager
import main.kotlin.event.EventManager
import main.kotlin.event.GroupManager
import main.kotlin.user.UserManager
import org.slf4j.LoggerFactory

/**
 * Provides the bridge between the Managers [EventManager], [UserManager], [GroupManager] and the [RequestController],
 * distributing the calls of the controller to the adequate manager.
 * Implements [RequestHandler]
 */

class RequestDispatcher(
    private val userManager: UserManager,
    private val eventManager: EventManager,
    private val groupManager: GroupManager,
    private val challengeManager: ChallengeManager
) : RequestHandler {

    val logger : org.slf4j.Logger = LoggerFactory.getLogger(RequestDispatcher::class.java)


    /**
     * Performs a login for a user by validating the token.
     * @param token the token
     * @param name the name of the user
     * @return [Result.Success] of the Account if successful, [Result.Failure] otherwise
     */
    override fun loginUser(token : String, name : String) : Result<Account> {
        return userManager.loginUser(token, name)
    }


    /**
     * Authenticates a given token String and returns the corresponding account.
     * @param token the token
     * @return [Result.Success] of the Account if successful, [Result.Failure] otherwise
     */
    override fun authenticateUser(token : String) : Result<Account> {
        return userManager.authenticateUser(token)
    }

    /**
     * Adds a user to the system.
     * @param user the user to add
     * @return the result of the database operation
     */
    override fun addUser(user : Account) : Result<Unit> {
        return userManager.addUser(user)
    }

    /**
     * Gets the information of a user with given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun getUserInfo(username: String): Result<Account> {
        return userManager.getUserInfo(username)
    }

    /**
     * Updates the information of a user with a given username.
     * @param username the name of the user
     * @param updatedUser the updated user information
     * @return the result of the database operation
     */
    override fun setUserInfo(username : String, updatedUser : Account) : Result<Unit> {
        return userManager.setUserInfo(username, updatedUser)
    }

    /**
     * Deletes the information of a user with a given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun deleteUserInfo(username : String) : Result<Unit> {
        return userManager.deleteUserInfo(username)
    }

    /**
     * Gets the groups of a user with a given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun getGroups(username: String): Result<List<Group>> {
        return groupManager.getGroups(username)
    }

    /**
     * Creates a given group for a user with a given username.
     * @param username the name of the user
     * @param group the created group
     * @return the result of the database operation
     */
    override fun createGroup(username: String, group : Group): Result<Unit> {
        return groupManager.createGroup(username, group)
    }

    /**
     * Updates an existing group of a user with a given username.
     * @param username the name of the user
     * @param updatedGroup the updated group
     * @return the result of the database operation
     */
    override fun updateGroup(username : String, updatedGroup : Group) : Result<Unit> {
        return groupManager.updateGroup(username, updatedGroup)
    }

    /**
     * Deletes an existing group of a user with a given username.
     * @param username the name of the user
     * @param groupID the ID of the group to be deleted
     * @return the result of the database operation
     */
    override fun deleteGroup(username: String, groupID: Long): Result<Unit> {
        return groupManager.deleteGroup(username, groupID)
    }

    /**
     * Creates a given appointment for a user with a given username.
     * @param username the name of the user
     * @param appointment the created appointment
     * @return the result of the database operation
     */
    override fun createAppointment(username: String, appointment: Appointment): Result<Unit> {
        return eventManager.createAppointment(username, appointment)
    }

    /**
     * Creates a given to-Do for a user with a given username.
     * @param username the name of the user
     * @param toDo the created to-Do
     * @return the result of the database operation
     */
    override fun createToDo(username: String, toDo: ToDo): Result<Unit> {
        return eventManager.createToDo(username, toDo)
    }

    /**
     * Creates a shared event for a list of users by checking for each user if the friendship exists.
     * If so, the event is shared with the other user.
     * @param username the name of the acting user
     * @param sharedEvent the shared event to share
     * @param userList the list of users to share the event with
     * @param
     */
    override fun createSharedEventAndShare(username: String, sharedEvent: SharedEvent,
                                           userList : List<RegularAccount>): Result<Unit> {
        val uniqueEventID : Long = eventManager.generateUniqueEventID()
        val oldEventCD = sharedEvent.completed
        val copyEvent = sharedEvent.copy(eventID = uniqueEventID,
            completed = EventCompletionDetail(uniqueEventID, oldEventCD.isCompleted, oldEventCD.completionTime))
        val accountList = when (val res = userManager.mapRegularToAccount(userList)) {
            is Result.Success -> res.value
            is Result.Failure -> return Result.Failure(res.status, res.error)
        }

        val res = eventManager.createSharedEvent(username, copyEvent)
        val copyEventNoGroup = copyEvent.copy(group = Group(0, "None", 0))

        for (user in accountList) {
            if (userManager.friendshipExists(username, user.username)) {
                eventManager.createSharedEvent(user.username, copyEventNoGroup)
            } else {
                return Result.Failure(HttpStatusCode.Forbidden, "Cannot share event with unfriended user")
            }
        }
        return res
    }

    /**
     * Gets the appointments of a user with a given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun getAppointments(username: String): Result<List<Appointment>> {
        return eventManager.getAppointments(username)
    }

    /**
     * Gets the to-Dos of a user with a given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun getToDos(username: String): Result<List<ToDo>> {
        return eventManager.getToDos(username)
    }

    /**
     * Gets the shared events of a user with a given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun getSharedEvents(username: String): Result<List<SharedEvent>> {
        return eventManager.getSharedEvents(username)
    }

    /**
     * Updates an existing appointment of a user with a given username.
     * @param username the name of the user
     * @param updatedAppointment the updated appointment
     * @return the result of the database operation
     */
    override fun updateAppointment(username : String, updatedAppointment : Appointment) : Result<Unit> {
        return eventManager.updateAppointment(username, updatedAppointment)
    }

    /**
     * Updates an existing to-Do of a user with a given username.
     * @param username the name of the user
     * @param updatedToDo the updated to-Do
     * @return the result of the database operation
     */
    override fun updateToDo(username : String, updatedToDo : ToDo) : Result<Unit> {
        return eventManager.updateToDo(username, updatedToDo)
    }

    /**
     * Updates an existing shared event of a user with a given username.
     * @param username the name of the user
     * @param updatedSharedEvent the updated shared event
     * @return the result of the database operation
     */
    override fun updateSharedEvent(username : String, updatedSharedEvent : SharedEvent) : Result<Unit> {
        return eventManager.updateSharedEvent(username, updatedSharedEvent)
    }

    /**
     * Deletes an existing appointment of a user with a given username.
     * @param username the name of the user
     * @param eventID the ID of the appointment to be deleted
     * @return the result of the database operation
     */
    override fun deleteAppointment(username: String, eventID: Long): Result<Unit> {
        return eventManager.deleteAppointment(username, eventID)
    }

    /**
     * Deletes an existing to-Do of a user with a given username.
     * @param username the name of the user
     * @param eventID the ID of the to-Do to be deleted
     * @return the result of the database operation
     */
    override fun deleteToDo(username: String, eventID: Long): Result<Unit> {
        return eventManager.deleteToDo(username, eventID)
    }

    /**
     * Deletes an existing shared event of a user with a given username.
     * @param username the name of the user
     * @param eventID the ID of the shared event to be deleted
     * @return the result of the database operation
     */
    override fun deleteSharedEvent(username: String, eventID: Long): Result<Unit> {
        return eventManager.deleteSharedEvent(username, eventID)
    }

    /**
     * Adds another user as a friend for a given user by creating a friend request.
     * @param actingUserName the name of the acting user
     * @param friendName the name of the friend to add
     * @return the result of the database operation
     */
    override fun addFriend(actingUserName: String, friendName: String): Result<Account> {
        return userManager.addFriend(actingUserName, friendName)
    }

    /**
     * Removes another user as a friend.
     * @param actingUserName the name of the acting user
     * @param friendName the name of the friend to remove
     * @return the result of the database operation
     */
    override fun removeFriend(actingUserName: String, friendName: String): Result<Unit> {
        return userManager.removeFriend(actingUserName, friendName)
    }

    /**
     * Returns the friends of a user with a given username.
     * @param username the name of the user
     * @return the result of the database operation
     */
    override fun getFriends(username: String): Result<List<Account>> {
        return userManager.getFriends(username)
    }

    /**
     * Returns a list of all the users of the system, ordered descendingly by their experience points
     * as a [LeaderboardEntry].
     * @return the result of the database operation
     */
    override fun getLeaderboard() : Result<List<LeaderboardEntry>> {
        return userManager.getLeaderboardList()
    }

    /**
     * Creates a global challenge, which means adding a shared event for every user with the specified minimum
     * experience points.
     * Gets called periodically every 7 days.
     * @return [Result.Success] if successful, [Result.Failure] otherwise
     */
    override fun createGlobalChallenge() : Result<Unit> {
        val challenge = challengeManager.getGlobalChallenge()
        userManager.getUserList()
            .onFailure { status, message -> return Result.Failure(status, message) }
            .onSuccess { it.forEach { user ->
                    if (user.experiencePoints.value >= challenge.minimumValue.value) {
                        eventManager.createSharedEvent(user.username, challenge.sharedEvent)
                        logger.info("Created global challenge ${challenge.sharedEvent.name} for ${user.username}.")
                    }
                }
            }
        return Result.Success(Unit)
    }
}
