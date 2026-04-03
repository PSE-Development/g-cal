package com.gcal.app.model.modelData.data

import com.gcal.app.model.modelFacade.general.Event
import com.gcal.app.model.modelFacade.general.Achievement
import com.gcal.app.model.modelFacade.general.ReportData

/**
 * A data snapshot of a user's progress, containing completed events and earned achievements.
 */
data class ReportUserData(
    private val events: List<Event>,
    private val achievements: List<Achievement>
) : ReportData {

    override fun completedEvents(): List<Event> = events

    override fun completedAchievements(): List<Achievement> = achievements
}