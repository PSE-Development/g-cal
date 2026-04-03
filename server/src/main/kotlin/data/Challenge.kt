package main.kotlin.data

import main.kotlin.data.events.SharedEvent

/**
 * The class models a global challenge, containing the [sharedEvent] for all the users and a [minimumValue] of
 * [ExperiencePoints] for users to be able to participate.
 */
data class Challenge(
    val minimumValue : ExperiencePoints,
    val sharedEvent : SharedEvent
)