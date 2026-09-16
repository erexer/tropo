package io.github.erexer.tropo

import android.app.Application
import io.github.erexer.tropo.di.AppContainer

class TropoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}