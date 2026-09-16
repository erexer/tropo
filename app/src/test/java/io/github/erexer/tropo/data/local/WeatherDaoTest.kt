package io.github.erexer.tropo.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetWeather() = runBlocking {
        val entity = WeatherEntity(1L, 22.5, 0, 50, 10.0, System.currentTimeMillis())
        dao.insertWeather(entity)
        val result = dao.getWeather(1L)
        assertEquals(22.5, result?.temperature ?: 0.0, 0.01)
    }
}