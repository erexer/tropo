package io.github.erexer.tropo.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TropoDatabaseMigrationTest {
    @Test
    fun versionSevenPreservesRowsAndLeavesNewDetailsUnknown() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val databaseName = "migration-seven-test.db"
            context.deleteDatabase(databaseName)
            context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { oldDatabase ->
                seedVersionSeven(oldDatabase)
                oldDatabase.version = 7
            }
            val database =
                Room
                    .databaseBuilder(context, TropoDatabase::class.java, databaseName)
                    .addMigrations(TropoDatabase.MIGRATION_7_8)
                    .allowMainThreadQueries()
                    .build()
            try {
                val dao = database.weatherDao()
                val hourly = dao.getHourlyForecast(1L).single()
                assertEquals(0.1f, hourly.precipInches, 0.001f)
                listOf(
                    hourly.feelsLike,
                    hourly.windGusts,
                    hourly.cloudCover,
                    hourly.freezingLevelMeters,
                    hourly.snowDepthMeters,
                    hourly.visibilityMeters,
                ).forEach { assertNull(it) }
                val current = dao.getWeather(1L)!!
                assertEquals(225.0, current.windDirectionDeg!!, 0.001)
                assertEquals(19.0, current.feelsLike, 0.001)
                assertNull(current.windGusts)
                assertNull(current.cloudCover)
                val daily = dao.getDailyForecast(1L).single()
                assertEquals("2026-10-06", daily.date)
                assertEquals("America/Chicago", daily.timezone)
                assertEquals(72f, daily.temperatureMax, 0.001f)
                assertNull(daily.uvIndexMax)
                assertNull(daily.daylightSeconds)
                assertNull(daily.precipitationHours)
                val location = dao.getFavoriteLocations().single()
                assertEquals("Springfield", location.name)
                assertTrue(location.isSelected)
                assertEquals(2, location.favoriteOrder)
                assertNull(location.admin1)
                assertNull(location.population)
            } finally {
                database.close()
                context.deleteDatabase(databaseName)
            }
        }

    private fun seedVersionSeven(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE `weather` (
                `locationId` INTEGER NOT NULL PRIMARY KEY, `temperature` REAL NOT NULL,
                `weatherCode` INTEGER NOT NULL, `humidity` INTEGER NOT NULL,
                `windSpeed` REAL NOT NULL, `updatedAt` INTEGER NOT NULL,
                `feelsLike` REAL NOT NULL DEFAULT 0.0, `isDay` INTEGER NOT NULL DEFAULT 1,
                `windDirectionDeg` REAL
            )""",
        )
        database.execSQL("INSERT INTO `weather` VALUES (1, 20, 0, 50, 5, 100, 19, 1, 225)")
        database.execSQL(
            """CREATE TABLE `locations` (
                `id` INTEGER NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `country` TEXT NOT NULL,
                `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `isSelected` INTEGER NOT NULL,
                `isFavorite` INTEGER NOT NULL DEFAULT 0, `favoriteOrder` INTEGER NOT NULL DEFAULT 0
            )""",
        )
        database.execSQL("INSERT INTO `locations` VALUES (1, 'Springfield', 'United States', 39.78, -89.65, 1, 1, 2)")
        seedHourly(database)
        listOf("rainInches REAL", "snowCentimeters REAL", "weatherCode INTEGER", "humidity REAL").forEach {
            database.execSQL("ALTER TABLE `hourly_forecasts` ADD COLUMN $it")
        }
        seedDailyAndAqi(database)
    }

    private fun seedDailyAndAqi(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE `daily_forecasts` (
                `locationId` INTEGER NOT NULL, `date` TEXT NOT NULL, `timezone` TEXT NOT NULL,
                `temperatureMax` REAL NOT NULL, `temperatureMin` REAL NOT NULL,
                `precipitationSum` REAL NOT NULL, `precipitationProbabilityMax` REAL NOT NULL,
                `sunrise` TEXT NOT NULL, `sunset` TEXT NOT NULL, `weatherCode` INTEGER NOT NULL,
                `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`locationId`, `date`)
            )""",
        )
        database.execSQL(
            """INSERT INTO `daily_forecasts` VALUES (
                1, '2026-10-06', 'America/Chicago', 72, 50, 0.2, 40,
                '2026-10-06T07:00', '2026-10-06T18:30', 2, 100
            )""",
        )
        database.execSQL(
            """CREATE TABLE `air_quality` (
                `locationId` INTEGER NOT NULL, `forecastTime` TEXT NOT NULL, `timezone` TEXT NOT NULL,
                `usAqi` REAL NOT NULL, `pm25` REAL, `fetchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`locationId`, `forecastTime`)
            )""",
        )
    }

    @Test
    fun versionOneDatabaseMigratesToDailyForecastsFavoritesAndAirQuality() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val databaseName = "migration-test.db"
            context.deleteDatabase(databaseName)

            val oldDatabase = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
            oldDatabase.execSQL(
                """CREATE TABLE `weather` (
                `locationId` INTEGER NOT NULL,
                `temperature` REAL NOT NULL,
                `weatherCode` INTEGER NOT NULL,
                `humidity` INTEGER NOT NULL,
                `windSpeed` REAL NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`locationId`)
            )""",
            )
            oldDatabase.execSQL(
                """CREATE TABLE `locations` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `country` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `isSelected` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )""",
            )
            seedHourly(oldDatabase)
            oldDatabase.execSQL("INSERT INTO `weather` VALUES (1, 20.0, 0, 50, 5.0, 100)")
            oldDatabase.version = 1
            oldDatabase.close()

            val database =
                Room
                    .databaseBuilder(context, TropoDatabase::class.java, databaseName)
                    .addMigrations(
                        TropoDatabase.MIGRATION_1_2,
                        TropoDatabase.MIGRATION_2_3,
                        TropoDatabase.MIGRATION_3_4,
                        TropoDatabase.MIGRATION_4_5,
                        TropoDatabase.MIGRATION_5_6,
                        TropoDatabase.MIGRATION_6_7,
                        TropoDatabase.MIGRATION_7_8,
                    ).allowMainThreadQueries()
                    .build()

            try {
                val preserved = database.weatherDao().getHourlyForecast(1L).single()
                assertEquals(0.1f, preserved.precipInches, 0.001f)
                assertNull(preserved.rainInches)
                assertNull(preserved.snowCentimeters)
                assertNull(preserved.weatherCode)
                assertNull(preserved.humidity)
                assertNull(preserved.feelsLike)
                assertNull(preserved.visibilityMeters)
                assertEquals(20.0, database.weatherDao().getWeather(1L)?.temperature)
                assertNull(database.weatherDao().getWeather(1L)?.windDirectionDeg)
                assertNull(database.weatherDao().getWeather(1L)?.windGusts)
                assertTrue(database.weatherDao().getDailyForecast(1L).isEmpty())
                assertTrue(database.weatherDao().getFavoriteLocations().isEmpty())
                assertTrue(database.weatherDao().getAirQuality(1L).isEmpty())
            } finally {
                database.close()
                context.deleteDatabase(databaseName)
            }
        }

    private fun seedHourly(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE `hourly_forecasts` (
                `locationId` INTEGER NOT NULL, `forecastTime` TEXT NOT NULL,
                `temperature` REAL NOT NULL, `precipProb` REAL NOT NULL,
                `precipInches` REAL NOT NULL, `dewPoint` REAL NOT NULL,
                `windSpeed` REAL NOT NULL, `windDirectionDeg` REAL NOT NULL,
                `uvIndex` REAL NOT NULL, `fetchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`locationId`, `forecastTime`)
            )""",
        )
        database.execSQL(
            "INSERT INTO `hourly_forecasts` VALUES (1, '2026-10-06T12:00', 68, 50, 0.1, 45, 5, 180, 2, 100)",
        )
    }
}
