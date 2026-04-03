package main.kotlin.data

import kotlinx.serialization.Serializable

/**
 * This class holds limited information of a user of the app, containing the [username], the amount of [experiencePoints]
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class RegularAccount(
    val username : String,
    var name : String,
    var experiencePoints : ExperiencePoints,
) {
    /**
     * Overrides toString() to allow for a more readable representation, mostly used for testing.
     */
    override fun toString(): String {
        return "Account(name='$username', totalExp=${experiencePoints.value})"
    }
}