package io.github.erexer.tropo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WeatherEntity::class,
        LocationEntity::class,
        HourlyForecastEntity::class,
        DailyForecastEntity::class,
        AirQualityEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class TropoDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        private const val DETAILS_SCHEMA_VERSION = 8
        private val HOURLY_DETAIL_COLUMNS =
            listOf("feelsLike", "windGusts", "cloudCover", "freezingLevelMeters", "snowDepthMeters", "visibilityMeters")
        internal val MIGRATION_7_8 =
            object : Migration(WIND_DIRECTION_SCHEMA_VERSION, DETAILS_SCHEMA_VERSION) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    HOURLY_DETAIL_COLUMNS
                        .forEach { db.execSQL("ALTER TABLE `hourly_forecasts` ADD COLUMN `$it` REAL") }
                    listOf("uvIndexMax", "daylightSeconds", "precipitationHours")
                        .forEach { db.execSQL("ALTER TABLE `daily_forecasts` ADD COLUMN `$it` REAL") }
                    db.execSQL("ALTER TABLE `weather` ADD COLUMN `windGusts` REAL")
                    db.execSQL("ALTER TABLE `weather` ADD COLUMN `cloudCover` INTEGER")
                    db.execSQL("ALTER TABLE `locations` ADD COLUMN `admin1` TEXT")
                    db.execSQL("ALTER TABLE `locations` ADD COLUMN `population` INTEGER")
                }
            }

        private const val WIND_DIRECTION_SCHEMA_VERSION = 7
        internal val MIGRATION_6_7 =
            object : Migration(HUMIDITY_SCHEMA_VERSION, WIND_DIRECTION_SCHEMA_VERSION) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `weather` ADD COLUMN `windDirectionDeg` REAL")
                }
            }

        private const val HUMIDITY_SCHEMA_VERSION = 6
        internal val MIGRATION_5_6 =
            object : Migration(PRECIPITATION_SCHEMA_VERSION, HUMIDITY_SCHEMA_VERSION) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `hourly_forecasts` ADD COLUMN `humidity` REAL")
                }
            }

        private const val PRECIPITATION_SCHEMA_VERSION = 5
        internal val MIGRATION_4_5 =
            object : Migration(4, PRECIPITATION_SCHEMA_VERSION) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `hourly_forecasts` ADD COLUMN `rainInches` REAL")
                    db.execSQL("ALTER TABLE `hourly_forecasts` ADD COLUMN `snowCentimeters` REAL")
                    db.execSQL("ALTER TABLE `hourly_forecasts` ADD COLUMN `weatherCode` INTEGER")
                }
            }

        internal val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `hourly_forecasts` (
                            `locationId` INTEGER NOT NULL,
                            `forecastTime` TEXT NOT NULL,
                            `temperature` REAL NOT NULL,
                            `precipProb` REAL NOT NULL,
                            `precipInches` REAL NOT NULL,
                            `dewPoint` REAL NOT NULL,
                            `windSpeed` REAL NOT NULL,
                            `windDirectionDeg` REAL NOT NULL,
                            `uvIndex` REAL NOT NULL,
                            `fetchedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`locationId`, `forecastTime`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        internal val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `weather` ADD COLUMN `feelsLike` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `weather` ADD COLUMN `isDay` INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE `locations` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `locations` ADD COLUMN `favoriteOrder` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `daily_forecasts` (
                            `locationId` INTEGER NOT NULL,
                            `date` TEXT NOT NULL,
                            `timezone` TEXT NOT NULL,
                            `temperatureMax` REAL NOT NULL,
                            `temperatureMin` REAL NOT NULL,
                            `precipitationSum` REAL NOT NULL,
                            `precipitationProbabilityMax` REAL NOT NULL,
                            `sunrise` TEXT NOT NULL,
                            `sunset` TEXT NOT NULL,
                            `weatherCode` INTEGER NOT NULL,
                            `fetchedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`locationId`, `date`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        internal val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `air_quality` (
                            `locationId` INTEGER NOT NULL,
                            `forecastTime` TEXT NOT NULL,
                            `timezone` TEXT NOT NULL,
                            `usAqi` REAL NOT NULL,
                            `pm25` REAL,
                            `fetchedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`locationId`, `forecastTime`)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
