package io.github.erexer.tropo.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.erexer.tropo.TropoApplication

class WeatherSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContainer = (applicationContext as TropoApplication).container
        return try {
            appContainer.weatherRepository.getCurrentWeather()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}