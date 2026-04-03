package main.kotlin.data

import io.ktor.http.HttpStatusCode

/**
 * This class offers a Result type, similar to the [kotlin.Result], but without managing a [Throwable],
 * just the error message and a [HttpStatusCode] to simplify the control flow.
 */
sealed class Result<T> {
    /**
     * Success Type
     * @param value the value if successful
     */
    data class Success<T>(val value: T) : Result<T>()

    /**
     * Failure Type
     * @param status the HTTP error status code
     * @param error the error message
     */
    data class Failure<T>(val status : HttpStatusCode, val error: String) : Result<T>()


    /**
     * Applies the lambda block in case the calling object has type [Success]
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) block(value)
        return this
    }

    /**
     * Applies the lambda block in case the calling object has type [Failure]
     */
    inline fun onFailure(block: (HttpStatusCode, String) -> Unit): Result<T> {
        if (this is Failure) block(status, error)
        return this
    }

    inline fun getOrElse(onFailure : (HttpStatusCode, String) -> T) : T {
        return when (this) {
            is Success -> value
            is Failure -> onFailure(status, error)
        }
    }
}