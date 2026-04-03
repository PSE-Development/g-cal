import io.mockk.every
import io.mockk.mockk
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.RegularAccount
import main.kotlin.data.Result
import main.kotlin.user.UserManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.ktorm.database.Database

class UserManagerTest : InitializeTests() {

    // test adding a user and retrieving its information
    @Test
    fun addingUser() {
        val user = Account("test1@gmail.com", "test", ExperiencePoints(20), ExperiencePoints(25))
        val res1 = userManager.addUser(user)
        Assertions.assertTrue(res1 is Result.Success)

        val res2 = userManager.getUserInfo(user.username)
        Assertions.assertTrue(res2 is Result.Success)
        Assertions.assertEquals(res2.getOrElse { _, err -> Assertions.fail(err) }.username, user.username)
    }

    // test updating information of a user
    @Test
    fun updateUser() {
        val user = Account("test1@gmail.com", "test", ExperiencePoints(20), ExperiencePoints(25))
        userManager.addUser(user)

        val newName = "test_changed"
        user.name = newName
        user.experiencePoints = ExperiencePoints(100)
        user.experiencePointsToday = ExperiencePoints(150)
        val res1 = userManager.setUserInfo(user.username, user)
        Assertions.assertTrue(res1 is Result.Success)

        val res2 = userManager.getUserInfo(user.username)
        val retrievedUser = res2.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(newName, retrievedUser.name)
        Assertions.assertEquals(100, retrievedUser.experiencePoints.value)
    }

    // test deleting a user
    @Test
    fun deleteUser() {
        val user = Account("test1", "name", ExperiencePoints(1000), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val res1 = userManager.deleteUserInfo(user.username)
        Assertions.assertTrue(res1 is Result.Success)

        val res2 = userManager.getUserInfo(user.username)
        Assertions.assertTrue(res2 is Result.Failure)
    }

    // test deleting a user who has events and groups (testing foreign key constraints)
    @Test
    fun deleteUserWithEvents() {
        val user = Account("test1", "name", ExperiencePoints(1000), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        eventManager.createAppointment(user.username, generateAppointment())
        groupManager.createGroup(user.username, generateGroup())

        val res1 = userManager.deleteUserInfo(user.username)
        Assertions.assertTrue(res1 is Result.Success)

        val res2 = userManager.getUserInfo(user.username)
        Assertions.assertTrue(res2 is Result.Failure)
    }

    // adds another user as a friend and retrieves friend list, asserting that it contains the other user
    @Test
    fun addFriend() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val res = userManager.addFriend(user1.username, user2.username)
        val friend = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(friend.username, user2.username)

        val res2 = userManager.getFriends(user1.username)
        val friendList = res2.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(friendList.any { it.username == user2.username })
    }

    // test adding a user, which is already a friend -> err
    @Test
    fun addAlreadyBefriendedUser() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        userManager.addFriend(user1.username, user2.username)
        val res = userManager.addFriend(user1.username, user2.username)
        Assertions.assertTrue(res is Result.Failure)
    }

