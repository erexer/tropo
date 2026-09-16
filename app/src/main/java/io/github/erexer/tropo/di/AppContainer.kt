package io.github.erexer.tropo.di

import android.content.Context
import androidx.room.Room
import io.github.erexer.tropo.data.local.TropoDatabase
import io.github.erexer.tropo.data.preferences.UserPreferences
import io.github.erexer.tropo.data.remote.GeocodingApi
import io.github.erexer.tropo.data.remote.WeatherApiClient
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.data.repository.WeatherRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AppContainer(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    val database: TropoDatabase by lazy {
        Room.databaseBuilder(context, TropoDatabase::class.java, "tropo_db").build()
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

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            weatherDao = database.weatherDao(),
            weatherApiClient = weatherApiClient,
            geocodingApi = geocodingApi,
            userPreferences = userPreferences
        )
    }
}