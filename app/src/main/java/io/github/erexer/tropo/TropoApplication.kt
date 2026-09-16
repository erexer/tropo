package com.tropo

import android.app.Application
import androidx.work.Configuration
import com.tropo.di.AppContainer

class TropoApplication : Application(), Configuration.Provider {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(appContainer.workerFactory)
            .build()
}