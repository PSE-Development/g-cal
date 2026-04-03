package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelData.data.dto.SharedEventRequestDTO
import kotlinx.serialization.Serializable

/**
 * Represents a request to create or update an event shared with multiple users.
 * Contains the event details and the list of participating recipients.
 */
@Serializable
data class SharedEventRequest(
    val eventDetails: UserSharedEvent,
    val participants: List<RegularUser>
)

fun SharedEventRequest.toDTO(): SharedEventRequestDTO =
    SharedEventRequestDTO(
        sharedEvent = this.eventDetails.toDTO(),
        users = this.participants.map { it.toDTO() }
    )