package com.gcal.app.model.modelData.data.system

import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gcal.app.model.modelData.data.dto.GroupDTO
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Defines a group category for events.
 * Use the [Group.from] factory to handle group creation and the [NoGroup] state.
 */
@Serializable
@SerialName("Group")
sealed interface Group {
    fun groupId(): Long
    fun groupName(): String

    @ColorInt
    fun groupColour(): Int

    companion object {
        fun asUserGroup(id: Long, name: String, @ColorInt color: Int): UserGroup =
            UserGroup(id, name, color)

        fun asUserGroup(group: Group): UserGroup =
            UserGroup(group.groupId(), group.groupName(), group.groupColour())

        /**
         * Creates a [Group] instance. Returns [NoGroup] if the [id] is "0" or null.
         */
        fun from(id: Long?, name: String, @ColorInt color: Int): Group {
            return if (id == null || id == NoGroup.ID) NoGroup
            else UserGroup(id, name, color)
        }

        /**
         * Creates a [Group] instance from the [Group] interface.
         */
        fun from(group: Group): Group =
            from(group.groupId(), group.groupName(), group.groupColour())
    }
}

/**
 * Sentinel object representing the absence of a group.
 */
@Serializable
@SerialName("NoGroup")
object NoGroup : Group {
    const val ID: Long = 0
    const val NAME: String = "None"
    const val COLOR: Int = 0

    override fun groupId(): Long = ID
    override fun groupName(): String = NAME
    override fun groupColour(): Int = COLOR
}

/**
 * Implementation for standard user-defined groups.
 */
@Serializable
@SerialName("UserGroup")
data class UserGroup(
    val id: Long,
    val name: String,
    val color: Int
) : Group {
    override fun groupId(): Long = id
    override fun groupName(): String = name
    override fun groupColour(): Int = color
}

fun Group.toDTO(): GroupDTO{
    return when (this) {
        is UserGroup -> GroupDTO(
            groupID = this.groupId(),
            name = this.groupName(),
            colour = this.groupColour()
        )

        is NoGroup -> GroupDTO(
            groupID = 0,
            name = "None",
            colour = 0
        )
    }
}
