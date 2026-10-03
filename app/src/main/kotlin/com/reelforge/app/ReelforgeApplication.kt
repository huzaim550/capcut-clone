package com.reelforge.app

import android.app.Application

/**
 * Owns the app-scoped [AppContainer] so the database lives as long as the process
 * rather than being rebuilt per Activity.
 */
class ReelforgeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
