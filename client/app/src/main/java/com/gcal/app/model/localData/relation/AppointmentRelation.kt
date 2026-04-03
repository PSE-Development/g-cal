package com.gcal.app.model.localData.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.gcal.app.model.localData.entity.AppointmentEntity
import com.gcal.app.model.localData.entity.EventCompletionEntity
import com.gcal.app.model.localData.entity.GroupEntity

/**
 * A data transfer object (POJO) that aggregates an appointment with its
 * completion status and associated group information.
 */
data class AppointmentRelation(
    @Embedded
    val appointment: AppointmentEntity,

    @Relation(
        parentColumn = "eventId",
        entityColumn = "eventId"
    )
    val completion: EventCompletionEntity,

    @Relation(
        parentColumn = "groupId",
        entityColumn = "id"
    )
    val group: GroupEntity?
)
