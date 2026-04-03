package com.gcal.app.model.modelFacade.general

interface ReportData {
    fun completedEvents(): List<Event>
    fun completedAchievements(): List<Achievement>
}