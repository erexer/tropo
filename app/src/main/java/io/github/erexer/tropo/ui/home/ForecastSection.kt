package io.github.erexer.tropo.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.R
import io.github.erexer.tropo.data.CivilTwilightCalculator
import io.github.erexer.tropo.data.DailyForecast
import io.github.erexer.tropo.data.DailyStatFormatter
import io.github.erexer.tropo.data.ForecastTimeFormatter
import io.github.erexer.tropo.data.ForecastTimeline
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.TemperatureConverter
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.preferences.GraphLayer
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.ui.GraphOverlay
import io.github.erexer.tropo.ui.WeatherGraph
import io.github.erexer.tropo.ui.WeatherGraphOptions
import io.github.erexer.tropo.ui.theme.WeatherValuePalette
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal data class ForecastFilterState(
    val layers: Set<GraphLayer>,
) {
    val showUv get() = GraphLayer.UV in layers
    val showDewPoint get() = GraphLayer.DEW_POINT in layers
    val showFeelsLike get() = GraphLayer.FEELS_LIKE in layers
    val showWind get() = GraphLayer.WIND in layers
    val showAirQuality get() = GraphLayer.AQI in layers
    val overlayOrder get() = listOf(GraphOverlay.WIND, GraphOverlay.UV, GraphOverlay.AIR_QUALITY)
}

