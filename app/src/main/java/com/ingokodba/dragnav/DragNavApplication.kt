package com.ingokodba.dragnav

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DragNavApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(this))
    }
}
