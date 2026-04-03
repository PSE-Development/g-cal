package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.ExperiencePoints
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.data.system.toXP
import kotlinx.serialization.Serializable

@Serializable
data class PersonalUserDTO(
    var username : String,
    val name: String,
    var experiencePoints : ExperiencePoints,
    var experiencePointsToday : ExperiencePoints
)

/**
 * Maps the object to a DTO.
 */
fun PersonalUserDTO.toDomain(): PersonalUser =
    PersonalUser(
        username = this.username,
        name = this.name,
        totalXp = this.experiencePoints.toXP(),
        dailyXp = this.experiencePointsToday.toXP()
    )