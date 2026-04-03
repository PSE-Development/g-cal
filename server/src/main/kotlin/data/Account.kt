package main.kotlin.data

import kotlinx.serialization.Serializable

/**
 * This class holds all the information of a user of the app, containing the [username], the amount of [experiencePoints]
 * and the [experiencePointsToday].
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class Account(
    val username : String,
    var name : String,
    var experiencePoints : ExperiencePoints,
    var experiencePointsToday : ExperiencePoints
) {
    /**
     * Overrides toString() to allow for a more readable representation, mostly used for testing.
     */
    override fun toString(): String {
        return "Account(username='$username', name='$name', " +
                "totalExp=${experiencePoints.value}, todayExp=${experiencePointsToday.value})"
    }

}
