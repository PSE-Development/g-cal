package main.kotlin.data.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import main.kotlin.data.EventCompletionDetail
import main.kotlin.data.ExperiencePoints
import main.kotlin.data.Group
import java.time.LocalDateTime

/**
 * This class models an appointment of the calendar, containing all the important information. Inherits from [Event],
 * offering a [group] and [start] date additionally.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
@SerialName("Appointment")
data class Appointment(
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