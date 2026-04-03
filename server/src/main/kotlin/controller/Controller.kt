package main.kotlin.controller


/**
 * Interface for a request controller, managing a server.
 */

interface Controller {

    /**
     * Method used for starting the server.
     * @param waitForServer states whether to run a blocking server or not (for tests)
     */
    fun start(waitForServer : Boolean)

    /**
     * Method used for stopping the server.
     */
    fun stop()
}