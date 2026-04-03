package main.kotlin.user

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.HttpStatusCode
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import main.kotlin.data.Result
import main.kotlin.database.Accounts
import main.kotlin.database.Groups
import main.kotlin.event.ERROR_MESSAGE
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.any
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf

const val NO_GROUP_ID : Long = 0
const val NO_GROUP_COLOUR = 0
const val NO_GROUP_NAME = "None"

const val INITIAL_EXPERIENCE_POINTS = 0

const val EMAIL_CLAIM = "email"
const val EXPIRATION_CLAIM = "exp"
const val TIME_TRANSLATION_FACTOR = 1000

/**
 * Authenticator Class to receive and issue Tokens.
 */
class UserAuthenticator(db: Database) {
    private val database = db

    /**
     * Logs a user in by authenticating his token. If the user did not already exist in the database, he is added.
     * If the login was successful, the corresponding [Account] is returned.
     * @param token the token
     * @name name the name of the user, used for inserting into the database
     * @return [Result.Success] of the [Account] if successful, [Result.Failure] otherwise
     */
    fun loginUser(token : String, name : String) : Result<Account> {
        val username = validateToken(token).getOrElse { status, err -> run {
            return Result.Failure(status, err)
        } }
        val user = getUserAccount(username) ?: run {
            val newUser = Account(
                username, name, ExperiencePoints(INITIAL_EXPERIENCE_POINTS),
                ExperiencePoints(INITIAL_EXPERIENCE_POINTS)
            )
            addUser(newUser).getOrElse { status, err -> return Result.Failure(status, err) }
            newUser
        }
        return Result.Success(user)
    }

    /**
     * Authenticates a user by validating its token. If the validation was successful, the corresponding [Account] is
     * returned.
     * @param token the token
     * @return [Result.Success] of the [Account] if successful, [Result.Failure] otherwise
     */
    fun authenticateUser(token : String) : Result<Account> {
        val username = validateToken(token).getOrElse { status, err -> return Result.Failure(status, err) }
        val user = getUserAccount(username) ?: return Result.Failure(HttpStatusCode.BadRequest, "User does not exist")
        return Result.Success(user)
    }

    /**
     * Validates a given token by decoding the JWT and extracting the username. If the token is expired, it is invalid.
     * @param token the token
     * @return [Result.Success] of the username if successful, [Result.Failure] otherwise
     */
    private fun validateToken(token : String) : Result<String> {
        var decoded : DecodedJWT
        try {
            decoded = JWT.decode(token)
        } catch (e : Exception) {
            return Result.Failure(HttpStatusCode.BadRequest,"Could not decode token because of ${e.message}")
        }
        val username = decoded.getClaim(EMAIL_CLAIM).asString()
            ?: return Result.Failure(HttpStatusCode.Forbidden, "No email claim found")
        val expired = decoded.getClaim(EXPIRATION_CLAIM).asInt()
            ?: return Result.Failure(HttpStatusCode.Forbidden, "No expired claim found")
        if (expired < (System.currentTimeMillis() / TIME_TRANSLATION_FACTOR)) {
            return Result.Failure(HttpStatusCode.Forbidden, "Token expired")
        }
        return Result.Success(username)
    }

    /**
     * Adds a given user [Account] to the database, if he does not already exist.
     * Adds a specific [Group] with [Group.groupID] 0 to model the group of events that have no assigned group.
     * The updating/deleting operations are considered successful even if no entry gets deleted/updated, as long as no
     * error occurs, which results in returning [Result.Success]
     * @param user the user account to add
     * @return [Result.Success] if successful, [Result.Failure] else
     */
    fun addUser(user : Account) : Result<Unit> {
        if (userExists(user.username)) {
            return Result.Failure(HttpStatusCode.BadRequest, "User already exists")
        }
        
        return try {
            database.useTransaction {
                database.insert(Accounts) {
                    set(it.username, user.username)
                    set(it.name, user.name)
                    set(it.experiencePoints, user.experiencePoints.value)
                    set(it.experiencePointsToday, user.experiencePointsToday.value)
                }
                val noGroupExists = database.sequenceOf(Groups).any {
                    (it.groupID eq NO_GROUP_ID) and (it.user eq user.username) }
                if (!noGroupExists) {
                    database.insert(Groups) {
                        set(it.groupID, NO_GROUP_ID)
                        set(it.name, NO_GROUP_NAME)
                        set(it.colour, NO_GROUP_COLOUR)
                        set(it.user, user.username)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e : Exception) {
            Result.Failure(HttpStatusCode.BadRequest, ERROR_MESSAGE.format(e.message))
        }
    }

    private fun getUserAccount(username : String) : Account? {
        val entity = database.sequenceOf(Accounts).firstOrNull { it.username eq username }
            ?: return null

        val account =  Account(entity.username, entity.name, ExperiencePoints(entity.experiencePoints),
            ExperiencePoints(entity.experiencePointsToday))
        return account
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
