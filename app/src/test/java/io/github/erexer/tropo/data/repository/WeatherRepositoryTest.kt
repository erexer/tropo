package io.github.erexer.tropo.data.repository

import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherDao
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.preferences.UserPreferences
import io.github.erexer.tropo.data.remote.GeocodingApi
import io.github.erexer.tropo.data.remote.WeatherApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun parseWmoCodeReturnsCorrectString() {
        val client = WeatherApiClient(HttpClient())
        assertEquals("Clear Sky", client.parseWmoCode(0))
        assertEquals("Thunderstorm", client.parseWmoCode(95))
        assertEquals("Unknown", client.parseWmoCode(999))
    }
}