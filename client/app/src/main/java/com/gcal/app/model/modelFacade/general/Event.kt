package com.gcal.app.model.modelFacade.general

import com.gcal.app.model.modelData.data.system.XP
import java.time.LocalDateTime

interface  Event {
    fun eventID(): Long
    fun eventName(): String
    fun description(): String
    fun end(): LocalDateTime?
    fun experiencePoints(): XP
    fun checkCompletion(): CompletionDetail
}