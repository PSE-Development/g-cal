package com.gcal.app.model.localData

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gcal.app.model.localData.entity.AppointmentEntity
import com.gcal.app.model.localData.entity.EventCompletionEntity
import com.gcal.app.model.localData.entity.EventEntity
import com.gcal.app.model.localData.entity.GroupEntity
import com.gcal.app.model.localData.entity.SharedEventEntity
import com.gcal.app.model.localData.entity.ToDoEntity
import com.gcal.app.model.localData.relation.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Optimized Data Access Object for all event types.
 */
@Dao
abstract class EventDao {

    // --- High-Level Relation Queries ---

    @Transaction
    @Query("SELECT * FROM todos WHERE deadlineTimestamp BETWEEN :start AND :end")
    abstract fun observeToDos(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<ToDoRelation>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE endTimeTimestamp >= :start AND startTimeTimestamp <= :end")
    abstract fun observeAppointments(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<AppointmentRelation>>

    @Transaction
    @Query("SELECT * FROM shared_events WHERE startTimeTimestamp >= :start AND endTimeTimestamp <= :end")
    abstract fun observeSharedEvents(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<SharedEventRelation>>

    @Query("SELECT * FROM event_groups ORDER BY name ASC")
    abstract fun observeGroups(): Flow<List<GroupEntity>>

    @Transaction
    @Query(
        " SELECT todos.* FROM todos " +
                "INNER JOIN event_completions ON todos . eventId = event_completions . eventId " +
                "WHERE event_completions . isCompleted " +
                "AND event_completions.completedAtTimestamp BETWEEN :start AND :end"
    )
    abstract fun observeCompletedToDos(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<ToDoRelation>>

    @Transaction
    @Query(
        " SELECT appointments.* FROM appointments " +
                "INNER JOIN event_completions ON appointments . eventId = event_completions . eventId " +
                "WHERE event_completions . isCompleted " +
                "AND event_completions.completedAtTimestamp BETWEEN :start AND :end"
    )
    abstract fun observeCompletedAppointments(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<AppointmentRelation>>

    // Insert Methods

    @Insert
    abstract suspend fun insertGroup(group: GroupEntity): Long

    @Transaction
    open suspend fun insertNewAppointment(
        appointment: AppointmentEntity
    ): Long {
        val eventId = insertNewEvent(
            EventEntity()
        )
        insertAppointment(
            AppointmentEntity(
                eventId = eventId,
                appointment.name,
                appointment.description,
                appointment.groupId,
                appointment.startTimeTimestamp,
                appointment.endTimeTimestamp,
                appointment.xpValue
            )
        )
        insertEmptyCompletionDetail(eventId)
        return eventId
    }


    @Transaction
    open suspend fun insertNewToDo(
        todo: ToDoEntity
    ): Long {
        val eventId = insertNewEvent(
            EventEntity()
        )
        insertToDo(
            ToDoEntity(
                eventId = eventId, todo.name, todo.description, todo.deadlineTimestamp, todo.xpValue
            )
        )
        insertEmptyCompletionDetail(eventId)
        return eventId
    }

    @Transaction
    open suspend fun upsertSharedEvent(
        sharedEvent: SharedEventEntity
    ) {
        val eventId = try {
            insertNewEvent(
                EventEntity(sharedEvent.eventId)
            )
        } catch (e: Exception) {
            Log.i(
                "EventDao.uspsertSharedEvent()",
                "Event with id ${sharedEvent.eventId} already exists, thus entity is updated"
            )
            updateSharedEvent(sharedEvent)
            return
        }
        Log.i(
            "EventDao.uspsertSharedEvent()",
            "Event with id ${sharedEvent.eventId} does not exist jet, thus creating new entity"
        )
        insertSharedEvent(
            SharedEventEntity(
                eventId = eventId,
                sharedEvent.name,
                sharedEvent.description,
                sharedEvent.groupId,
                sharedEvent.startTimeTimestamp,
                sharedEvent.endTimeTimestamp,
                sharedEvent.xpValue
            )
        )
        insertEmptyCompletionDetail(eventId)
        Log.i("EventDao.uspsertSharedEvent()", "Event with id $eventId created Succesfull")
    }


    // --- Universal Write Operations (Upsert) ---

    @Update
    abstract suspend fun updateAppointment(appointments: AppointmentEntity)

    @Update
    abstract suspend fun updateToDo(toDos: ToDoEntity)

    @Update
    abstract suspend fun updateSharedEvent(sharedEvents: SharedEventEntity)

    @Update
    abstract suspend fun updateCompletionDetail(details: EventCompletionEntity)

    @Update
    abstract suspend fun updateGroup(groups: GroupEntity)

    @Update
    abstract suspend fun updateGroups(groups: List<GroupEntity>)

    @Update
    abstract suspend fun updateToDos(toDos: List<ToDoEntity>)

    @Update
    abstract suspend fun updateAppointments(appointments: List<AppointmentEntity>)

    @Update
    abstract suspend fun updateCompletionDetails(details: List<EventCompletionEntity>)

    @Transaction
    open suspend fun upsertSharedEvents(sharedEvents: List<SharedEventEntity>) {
        for (sharedEvent in sharedEvents) {
            upsertSharedEvent(sharedEvent)
        }
    }


    // --- Delete Operations ---
    @Query("""
        DELETE FROM base_events 
        WHERE eventId IN (SELECT eventId FROM shared_events)
    """)
    abstract fun clearSharedEvents()


    @Transaction
    open suspend fun deleteAppointment(appointment: AppointmentEntity) {
        deleteBaseEventById(appointment.eventId)
    }

    @Transaction
    open suspend fun deleteToDo(toDo: ToDoEntity) {
        deleteBaseEventById(toDo.eventId)
    }

    @Transaction
    open suspend fun deleteSharedEvent(sharedEvent: SharedEventEntity) {
        deleteBaseEventById(sharedEvent.eventId)
    }

    @Delete
    abstract suspend fun deleteGroup(group: GroupEntity)

    // Private Methods
    @Query("SELECT EXISTS(SELECT 1 FROM base_events WHERE eventId = :id)")
    protected abstract fun containsEvent(id: Long): Boolean

    @Insert
    protected abstract suspend fun insertNewEvent(baseEvent: EventEntity): Long

    @Insert
    protected abstract suspend fun insertAppointment(appointment: AppointmentEntity)

    @Insert
    protected abstract suspend fun insertToDo(toDo: ToDoEntity)

    @Insert
    protected abstract suspend fun insertSharedEvent(sharedEvent: SharedEventEntity)

    @Insert
    protected abstract suspend fun insertCompletionDetail(detail: EventCompletionEntity)

    @Transaction
    protected open suspend fun insertEmptyCompletionDetail(eventId: Long) {
        insertCompletionDetail(EventCompletionEntity(eventId, null, false))
    }

    @Query("DELETE FROM base_events WHERE eventId = :id")
    protected abstract suspend fun deleteBaseEventById(id: Long)
}