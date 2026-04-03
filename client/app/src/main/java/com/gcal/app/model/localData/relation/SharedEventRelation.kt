package com.gcal.app.model.localData.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.gcal.app.model.localData.entity.SharedEventEntity
import com.gcal.app.model.localData.entity.EventCompletionEntity
import com.gcal.app.model.localData.entity.GroupEntity

/**
 * Aggregates a shared event with its associated group and completion status.
 * This class is a POJO used by Room to perform join-like queries.
 */
data class SharedEventRelation(
    @Embedded
    val sharedEvent: SharedEventEntity,

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
