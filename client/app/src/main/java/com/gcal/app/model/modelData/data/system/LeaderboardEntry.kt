package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelData.data.dto.LeaderboardEntryDTO
import com.gcal.app.model.modelFacade.general.User
import kotlinx.serialization.Serializable

/**
 * Represents a user's standing on the global leaderboard.
 */
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val name: String,
    val totalXp: XP
) : User {

    override fun username(): String = username

    override fun name(): String = name

    override fun experiencePoints(): XP = totalXp

    fun rank(): Int = rank
}

fun LeaderboardEntry.toDTO(): LeaderboardEntryDTO = LeaderboardEntryDTO(
    placement = rank(),
    username = username(),
    name = name(),
    experiencePoints = when(experiencePoints()) {
        is NoXp -> ExperiencePoints(0)
        is XpValue -> ExperiencePoints(experiencePoints().value())
    }
)