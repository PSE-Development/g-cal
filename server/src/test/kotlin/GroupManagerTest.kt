import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import main.kotlin.data.Result
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GroupManagerTest : InitializeTests() {

    // test adding a new group
    @Test
    fun addGroup() {
        val user = Account("test", "name", ExperiencePoints(0), ExperiencePoints(0))
        val group = generateGroup()
        val res1 = groupManager.createGroup(user.username, group)
        Assertions.assertTrue(res1 is Result.Failure)
        userAuthenticator.addUser(user)

        val res2 = groupManager.createGroup(user.username, group)
        Assertions.assertTrue(res2 is Result.Success)

        Assertions.assertTrue(groupManager.getGroups(user.username).getOrElse { _, err -> Assertions.fail(err) }
            .any { it.groupID == group.groupID})
    }

    // try adding an already existing group -> error
    @Test
    fun addExistingGroup() {
        val user = Account("test", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val group = Group(5, "duplicate", 51)
        groupManager.createGroup(user.username, group)
        val res = groupManager.createGroup(user.username, group)
        Assertions.assertTrue(res is Result.Failure)
    }

    // get the groups of a user
    @Test
    fun getGroups() {
        val user = Account("test", "name", ExperiencePoints(0), ExperiencePoints(0))
        val res1 = groupManager.getGroups(user.username)
        Assertions.assertTrue(res1 is Result.Failure)

        userAuthenticator.addUser(user)

        val group1 = Group(5, "test1", 51)
        val group2 = Group(6, "test2", 51)
        groupManager.createGroup(user.username, group1)
        groupManager.createGroup(user.username, group2)
        val res2 = groupManager.getGroups(user.username)
        val groupList = res2.getOrElse { _, err -> Assertions.fail(err) }

        Assertions.assertTrue(groupList.contains(group1))
        Assertions.assertTrue(groupList.contains(group2))
    }

    // test to delete a group
    @Test
    fun deleteGroup() {
        val user1 = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val group = generateGroup()
        groupManager.createGroup(user1.username, group)

        val res1 = groupManager.deleteGroup(user1.username, group.groupID)
        Assertions.assertTrue(res1 is Result.Success)

        Assertions.assertTrue(groupManager.getGroups(user1.username).getOrElse { _, err -> Assertions.fail(err) }
            .none { it.groupID == group.groupID })

        val user2 = Account("USERNAME_2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val res2 = groupManager.deleteGroup(user2.username, group.groupID)
        Assertions.assertTrue(res2 is Result.Failure)
    }

    // test to delete the NoGroup -> error
    @Test
    fun deleteNoGroup() {
        val user = Account("test", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)

        val res = groupManager.deleteGroup(user.username, 0)
        Assertions.assertTrue(res is Result.Failure)

        Assertions.assertTrue(groupManager.getGroups(user.username).getOrElse { _, err -> Assertions.fail(err) }
            .any { it.groupID == 0L })
    }

    // test updating information of a group
    @Test
    fun updateGroup() {
        val user1 = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val group = generateGroup()
        groupManager.createGroup(user1.username, group)

        val updatedName = "changedName"
        group.name = updatedName
        group.colour = 5
        val res1 = groupManager.updateGroup(user1.username, group)
        Assertions.assertTrue(res1 is Result.Success)

        Assertions.assertTrue(groupManager.getGroups(user1.username).getOrElse { _, err -> Assertions.fail(err) }
            .any { (it.groupID == group.groupID) && (it.name == group.name) && (it.colour == group.colour) })

        val res2 = groupManager.updateGroup(user1.username, Group(0, "changed", 5))
        Assertions.assertTrue(res2 is Result.Failure)

        val user2 = Account("USERNAME_2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val res3 = groupManager.updateGroup(user2.username, group)
        Assertions.assertTrue(res3 is Result.Failure)
    }
}
