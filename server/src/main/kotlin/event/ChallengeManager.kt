package main.kotlin.event

import main.kotlin.data.Challenge
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import main.kotlin.data.events.SharedEvent
import main.kotlin.database.Events
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.any
import org.ktorm.entity.sequenceOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * This class manages the global challenges, being instantiated with a set list of global challenges, and rotating
 * through them every time the method is called.
 * To ensure uniqueness of the eventIDs of the [SharedEvent] inside of the [Challenge],
 * a random seed [RANDOM_SEED] is used.
 */

const val RANDOM_SEED = 25235324
const val WEEK_RANGE = 6

class ChallengeManager(db : Database) {
    val database = db
    val random = Random(RANDOM_SEED)
    val usedEventIDs = mutableSetOf<Long>()
    val challenges : MutableList<Challenge> = ArrayList()
    var rotationIndex = 0

    init {
        instantiateChallenges()
    }

    private fun instantiateChallenges() {
        val noGroup = Group(0, "NoGroup", 0)

        val eventID : Long = 0
        challenges.add(Challenge(ExperiencePoints(500),
            SharedEvent(eventID, "Digital Detox", "Take some time off your phone",
            LocalDateTime.of(2026, 1, 1, 18, 0, 0),
                LocalDateTime.of(2026, 1, 1, 20, 0, 0),
                noGroup, ExperiencePoints(20), EventCompletionDetail(eventID, false, null))))

        challenges.add(Challenge(ExperiencePoints(700),
            SharedEvent(eventID, "Spaziergang", "Genieße die Natur und mache einen Spaziergang",
                LocalDateTime.of(2026, 1, 1, 16, 0, 0),
                LocalDateTime.of(2026, 1, 1, 16, 30, 0),
                noGroup, ExperiencePoints(15), EventCompletionDetail(eventID, false, null))))

        challenges.add(Challenge(ExperiencePoints(0),
            SharedEvent(eventID, "Wasser trinken", "Trinke ein bisschen Wasser",
                LocalDateTime.of(2026, 1, 1, 14, 0, 0),
                LocalDateTime.of(2026, 1, 1, 14, 15, 0),
                noGroup, ExperiencePoints(5), EventCompletionDetail(eventID, false, null))))

        challenges.add(Challenge(ExperiencePoints(75),
            SharedEvent(eventID, "Week recap",
                "Mache einen Recap über deine letzten Wochen und deine Termine und Aufgaben",
                LocalDateTime.of(2026, 1, 1, 19, 0, 0),
                LocalDateTime.of(2026, 1, 1, 20, 0, 0),
                noGroup, ExperiencePoints(34), EventCompletionDetail(eventID, false, null))))
    }


    /**
     * Returns a global challenge by rotating through the preset list of [Challenge]s.
     * Since the challenges are only created once, the dates have to be changed accordingly.
     * At the time of creating the challenge, a random amount of days between 1 and 6 is added to ensure that the
     * challenges appear randomly. The time of day of the challenge stays the same.
     */
    fun getGlobalChallenge() : Challenge {
        val challenge = challenges[rotationIndex]

        val dayShift = (1..WEEK_RANGE).random().toLong()
        val targetDay = LocalDate.now().plusDays(dayShift)

        val duration = Duration.between(
            challenge.sharedEvent.start,
            challenge.sharedEvent.end
        )
        val newStart = LocalDateTime.of(targetDay, challenge.sharedEvent.start.toLocalTime())
        val newEnd = newStart.plus(duration)

        val newEventID = generateUniqueEventID()
        val oldEventCD = challenge.sharedEvent.completed
        val copyEvent = challenge.sharedEvent.copy(
            eventID = newEventID,
            start = newStart,
            end = newEnd,
            completed = EventCompletionDetail(newEventID, oldEventCD.isCompleted, oldEventCD.completionTime)
        )
        val copyChallenge = challenge.copy(sharedEvent = copyEvent)
        rotationIndex = (rotationIndex + 1) % challenges.size
        return copyChallenge
    }

    private fun generateUniqueEventID(): Long {
        var eventID : Long
        do {
            eventID = random.nextLong(0, ID_NUMBER_RANGE.toLong())
        } while (usedEventIDs.contains(eventID) || database.sequenceOf(Events).any { it.eventID eq eventID})
        return eventID
    }
}
