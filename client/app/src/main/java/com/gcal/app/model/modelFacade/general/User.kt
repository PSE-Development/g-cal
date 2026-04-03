package com.gcal.app.model.modelFacade.general

import com.gcal.app.model.modelData.data.system.XP


interface User {
    fun username(): String
    fun name(): String
    fun experiencePoints(): XP
}