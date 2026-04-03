package main.kotlin.data

import kotlinx.serialization.Serializable

/**
 * This class models a Group of the calendar, containing an identifying [groupID], a [name] and a [colour] code.
 * Annotates [kotlin.io.Serializable] to allow for serialization as a DTO.
 */
@Serializable
data class Group(
    val groupID : Long,
    var name : String,
    var colour : Int
)