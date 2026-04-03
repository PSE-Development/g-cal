package com.gcal.app.model.modelFacade

sealed class Response<out T> {
    data class Success<out T>(val data: T, val statusCode: Int? = null) : Response<T>()

    data class Error(val e: Exception?=null, val statusCode: Int? = null) : Response<Nothing>()
}