package main.kotlin.controller.exceptions

/**
 * Custom Exception, allowing a more detailed control flow. Used when the values for port and url
 * used for the server are invalid.
 * Inherits from [Exception].
 * @param message the message of the exception
 */
class ConnectionException(
    message : String) : Exception(message)