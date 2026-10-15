package io.github.erexer.tropo.data.repository

import androidx.test.core.app.ApplicationProvider
import io.github.erexer.tropo.data.local.AirQualityEntity
import io.github.erexer.tropo.data.local.DailyForecastEntity
import io.github.erexer.tropo.data.local.HourlyForecastEntity
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherDao
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.preferences.UserPreferences
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.remote.AirQualityApiClient
import io.github.erexer.tropo.data.remote.GeocodingApi
import io.github.erexer.tropo.data.remote.WeatherApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class WeatherRepositoryImplTest {
    @Test
    fun favoriteWeatherUsesCacheAndIsolatesFailuresWithoutSelectingCities() =
        runBlocking {
            val dao = TestWeatherDao()
            val failed = LocationEntity(2, "Offline", "", 2.0, 2.0, isFavorite = true, admin1 = "")
            val online = LocationEntity(3, "Online", "", 3.0, 3.0, isFavorite = true, admin1 = "")
            dao.insertLocations(listOf(failed, online))
            dao.insertWeather(WeatherEntity(2, 18.0, 0, 50, 5.0, 0))
            val client =
                createClient(
                    MockEngine { request ->
                        if (request.url.parameters["latitude"] == "2.0") throw IOException("Offline city")
                        respond(
                            """{"current":{"temperature_2m":22,"relative_humidity_2m":50,"apparent_temperature":21,
                "wind_speed_10m":5,"weather_code":1,"is_day":1,"time":"2026-10-09T12:00"}}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            try {
                val emissions = mutableListOf<Map<Long, LocationWeather>>()
                createRepository(dao, client).locationWeather(listOf(failed, online), WeatherRefresh.FORCE).collect {
                    emissions += it
                }
                assertEquals(18.0, emissions.first()[2L]?.weather?.temperature)
                assertEquals(18.0, emissions.last()[2L]?.weather?.temperature)
                assertTrue(emissions.last()[2L]?.isCached == true)
                assertEquals(22.0, emissions.last()[3L]?.weather?.temperature)
                assertEquals(1L, dao.getSelectedLocation()?.id)
            } finally {
                client.close()
            }
        }

    @Test
    fun currentWindDirectionIsCached() =
        runBlocking {
            val dao = TestWeatherDao()
            val response =
                buildJsonObject {
                    putJsonObject("current") {
                        put("temperature_2m", 20.0)
                        put("relative_humidity_2m", 50)
                        put("apparent_temperature", 19.0)
                        put("wind_speed_10m", 10.0)
                        put("wind_direction_10m", 225.0)
                        put("weather_code", 0)
                        put("is_day", 1)
                        put("time", "2026-10-07T12:00")
                    }
                }
            val client =
                createClient(
                    MockEngine { request ->
                        assertTrue(
                            request.url.parameters["current"]
                                .orEmpty()
                                .contains("wind_direction_10m"),
                        )
                        respond(response.toString(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    },
                )
            try {
                val result = createRepository(dao, client).getCurrentWeather()
                assertEquals(225.0, result?.windDirectionDeg)
                assertEquals(225.0, dao.getWeather(dao.selectedLocation.id)?.windDirectionDeg)
            } finally {
                client.close()
            }
        }

    @Test
    fun forecastPersistsHourlyAndDailyData() =
        runBlocking {
            val dao = TestWeatherDao()
            val fixture = Json.parseToJsonElement(FORECAST_JSON).jsonObject
            val components =
                mapOf(
                    "rain" to JsonArray(listOf(JsonPrimitive(0.03f))),
                    "showers" to JsonArray(listOf(JsonPrimitive(0.02f))),
                    "snowfall" to JsonArray(listOf(JsonPrimitive(1f))),
                    "weather_code" to JsonArray(listOf(JsonPrimitive(73))),
                    "relative_humidity_2m" to JsonArray(listOf(JsonPrimitive(65f))),
                )
            val hourly = JsonObject(fixture.getValue("hourly").jsonObject + components)
            val response = JsonObject(fixture + ("hourly" to hourly))
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = response.toString(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            try {
                val result = createRepository(dao, client).getForecast()

                assertEquals(
                    68f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .temperature,
                    0.01f,
                )
                assertEquals(
                    "2026-10-01",
                    result
                        .getOrThrow()
                        .daily
                        .single()
                        .date,
                )
                assertEquals("America/Los_Angeles", result.getOrThrow().timezone)
                assertEquals(1, dao.getHourlyForecast(dao.selectedLocation.id).size)
                assertEquals(1, dao.getDailyForecast(dao.selectedLocation.id).size)
                assertEquals(
                    0.05f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .rainInches!!,
                    0.001f,
                )
                assertEquals(
                    2.54f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .snowCentimeters!!,
                    0.001f,
                )
                assertEquals(
                    73,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .weatherCode,
                )
                assertEquals(
                    65f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .humidity,
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun forecastFallsBackToCacheWhenNetworkFails() =
        runBlocking {
            val dao = TestWeatherDao()
            val cachedPoint =
                HourlyForecastEntity(
                    locationId = dao.selectedLocation.id,
                    forecastTime = "2026-10-01T12:00",
                    temperature = 60f,
                    precipProb = 10f,
                    precipInches = 0f,
                    dewPoint = 45f,
                    windSpeed = 4f,
                    windDirectionDeg = 180f,
                    uvIndex = 2f,
                    fetchedAt = 100L,
                    rainInches = 0.05f,
                    snowCentimeters = 0.5f,
                    weatherCode = 73,
                    humidity = 70f,
                )
            dao.replaceHourlyForecast(dao.selectedLocation.id, listOf(cachedPoint))
            val client = createClient(MockEngine { throw IOException("offline") })

            try {
                val result = createRepository(dao, client).getForecast()

                assertTrue(result.isSuccess)
                assertTrue(result.getOrThrow().isCached)
                assertEquals(
                    60f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .temperature,
                    0.01f,
                )
                assertEquals(
                    0.05f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .rainInches,
                )
                assertEquals(
                    0.5f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .snowCentimeters,
                )
                assertEquals(
                    73,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .weatherCode,
                )
                assertEquals(
                    70f,
                    result
                        .getOrThrow()
                        .hourly
                        .single()
                        .humidity,
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun airQualityPersistsAndReturnsUsAqiAndPm25() =
        runBlocking {
            val dao = TestWeatherDao()
            val client =
                createClient(
                    MockEngine { request ->
                        if (request.url.host != "air-quality-api.open-meteo.com") {
                            throw IOException("Unexpected weather request")
                        }
                        respond(
                            content = AIR_QUALITY_JSON,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )

            try {
                val result = createRepository(dao, client).getAirQuality().getOrThrow()

                assertEquals(42f, result.points.single().usAqi)
                assertEquals(8.5f, result.points.single().pm25!!, 0.01f)
                assertEquals("America/New_York", result.timezone)
                assertEquals(1, dao.getAirQuality(dao.selectedLocation.id).size)
            } finally {
                client.close()
            }
        }

    @Test
    fun airQualityIsCachedSeparatelyAndFallsBackWhenUnavailable() =
        runBlocking {
            val dao = TestWeatherDao()
            val cached =
                AirQualityEntity(
                    locationId = dao.selectedLocation.id,
                    forecastTime = "2026-10-05T12:00",
                    timezone = "America/New_York",
                    usAqi = 45f,
                    pm25 = 8f,
                    fetchedAt = 100L,
                )
            dao.replaceAirQuality(dao.selectedLocation.id, listOf(cached))
            val client = createClient(MockEngine { throw IOException("AQI offline") })

            try {
                val result = createRepository(dao, client).getAirQuality().getOrThrow()

                assertTrue(result.isCached)
                assertEquals(45f, result.points.single().usAqi)
                assertTrue(dao.getHourlyForecast(dao.selectedLocation.id).isEmpty())
            } finally {
                client.close()
            }
        }

    @Test
    fun weatherForecastSucceedsWhenAirQualityFails() =
        runBlocking {
            val dao = TestWeatherDao()
            val client =
                createClient(
                    MockEngine { request ->
                        if (request.url.host == "air-quality-api.open-meteo.com") {
                            throw IOException("AQI unavailable")
                        }
                        respond(
                            content = FORECAST_JSON,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )

            try {
                val repository = createRepository(dao, client)

                assertTrue(repository.getForecast().isSuccess)
                assertTrue(repository.getAirQuality().isFailure)
            } finally {
                client.close()
            }
        }

    @Test
    fun savingSelectedLocationAsFavoriteKeepsItSelected() =
        runBlocking {
            val dao = TestWeatherDao()
            val client = createClient(MockEngine { throw IOException("Unexpected network request") })
            try {
                createRepository(dao, client).setFavorite(dao.selectedLocation.copy(isSelected = false), true)

                assertEquals(dao.selectedLocation.id, dao.getSelectedLocation()?.id)
                assertTrue(dao.getLocation(dao.selectedLocation.id)?.isFavorite == true)
            } finally {
                client.close()
            }
        }

    @Test
    fun favoriteOrderCanBeRearranged() =
        runBlocking {
            val dao = TestWeatherDao()
            dao.insertLocations(
                listOf(LocationEntity(2L, "Seattle", "United States", 47.6, -122.3, isFavorite = true)),
            )
            val client = createClient(MockEngine { throw IOException("Unexpected network request") })
            try {
                val repository = createRepository(dao, client)
                repository.setFavorite(dao.selectedLocation, true)
                repository.reorderFavorites(listOf(2L, 1L))

                assertEquals(listOf(2L, 1L), repository.getFavoriteLocations().map { it.id })
            } finally {
                client.close()
            }
        }

    @Test
    fun savedLocationsLookUpStateOnce() =
        runBlocking {
            val dao = TestWeatherDao()
            dao.insertLocations(listOf(LocationEntity(2L, "Bend", "United States", 44.1, -121.3, isFavorite = true)))
            var lookups = 0
            val client =
                createClient(
                    MockEngine { request ->
                        lookups++
                        assertEquals("/v1/get", request.url.encodedPath)
                        respond(
                            content =
                                """{"id":2,"name":"Bend","latitude":44.1,"longitude":-121.3,
                                "admin1":"Oregon","population":99000}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            try {
                val repository = createRepository(dao, client, this)
                val updated = async { repository.locationUpdates().first { it > 0 } }
                assertNull(repository.getFavoriteLocations().single().admin1)
                updated.await()
                val favorite = repository.getFavoriteLocations().single()
                assertEquals("Oregon", favorite.admin1)
                assertEquals(99_000L, favorite.population)
                repository.getFavoriteLocations()
                assertEquals(1, lookups)
            } finally {
                client.close()
            }
        }

    @Test
    fun failedLocationBackfillDoesNotBlockOrRetryEveryRead() =
        runBlocking {
            val dao = TestWeatherDao()
            dao.insertLocations(listOf(LocationEntity(2L, "Bend", "United States", 44.1, -121.3, isFavorite = true)))
            var lookups = 0
            val attempted = CompletableDeferred<Unit>()
            val client =
                createClient(
                    MockEngine {
                        lookups++
                        attempted.complete(Unit)
                        throw IOException("Offline")
                    },
                )
            try {
                val repository = createRepository(dao, client, this)
                assertEquals("Bend", repository.getFavoriteLocations().single().name)
                attempted.await()
                repeat(3) { repository.getFavoriteLocations() }
                assertEquals(1, lookups)
                assertNull(repository.getFavoriteLocations().single().admin1)
            } finally {
                client.close()
            }
        }

    @Test
    fun changesNotifyWidgetsWithoutCacheReadRequests() =
        runBlocking {
            val dao = TestWeatherDao()
            val cached = WeatherEntity(1L, 20.0, 0, 50, 5.0, 1L)
            dao.insertWeather(cached)
            var updates = 0
            val client = createClient(MockEngine { throw IOException("Cache-only render requested network") })
            try {
                val repository = createRepository(dao, client, this) { updates++ }
                assertEquals(cached, repository.currentReading(WeatherRefresh.CACHE_ONLY).weather)
                assertEquals(0, updates)
                repository.selectLocation(dao.selectedLocation)
                repository.setUnits(WeatherUnits())
                kotlinx.coroutines.yield()
                assertEquals(2, updates)
            } finally {
                client.close()
            }
        }

    private fun createClient(engine: MockEngine) =
        HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun createRepository(
        dao: WeatherDao,
        client: HttpClient,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        refreshWidgets: suspend () -> Unit = {},
    ) = WeatherRepositoryImpl(
        weatherDao = dao,
        weatherApiClient = WeatherApiClient(client),
        airQualityApiClient = AirQualityApiClient(client),
        geocodingApi = GeocodingApi(client),
        userPreferences = UserPreferences(ApplicationProvider.getApplicationContext()),
        detailScope = scope,
        refreshWidgets = refreshWidgets,
    )

    private class TestWeatherDao : WeatherDao {
        private val locations =
            mutableMapOf(
                1L to LocationEntity(1L, "Paris", "France", 48.85, 2.35, true, admin1 = ""),
            )
        val selectedLocation: LocationEntity
            get() = locations.values.first { it.isSelected }
        private val forecasts = mutableMapOf<Long, MutableList<HourlyForecastEntity>>()
        private val dailyForecasts = mutableMapOf<Long, MutableList<DailyForecastEntity>>()
        private val airQuality = mutableMapOf<Long, MutableList<AirQualityEntity>>()

        private val weather = mutableMapOf<Long, WeatherEntity>()

        override suspend fun getWeather(locationId: Long): WeatherEntity? = weather[locationId]

        override suspend fun insertWeather(weather: WeatherEntity) {
            this.weather[weather.locationId] = weather
        }

        override suspend fun getSelectedLocation(): LocationEntity? = locations.values.firstOrNull { it.isSelected }

        override fun observeSelectedLocation(): Flow<LocationEntity?> = flow { emit(getSelectedLocation()) }

        override fun observeSelectedWeather(): Flow<WeatherEntity?> =
            flow {
                emit(getSelectedLocation()?.let { getWeather(it.id) })
            }

        override suspend fun getAllLocations(): List<LocationEntity> = locations.values.toList()

        override suspend fun getLocation(locationId: Long): LocationEntity? = locations[locationId]

        override suspend fun getFavoriteLocations(): List<LocationEntity> = locations.values.filter { it.isFavorite }.sortedBy { it.favoriteOrder }

        override suspend fun insertLocations(locations: List<LocationEntity>) {
            locations.forEach { this.locations[it.id] = it }
        }

        override suspend fun setSelectedLocation(selectedId: Long) {
            locations.replaceAll { id, location -> location.copy(isSelected = id == selectedId) }
        }

        override suspend fun updateFavorite(
            locationId: Long,
            isFavorite: Boolean,
            favoriteOrder: Int,
        ) {
            locations[locationId]?.let {
                locations[locationId] = it.copy(isFavorite = isFavorite, favoriteOrder = favoriteOrder)
            }
        }

        override suspend fun updateFavoriteOrder(
            locationId: Long,
            favoriteOrder: Int,
        ) {
            locations[locationId]?.let { locations[locationId] = it.copy(favoriteOrder = favoriteOrder) }
        }

        override suspend fun updateLocationDetails(
            locationId: Long,
            admin1: String?,
            population: Long?,
        ) {
            locations[locationId]?.let { locations[locationId] = it.copy(admin1 = admin1, population = population) }
        }

        override suspend fun getHourlyForecast(locationId: Long): List<HourlyForecastEntity> = forecasts[locationId].orEmpty()

        override suspend fun deleteHourlyForecast(locationId: Long) {
            forecasts.remove(locationId)
        }

        override suspend fun insertHourlyForecast(forecasts: List<HourlyForecastEntity>) {
            forecasts.groupBy { it.locationId }.forEach { (locationId, rows) ->
                this.forecasts.getOrPut(locationId) { mutableListOf() }.addAll(rows)
            }
        }

        override suspend fun replaceHourlyForecast(
            locationId: Long,
            forecasts: List<HourlyForecastEntity>,
        ) {
            deleteHourlyForecast(locationId)
            insertHourlyForecast(forecasts)
        }

        override suspend fun getDailyForecast(locationId: Long): List<DailyForecastEntity> = dailyForecasts[locationId].orEmpty()

        override suspend fun deleteDailyForecast(locationId: Long) {
            dailyForecasts.remove(locationId)
        }

        override suspend fun insertDailyForecast(forecasts: List<DailyForecastEntity>) {
            forecasts.groupBy { it.locationId }.forEach { (locationId, rows) ->
                dailyForecasts.getOrPut(locationId) { mutableListOf() }.addAll(rows)
            }
        }

        override suspend fun replaceDailyForecast(
            locationId: Long,
            forecasts: List<DailyForecastEntity>,
        ) {
            deleteDailyForecast(locationId)
            insertDailyForecast(forecasts)
        }

        override suspend fun getAirQuality(locationId: Long): List<AirQualityEntity> = airQuality[locationId].orEmpty()

        override suspend fun deleteAirQuality(locationId: Long) {
            airQuality.remove(locationId)
        }

        override suspend fun insertAirQuality(points: List<AirQualityEntity>) {
            points.groupBy { it.locationId }.forEach { (locationId, rows) ->
                airQuality.getOrPut(locationId) { mutableListOf() }.addAll(rows)
            }
        }

        override suspend fun replaceAirQuality(
            locationId: Long,
            points: List<AirQualityEntity>,
        ) {
            deleteAirQuality(locationId)
            insertAirQuality(points)
        }
    }

    private companion object {
        const val FORECAST_JSON = """{"timezone":"America/Los_Angeles","hourly":{"time":["2026-10-01T12:00"],"temperature_2m":[68.0],"precipitation_probability":[20],"precipitation":[0.1],"dew_point_2m":[55.0],"wind_speed_10m":[5.0],"wind_direction_10m":[180],"uv_index":[4.0]},"daily":{"time":["2026-10-01"],"temperature_2m_max":[72.0],"temperature_2m_min":[55.0],"precipitation_sum":[0.2],"precipitation_probability_max":[40],"sunrise":["2026-10-01T06:30"],"sunset":["2026-10-01T18:10"],"weather_code":[2]}}"""
        const val AIR_QUALITY_JSON = """{"timezone":"America/New_York","hourly":{"time":["2026-10-05T12:00"],"us_aqi":[42],"pm2_5":[8.5]}}"""
    }
}
