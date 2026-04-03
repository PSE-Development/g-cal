package com.gcal.app.model.modelFacade.general

import java.time.LocalDateTime


interface Achievement {
    fun achievementName(): String
    fun achievementDescription(): String
    fun isCompleted(): Boolean
    fun completionDate(): LocalDateTime?
}