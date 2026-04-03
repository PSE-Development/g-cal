import io.mockk.every
import io.mockk.mockk
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Result
import main.kotlin.event.ResourceAuthorizer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.ktorm.database.Database

class AuthorizerTest : InitializeTests() {

    // test valid access to a group
    @Test
    fun validGroupAccess() {
        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val group = generateGroup()
        groupManager.createGroup(user.username, group)

        val res = resourceAuthorizer.authorizeGroup(user.username, group.groupID)
        Assertions.assertTrue(res is Result.Success)
    }

    // test invalid access to a group -> error
    @Test
    fun invalidGroupAccess() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val group = generateGroup()
        groupManager.createGroup(user1.username, group)

        val res = resourceAuthorizer.authorizeGroup(user2.username, group.groupID)
        val access = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertFalse(access)
    }

    // test group access for an invalid user -> error
    @Test
    fun testGroupAccessInvalidUser() {
        val res = resourceAuthorizer.authorizeGroup("name", 99L)
        val access = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertFalse(access)
    }

    // test valid access to an event
    @Test
    fun validEventAccess() {
        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val appointment = generateAppointment()
        eventManager.createAppointment(user.username, appointment)

        val res = resourceAuthorizer.authorizeEvent(user.username, appointment.eventID)
        Assertions.assertTrue(res is Result.Success)
    }

    // test invalid access to an event -> error
    @Test
    fun invalidEventAccess() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val appointment = generateAppointment()
        eventManager.createAppointment(user1.username, appointment)

        val res = resourceAuthorizer.authorizeEvent(user2.username, appointment.eventID)
        val access = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertFalse(access)
    }

    // test event access for an invalid user -> error
    @Test
    fun testEventAccessInvalidUser() {
        val res1 = resourceAuthorizer.authorizeEvent("name", 99L)
        var access = res1.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertFalse(access)

        val user = Account("test", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        eventManager.createAppointment(user.username, generateAppointment())
        val res2 = resourceAuthorizer.authorizeEvent("name", 99L)
        access = res2.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertFalse(access)
    }

    // test group access when the database is invalid -> error
    @Test
    fun testGroupAccessInvalidDB() {
        val db = mockk<Database>()
        every { db.dialect } throws RuntimeException("DB error")
        val authorizer = ResourceAuthorizer(db)

        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val res = authorizer.authorizeGroup(user.username, 0)
        Assertions.assertTrue(res is Result.Failure)
    }

    // test event access when the database is invalid -> error
    @Test
    fun testEventAccessInvalidDB() {
        val db = mockk<Database>()
        every { db.dialect } throws RuntimeException("DB error")
        val authorizer = ResourceAuthorizer(db)

        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val res = authorizer.authorizeEvent(user.username, 0)
        Assertions.assertTrue(res is Result.Failure)
    }
}