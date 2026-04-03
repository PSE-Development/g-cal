package main.kotlin.data
import java.time.LocalDateTime

import kotlinx.serialization.Serializable
import main.kotlin.data.events.LocalDateTimeSerializer

/**
 * This class encapsulates all information regarding the completion of an event, such as
 * the [eventId], a [isCompleted] flag and the (possible) date of completion. [completionTime] is nullable since
 * some events are still unfinished.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class EventCompletionDetail(
    var eventId : Long,
    var isCompleted : Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    var completionTime : LocalDateTime?
)