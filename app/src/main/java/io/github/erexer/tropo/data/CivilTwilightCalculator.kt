package io.github.erexer.tropo.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class CivilTwilight(
    val firstLight: LocalDateTime?,
    val lastLight: LocalDateTime?,
)

object CivilTwilightCalculator {
    private const val CIVIL_ZENITH_DEGREES = 96.0
    private const val LONGITUDE_HOURS = 15.0
    private const val MEAN_ANOMALY_FACTOR = 0.9856
    private const val MEAN_ANOMALY_OFFSET = 3.289
    private const val EQUATION_SIN_FACTOR = 1.916
    private const val EQUATION_DOUBLE_SIN_FACTOR = 0.020
    private const val ECLIPTIC_OFFSET = 282.634
    private const val RIGHT_ASCENSION_SCALE = 0.91764
    private const val DECLINATION_SCALE = 0.39782
    private const val LOCAL_TIME_FACTOR = 0.06571
    private const val LOCAL_TIME_OFFSET = 6.622
    private const val HOURS_PER_DAY = 24.0
    private const val SECONDS_PER_HOUR = 3600.0

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        timezone: String,
    ): CivilTwilight {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return CivilTwilight(null, null)
        }

        return CivilTwilight(
            firstLight = eventTime(date, latitude, longitude, zone, isSunrise = true),
            lastLight = eventTime(date, latitude, longitude, zone, isSunrise = false),
        )
    }

    private fun eventTime(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        isSunrise: Boolean,
    ): LocalDateTime? {
        val longitudeHour = longitude / LONGITUDE_HOURS
        val localApproximateHour = (if (isSunrise) 6.0 else 18.0) - longitudeHour
        val approximateTime = date.dayOfYear + localApproximateHour / HOURS_PER_DAY
        val meanAnomaly = MEAN_ANOMALY_FACTOR * approximateTime - MEAN_ANOMALY_OFFSET
        val trueLongitude =
            normalizeDegrees(
                meanAnomaly +
                    EQUATION_SIN_FACTOR * sin(degreesToRadians(meanAnomaly)) +
                    EQUATION_DOUBLE_SIN_FACTOR * sin(2 * degreesToRadians(meanAnomaly)) +
                    ECLIPTIC_OFFSET,
            )
        var rightAscension =
            normalizeDegrees(
                radiansToDegrees(atan(RIGHT_ASCENSION_SCALE * tan(degreesToRadians(trueLongitude)))),
            )
        rightAscension += floor(trueLongitude / 90.0) * 90.0 - floor(rightAscension / 90.0) * 90.0
        rightAscension /= LONGITUDE_HOURS

        val sinDeclination = DECLINATION_SCALE * sin(degreesToRadians(trueLongitude))
        val cosDeclination = cos(asin(sinDeclination))
        val cosHourAngle =
            (
                cos(degreesToRadians(CIVIL_ZENITH_DEGREES)) -
                    sinDeclination * sin(degreesToRadians(latitude))
            ) / (cosDeclination * cos(degreesToRadians(latitude)))
        if (cosHourAngle !in -1.0..1.0) {
            return null
        }

        var hourAngle = radiansToDegrees(acos(cosHourAngle))
        if (isSunrise) {
            hourAngle = 360.0 - hourAngle
        }
        val localMeanTime =
            hourAngle / LONGITUDE_HOURS + rightAscension -
                LOCAL_TIME_FACTOR * approximateTime - LOCAL_TIME_OFFSET
        val utcHour = normalizeHours(localMeanTime - longitudeHour)
        val instant =
            Instant
                .parse("${date}T00:00:00Z")
                .plusSeconds((utcHour * SECONDS_PER_HOUR).toLong())
        var localDateTime = instant.atZone(zone).toLocalDateTime()
        if (localDateTime.toLocalDate().isAfter(date)) {
            localDateTime = localDateTime.minusDays(1)
        } else if (localDateTime.toLocalDate().isBefore(date)) {
            localDateTime = localDateTime.plusDays(1)
        }
        return localDateTime
    }

    private fun degreesToRadians(degrees: Double) = Math.toRadians(degrees)

    private fun radiansToDegrees(radians: Double) = Math.toDegrees(radians)

    private fun normalizeDegrees(degrees: Double) = ((degrees % 360.0) + 360.0) % 360.0

    private fun normalizeHours(hours: Double) = ((hours % HOURS_PER_DAY) + HOURS_PER_DAY) % HOURS_PER_DAY
}
