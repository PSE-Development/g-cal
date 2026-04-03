package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.ExperiencePoints
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.LocalDateTimeSerializer
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.data.system.toXP
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserAppointmentDTO(
    val eventID : Long,
    var name : String,
    var description : String,
    @Serializable(with = LocalDateTimeSerializer::class)
    var start : LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    var end : LocalDateTime,
    var group : GroupDTO,
    var experiencePoints: ExperiencePoints,
    var completed : EventCD,
)

fun UserAppointmentDTO.toDomain(): UserAppointment =
    UserAppointment(
        id = this.eventID,
        name = this.name,
        description = this.description,
        startAt = this.start,
        endAt = this.end,
        group = this.group.toDomain(),
        xpValue = this.experiencePoints.toXP(),
        detail = completed
    )