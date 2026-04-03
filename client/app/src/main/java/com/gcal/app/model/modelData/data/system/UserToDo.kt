package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelData.data.dto.UserToDoDTO
import com.gcal.app.model.modelFacade.general.CompletionDetail
import com.gcal.app.model.modelFacade.general.ToDo
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Represents a personal task or to-do item within the system.
 * Implements [ToDo] to provide standardized access to deadlines and completion status.
 */
@Serializable
data class UserToDo(
    private val id: Long,
    private val name: String,
    private val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    private val deadline: LocalDateTime?,
    private val xpValue: XP,
    private val detail: EventCD
) : ToDo {

    override fun eventID(): Long = id

    override fun eventName(): String = name

    override fun description(): String = description

    override fun end(): LocalDateTime? = deadline

    override fun experiencePoints(): XP = xpValue

    override fun checkCompletion(): CompletionDetail = detail
}

fun UserToDo.toDTO(): UserToDoDTO =
    UserToDoDTO(
        eventID = this.eventID(),
        name = this.eventName(),
        description = this.description(),
        end = this.end(),
        experiencePoints = when(this.experiencePoints()) {
            is NoXp -> ExperiencePoints(0)
            is XpValue -> ExperiencePoints(this.experiencePoints().value())
        },
        completed = this.checkCompletion() as EventCD
    )

