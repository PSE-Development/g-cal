package main.kotlin.event

import main.kotlin.data.Result
import main.kotlin.data.events.ToDo
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.Appointment

/**
 * Interface for managing the events by database access.
 */

interface EventManagement {

    /**
     * Creates an appointment for a given user.
     */
    fun createAppointment(username : String, appointment : Appointment) : Result<Unit>

    /**
     * Creates a to-Do for a given user.
     */
    fun createToDo(username : String, toDo : ToDo) : Result<Unit>

    /**
     * Creates a shared event for a given user.
     */
    fun createSharedEvent(username : String, sharedEvent: SharedEvent) : Result<Unit>

    /**
     * Returns the appointments of a user.
     */
    fun getAppointments(username : String) : Result<List<Appointment>>

    /**
     * Returns the to-Dos of a user.
     */
    fun getToDos(username : String) : Result<List<ToDo>>

    /**
     * Returns the shared events of a user.
     */
    fun getSharedEvents(username : String) : Result<List<SharedEvent>>

    /**
     * Updates an existing appointment for a user.
     */
    fun updateAppointment(username : String, updatedAppointment: Appointment) : Result<Unit>

    /**
     * Updates an existing to-Do for a user.
     */
    fun updateToDo(username : String, updatedToDo: ToDo) : Result<Unit>


    /**
     * Updates an existing shared event for a user.
     */
    fun updateSharedEvent(username : String, updatedSharedEvent: SharedEvent) : Result<Unit>

    /**
     * Deletes an existing appointment for a user.
     */
    fun deleteAppointment(username : String, eventID: Long) : Result<Unit>

    /**
     * Deletes an existing to-Do for a user.
     */
    fun deleteToDo(username : String, eventID: Long) : Result<Unit>

    /**
     * Deletes an existing shared event for a user.
     */
    fun deleteSharedEvent(username : String, eventID: Long) : Result<Unit>

    /**
     * Generates a unique eventID which is not used by any user.
     */
    fun generateUniqueEventID() : Long
}
