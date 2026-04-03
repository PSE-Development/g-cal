package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelData.data.dto.GroupDTO
import com.gcal.app.model.modelData.data.dto.UserAppointmentDTO
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.CompletionDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import kotlin.compareTo

/**
 * Implementation of [Appointment] representing a user-specific calendar event.
 * Includes group association, timing, and completion tracking.
 */
@Serializable
@SerialName("Appointment")
data class UserAppointment(
    private val id: Long,
    private val name: String,
    private val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    private val startAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    private val endAt: LocalDateTime,
    private val group: Group,
    private val xpValue: XP,
    private val detail: EventCD
) : Appointment {

    override fun eventID(): Long = id

    override fun eventName(): String = name

    override fun description(): String = description

    override fun start(): LocalDateTime = startAt

    override fun end(): LocalDateTime = endAt

    override fun group(): Group = group

    override fun experiencePoints(): XP = xpValue

    override fun checkCompletion(): CompletionDetail = detail
}

fun UserAppointment.toDTO(): UserAppointmentDTO =
    UserAppointmentDTO(
        eventID = this.eventID(),
        name = this.eventName(),
        description = this.description(),
        start = this.start(),
        end = this.end(),
        group = when (this.group()) {
            is UserGroup -> GroupDTO(
                groupID = this.group().groupId(),
                name = this.group().groupName(),
                colour = this.group().groupColour()
            )
            is NoGroup -> GroupDTO(
                groupID = 0,
                name = "None",
                colour = 0
            )
        },
        experiencePoints = when(this.experiencePoints()) {
            is NoXp -> ExperiencePoints(0)
            is XpValue -> ExperiencePoints(this.experiencePoints().value())
        },
        completed = this.checkCompletion() as EventCD
    )
