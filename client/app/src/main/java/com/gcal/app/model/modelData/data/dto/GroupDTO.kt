package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.NoGroup
import com.gcal.app.model.modelData.data.system.UserGroup
import kotlinx.serialization.Serializable

@Serializable
data class GroupDTO(
    var groupID : Long,
    var name : String,
    var colour : Int
)

/**
 * Maps the object to a DTO.
 */
fun GroupDTO.toDomain(): Group =
    if (this.groupID == 0L) {
        NoGroup
    } else {
        UserGroup(
            id = this.groupID,
            name = this.name,
            color = this.colour
        )
    }