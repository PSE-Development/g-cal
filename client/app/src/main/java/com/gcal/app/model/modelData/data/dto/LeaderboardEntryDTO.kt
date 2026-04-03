package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.ExperiencePoints
import com.gcal.app.model.modelData.data.system.LeaderboardEntry
import com.gcal.app.model.modelData.data.system.NoXp
import com.gcal.app.model.modelData.data.system.XpValue
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntryDTO(
    val placement: Int,
    val username : String,
    val name: String,
    val experiencePoints : ExperiencePoints
) {
    /**
     * Overrides toString() to allow for a more readable representation, mostly used for testing.
     */
    override fun toString(): String {
        return "#$placement | $username | ${experiencePoints.value} XP"
    }
}

/**
 * Maps the object to a DTO.
 */
fun LeaderboardEntryDTO.toDomain(): LeaderboardEntry =
    LeaderboardEntry(
        rank = this.placement,
        username = this.username,
        name = this.name,
        totalXp = if (this.experiencePoints.value <= 0) {
            NoXp
        } else {
            XpValue(this.experiencePoints.value)
        }
    )