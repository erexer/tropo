package com.tropo.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherDaoTest {

    private lateinit var database: TropoDatabase
    private lateinit var dao: WeatherDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TropoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.weatherDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insertLocation and getPrimaryLocation returns inserted entity`() = runTest {
        val location = LocationEntity(
            id = "loc_1",
            name = "Seattle",
            latitude = 47.6062,
            longitude = -122.3321,
            isPrimary = true
        )

        dao.insertLocation(location)
        val retrieved = dao.getPrimaryLocation()

        assertNotNull(retrieved)
        assertEquals("Seattle", retrieved?.name)
        assertEquals(true, retrieved?.isPrimary)
    }

    @Test
    fun `getPrimaryLocationWithWeather returns populated relation`() = runTest {
        val location = LocationEntity(
            id = "loc_seattle",
            name = "Seattle",
            latitude = 47.6062,
            longitude = -122.3321,
            isPrimary = true
        )
        val weather = WeatherEntity(
            locationId = "loc_seattle",
            currentTemperature = 18.5,
            windSpeed = 12.3,
            weatherCode = 61,
            conditionDescription = "Rain",
            updatedAtTimestamp = System.currentTimeMillis()
        )

        dao.insertLocation(location)
        dao.insertWeather(weather)

        val relation = dao.getPrimaryLocationWithWeather()

        assertNotNull(relation)
        assertEquals("Seattle", relation?.location?.name)
        assertEquals(18.5, relation?.weather?.currentTemperature ?: 0.0, 0.01)
    }
}