    // test adding a non-existing user -> error
    @Test
    fun addNonExistingFriend() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)

        val res = userManager.addFriend(user1.username, "none")
        Assertions.assertTrue(res is Result.Failure)
    }

    // tests removing a friend
    @Test
    fun removeFriend() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)
        userManager.addFriend(user1.username, user2.username)

        val res1 = userManager.removeFriend(user1.username, user2.username)
        Assertions.assertTrue(res1 is Result.Success)

        val res2 = userManager.getFriends(user1.username)
        val friendList = res2.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(friendList.none { it.username == user2.username })
    }

    @Test
    fun testNonExistentUser() {
        val username = "nonexistent"
        val res1 = userManager.getFriends(username)
        Assertions.assertTrue(res1 is Result.Failure)
        val res2 = userManager.getUserInfo(username)
        Assertions.assertTrue(res2 is Result.Failure)
        val res3 = userManager.setUserInfo(username, Account(username, "test", ExperiencePoints(0), ExperiencePoints(0)))
        Assertions.assertTrue(res3 is Result.Failure)
    }

    @Test
    fun testAddingFriend() {
        val username1 = "user1"
        val user1 = Account(username1, "name", ExperiencePoints(0), ExperiencePoints(0))
        val username2 = "user2"
        val user2 = Account(username2, "name", ExperiencePoints(0), ExperiencePoints(0))
        val res1 = userManager.addFriend(username1, username2)
        Assertions.assertTrue(res1 is Result.Failure)
        userAuthenticator.addUser(user1)
        val res2 = userManager.addFriend(username1, username1)
        Assertions.assertTrue(res2 is Result.Failure)
        val res3 = userManager.addFriend(username1, username2)
        Assertions.assertTrue(res3 is Result.Failure)

        userAuthenticator.addUser(user2)
        val res4 = userManager.addFriend(username1, username2)
        Assertions.assertTrue(res4 is Result.Success)
        val res5 = userManager.addFriend(username1, username2)
        Assertions.assertTrue(res5 is Result.Failure)
    }

    // tests getting the list of all the users
    @Test
    fun getUserList() {
        val user1 = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val res = userManager.getUserList()
        val userList = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(2, userList.size)
        Assertions.assertTrue(userList.contains(user1))
        Assertions.assertTrue(userList.contains(user2))
    }

    // tests getting the leaderboard, asserting that it is sorted by experience points descendingly
    @Test
    fun getLeaderboard() {
        val username1 = "user1s"
        val user1 = Account(username1, "name", ExperiencePoints(100), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val username2 = "user2s"
        val user2 = Account(username2, "name", ExperiencePoints(20), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val res = userManager.getLeaderboardList()
        val leaderboardList = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertEquals(2, leaderboardList.size)
        val first = leaderboardList.first()
        val last = leaderboardList.last()
        Assertions.assertTrue(first.placement == 1)
        Assertions.assertTrue(first.username == user1.username)
        Assertions.assertTrue(first.name == user1.name)
        Assertions.assertTrue(first.experiencePoints.value == user1.experiencePoints.value)

        Assertions.assertTrue(last.placement == 2)
        Assertions.assertTrue(last.username == user2.username)
        Assertions.assertTrue(last.name == user2.name)
        Assertions.assertTrue(last.experiencePoints.value == user2.experiencePoints.value)
    }

    @Test
    fun testFriendshipChecking() {
        val user1 = Account("username", "name", ExperiencePoints(100), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account( "nonexistent", "name", ExperiencePoints(100), ExperiencePoints(0))

        Assertions.assertFalse(userManager.friendshipExists(user1.username, user2.username))
        Assertions.assertFalse(userManager.friendshipExists(user2.username, user1.username))

        userAuthenticator.addUser(user2)
        userManager.addFriend(user1.username, user2.username)
        Assertions.assertTrue(userManager.friendshipExists(user1.username, user2.username))
    }

    @Test
    fun addExistingUser () {
        val user = Account("username", "name", ExperiencePoints(100), ExperiencePoints(0))
        userManager.addUser(user)
        val res = userManager.addUser(user)
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun testValidAuthentification() {
        userAuthenticator.loginUser(TOKEN_1, "name")
        val res = userManager.authenticateUser(TOKEN_1)
        Assertions.assertTrue(res is Result.Success)
    }

    @Test
    fun testInvalidAuthentification() {
        val res = userManager.authenticateUser(TOKEN_INVALID)
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun testValidAccountMapping() {
        val user = RegularAccount(USERNAME_1, "name", ExperiencePoints(0))
        user.name = "newName"
        user.experiencePoints = ExperiencePoints(user.experiencePoints.value + 100)
        userAuthenticator.loginUser(TOKEN_1, user.name)
        val res = userManager.mapRegularToAccount(listOf(user))
        val userList = res.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(userList.any { (it.username == user.username) && (it.name == user.name)
                && (it.experiencePoints.value == 0) })
    }

    @Test
    fun testInvalidAccountMapping() {
        val user = RegularAccount(USERNAME_1, "name", ExperiencePoints(0))
        val res = userManager.mapRegularToAccount(listOf(user))
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun invalidUserUpdate() {
        val user = Account(USERNAME_1, "name", ExperiencePoints(0), ExperiencePoints(0))
        userManager.loginUser(TOKEN_1, user.name)
        val res = userManager.setUserInfo(user.username, user.copy(username="test"))
        Assertions.assertTrue(res is Result.Failure)
    }

    @Test
    fun testInvalidUserManagerDB() {
        val db = mockk<Database>()
        every { db.dialect } throws RuntimeException("DB error")
        every { db.executeQuery(any()) } throws RuntimeException("DB Error")
        val manager = UserManager(db)

        val user = Account("test1", "name", ExperiencePoints(0), ExperiencePoints(0))
        val res4 = manager.getUserInfo(user.username)
        Assertions.assertTrue(res4 is Result.Failure)
        val res5 = manager.setUserInfo(user.username, user)
        Assertions.assertTrue(res5 is Result.Failure)
        val res6 = manager.deleteUserInfo(user.username)
        Assertions.assertTrue(res6 is Result.Failure)
        val res7 = manager.addFriend(user.username, "friend")
        Assertions.assertTrue(res7 is Result.Failure)
        val res8 = manager.removeFriend(user.username, "friend")
        Assertions.assertTrue(res8 is Result.Failure)
        val res9 = manager.getFriends(user.username)
        Assertions.assertTrue(res9 is Result.Failure)
        val res10 = manager.getLeaderboardList()
        Assertions.assertTrue(res10 is Result.Failure)
        val res11 = manager.getUserList()
        Assertions.assertTrue(res11 is Result.Failure)
        val res12 = manager.friendshipExists(user.username, "friend")
        Assertions.assertFalse(res12)
        val res13 = manager.mapRegularToAccount(listOf(RegularAccount(user.username, user.name, user.experiencePoints)))
        Assertions.assertTrue(res13 is Result.Failure)
    }
}
