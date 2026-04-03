import io.mockk.every
import io.mockk.mockk
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Result
import main.kotlin.database.Groups
import main.kotlin.user.NO_GROUP_COLOUR
import main.kotlin.user.NO_GROUP_ID
import main.kotlin.user.NO_GROUP_NAME
import main.kotlin.user.UserAuthenticator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.insert

class UserAuthenticatorTest : InitializeTests() {

    // test user login with a valid token
    @Test
    fun loginUser() {
        val res = userAuthenticator.loginUser(TOKEN_1, "test")
        val user = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(USERNAME_1, user.username)
        Assertions.assertEquals("test", user.name)
    }

    @Test
    fun loginMultipleTimes() {
        userAuthenticator.loginUser(TOKEN_1, "test")
        val res = userAuthenticator.loginUser(TOKEN_1, "test")
        Assertions.assertTrue(res is Result.Success)
    }

    // test user login with an invalid token -> error
    @Test
    fun invalidLogin() {
        val res = userAuthenticator.loginUser(TOKEN_INVALID, "test")
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun noEmailClaimTokenLogin() {
        val res = userAuthenticator.loginUser(TOKEN_NO_EMAIL_CLAIM, "test")
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun authorizeTokenWithNoExp() {
        val res = userAuthenticator.authenticateUser(TOKEN_NO_EXP_CLAIM)
        Assertions.assertTrue(res is Result.Failure)
    }

    // test authorizing a valid token
    @Test
    fun authorizeUser() {
        userAuthenticator.loginUser(TOKEN_1, "test")

        val res = userAuthenticator.authenticateUser(TOKEN_1)
        val user = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(USERNAME_1, user.username)
    }

    // test authorizing an invalid token -> error
    @Test
    fun invalidTokenAuthorization() {
        userAuthenticator.loginUser(TOKEN_1, "test")
        val res1 = userAuthenticator.authenticateUser(TOKEN_INVALID)
        Assertions.assertTrue(res1 is Result.Failure)

        val res2 = userAuthenticator.authenticateUser(TOKEN_EXPIRED)
        Assertions.assertTrue(res2 is Result.Failure)

        val res3 = userAuthenticator.authenticateUser(TOKEN_NON_EXISTENT)
        Assertions.assertTrue(res3 is Result.Failure)
    }

    // test adding a new user
    @Test
    fun addUser() {
        val user = Account("test", "name", ExperiencePoints(0), ExperiencePoints(0))
        val res = userAuthenticator.addUser(user)
        Assertions.assertTrue(res is Result.Success)
    }

    @Test
    fun addUserGroup() {
        val user = Account("same_name", "name1", ExperiencePoints(0), ExperiencePoints(0))
        database.useConnection { conn -> conn.createStatement().execute("SET REFERENTIAL_INTEGRITY FALSE") }
        database.insert(Groups) {
            set(it.groupID, NO_GROUP_ID)
            set(it.name, NO_GROUP_NAME)
            set(it.colour, NO_GROUP_COLOUR)
            set(it.user, user.username)
        }
        val res = userAuthenticator.addUser(user)
        Assertions.assertTrue(res is Result.Success)
        database.useConnection { conn -> conn.createStatement().execute("SET REFERENTIAL_INTEGRITY TRUE") }
    }

    // test adding an already existing user -> error
    @Test
    fun addAlreadyExistingUser() {
        val user1 = Account("same_name", "name1", ExperiencePoints(0), ExperiencePoints(0))
        val user2 = Account("same_name", "name2", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val res = userAuthenticator.addUser(user2)
        Assertions.assertTrue(res is Result.Failure)
    }

    // tests adding user with an invalid database -> error
    @Test
    fun testInvalidUserDB() {
        val db = mockk<Database>()
        every { db.dialect } throws RuntimeException("DB error")
        val authenticator = UserAuthenticator(db)

        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        val res = authenticator.addUser(user)
        Assertions.assertTrue(res is Result.Failure)
    }
}
