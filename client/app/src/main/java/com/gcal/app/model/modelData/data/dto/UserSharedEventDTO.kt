package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.ExperiencePoints
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.LocalDateTimeSerializer
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.data.system.toXP
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserSharedEventDTO(
    val eventID: Long,
    val name: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val start: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val end: LocalDateTime,
    val group: GroupDTO,
    val experiencePoints: ExperiencePoints,
    val completed: EventCD
)

fun UserSharedEventDTO.toDomain(): UserSharedEvent =
    UserSharedEvent(
        id = this.eventID,
        name = this.name,
        description = this.description,
        startAt = this.start,
        endAt = this.end,
        group = this.group.toDomain(),
        xpValue = this.experiencePoints.toXP(),
        detail = this.completed
    )