package com.gcal.app.model.modelFacade.general

import java.time.LocalDateTime

interface CompletionDetail {
    fun completed(): Boolean
    fun identifier(): Long
    fun completionDate(): LocalDateTime?
}