package main.kotlin.event

import main.kotlin.data.Result

/**
 * Interface for an Authorizer to authorize access for a given user on an different objects.
 */
interface Authorizer {

    /**
     * Authorizes the access of a user to an event.
     */
    fun authorizeEvent(username : String, eventID : Long) : Result<Boolean>

    /**
     * Authorizes the access of a user to a group.
     */
    fun authorizeGroup(username : String, groupID : Long) : Result<Boolean>
}