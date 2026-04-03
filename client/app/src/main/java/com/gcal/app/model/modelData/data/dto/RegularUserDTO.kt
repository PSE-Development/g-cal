package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.ExperiencePoints
import com.gcal.app.model.modelData.data.system.RegularUser
import com.gcal.app.model.modelData.data.system.toXP
import kotlinx.serialization.Serializable

@Serializable
data class RegularUserDTO(
    var username : String,
    val name: String,
    var experiencePoints : ExperiencePoints,
)

fun RegularUserDTO.toDomain(): RegularUser = RegularUser(
    username = this.username,
    name = this.name,
    totalXp = this.experiencePoints.toXP()
)