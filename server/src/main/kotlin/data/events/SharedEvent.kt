package main.kotlin.data.events

import main.kotlin.data.Group
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.EventCompletionDetail
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * This class models a shared event of the calendar, containing all the important information. Inherits from [Event],
 * offering a [group] and [start] date additionally.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
@SerialName("SharedEvent")
data class SharedEvent(
    override val eventID : Long,
    override var name : String,
    override var description : String,
    @Serializable(with = LocalDateTimeSerializer::class)
    var start : LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    override var end : LocalDateTime?,
    var group : Group,
    override var experiencePoints : ExperiencePoints,
    override var completed : EventCompletionDetail,
) : Event()