internal data class ForecastActions(
    val onFiltersChanged: (ForecastFilterState) -> Unit,
    val onPointSelected: (Int) -> Unit,
    val onHourlyFieldsChanged: (Set<HourlyField>) -> Unit,
    val onNow: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
@Suppress("FunctionNaming")
internal fun ForecastSection(
    state: WeatherUiState,
    filters: ForecastFilterState,
    clock: Instant,
    actions: ForecastActions,
    textColor: Color,
) {
    val points = state.forecast
    val chartPoints = if (state.isCelsius) points.map(HourlyPoint::toCelsius) else points
    val currentIndex = ForecastTimeline.currentIndex(points, state.forecastTimezone, clock)
    Column {
        ForecastHeading(state.forecastDays, actions.onNow)
        ForecastStatus(state, actions.onRetry)
        val date = points.getOrNull(state.selectedForecastIndex)?.forecastTime?.take(DATE_LENGTH)
        if (state.dailyStats.isNotEmpty()) {
            state.dailyForecast.firstOrNull { it.date == date }?.let { day ->
                DailyForecastDetails(day, state)
            }
        }
        WeatherGraph(
            points = chartPoints,
            selectedIndex = state.selectedForecastIndex,
            onPointSelected = actions.onPointSelected,
            modifier = Modifier.fullBleed(SCREEN_HORIZONTAL_PADDING_DP.dp),
            options =
                WeatherGraphOptions(
                    showUv = filters.showUv,
                    showDewPoint = filters.showDewPoint,
                    showFeelsLike = filters.showFeelsLike,
                    showWind = filters.showWind,
                    showAirQuality = filters.showAirQuality,
                    overlayOrder = filters.overlayOrder,
                    isCelsius = state.isCelsius,
                    airQuality = state.airQuality,
                    currentIndex = currentIndex,
                    currentTemperature =
                        state.weather?.let {
                            state.units.currentTemperature(it.temperature).toFloat()
                        },
                    daily = state.dailyForecast,
                    timezone = state.forecastTimezone,
                    units = state.units,
                    navigationRevision = state.nowRevision,
                    selectedDate = points.getOrNull(state.selectedForecastIndex)?.forecastTime?.take(DATE_LENGTH),
                ),
        )
        ForecastGraphFilters(filters, actions.onFiltersChanged)
        AirQualityStatus(state, filters)
        HourlySection(state, actions, textColor, clock)
    }
}

@Composable
@Suppress("FunctionNaming")
private fun ForecastHeading(
    days: Int,
    onNow: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$days-day outlook", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onNow) {
            Icon(painterResource(R.drawable.ic_now), "Return forecast to current hour", modifier = Modifier.size(18.dp))
            Text("Now", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun ForecastStatus(
    state: WeatherUiState,
    onRetry: () -> Unit,
) {
    (state.graphError ?: state.forecastErrorMessage ?: state.errorMessage)?.let { error ->
        Text(error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry, enabled = !state.isRefreshing) { Text("Retry") }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun AirQualityStatus(
    state: WeatherUiState,
    filters: ForecastFilterState,
) {
    val airQualityEnd = state.airQuality.lastOrNull()?.forecastTime
    if (filters.showAirQuality && airQualityEnd != null && state.airQuality.size < state.forecast.size) {
        Text(
            "AQI coverage through ${airQualityEnd.take(DATE_LENGTH)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (filters.showAirQuality && state.airQuality.isEmpty()) {
        Text("Air-quality forecast unavailable", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
@Suppress("FunctionNaming")
private fun ForecastGraphFilters(
    filters: ForecastFilterState,
    onFiltersChanged: (ForecastFilterState) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GraphLayer.entries.forEach { layer ->
            FilterChip(
                selected = layer in filters.layers,
                onClick = {
                    val layers = if (layer in filters.layers) filters.layers - layer else filters.layers + layer
                    onFiltersChanged(filters.copy(layers = layers))
                },
                label = { Text(layer.label) },
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun DailyForecastDetails(
    forecast: DailyForecast,
    state: WeatherUiState,
) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            LocalDate.parse(forecast.date).format(DateTimeFormatter.ofPattern("EEE, MMM d", locale)),
            modifier = Modifier.height(28.dp * scale),
            style = MaterialTheme.typography.titleMedium,
        )
        if (DailyStat.SUN_TIMES in state.dailyStats) SunTimes(forecast, state)
        state.dailyStats.filter { it != DailyStat.SUN_TIMES }.forEach { stat ->
            val text =
                when (stat) {
                    DailyStat.UV_MAX -> DailyStatFormatter.uvHigh(forecast, state.forecast)
                    DailyStat.DAYLIGHT -> forecast.daylightSeconds?.let(DailyStatFormatter::daylight)
                    DailyStat.PRECIPITATION_HOURS ->
                        forecast.precipitationHours?.let(DailyStatFormatter::precipitationHours)
                    DailyStat.SUN_TIMES -> null
                }
            Text(
                text ?: "${stat.label} unavailable",
                modifier = Modifier.height((if (stat == DailyStat.UV_MAX) 40.dp else 28.dp) * scale),
                color =
                    if (stat == DailyStat.UV_MAX && text != null) {
                        val high = DailyStatFormatter.uvValue(forecast, state.forecast) ?: 0f
                        WeatherValuePalette.uv(high.roundToInt().toFloat())
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun SunTimes(
    forecast: DailyForecast,
    state: WeatherUiState,
) {
    val twilight =
        state.location?.let {
            CivilTwilightCalculator.calculate(
                LocalDate.parse(forecast.date),
                it.latitude,
                it.longitude,
                state.forecastTimezone,
            )
        }
    val first = twilight?.firstLight?.format(TWENTY_FOUR_HOUR_FORMATTER) ?: "–"
    val last = twilight?.lastLight?.format(TWENTY_FOUR_HOUR_FORMATTER) ?: "–"
    Text(
        "First light $first · Sunrise ${ForecastTimeFormatter.format(forecast.sunrise)}\n" +
            "Sunset ${ForecastTimeFormatter.format(forecast.sunset)} · Last light $last",
        modifier = Modifier.height(48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
}

@Composable
@Suppress("FunctionNaming")
internal fun ForecastError() {
    Text(
        "Hourly forecast unavailable",
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.error,
    )
}

private fun HourlyPoint.toCelsius() =
    copy(
        temperature = TemperatureConverter.fahrenheitToCelsius(temperature),
        dewPoint = TemperatureConverter.fahrenheitToCelsius(dewPoint),
        feelsLike = feelsLike?.let(TemperatureConverter::fahrenheitToCelsius),
    )

// Cancels the parent's horizontal padding so the child spans the full screen width
private fun Modifier.fullBleed(horizontal: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = horizontal.roundToPx() * 2
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    }

internal const val DATE_LENGTH = 10

private val TWENTY_FOUR_HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
