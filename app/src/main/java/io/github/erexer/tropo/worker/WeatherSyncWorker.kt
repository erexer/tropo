package com.tropo.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tropo.data.local.WeatherDao
import com.tropo.data.preferences.UserPreferencesRepository
import com.tropo.data.remote.WeatherApiClient
import com.tropo.ui.widget.TropoWeatherWidget
import kotlinx.coroutines.flow.first

class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val weatherApiClient: WeatherApiClient,
    private val weatherDao: WeatherDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userPrefs = userPreferencesRepository.userPreferencesFlow.first()
            val primaryLocation = weatherDao.getPrimaryLocation()

            if (primaryLocation != null) {
                val remoteData = weatherApiClient.getForecast(
                    latitude = primaryLocation.latitude,
                    longitude = primaryLocation.longitude
                )
                weatherDao.insertWeather(remoteData.toEntity(primaryLocation.id))
            }

            TropoWeatherWidget().updateAll(applicationContext)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}