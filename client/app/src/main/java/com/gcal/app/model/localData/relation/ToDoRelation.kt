package com.gcal.app.model.localData.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.gcal.app.model.localData.entity.ToDoEntity
import com.gcal.app.model.localData.entity.EventCompletionEntity

/**
 * Aggregates a To-Do item with its completion status.
 * Used for combined database queries without being a standalone table.
 */
data class ToDoRelation(
    @Embedded
    val toDo: ToDoEntity,

    @Relation(
        parentColumn = "eventId",
        entityColumn = "eventId"
    )
    val completion: EventCompletionEntity
)