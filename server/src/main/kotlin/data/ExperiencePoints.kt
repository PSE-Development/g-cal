package main.kotlin.data

import kotlinx.serialization.Serializable

/**
 * This is a wrapper class for the [value] of the amount of experience points the users.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class ExperiencePoints(
    val value : Int
)