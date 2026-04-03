package com.gcal.app.model

import android.app.Application
import android.content.Context

class MyContext: Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}