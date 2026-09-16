package com.tropo.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.tropo.data.local.TropoDatabase
import com.tropo.data.local.WeatherDao
import com.tropo.data.preferences.UserPreferencesRepository
import com.tropo.data.remote.WeatherApiClient
import com.tropo.worker.WeatherSyncWorker

class AppContainer(private val context: Context) {
    val database: TropoDatabase by lazy { TropoDatabase.getInstance(context) }
    val weatherDao: WeatherDao by lazy { database.weatherDao() }
    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(context) }
    val weatherApiClient: WeatherApiClient by lazy { WeatherApiClient() }

    val workerFactory: WorkerFactory by lazy {
        TropoWorkerFactory(weatherApiClient, weatherDao, userPreferencesRepository)
    }
}

class TropoWorkerFactory(
    private val weatherApiClient: WeatherApiClient,
    private val weatherDao: WeatherDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            WeatherSyncWorker::class.java.name -> {
                WeatherSyncWorker(
                    appContext,
                    workerParameters,
                    weatherApiClient,
                    weatherDao,
                    userPreferencesRepository
                )
            }
            else -> null
        }
    }
}