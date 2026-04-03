package com.gcal.app.model.modelData.data.dto

import com.gcal.app.model.modelData.data.system.RegularUser
import kotlinx.serialization.Serializable

@Serializable
data class SharedEventRequestDTO(
    val sharedEvent: UserSharedEventDTO,
    val users: List<RegularUserDTO>
)