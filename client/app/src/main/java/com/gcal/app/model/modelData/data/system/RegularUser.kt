package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelData.data.dto.RegularUserDTO
import com.gcal.app.model.modelFacade.general.User
import kotlinx.serialization.Serializable

/**
 * Represents a standard user within the system, typically used for friends or group members.
 */
@Serializable
data class RegularUser(
    val username: String,
    val name: String,
    val totalXp: XP
) : User {

    override fun username(): String = username

    override fun name(): String = name

    override fun experiencePoints(): XP = totalXp
}

fun RegularUser.toDTO(): RegularUserDTO =
    RegularUserDTO(
        username = this.username(),
        name = this.name(),
        experiencePoints = when(this.experiencePoints()) {
            is XpValue -> ExperiencePoints(this.experiencePoints().value())
            is NoXp -> ExperiencePoints(0)
        }
    )
