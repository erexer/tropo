package io.github.erexer.tropo.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.data.DailyForecast
import io.github.erexer.tropo.data.DayNarrative
import io.github.erexer.tropo.data.ForecastTimeline
import io.github.erexer.tropo.data.PrecipitationOutlook
import io.github.erexer.tropo.data.WeatherCondition
import io.github.erexer.tropo.data.WindDirectionFormatter
import io.github.erexer.tropo.data.preferences.CurrentField
import io.github.erexer.tropo.ui.common.WeatherIcon
import io.github.erexer.tropo.ui.theme.WeatherPalette
import io.github.erexer.tropo.ui.theme.WeatherValuePalette
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
@Suppress("FunctionNaming")
internal fun CollapsedConditions(
    state: WeatherUiState,
    collapse: Float,
) {
    if (CurrentField.TEMPERATURE !in state.currentFields && CurrentField.CONDITION !in state.currentFields) {
        return
    }
    val temperature =
        state.weather?.let {
            "${state.units.currentTemperature(it.temperature).toInt()}${state.units.temperature.label}"
        } ?: "–"
    Row(
        modifier = Modifier.graphicsLayer { alpha = collapse },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.weather?.takeIf { CurrentField.CONDITION in state.currentFields }?.let { weather ->
            WeatherIcon(
                weather.weatherCode,
                modifier = Modifier.size(24.dp),
                light = if (weather.isDay) WeatherCondition.Light.DAY else WeatherCondition.Light.NIGHT,
            )
        }
        if (CurrentField.TEMPERATURE in state.currentFields) {
            Text(
                temperature,
                color = state.weather?.let { WeatherPalette.temperatureC(it.temperature) } ?: Color.Unspecified,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
internal fun CurrentConditions(
    state: WeatherUiState,
    colors: ColorScheme,
    onRetry: () -> Unit,
    collapse: Float,
    clock: Instant,
) {
    if (state.currentFields.isEmpty()) {
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val weather = state.weather
        if (weather == null) {
            Text(state.errorMessage ?: "Weather unavailable", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
            return@Column
        }

        val unit = state.units.temperature.label
        if (CurrentField.TEMPERATURE in state.currentFields) {
            Text(
                "${state.units.currentTemperature(weather.temperature).toInt()}$unit",
                modifier = Modifier.graphicsLayer { alpha = 1f - collapse },
                color = WeatherPalette.temperatureC(weather.temperature),
                fontSize = 68.sp,
                lineHeight = 76.sp,
            )
        }
        if (CurrentField.HIGH_LOW in state.currentFields) {
            CurrentHighLow(state, clock)
        }
        if (CurrentField.CONDITION in state.currentFields) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WeatherIcon(
                    weather.weatherCode,
                    modifier = Modifier.size(28.dp).graphicsLayer { alpha = 1f - collapse },
                    light = if (weather.isDay) WeatherCondition.Light.DAY else WeatherCondition.Light.NIGHT,
                )
                Text(WeatherCondition.label(weather.weatherCode), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (CurrentField.NARRATIVE in state.currentFields) {
            TodayNarrative(state, clock)
        }
        CurrentThermal(state, colors)
        CurrentWind(state, colors)
        if (CurrentField.PRECIPITATION in state.currentFields) {
            PrecipitationLine(state, clock)
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun CurrentThermal(
    state: WeatherUiState,
    colors: ColorScheme,
) {
    val weather = state.weather ?: return
    val unit = state.units.temperature.label
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (CurrentField.FEELS_LIKE in state.currentFields) {
            Text(
                buildAnnotatedString {
                    append("Feels like ")
                    withStyle(SpanStyle(color = WeatherPalette.temperatureC(weather.feelsLike))) {
                        append("${state.units.currentTemperature(weather.feelsLike).toInt()}$unit")
                    }
                },
                color = colors.onSurfaceVariant,
            )
        }
        if (CurrentField.HUMIDITY in state.currentFields) {
            Text("Humidity ${weather.humidity}%", color = colors.onSurfaceVariant)
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun CurrentHighLow(
    state: WeatherUiState,
    clock: Instant,
) {
    val day = ForecastTimeline.today(state.dailyForecast, state.forecastTimezone, clock)
    val high = day?.temperatureMax?.takeIf(Float::isFinite)
    val low = day?.temperatureMin?.takeIf(Float::isFinite)
    val highText = high?.let { "${state.units.forecastTemperature(it).toInt()}°" } ?: "–"
    val lowText = low?.let { "${state.units.forecastTemperature(it).toInt()}°" } ?: "–"
    Text(
        buildAnnotatedString {
            append("↑ ")
            withStyle(SpanStyle(color = high?.let { WeatherPalette.temperatureF(it) } ?: Color.Unspecified)) {
                append(highText)
            }
            append(" · ↓ ")
            withStyle(SpanStyle(color = low?.let { WeatherPalette.temperatureF(it) } ?: Color.Unspecified)) {
                append(lowText)
            }
        },
        modifier =
            Modifier.padding(bottom = 6.dp).semantics {
                contentDescription = "Today's high $highText, low $lowText"
            },
        fontSize = 14.sp,
    )
}

@Composable
@Suppress("FunctionNaming")
private fun CurrentWind(
    state: WeatherUiState,
    colors: ColorScheme,
) {
    val weather = state.weather ?: return
    val direction =
        weather.windDirectionDeg
            ?.toFloat()
            ?.takeIf(Float::isFinite)
            ?.let(WindDirectionFormatter::compact) ?: "–"
    if (CurrentField.WIND in state.currentFields) {
        Text(
            "$direction · ${state.units.currentWindText(weather.windSpeed)}",
            color = WeatherPalette.wind(),
            fontSize = 16.sp,
        )
    }
    listOfNotNull(
        weather.windGusts
            ?.takeIf { CurrentField.GUSTS in state.currentFields }
            ?.let { "Gusts ${state.units.currentWindText(it)}" },
        weather.cloudCover?.takeIf { CurrentField.CLOUD_COVER in state.currentFields }?.let { "Cloud $it%" },
    ).takeIf { it.isNotEmpty() }?.let { parts ->
        Text(parts.joinToString(" · "), color = colors.onSurfaceVariant)
    }
}

@Composable
@Suppress("FunctionNaming")
private fun PrecipitationLine(
    state: WeatherUiState,
    clock: Instant,
) {
    if (state.forecast.isEmpty()) return
    val now = state.forecastNow(clock)
    val outlook = PrecipitationOutlook.find(state.forecast, now)
    val color =
        outlook?.let {
            WeatherValuePalette.precipitation(it.kind.type)
        } ?: WeatherPalette.precipitation()
    Text(
        outlook?.label(state.units, now)
            ?: PrecipitationOutlook.emptyLabel(state.forecast, now),
        modifier = Modifier.padding(top = 4.dp),
        color = color,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
@Suppress("FunctionNaming")
private fun TodayNarrative(
    state: WeatherUiState,
    clock: Instant,
) {
    val now = state.forecastNow(clock)
    val segments = DayNarrative.from(state.forecast, now)
    if (segments.isEmpty()) return
    val today = state.dailyForecast.firstOrNull { it.date == now.toLocalDate().toString() }
    Row(
        modifier = Modifier.padding(vertical = 4.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
            WeatherIcon(segment.weatherCode, modifier = Modifier.size(18.dp), light = lightAt(segment.start, today))
            val label =
                if (index == 0) {
                    segment.label
                } else {
                    "${segment.label.lowercase()} after %02d".format(segment.start.hour)
                }
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun lightAt(
    time: LocalDateTime,
    day: DailyForecast?,
): WeatherCondition.Light {
    val sunrise = day?.sunrise?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val sunset = day?.sunset?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val night = sunrise != null && sunset != null && (time.isBefore(sunrise) || !time.isBefore(sunset))
    return if (night) WeatherCondition.Light.NIGHT else WeatherCondition.Light.DAY
}

private fun WeatherUiState.forecastNow(clock: Instant): LocalDateTime {
    val zone = runCatching { ZoneId.of(forecastTimezone) }.getOrDefault(ZoneId.of("UTC"))
    return clock.atZone(zone).toLocalDateTime()
}
