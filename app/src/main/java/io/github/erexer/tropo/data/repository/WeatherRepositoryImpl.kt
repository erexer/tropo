package io.github.erexer.tropo.data.repository

import io.github.erexer.tropo.data.AirQualityPoint
import io.github.erexer.tropo.data.AirQualitySnapshot
import io.github.erexer.tropo.data.DailyForecast
import io.github.erexer.tropo.data.ForecastSnapshot
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.LengthConverter
import io.github.erexer.tropo.data.local.AirQualityEntity
import io.github.erexer.tropo.data.local.DailyForecastEntity
import io.github.erexer.tropo.data.local.HourlyForecastEntity
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherDao
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.location.DeviceLocationSource
import io.github.erexer.tropo.data.preferences.AppThemeMode
import io.github.erexer.tropo.data.preferences.CurrentField
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.preferences.GraphLayer
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.data.preferences.RainUnit
import io.github.erexer.tropo.data.preferences.TemperatureUnit
import io.github.erexer.tropo.data.preferences.UserPreferences
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.preferences.WindUnit
import io.github.erexer.tropo.data.remote.AirQualityApiClient
import io.github.erexer.tropo.data.remote.GeocodingApi
import io.github.erexer.tropo.data.remote.WeatherApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

private const val HOUR_LABEL_LENGTH = 2
private const val CENTIMETERS_PER_INCH = 2.54f
private const val DETAIL_RETRY_MINUTES = 15L
private const val DETAIL_TIMEOUT_MILLIS = 5_000L
private const val WEATHER_CONCURRENCY = 3
private const val WEATHER_TIMEOUT = 15_000L
private const val WEATHER_TTL_MINUTES = 15L
private val WEATHER_TTL = TimeUnit.MINUTES.toMillis(WEATHER_TTL_MINUTES)

