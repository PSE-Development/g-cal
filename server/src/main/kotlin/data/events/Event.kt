package main.kotlin.data.events

import main.kotlin.data.EventCompletionDetail
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import main.kotlin.data.ExperiencePoints

/**
 * This class models the superclass Events, containing all the important information.
 * [Appointment], [ToDo], [SharedEvent] inherit from this class.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
sealed class Event {
    abstract val eventID: Long
    abstract var name: String
    abstract var description: String
    abstract var end: LocalDateTime?
    abstract var experiencePoints: ExperiencePoints
    abstract var completed: EventCompletionDetail
}
