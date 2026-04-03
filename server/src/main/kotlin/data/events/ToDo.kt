package main.kotlin.data.events

import main.kotlin.data.EventCompletionDetail
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import main.kotlin.data.ExperiencePoints

/**
 * This class models a to-Do of the calendar, containing all the important information. Inherits from [Event],
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
@SerialName("ToDo")
data class ToDo(
    override val eventID : Long,
    override var name : String,
    override var description : String,
    @Serializable(with = LocalDateTimeSerializer::class)
    override var end : LocalDateTime?,
    override var experiencePoints : ExperiencePoints,
    override var completed : EventCompletionDetail,
) : Event()