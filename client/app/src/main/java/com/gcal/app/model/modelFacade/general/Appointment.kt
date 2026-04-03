package com.gcal.app.model.modelFacade.general

import com.gcal.app.model.modelData.data.system.Group
import java.time.LocalDateTime

interface Appointment : Event {
    override fun end(): LocalDateTime
    fun group(): Group
    fun start(): LocalDateTime
}