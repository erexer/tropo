package io.github.erexer.tropo.data.local

import androidx.room.TypeConverter
import io.github.erexer.tropo.data.HourlyPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromHourlyPointList(value: List<HourlyPoint>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toHourlyPointList(value: String): List<HourlyPoint> {
        return Json.decodeFromString(value)
    }
}