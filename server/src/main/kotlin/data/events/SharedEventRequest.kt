package main.kotlin.data.events

import kotlinx.serialization.Serializable
import main.kotlin.data.RegularAccount

/**
 * A wrapper class to allow for efficient communication between client und server.
 * This DTO contains the [sharedEvent] as well as all the [users] to share it with.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class SharedEventRequest(
    val sharedEvent: SharedEvent,
    val users: List<RegularAccount>
)

