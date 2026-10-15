package io.github.erexer.tropo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.data.DailyForecast
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.PrecipitationType
import io.github.erexer.tropo.data.WindDirectionFormatter
import io.github.erexer.tropo.ui.common.WeatherIcon
import io.github.erexer.tropo.ui.theme.TemperaturePalette
import io.github.erexer.tropo.ui.theme.WeatherPalette
import io.github.erexer.tropo.ui.theme.WeatherValuePalette
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@Suppress("FunctionNaming")
internal fun ValueAxis(
    axis: AxisSpec,
    headerHeight: Dp,
    chartHeight: Dp,
    textScale: Float,
) {
    val unitHeight = AXIS_UNIT_HEIGHT_DP.dp * textScale
    Column(
        modifier =
            Modifier
                .width(VALUE_AXIS_WIDTH_DP.dp * textScale)
                .padding(top = (headerHeight - unitHeight).coerceAtLeast(0.dp)),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            axis.unit,
            color = axis.color,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            modifier = Modifier.height(unitHeight),
        )
        Column(
            modifier = Modifier.height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            GraphLayout.ticks(0f, axis.maximum).forEach { value ->
                Text(
                    GraphLayout.tickLabel(value),
                    color = if (axis.unit == "UV") WeatherValuePalette.uv(value) else axis.color,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
internal fun GraphDailySummary(
    date: LocalDate,
    forecast: DailyForecast?,
    points: List<HourlyPoint>,
    options: WeatherGraphOptions,
    modifier: Modifier,
) {
    val zone = runCatching { ZoneId.of(options.timezone) }.getOrDefault(ZoneId.of("UTC"))
    val largeText = LocalDensity.current.fontScale > 1f
    val locale = LocalConfiguration.current.locales[0]
    val label =
        when {
            date == LocalDate.now(zone) -> if (largeText) "Today" else "TODAY"
            largeText -> date.format(DateTimeFormatter.ofPattern("EEE\ndd", locale))
            else -> date.format(DateTimeFormatter.ofPattern("EEE dd", locale))
        }
    val amount = forecast?.precipitationSum?.let(options.units::precipitation)
    val selected = options.selectedDate == date.toString()
    val precipitationTone = WeatherValuePalette.precipitation(PrecipitationType.fromCode(forecast?.weatherCode))
    Column(
        modifier =
            modifier
                .height(dailyHeaderHeight())
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            modifier =
                Modifier.height(
                    (if (largeText) 28.dp else 14.dp) * LocalDensity.current.fontScale.coerceAtLeast(1f),
                ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        forecast?.let { WeatherIcon(it.weatherCode, modifier = Modifier.size(24.dp)) }
            ?: Text("–", fontSize = 20.sp, lineHeight = 24.sp)
        GraphDayTemperatures(forecast, points, options)
        GraphDayWind(points, options)
        Text(
            "${forecast?.precipitationProbabilityMax?.toInt() ?: "–"}%",
            color = precipitationTone,
            fontSize = 12.sp,
            lineHeight = 14.sp,
        )
        Text(
            amount?.let { "%.2f %s".format(Locale.getDefault(), it, options.units.rain.label) } ?: "–",
            color = precipitationTone,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun GraphDayTemperatures(
    forecast: DailyForecast?,
    points: List<HourlyPoint>,
    options: WeatherGraphOptions,
) {
    val highF = forecast?.temperatureMax ?: points.maxOfOrNull { options.fahrenheit(it.temperature) }
    val lowF = forecast?.temperatureMin ?: points.minOfOrNull { options.fahrenheit(it.temperature) }
    val high = highF?.let(options.units::forecastTemperature)
    val low = lowF?.let(options.units::forecastTemperature)
    val isDark = WeatherPalette.isDark()
    val highColor = highF?.let { TemperaturePalette.color(it, isDark) }
    val lowColor = lowF?.let { TemperaturePalette.color(it, isDark) }
    val largeText = LocalDensity.current.fontScale > 1f
    val label =
        buildAnnotatedString {
            if (largeText) append("↑ ")
            withStyle(SpanStyle(color = highColor ?: Color.Unspecified)) { append("${high?.toInt() ?: "–"}°") }
            append(if (largeText) "\n↓ " else "|")
            withStyle(SpanStyle(color = lowColor ?: Color.Unspecified)) { append("${low?.toInt() ?: "–"}°") }
        }
    Text(label, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 2)
}

@Composable
@Suppress("FunctionNaming")
private fun GraphDayWind(
    points: List<HourlyPoint>,
    options: WeatherGraphOptions,
) {
    val wind = points.filter { it.windSpeed.isFinite() }.maxByOrNull { it.windSpeed }
    val speed = wind?.let { options.units.forecastWindText(it.windSpeed) } ?: "–"
    val arrow =
        wind
            ?.windDirectionDeg
            ?.takeIf(
                Float::isFinite,
            )?.let { WindDirectionFormatter.describeWindFrom(it).substringBefore(" from") }
    Text(
        if (arrow == null) speed else "$arrow\u00A0$speed",
        fontSize = 12.sp,
        lineHeight = 14.sp,
        maxLines = 2,
        modifier = Modifier.height(28.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
internal fun dailyHeaderHeight(): Dp {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val extra = if (scale > 1f) LARGE_TEXT_HEADER_EXTRA_DP.dp else 0.dp
    return DAILY_HEADER_HEIGHT_DP.dp * scale + extra
}

private fun WeatherGraphOptions.fahrenheit(value: Float): Float =
    if (isCelsius) {
        value * FAHRENHEIT_PER_CELSIUS + FREEZING_F
    } else {
        value
    }

private const val VALUE_AXIS_WIDTH_DP = 28
private const val AXIS_UNIT_HEIGHT_DP = 14
private const val DAILY_HEADER_HEIGHT_DP = 112
private const val LARGE_TEXT_HEADER_EXTRA_DP = 48
