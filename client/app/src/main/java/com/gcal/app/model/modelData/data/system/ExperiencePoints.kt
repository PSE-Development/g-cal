package com.gcal.app.model.modelData.data.system

import kotlinx.serialization.Serializable

@Serializable
data class ExperiencePoints(
    var value: Int
)

fun ExperiencePoints.toXP(): XP =
    if (value <= 0) NoXp
    else XpValue(value)