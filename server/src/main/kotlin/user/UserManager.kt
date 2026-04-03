package main.kotlin.user

import io.ktor.http.HttpStatusCode
import main.kotlin.data.Account
import main.kotlin.data.Result
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.RegularAccount
import main.kotlin.data.LeaderboardEntry
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import main.kotlin.database.Accounts
import main.kotlin.database.Groups
import main.kotlin.database.Friends
import main.kotlin.database.Events
import org.ktorm.dsl.eq
import org.ktorm.entity.any
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.dsl.update
import org.ktorm.dsl.delete
import org.ktorm.dsl.and
import org.ktorm.dsl.or
import org.ktorm.entity.filter
import org.ktorm.entity.sortedByDescending
import org.ktorm.entity.toList

import main.kotlin.event.ERROR_MESSAGE
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.map

/**
 * This class offers all access points to the database regarding user and friend management.
 */

class UserManager (db : Database) {
    private val database = db
    private val userAuthenticator = UserAuthenticator(database)

    /**
     * Performs a login for a user by validating the token, returning the corresponding [Account].
     * @param token the token
     * @param name the name of the user
     * @return [Result.Success] of the Account if successful, [Result.Failure] otherwise
     */
    fun loginUser(token : String, name : String) : Result<Account> {
        return userAuthenticator.loginUser(token, name)
    }

    /**
     * Authenticates a given token String and returns the corresponding [Account].
     * @param token the token
     * @return [Result.Success] of the Account if successful, [Result.Failure] otherwise
     */
    fun authenticateUser(token : String) : Result<Account> {
        return userAuthenticator.authenticateUser(token)
    }

    /**
     * Adds a given user [Account] to the database, if he does not already exist.
     * @param user the user account to add
     * @return [Result.Success] if successful, [Result.Failure] else
     */
    fun addUser(user : Account) : Result<Unit> {
        if (userExists(user.username)) return Result.Failure(HttpStatusCode.Forbidden, "Username already exists")
        return userAuthenticator.addUser(user)
    }

    /**
     * Returns the user information as the corresponding [Account] for a given username.
     * @param username the username
     * @return [Result.Success] of the Account if valid, [Result.Failure] else
     */
    fun getUserInfo(username : String) : Result<Account> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")

        val user = try {
            database.sequenceOf(Accounts)
                .find { it.username eq username }
                ?: return Result.Failure(HttpStatusCode.NotFound, "Could not get user information")
        } catch (e : Exception) {
            return Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
        val account = Account(user.username, user.name,
            ExperiencePoints(user.experiencePoints), ExperiencePoints(user.experiencePointsToday))
        return Result.Success(account)
    }

    /**
     * Updates the user information for a given username and replaces the current information (except the username)
     * of in the database.
     * @param username the username
     * @param updatedUser the updated user with the new information
     * @return [Result.Success] if successful, [Result.Failure] else
     */
    fun setUserInfo(username : String, updatedUser : Account) : Result<Unit> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")
        if (username != updatedUser.username) return Result.Failure(HttpStatusCode.Forbidden, "Cannot change username")

