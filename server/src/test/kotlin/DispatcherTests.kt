import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import main.kotlin.data.Account
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.RegularAccount
import main.kotlin.data.Result
import main.kotlin.event.ID_NUMBER_RANGE
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DispatcherTests : InitializeTests() {

    @Test
    fun testChallengeSharing() {
        val user1 = Account("test1", "name", ExperiencePoints(1000), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)

        val res1 = dispatcher.createGlobalChallenge()
        Assertions.assertTrue(res1 is Result.Success)
        val res2 = eventManager.getSharedEvents(user1.username)
        var eventList = res2.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertTrue(eventList.any { it.name == "Digital Detox"} )

        val res3 = eventManager.getSharedEvents(user2.username)
        eventList = res3.getOrElse { _, err -> Assertions.fail(err) }
        Assertions.assertFalse(eventList.any { it.name == "Digital Detox"} )
    }

    // test sharing an event with users
    @Test
    fun eventSharingTest() {
        val user1 = Account("test1", "name", ExperiencePoints(1000), ExperiencePoints(0))
        userAuthenticator.addUser(user1)
        val user2 = Account("test2", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user2)
        userManager.addFriend(user1.username, user2.username)

        val sharedEvent = generateSharedEvent()
        val res = dispatcher.createSharedEventAndShare(user1.username, sharedEvent,
            listOf(RegularAccount("test2", "name", ExperiencePoints(0))))
        Assertions.assertTrue(res is Result.Success)
    }

    // test the creation of global challenges
    @Test
    fun testGlobalChallenge() {
        Assertions.assertEquals(0, challengeManager.rotationIndex)
        val challenge1 = challengeManager.getGlobalChallenge()
        Assertions.assertEquals("Digital Detox", challenge1.sharedEvent.name)
        Assertions.assertEquals(500, challenge1.minimumValue.value)
        Assertions.assertEquals(1, challengeManager.rotationIndex)

        val challenge2 = challengeManager.getGlobalChallenge()
        Assertions.assertEquals("Spaziergang", challenge2.sharedEvent.name)
        Assertions.assertEquals(700, challenge2.minimumValue.value)
        Assertions.assertEquals(2, challengeManager.rotationIndex)
    }

    @Test
    fun testUniquenessOfEventIDs() {
        val user = Account("USERNAME_1", "name", ExperiencePoints(0), ExperiencePoints(0))
        userAuthenticator.addUser(user)
        val eventID = 37130L
        val appointment = generateAppointment()
        val copyAppointment = appointment.copy(eventID = eventID)
        eventManager.createAppointment(user.username, copyAppointment)
        challengeManager.usedEventIDs.add(eventID)

        val challenge = challengeManager.getGlobalChallenge()
        println(challenge.sharedEvent.eventID)
        Assertions.assertNotEquals(eventID, challenge.sharedEvent.eventID)
    }
}
