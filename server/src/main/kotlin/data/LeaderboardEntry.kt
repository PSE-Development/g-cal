package main.kotlin.data

import kotlinx.serialization.Serializable

/**
 * This class models a Leaderboard entry, containing a [placement], [username], [name] and the amount
 * of [experiencePoints]. The representation is used for ranking the users by level.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class LeaderboardEntry(
    val placement: Int,
    val username : String,
    val name : String,
    val experiencePoints : ExperiencePoints
) {
    /**
     * Overrides toString() to allow for a more readable representation, mostly used for testing.
     */
    override fun toString(): String {
        return "#$placement | $username | $name | ${experiencePoints.value} XP"
    }
}
