package com.gcal.app.model.modelData.data.system

import com.gcal.app.model.modelFacade.general.CompletionDetail
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Data class representing the completion details of an event.
 * Implements [CompletionDetail] for system-wide status tracking.
 */
@Serializable
data class EventCD(
    val eventId: Long,
    val isCompleted: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    private val completionTime: LocalDateTime?
) : CompletionDetail {

    override fun completed(): Boolean = isCompleted

    override fun identifier(): Long = eventId

    override fun completionDate(): LocalDateTime? = completionTime
}