package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.ExperiencePoints
import com.gcal.app.model.modelData.data.system.LocalDateTimeSerializer
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.data.system.toXP
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserToDoDTO(
    val eventID: Long,
    val name: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val end: LocalDateTime?,
    val experiencePoints: ExperiencePoints,
    val completed: EventCD
)

fun UserToDoDTO.toDomain(): UserToDo =
    UserToDo(
        id = this.eventID,
        name = this.name,
        description = this.description,
        deadline = this.end,
        xpValue = this.experiencePoints.toXP(),
        detail = this.completed
    )