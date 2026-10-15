package io.github.erexer.tropo.di

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import io.github.erexer.tropo.data.local.TropoDatabase
import io.github.erexer.tropo.data.location.AndroidDeviceLocation
import io.github.erexer.tropo.data.preferences.UserPreferences
import io.github.erexer.tropo.data.remote.AirQualityApiClient
import io.github.erexer.tropo.data.remote.GeocodingApi
import io.github.erexer.tropo.data.remote.WeatherApiClient
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.data.repository.WeatherRepositoryImpl
import io.github.erexer.tropo.ui.widget.TropoWeatherWidget
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

class AppContainer(
    private val context: Context,
) {
    private val detailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val httpClient =
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    val database: TropoDatabase by lazy {
        Room
            .databaseBuilder(context, TropoDatabase::class.java, "tropo_db")
            .addMigrations(
                TropoDatabase.MIGRATION_1_2,
                TropoDatabase.MIGRATION_2_3,
                TropoDatabase.MIGRATION_3_4,
                TropoDatabase.MIGRATION_4_5,
                TropoDatabase.MIGRATION_5_6,
                TropoDatabase.MIGRATION_6_7,
                TropoDatabase.MIGRATION_7_8,
            ).build()
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(context)
    }

    val weatherApiClient: WeatherApiClient by lazy {
        WeatherApiClient(httpClient)
    }

    val geocodingApi: GeocodingApi by lazy {
        GeocodingApi(httpClient)
    }

    val airQualityApiClient: AirQualityApiClient by lazy {
        AirQualityApiClient(httpClient)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            weatherDao = database.weatherDao(),
            weatherApiClient = weatherApiClient,
            airQualityApiClient = airQualityApiClient,
            geocodingApi = geocodingApi,
            userPreferences = userPreferences,
            detailScope = detailScope,
            refreshWidgets = { TropoWeatherWidget(WeatherRefresh.CACHE_ONLY).updateAll(context) },
            deviceSource = AndroidDeviceLocation(context),
        )
    }
}