class WeatherRepositoryImpl
    @Suppress("LongParameterList")
    constructor(
        private val weatherDao: WeatherDao,
        private val weatherApiClient: WeatherApiClient,
        private val airQualityApiClient: AirQualityApiClient,
        private val geocodingApi: GeocodingApi,
        private val userPreferences: UserPreferences,
        private val detailScope: CoroutineScope,
        private val refreshWidgets: suspend () -> Unit,
        private val deviceSource: DeviceLocationSource? = null,
    ) : WeatherRepository {
        private val detailAttempts = mutableMapOf<Long, Long>()
        private val detailsRevision = MutableStateFlow(0)

        override fun locationUpdates(): Flow<Int> = detailsRevision

        override fun widgetState(): Flow<WidgetSnapshot> =
            combine(weatherDao.observeSelectedLocation(), weatherDao.observeSelectedWeather(), userPreferences.units) {
                location,
                weather,
                units,
                ->
                WidgetSnapshot(location, weather?.takeIf { it.locationId == location?.id }, units)
            }

        override suspend fun deviceLocation(): Result<LocationEntity> =
            try {
                Result.success(requireNotNull(deviceSource) { "Device location unavailable" }.locate())
            } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                Result.failure(IllegalStateException("Device position timed out. Retry outdoors.", error))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }

        override fun currentFields(): Flow<Set<CurrentField>> = userPreferences.currentFields

        override suspend fun setCurrentFields(fields: Set<CurrentField>) = userPreferences.setCurrentFields(fields)

        override fun graphLayers(): Flow<Set<GraphLayer>> = userPreferences.graphLayers

        override suspend fun setGraphLayers(layers: Set<GraphLayer>) = userPreferences.setGraphLayers(layers)

        override suspend fun getCurrentWeather(): WeatherEntity? = currentReading(WeatherRefresh.FORCE).weather

        override suspend fun currentReading(refresh: WeatherRefresh): LocationWeather {
            val location = getSelectedLocation() ?: return LocationWeather(null, true)
            val cached = weatherDao.getWeather(location.id)
            val stale = cached == null || System.currentTimeMillis() - cached.updatedAt >= WEATHER_TTL
            return if (refresh == WeatherRefresh.CACHE_ONLY || (refresh == WeatherRefresh.STALE && !stale)) {
                LocationWeather(cached, stale)
            } else {
                queueDetails(location)
                fetchCurrent(location)
            }
        }

        override fun locationWeather(
            locations: List<LocationEntity>,
            refresh: WeatherRefresh,
        ): Flow<Map<Long, LocationWeather>> =
            channelFlow {
                val cities = locations.distinctBy { it.id }
                var readings =
                    cities.associate { city ->
                        val cached = weatherDao.getWeather(city.id)
                        val stale = cached == null || System.currentTimeMillis() - cached.updatedAt >= WEATHER_TTL
                        city.id to LocationWeather(cached, stale)
                    }
                send(readings)
                val slots = Semaphore(WEATHER_CONCURRENCY)
                val lock = Mutex()
                coroutineScope {
                    val force = refresh == WeatherRefresh.FORCE
                    val pending =
                        cities.filter {
                            refresh != WeatherRefresh.CACHE_ONLY && (force || readings[it.id]?.isCached != false)
                        }
                    pending.forEach { city ->
                        launch {
                            val reading = slots.withPermit { fetchCurrent(city) }
                            lock.withLock {
                                readings = readings + (city.id to reading)
                                send(readings)
                            }
                        }
                    }
                }
            }

        private suspend fun fetchCurrent(location: LocationEntity): LocationWeather {
            return try {
                val response =
                    withTimeout(WEATHER_TIMEOUT) {
                        weatherApiClient.fetchWeather(location.latitude, location.longitude)
                    }
                val current = response.currentWeather
                if (current == null) {
                    return LocationWeather(weatherDao.getWeather(location.id), true)
                }
                val entity =
                    WeatherEntity(
                        locationId = location.id,
                        temperature = current.temperature,
                        weatherCode = current.weathercode,
                        humidity = current.humidity,
                        windSpeed = current.windspeed,
                        updatedAt = System.currentTimeMillis(),
                        feelsLike = current.feelsLike,
                        isDay = current.isDay == 1,
                        windDirectionDeg = current.windDirection,
                        windGusts = current.windGusts,
                        cloudCover = current.cloudCover,
                    )
                weatherDao.insertWeather(entity)
                if (weatherDao.getSelectedLocation()?.id == location.id) {
                    notifyWidgets()
                }
                LocationWeather(entity, false)
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                LocationWeather(weatherDao.getWeather(location.id), true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LocationWeather(weatherDao.getWeather(location.id), true)
            }
        }

        override suspend fun getForecast(forecastDays: Int): Result<ForecastSnapshot> {
            val location =
                getSelectedLocation()
                    ?: return Result.failure(IllegalStateException("No location selected"))

            return try {
                val response =
                    weatherApiClient.fetchHourlyForecast(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        forecastDays = forecastDays,
                    )
                val hourly = response.hourly
                val valueLists =
                    listOf(
                        hourly.temperature,
                        hourly.precipProbability,
                        hourly.precipitation,
                        hourly.dewPoint,
                        hourly.windSpeed,
                        hourly.windDirection,
                        hourly.uvIndex,
                    )
                val pointCount = minOf(hourly.time.size, valueLists.minOf { it.size })
                require(pointCount > 0) { "Forecast response contained no hourly data" }

                val fetchedAt = System.currentTimeMillis()
                val lengthUnit = { key: String -> response.hourlyUnits[key] }
                val forecasts =
                    (0 until pointCount).map { index ->
                        HourlyForecastEntity(
                            locationId = location.id,
                            forecastTime = hourly.time[index],
                            temperature = hourly.temperature[index],
                            precipProb = hourly.precipProbability[index],
                            precipInches = hourly.precipitation[index],
                            dewPoint = hourly.dewPoint[index],
                            windSpeed = hourly.windSpeed[index],
                            windDirectionDeg = hourly.windDirection[index],
                            uvIndex = hourly.uvIndex[index],
                            fetchedAt = fetchedAt,
                            rainInches =
                                hourly.rain?.getOrNull(index)?.let { rain ->
                                    rain + (hourly.showers?.getOrNull(index) ?: 0f)
                                },
                            snowCentimeters = hourly.snowfall?.getOrNull(index)?.times(CENTIMETERS_PER_INCH),
                            weatherCode = hourly.weatherCode?.getOrNull(index),
                            humidity = hourly.humidity?.getOrNull(index),
                            feelsLike = hourly.apparentTemperature?.getOrNull(index),
                            windGusts = hourly.windGusts?.getOrNull(index),
                            cloudCover = hourly.cloudCover?.getOrNull(index),
                            freezingLevelMeters =
                                LengthConverter.toMeters(
                                    hourly.freezingLevel?.getOrNull(index),
                                    lengthUnit("freezing_level_height"),
                                ),
                            snowDepthMeters =
                                LengthConverter.toMeters(hourly.snowDepth?.getOrNull(index), lengthUnit("snow_depth")),
                            visibilityMeters =
                                LengthConverter.toMeters(hourly.visibility?.getOrNull(index), lengthUnit("visibility")),
                        )
                    }

                val daily =
                    response.daily
                        ?.let { data ->
                            val dailyCount =
                                minOf(
                                    data.time.size,
                                    data.temperatureMax.size,
                                    data.temperatureMin.size,
                                    data.precipitationSum.size,
                                    data.precipitationProbabilityMax.size,
                                    data.sunrise.size,
                                    data.sunset.size,
                                    data.weatherCode.size,
                                )
                            (0 until dailyCount).map { index ->
                                DailyForecastEntity(
                                    locationId = location.id,
                                    date = data.time[index],
                                    timezone = response.timezone ?: "UTC",
                                    temperatureMax = data.temperatureMax[index],
                                    temperatureMin = data.temperatureMin[index],
                                    precipitationSum = data.precipitationSum[index],
                                    precipitationProbabilityMax = data.precipitationProbabilityMax[index],
                                    sunrise = data.sunrise[index],
                                    sunset = data.sunset[index],
                                    weatherCode = data.weatherCode[index],
                                    fetchedAt = fetchedAt,
                                    uvIndexMax = data.uvIndexMax?.getOrNull(index),
                                    daylightSeconds = data.daylightDuration?.getOrNull(index),
                                    precipitationHours = data.precipitationHours?.getOrNull(index),
                                )
                            }
                        }.orEmpty()

                weatherDao.replaceHourlyForecast(location.id, forecasts)
                weatherDao.replaceDailyForecast(location.id, daily)
                Result.success(
                    ForecastSnapshot(
                        hourly = forecasts.map { it.toHourlyPoint() },
                        daily = daily.map { it.toDailyForecast() },
                        timezone = response.timezone ?: "UTC",
                        fetchedAt = fetchedAt,
                        isCached = false,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val cachedForecast = weatherDao.getHourlyForecast(location.id)
                val cachedDailyForecast = weatherDao.getDailyForecast(location.id)
                if (cachedForecast.isNotEmpty() || cachedDailyForecast.isNotEmpty()) {
                    Result.success(
                        ForecastSnapshot(
                            hourly = cachedForecast.map { it.toHourlyPoint() },
                            daily = cachedDailyForecast.map { it.toDailyForecast() },
                            timezone = cachedDailyForecast.firstOrNull()?.timezone ?: "UTC",
                            fetchedAt =
                                maxOf(
                                    cachedForecast.maxOfOrNull { it.fetchedAt } ?: 0L,
                                    cachedDailyForecast.maxOfOrNull { it.fetchedAt } ?: 0L,
                                ),
                            isCached = true,
                        ),
                    )
                } else {
                    Result.failure(error)
                }
            }
        }

        override suspend fun getAirQuality(): Result<AirQualitySnapshot> {
            val location =
                getSelectedLocation()
                    ?: return Result.failure(IllegalStateException("No location selected"))

            return try {
                val response = airQualityApiClient.fetch(location.latitude, location.longitude)
                val pointCount = minOf(response.hourly.time.size, response.hourly.usAqi.size)
                require(pointCount > 0) { "Air-quality response contained no hourly data" }
                val fetchedAt = System.currentTimeMillis()
                val timezone = response.timezone ?: "UTC"
                val rows =
                    (0 until pointCount).map { index ->
                        AirQualityEntity(
                            locationId = location.id,
                            forecastTime = response.hourly.time[index],
                            timezone = timezone,
                            usAqi = response.hourly.usAqi[index],
                            pm25 = response.hourly.pm25?.getOrNull(index),
                            fetchedAt = fetchedAt,
                        )
                    }
                weatherDao.replaceAirQuality(location.id, rows)
                Result.success(AirQualitySnapshot(rows.map { it.toAirQualityPoint() }, timezone, fetchedAt, false))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val cached = weatherDao.getAirQuality(location.id)
                if (cached.isNotEmpty()) {
                    Result.success(
                        AirQualitySnapshot(
                            points = cached.map { it.toAirQualityPoint() },
                            timezone = cached.first().timezone,
                            fetchedAt = cached.maxOf { it.fetchedAt },
                            isCached = true,
                        ),
                    )
                } else {
                    Result.failure(error)
                }
            }
        }

        override suspend fun getSelectedLocation(): LocationEntity? =
            weatherDao.getSelectedLocation() ?: LocationEntity(
                id = 2988507,
                name = "Paris",
                country = "France",
                latitude = 48.8534,
                longitude = 2.3488,
                isSelected = true,
            ).also { weatherDao.insertLocations(listOf(it)) }

        override suspend fun searchLocations(query: String): List<LocationEntity> {
            val response = geocodingApi.searchLocation(query)
            val favorites = weatherDao.getFavoriteLocations().associateBy { it.id }
            return response.results?.map {
                LocationEntity(
                    id = it.id,
                    name = it.name,
                    country = it.country,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isFavorite = favorites.containsKey(it.id),
                    favoriteOrder = favorites[it.id]?.favoriteOrder ?: 0,
                    admin1 = it.admin1.orEmpty(),
                    population = it.population,
                )
            } ?: emptyList()
        }

        override suspend fun getFavoriteLocations(): List<LocationEntity> =
            weatherDao.getFavoriteLocations().also { locations -> locations.forEach(::queueDetails) }

        private fun queueDetails(location: LocationEntity) {
            if (location.admin1 != null) return
            val now = System.nanoTime()
            synchronized(detailAttempts) {
                val last = detailAttempts[location.id]
                if (last != null && now - last < TimeUnit.MINUTES.toNanos(DETAIL_RETRY_MINUTES)) return
                detailAttempts[location.id] = now
            }
            detailScope.launch {
                withTimeoutOrNull(DETAIL_TIMEOUT_MILLIS) { withDetails(location) }
            }
        }

        // Locations saved before state/population were stored are looked up once; "" marks a completed lookup
        private suspend fun withDetails(location: LocationEntity): LocationEntity {
            if (location.admin1 != null) {
                return location
            }
            return try {
                val result = geocodingApi.getLocation(location.id)
                val detailed = location.copy(admin1 = result.admin1.orEmpty(), population = result.population)
                weatherDao.updateLocationDetails(location.id, detailed.admin1, detailed.population)
                detailsRevision.update { it + 1 }
                detailed
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                location
            }
        }

        override suspend fun selectLocation(location: LocationEntity) {
            weatherDao.selectLocation(location)
            notifyWidgets()
        }

        override suspend fun setFavorite(
            location: LocationEntity,
            isFavorite: Boolean,
        ) {
            val savedLocation = weatherDao.getLocation(location.id)
            val favoriteOrder =
                when {
                    !isFavorite -> 0
                    savedLocation?.isFavorite == true -> savedLocation.favoriteOrder
                    else -> (weatherDao.getFavoriteLocations().maxOfOrNull { it.favoriteOrder } ?: -1) + 1
                }
            weatherDao.insertLocations(
                listOf(
                    location.copy(
                        isSelected = savedLocation?.isSelected ?: location.isSelected,
                        isFavorite = isFavorite,
                        favoriteOrder = favoriteOrder,
                    ),
                ),
            )
            weatherDao.updateFavorite(location.id, isFavorite, favoriteOrder)
        }

        override suspend fun reorderFavorites(locationIds: List<Long>) {
            locationIds.forEachIndexed { index, locationId ->
                weatherDao.updateFavoriteOrder(locationId, index)
            }
        }

        override fun themeMode(): Flow<AppThemeMode> = userPreferences.themeMode

        override suspend fun setThemeMode(themeMode: AppThemeMode) {
            userPreferences.setThemeMode(themeMode)
        }

        override fun hourlyFields(): Flow<Set<HourlyField>> = userPreferences.hourlyFields

        override fun units(): Flow<WeatherUnits> = userPreferences.units

        override suspend fun setUnits(units: WeatherUnits) {
            userPreferences.setUnits(units)
            notifyWidgets()
        }

        override suspend fun setTemperature(unit: TemperatureUnit) {
            userPreferences.setTemperature(unit)
            notifyWidgets()
        }

        override suspend fun setWind(unit: WindUnit) {
            userPreferences.setWind(unit)
            notifyWidgets()
        }

        override suspend fun setRain(unit: RainUnit) {
            userPreferences.setRain(unit)
            notifyWidgets()
        }

        private fun notifyWidgets() {
            detailScope.launch {
                try {
                    refreshWidgets()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    return@launch
                }
            }
        }

        override suspend fun setHourlyFields(fields: Set<HourlyField>) {
            userPreferences.setHourlyFields(fields)
        }

        override fun dailyStats(): Flow<Set<DailyStat>> = userPreferences.dailyStats

        override suspend fun setDailyStats(stats: Set<DailyStat>) {
            userPreferences.setDailyStats(stats)
        }

        private fun HourlyForecastEntity.toHourlyPoint() =
            HourlyPoint(
                forecastTime = forecastTime,
                hourLabel = forecastTime.substringAfter("T").take(HOUR_LABEL_LENGTH),
                temperature = temperature,
                precipProb = precipProb,
                precipInches = precipInches,
                dewPoint = dewPoint,
                windSpeed = windSpeed,
                windDirectionDeg = windDirectionDeg,
                uvIndex = uvIndex,
                rainInches = rainInches,
                snowCentimeters = snowCentimeters,
                weatherCode = weatherCode,
                humidity = humidity,
                feelsLike = feelsLike,
                windGusts = windGusts,
                cloudCover = cloudCover,
                freezingLevelMeters = freezingLevelMeters,
                snowDepthMeters = snowDepthMeters,
                visibilityMeters = visibilityMeters,
            )

        private fun DailyForecastEntity.toDailyForecast() =
            DailyForecast(
                date = date,
                temperatureMax = temperatureMax,
                temperatureMin = temperatureMin,
                precipitationSum = precipitationSum,
                precipitationProbabilityMax = precipitationProbabilityMax,
                sunrise = sunrise,
                sunset = sunset,
                weatherCode = weatherCode,
                uvIndexMax = uvIndexMax,
                daylightSeconds = daylightSeconds,
                precipitationHours = precipitationHours,
            )

        private fun AirQualityEntity.toAirQualityPoint() =
            AirQualityPoint(
                forecastTime = forecastTime,
                hourLabel = forecastTime.substringAfter("T").take(HOUR_LABEL_LENGTH),
                usAqi = usAqi,
                pm25 = pm25,
            )
    }
