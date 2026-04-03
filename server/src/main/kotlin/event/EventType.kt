package main.kotlin.event

import main.kotlin.data.events.ToDo
import main.kotlin.data.events.SharedEvent
import main.kotlin.data.events.Appointment

/**
 * Enum for the different types of events. Used in the database to differentiate between
 * the subclasses of events without adding unnecessary complexity.
 */
enum class EventType {

    /**
     * The event is of type [Appointment]
     */
    APPOINTMENT_TYPE,

    /**
     * The event is of type [ToDo]
     */
    TODO_TYPE,

    /**
     * The event is of type [SharedEvent]
     */
    SHARED_EVENT_TYPE
}
