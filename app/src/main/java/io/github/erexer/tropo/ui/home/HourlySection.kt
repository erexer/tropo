package io.github.erexer.tropo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.R
import io.github.erexer.tropo.data.AirQualityCategory
import io.github.erexer.tropo.data.AirQualityPoint
import io.github.erexer.tropo.data.ForecastTimeline
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.PrecipitationType
import io.github.erexer.tropo.data.WindDirectionFormatter
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.preferences.elevationText
import io.github.erexer.tropo.data.preferences.snowDepthText
import io.github.erexer.tropo.data.preferences.visibilityText
import io.github.erexer.tropo.ui.theme.WeatherPalette
import io.github.erexer.tropo.ui.theme.WeatherValuePalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@Suppress("FunctionNaming")
internal fun HourlySection(
    state: WeatherUiState,
    actions: ForecastActions,
    textColor: Color,
    clock: Instant,
) {
    val locale = LocalConfiguration.current.locales[0]
    val points = state.forecast
    val airQualityByTime = state.airQuality.associateBy { it.forecastTime }
    val hourlyListState = rememberLazyListState()
    val firstVisibleIndex by remember { derivedStateOf { hourlyListState.firstVisibleItemIndex } }
    val visibleHourlyDate =
        points
            .getOrNull(firstVisibleIndex)
            ?.forecastTime
            ?.take(DATE_LENGTH)
            ?.let(LocalDate::parse)
    LaunchedEffect(state.selectedForecastIndex, points.size, state.nowRevision) {
        if (state.selectedForecastIndex in points.indices) {
            hourlyListState.animateScrollToItem(state.selectedForecastIndex)
        }
    }
    Column {
        HourlyHeader(state, actions.onHourlyFieldsChanged, visibleHourlyDate)
        LazyRow(
            state = hourlyListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(points, key = { _, point -> point.forecastTime }) { index, point ->
                val startsDay =
                    index == 0 ||
                        point.forecastTime.take(DATE_LENGTH) != points[index - 1].forecastTime.take(DATE_LENGTH)
                if (startsDay && index != firstVisibleIndex) {
                    Text(
                        LocalDate
                            .parse(point.forecastTime.take(DATE_LENGTH))
                            .format(DateTimeFormatter.ofPattern("EEE\nMMM d", locale)),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                HourlyForecastCard(
                    point = point,
                    state = state,
                    clock = clock,
                    airQuality = airQualityByTime[point.forecastTime],
                    onClick = { actions.onPointSelected(index) },
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HourlyHeader(
    state: WeatherUiState,
    onFieldsChanged: (Set<HourlyField>) -> Unit,
    date: LocalDate?,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val locale = LocalConfiguration.current.locales[0]
    var showHourlyFields by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        date?.let {
            Text(
                "Hourly · ${formatForecastDate(date, state.forecastTimezone, locale)}",
                modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 4.dp),
                color = textColor,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        TextButton(onClick = { showHourlyFields = true }) { Text("Hourly fields") }
    }
    state.hourlyFieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (showHourlyFields) {
        HourlyFieldsDialog(state.hourlyFields, onFieldsChanged, onDismiss = { showHourlyFields = false })
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HourlyFieldsDialog(
    fields: Set<HourlyField>,
    onChange: (Set<HourlyField>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hourly fields") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                HourlyField.entries.forEach { field ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = field in fields,
                                    role = Role.Checkbox,
                                    onValueChange = { selected ->
                                        onChange(if (selected) fields + field else fields - field)
                                    },
                                ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = field in fields, onCheckedChange = null)
                        Text(field.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = { onChange(HourlyField.defaults) }) { Text("Reset to defaults") } },
    )
}

@Composable
@Suppress("FunctionNaming")
private fun HourlyForecastCard(
    point: HourlyPoint,
    state: WeatherUiState,
    clock: Instant,
    airQuality: AirQualityPoint?,
    onClick: () -> Unit,
) {
    val isSelected = state.forecast.getOrNull(state.selectedForecastIndex)?.forecastTime == point.forecastTime
    val isPast = ForecastTimeline.isPast(point.forecastTime, state.forecastTimezone, clock)
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(136.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else if (isPast) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                point.hourLabel + if (isPast) " · Past" else "",
                fontSize = 15.sp,
                color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            val core = HourlyField.defaults.filter { it in state.hourlyFields }
            val extras = HourlyField.entries.filter { it in state.hourlyFields && it !in HourlyField.defaults }
            core.forEach { field ->
                HourlyFieldValue(
                    field,
                    point,
                    airQuality,
                    units = state.units,
                )
            }
            if (core.isNotEmpty() && extras.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            }
            extras.forEach { field -> HourlyFieldValue(field, point, airQuality, state.units) }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HourlyFieldValue(
    field: HourlyField,
    point: HourlyPoint,
    airQuality: AirQualityPoint?,
    units: WeatherUnits,
) {
    val temperatureUnit = units.temperature.label
    when (field) {
        HourlyField.TEMPERATURE ->
            Text(
                units.forecastTemperature(point.temperature).hourlyValue(temperatureUnit),
                color = WeatherPalette.temperatureF(point.temperature),
                fontSize = 19.sp,
                lineHeight = 24.sp,
            )
        HourlyField.PRECIPITATION_AMOUNT -> HourlyPrecipitation(point, units)
        HourlyField.PRECIPITATION_CHANCE ->
            Text(
                "${point.precipProb.hourlyValue("%")} chance",
                color = WeatherValuePalette.precipitation(PrecipitationType.expected(point)),
                fontSize = 13.sp,
            )
        HourlyField.WIND -> {
            Text(
                "${point.windDirectionDeg.takeIf(Float::isFinite)?.let(WindDirectionFormatter::compact) ?: "–"} · " +
                    units.forecastWindText(point.windSpeed),
                color = WeatherPalette.wind(),
                fontSize = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        HourlyField.HUMIDITY -> Text("Humidity ${point.humidity.hourlyValue("%")}", fontSize = 12.sp)
        HourlyField.DEW_POINT -> HourlyTemperature("Dew", point.dewPoint, units)
        HourlyField.UV ->
            Text(
                "UV ${point.uvIndex.hourlyValue("")}",
                color = WeatherValuePalette.uv(point.uvIndex),
                fontSize = 12.sp,
            )
        HourlyField.AQI -> {
            Text("AQI ${airQuality?.usAqi.hourlyValue("")}", color = WeatherPalette.airQuality(), fontSize = 13.sp)
            Text(
                airQuality?.let { AirQualityCategory.label(it.usAqi) } ?: "Unavailable",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        else -> HourlyExtraValue(field, point, units)
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HourlyExtraValue(
    field: HourlyField,
    point: HourlyPoint,
    units: WeatherUnits,
) {
    when (field) {
        HourlyField.FEELS_LIKE -> HourlyTemperature("Feels like", point.feelsLike, units)
        HourlyField.WIND_GUSTS ->
            Text(
                "Gusts ${point.windGusts?.let(units::forecastWindText) ?: "–"}",
                color = WeatherPalette.reading(WeatherPalette.Series.GUSTS),
                fontSize = 12.sp,
                maxLines = 1,
            )
        HourlyField.CLOUD_COVER ->
            Text(
                "Cloud ${point.cloudCover.hourlyValue("%")}",
                color = WeatherPalette.reading(WeatherPalette.Series.CLOUD_COVER),
                fontSize = 12.sp,
            )
        HourlyField.FREEZING_LEVEL ->
            Text(
                "Freezing ${point.freezingLevelMeters?.let(units::elevationText) ?: "–"}",
                fontSize = 12.sp,
                maxLines = 1,
            )
        HourlyField.SNOW_DEPTH ->
            Text(
                "Snow depth ${point.snowDepthMeters?.let(units::snowDepthText) ?: "–"}",
                fontSize = 12.sp,
                maxLines = 1,
            )
        HourlyField.VISIBILITY ->
            Text(
                "Visibility ${point.visibilityMeters?.let(units::visibilityText) ?: "–"}",
                fontSize = 12.sp,
                maxLines = 1,
            )
        else -> Unit
    }
}

private fun Float?.hourlyValue(unit: String): String = if (this != null && isFinite()) "${toInt()}$unit" else "–"

@Composable
@Suppress("FunctionNaming")
private fun HourlyTemperature(
    label: String,
    fahrenheit: Float?,
    units: WeatherUnits,
) {
    Text(
        buildAnnotatedString {
            append("$label ")
            val color =
                fahrenheit?.takeIf(Float::isFinite)?.let { WeatherPalette.temperatureF(it) } ?: Color.Unspecified
            withStyle(SpanStyle(color = color)) {
                append(fahrenheit?.let(units::forecastTemperature).hourlyValue(units.temperature.label))
            }
        },
        fontSize = 12.sp,
    )
}

@Composable
@Suppress("FunctionNaming")
private fun HourlyPrecipitation(
    point: HourlyPoint,
    units: WeatherUnits,
) {
    val locale = LocalConfiguration.current.locales[0]
    val type = PrecipitationType.of(point)
    val color = WeatherValuePalette.precipitation(type)
    val amount = units.precipitation(point.precipInches)
    val unit = units.rain.label
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (type == PrecipitationType.RAIN || type == PrecipitationType.MIXED || type == PrecipitationType.FREEZING) {
            Icon(painterResource(R.drawable.ic_rain), "Rain", modifier = Modifier.size(16.dp), tint = color)
        }
        if (type == PrecipitationType.SNOW || type == PrecipitationType.MIXED) {
            Icon(painterResource(R.drawable.ic_snow), "Snow", modifier = Modifier.size(16.dp), tint = color)
        }
        val prefix =
            when (type) {
                PrecipitationType.FREEZING -> "Freezing "
                PrecipitationType.MIXED -> "Mixed "
                else -> ""
            }
        val amountText = "%.2f".format(locale, amount)
        Text("$prefix$amountText $unit", color = color, fontSize = 14.sp)
    }
    point.snowCentimeters?.takeIf { it > 0f }?.let { depth ->
        val snowfall = "%.1f".format(locale, units.snowfall(depth))
        Text("Snow $snowfall ${units.snowfallLabel}", color = color, fontSize = 12.sp)
    }
}

private fun formatForecastDate(
    date: LocalDate,
    timezone: String,
    locale: Locale,
): String {
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("UTC"))
    val today = LocalDate.now(zone)
    val dateText = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    return when (date) {
        today -> "Today · $dateText"
        today.plusDays(1) -> "Tomorrow · $dateText"
        else -> dateText
    }
}
