package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelData.data.dto.PersonalUserDTO
import com.gcal.app.model.modelFacade.general.User
import kotlinx.serialization.Serializable

/**
 * Represents the authenticated local user, including their total and daily experience progress.
 */
@Serializable
data class PersonalUser(
    val username: String,
    val name: String,
    val totalXp: XP,
    val dailyXp: XP
) : User {

    override fun username(): String = username

    override fun name(): String = name

    override fun experiencePoints(): XP = totalXp

    fun dailyProgress(): XP = dailyXp
}

fun PersonalUser.toDTO(): PersonalUserDTO =
    PersonalUserDTO(
        username = this.username(),
        name = this.name(),
        experiencePoints = when(this.experiencePoints()) {
            is NoXp -> ExperiencePoints(0)
            is XpValue -> ExperiencePoints(experiencePoints().value())
        },
        experiencePointsToday = when(this.dailyProgress()) {
            is NoXp -> ExperiencePoints(0)
            is XpValue -> ExperiencePoints(dailyProgress().value())
        }
    )