        return try {
            database.update(Accounts) {
                set(it.name, updatedUser.name)
                set(it.experiencePoints, updatedUser.experiencePoints.value)
                set(it.experiencePointsToday, updatedUser.experiencePointsToday.value)
                where { it.username eq username }
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Deletes a given user and all information of him from the database, such as his Events, his Groups and his
     * friendships
     * @param username the username of the user to delete
     * @return [Result.Success] if successful, [Result.Failure] else
     */
    fun deleteUserInfo(username : String) : Result<Unit> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")

        return try {
            database.useTransaction {
                database.delete(Friends) {
                    (it.initiatingUser eq username) or (it.befriendedUser eq username)
                }
                database.delete(Accounts) {
                    it.username eq username
                }
                database.delete(Events) {
                    (it.user eq username)
                }
                database.delete(Groups) {
                    (it.user eq username)
                }
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Adds a friend for a given user.
     * @param actingUserName the user sending the friend request
     * @param friendName the name of the user to add
     * @return [Result.Success] if successful, [Result.Failure] else
     */
    fun addFriend(actingUserName : String, friendName : String) : Result<Account> {
        if (!userExists(actingUserName) || !userExists(friendName))
            return Result.Failure(HttpStatusCode.NotFound,"User not found")
        if (actingUserName == friendName)
            return Result.Failure(HttpStatusCode.BadRequest, "Cannot add yourself")
        if (friendshipExists(actingUserName, friendName))
            return Result.Failure(HttpStatusCode.Forbidden, "Friendship already exists")

        return try {
            database.insert(Friends) {
                set(it.initiatingUser, actingUserName)
                set(it.befriendedUser, friendName)
            }
            val accountEntity = database.sequenceOf(Accounts).find { it.username eq friendName }
                ?: throw IllegalArgumentException("Friend does not exist")
            val account = Account(accountEntity.username, accountEntity.name,
                ExperiencePoints(accountEntity.experiencePoints),
                ExperiencePoints(accountEntity.experiencePointsToday))
            Result.Success(account)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Removes a given user as a friend from the acting user.
     * @param actingUserName the user removing the friend
     * @param friendName the name of the friend to be removed
     * @return [Result.Success] if successful, [Result.Failure] else
     */
    fun removeFriend(actingUserName : String, friendName : String) : Result<Unit> {
        return try {
            database.delete(Friends) {
                (it.initiatingUser eq actingUserName) and (it.befriendedUser eq friendName)
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns a List of [Account] of all the friends of a given user.
     * @param username the name of the user
     * @return [Result.Success] of the List of [Account]s if successful, [Result.Failure] else
     */
    fun getFriends(username : String) : Result<List<Account>> {
        if (!userExists(username)) return Result.Failure(HttpStatusCode.NotFound, "User not found")

        return try {
            val list = database.sequenceOf(Friends)
                .filter { it.initiatingUser eq username }
                .toList()
                .map { entity ->
                    val user = entity.befriendedUser
                    Account(user.username, user.name, ExperiencePoints(user.experiencePoints),
                        ExperiencePoints(user.experiencePointsToday))
                }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns a List of [LeaderboardEntry], which is an ordered list of users by their amount of
     * [Account.experiencePoints].
     * @return [Result.Success] of the List of [LeaderboardEntry]s if successful, [Result.Failure] else
     */
    fun getLeaderboardList() : Result<List<LeaderboardEntry>> {
        return try {
            val list = database.sequenceOf(Accounts)
                .sortedByDescending { it.experiencePoints }
                .toList()
                .mapIndexed { index, account ->
                    LeaderboardEntry(
                        placement = (index + 1),
                        username = account.username,
                        name = account.name,
                        experiencePoints = ExperiencePoints(account.experiencePoints)
                    )
                }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns all the users as a List of [Account].
     * @return [Result.Success] of the list of [Account] if successful, [Result.Failure] otherwise
     */
    fun getUserList() : Result<List<Account>> {
        return try {
            val list = database.sequenceOf(Accounts)
                .map {
                    Account(it.username, it.name,
                        ExperiencePoints(it.experiencePoints),
                        ExperiencePoints(it.experiencePointsToday))
                }
                .toList()
            Result.Success(list)

        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    /**
     * Returns whether a friendship between to users exists.
     * @param user1 name of the first user
     * @param user2 name of the second user
     * @return true if the friendship exists, false otherwise
     */
    fun friendshipExists(user1 : String, user2 : String) : Boolean {
        return try {
            database.sequenceOf(Friends)
                .any { (it.initiatingUser eq user1) and (it.befriendedUser eq user2) }
        } catch (_ : Exception) {
            false
        }
    }

    /**
     * Maps a List of [RegularAccount]s to a List of the corresponding [Account]s.
     * @param regularAccounts
     */
    fun mapRegularToAccount(regularAccounts : List<RegularAccount>) : Result<List<Account>> {
        return try {
            val list = regularAccounts.map { account ->
                val accountEntity = database.sequenceOf(Accounts).firstOrNull { it.username eq account.username }
                    ?: throw IllegalArgumentException("Could not find user ${account.username}")
                Account(accountEntity.username, accountEntity.name,ExperiencePoints(accountEntity.experiencePoints),
                    ExperiencePoints(accountEntity.experiencePointsToday))
            }
            Result.Success(list)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.NotFound, ERROR_MESSAGE.format(e.message))
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
}
