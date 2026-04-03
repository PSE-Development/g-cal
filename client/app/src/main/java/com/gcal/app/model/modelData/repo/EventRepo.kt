package com.gcal.app.model.modelData.repo

import android.util.Log
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.asDatabaseEntity
import com.gcal.app.model.asDomainModel
import com.gcal.app.model.modelData.XpDistributor
import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.NoXp
import com.gcal.app.model.modelData.data.system.RegularUser
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.UserGroup
import com.gcal.app.model.modelFacade.RequestAPI
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.ToDo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.collections.map


class EventRepo(
    val api: RequestAPI,
    val database: LocalData,
    val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val dataAccessObject = database.eventDao()

    /**
     * Observes appointments within a specific timeframe and triggers a background refresh.
     */
    fun getAppointmentsIn(start: LocalDateTime, end: LocalDateTime): Flow<List<Appointment>> {
        refreshAppointments()
        return dataAccessObject.observeAppointments(start, end)
            .map { list -> list.map { it.asDomainModel() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Observes personal to-dos within a specific timeframe and triggers a background refresh.
     */
    fun getToDoIn(start: LocalDateTime, end: LocalDateTime): Flow<List<ToDo>> {
        refreshToDo()
        return dataAccessObject.observeToDos(start, end)
            .map { list -> list.map { it.asDomainModel() } }
            .flowOn(ioDispatcher)
    }

    fun getCompletedToDoIn(start: LocalDateTime, end: LocalDateTime): Flow<List<ToDo>> {
        refreshToDo()
        return dataAccessObject.observeCompletedToDos(start, end)
            .map { list ->
                list.map { it.asDomainModel() }
            }.flowOn(ioDispatcher)
    }

    /**
     * Observes shared group events within a specific timeframe and triggers a background refresh.
     */
    fun getSharedEventsIn(start: LocalDateTime, end: LocalDateTime): Flow<List<SharedEvent>> {
        refreshSharedEvents()
        return dataAccessObject.observeSharedEvents(start, end)
            .map { list -> list.map { it.asDomainModel() } }
            .flowOn(ioDispatcher)
    }


//  Creation Methods

    /**
     * Sends a new appointment to the server and persists it locally upon success.
     */
    suspend fun createAppointment(event: UserAppointment): Response<Unit> {
        // We first have to create the event locally, to generate a valid EventId
        val newId = database.eventDao().insertNewAppointment(event.asDatabaseEntity())
        val newAppointment = UserAppointment(
            newId,
            event.eventName(),
            event.description(),
            event.start(),
            event.end(),
            event.group(),
            NoXp,
            EventCD(newId, false, null)
        )
        return try {
            val response = api.addUserAppointments(newAppointment)
            if (response is Response.Success) {
                Response.Success(Unit)
            } else {
                // If server cannot create Shared Event, we have to delete local copy
                database.eventDao().deleteAppointment(newAppointment.asDatabaseEntity())
                Response.Error(
                    Exception("Remote server failed to save appointment")
                )
            }
        } catch (e: Exception) {
            // If server does not answer, we have to delete local copy
            database.eventDao().deleteAppointment(newAppointment.asDatabaseEntity())
            Response.Error(e)
        }
    }

    /**
     * Sends a new To-Do to the server and persists it locally upon success.
     */
    suspend fun createTodo(event: UserToDo): Response<Unit> {
        val newId = database.eventDao().insertNewToDo(event.asDatabaseEntity())
        val newAppointment = UserToDo(
            newId,
            event.eventName(),
            event.description(),
            event.end(),
            NoXp,
            EventCD(newId, false, null)
        )
        return try {
            val response = api.addUserToDo(newAppointment)
            if (response is Response.Success) {
                Response.Success(Unit, response.statusCode)
            } else {
                database.eventDao().deleteToDo(newAppointment.asDatabaseEntity())
                Response.Error(Exception("Remote server failed to save To-Do"))
            }
        } catch (e: Exception) {
            database.eventDao().deleteToDo(newAppointment.asDatabaseEntity())
            Response.Error(e)
        }
    }

    /**
     * Registers a shared event and its participants, then updates local storage.
     */
    suspend fun createSharedEvent(
        event: UserSharedEvent,
        participants: List<RegularUser>
    ): Response<Unit> {
        return try {
            val response = api.addSharedEvent(event, participants)
            if (response is Response.Success) {
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Remote server failed to save shared event"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    /**
     * Syncs a new user group with the server and saves it to the local database.
     */
    suspend fun createGroup(userGroup: Group): Response<Unit> {
        val groupId = database.eventDao().insertGroup(userGroup.asDatabaseEntity())
        val newGroup = UserGroup(groupId, userGroup.groupName(), userGroup.groupColour())
        return try {
            val response = api.addUserGroup(newGroup)
            if (response is Response.Success) {
                Response.Success(Unit, response.statusCode)
            } else {
                database.eventDao().deleteGroup(newGroup.asDatabaseEntity())
                Response.Error(Exception("Remote server failed to save group"))
            }
        } catch (e: Exception) {
            database.eventDao().deleteGroup(newGroup.asDatabaseEntity())
            Response.Error(e)
        }
    }

//  Update Methods

    /**
     * Updates an appointment on the server and synchronizes the local database.
     */
    suspend fun updateAppointment(event: UserAppointment): Response<Unit> {
        return try {
            val response = api.updateUserAppointment(event)
            if (response is Response.Success) {
                database.eventDao().updateAppointment(event.asDatabaseEntity())
                database.eventDao()
                    .updateCompletionDetail(event.checkCompletion().asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Server update for appointment failed"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    /**
     * Syncs To-Do changes with the server and updates local records.
     */
    suspend fun updateToDo(event: UserToDo): Response<Unit> {
        return try {
            val response = api.updateUserToDo(event)
            if (response is Response.Success) {
                database.eventDao().updateToDo(event.asDatabaseEntity())
                database.eventDao()
                    .updateCompletionDetail(event.checkCompletion().asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Server update for To-Do failed"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    /**
     * Updates group information and persists it to the local database.
     */
    suspend fun updateGroup(userGroup: UserGroup): Response<Unit> {
        return try {
            val response = api.updateGroup(userGroup)
            if (response is Response.Success) {
                database.eventDao().updateGroup(userGroup.asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Server update for group failed"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    /**
     * Updates a shared event's data and ensures local consistency.
     */
    suspend fun updateSharedEvent(event: UserSharedEvent): Response<Unit> {
        return try {
            val response = api.updateSharedEvent(event)
            if (response is Response.Success) {
                database.eventDao().updateSharedEvent(event.asDatabaseEntity())
                database.eventDao()
                    .updateCompletionDetail(event.checkCompletion().asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Server update for shared event failed"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }
//  Deletion Methods

    /**
     * Deletes an appointment from the server and removes it from the local database upon success.
     */
    suspend fun deleteAppointment(event: UserAppointment): Response<Unit> {
        return try {
            val response = api.deleteUserAppointment(event)
            if (response is Response.Success) {
                database.eventDao().deleteAppointment(event.asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(
                    Exception("Failed to delete appointment from server")
                )
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    /**
     * Deletes an shared event from the server and removes it from the local database upon success.
     */
    suspend fun deleteSharedEvent(event: UserSharedEvent): Response<Unit> {
        return try {
            val response = api.deleteSharedEvent(event)
            if (response is Response.Success) {
                database.eventDao().deleteSharedEvent(event.asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(
                    Exception("Failed to delete shared event from server")
                )
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    /**
     * Removes a To-Do from the remote server and the local cache.
     */
    suspend fun deleteToDo(event: UserToDo): Response<Unit> {
        return try {
            val response = api.deleteUserToDo(event)
            if (response is Response.Success) {
                database.eventDao().deleteToDo(event.asDatabaseEntity())
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(
                    Exception("Failed to delete To-Do from server")
                )
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

//  Async Update of database with API

    /**
     * Synchronizes local appointments with the server in the background.
     */
    fun refreshAppointments() {
        coroutineScope.launch {
            try {
                val response = api.getUserAppointments()
                if (response is Response.Success) {
                    val appointments = response.data

                    database.eventDao().apply {
                        updateAppointments(appointments.map { it.asDatabaseEntity() })
                        updateGroups(appointments.map { it.group().asDatabaseEntity() })
                        updateCompletionDetails(appointments.map {
                            it.checkCompletion().asDatabaseEntity()
                        })
                    }
                } else {
                    Log.e("EventRepo.refreshAppointments()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("EventRepo.refreshAppointments()", "Error mapping Appointment: ${e.message}")
            }
        }
    }

    /**
     * Fetches shared events from the remote API and updates the local cache.
     */
    fun refreshSharedEvents() {
        coroutineScope.launch {
            Log.i("EventRepo.refreshSharedEvents()", "Start refreshing shared events...")
            try {
                val response = api.getUserSharedEvents()
                if (response is Response.Success) {
                    dataAccessObject.clearSharedEvents()
                    Log.i("EventRepo.refreshSharedEvents()", "Cleared all Shared Events")
                    val sharedEvents = response.data
                    Log.i(
                        "EventRepo.refreshSharedEvents()",
                        "Received ${sharedEvents.size} shared events from server"
                    )
                    database.eventDao().apply {
                        upsertSharedEvents(sharedEvents.map {
                            XpDistributor.calculateSharedEvent(it).asDatabaseEntity()
                        })
                        updateCompletionDetails(sharedEvents.map {
                            it.checkCompletion().asDatabaseEntity()
                        })
                    }
                    Log.i(
                        "EventRepo.refreshSharedEvents()",
                        "Shared events updated in Local Database"
                    )
                } else {
                    Log.e("EventRepo.refreshSharedEvents()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("EventRepo.refreshSharedEvents()", "Error Mapping SharedEvents: ${e.message}")
            }
        }
    }

    /**
     * Updates the local To-Do list with the latest data from the server.
     */
    fun refreshToDo() {
        coroutineScope.launch {
            try {
                val response = api.getUserToDo()
                if (response is Response.Success) {
                    val toDos = response.data
                    database.eventDao().apply {
                        updateToDos(toDos.map { it.asDatabaseEntity() })
                        updateCompletionDetails(toDos.map {
                            it.checkCompletion().asDatabaseEntity()
                        })
                    }
                } else {
                    Log.e("EventRepo.refreshToDo()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("EventRepo.refreshToDo()", "Error mapping ToDo: ${e.message}")
            }
        }
    }

    fun getGroups(): Flow<List<Group>> {
        refreshGroups()
        return dataAccessObject.observeGroups()
            .map { list -> list.map { it.asDomainModel() } }
            .flowOn(Dispatchers.IO)
    }


    private fun refreshGroups() {
        coroutineScope.launch {
            try {
                val response = api.getUserGroups()
                if (response is Response.Success) {
                    val toDos = response.data
                    database.eventDao().updateGroups(toDos.map { it.asDatabaseEntity() })
                } else {
                    Log.e("EventRepo.refreshGroups()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("EventRepo.refreshGroups()", "Error mapping Groups: ${e.message}")
            }
        }
    }
}