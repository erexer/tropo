package io.github.erexer.tropo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.data.AirQualityPoint
import io.github.erexer.tropo.data.DailyForecast
import io.github.erexer.tropo.data.ForecastTimeline
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.PrecipitationType
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.ui.theme.TemperaturePalette
import io.github.erexer.tropo.ui.theme.WeatherPalette
import io.github.erexer.tropo.ui.theme.WeatherValuePalette
import java.time.LocalDateTime
import kotlin.math.roundToInt

enum class GraphOverlay { WIND, AIR_QUALITY, UV }

data class WeatherGraphOptions(
    val isCelsius: Boolean,
    val showUv: Boolean,
    val showDewPoint: Boolean,
    val showWind: Boolean,
    val showAirQuality: Boolean,
    val airQuality: List<AirQualityPoint>,
    val currentIndex: Int?,
    val currentTemperature: Float?,
    val daily: List<DailyForecast> = emptyList(),
    val timezone: String = "UTC",
    val units: WeatherUnits = WeatherUnits(),
    val navigationRevision: Int = 0,
    val selectedDate: String? = null,
    val overlayOrder: List<GraphOverlay> = GraphOverlay.entries,
    val showFeelsLike: Boolean = false,
)

internal data class AxisSpec(
    val unit: String,
    val maximum: Float,
    val color: Color,
)

@Composable
fun WeatherGraph(
    points: List<HourlyPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit,
    options: WeatherGraphOptions,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val isDark = WeatherPalette.isDark()
    val toFahrenheit: (Float) -> Float =
        if (options.isCelsius) {
            { it * FAHRENHEIT_PER_CELSIUS + FREEZING_F }
        } else {
            { it }
        }
    val precipitationColor = WeatherPalette.precipitation()
    val dewPointColor = WeatherPalette.dewPoint()
    val windColor = WeatherPalette.wind()
    val airQualityColor = WeatherPalette.airQuality()
    val uvColor = WeatherPalette.uv()
    val precipitationColors = points.map { WeatherValuePalette.precipitation(PrecipitationType.expected(it)) }
    val uvColors = points.map { WeatherValuePalette.uv(it.uvIndex) }
    val currentColor = MaterialTheme.colorScheme.primary
    val separatorColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val airQualityByTime = options.airQuality.associateBy { it.forecastTime }
    val windValues = points.map { options.units.forecastWind(it.windSpeed) }
    val aqiValues = points.map { airQualityByTime[it.forecastTime]?.usAqi ?: Float.NaN }
    val times = points.map { LocalDateTime.parse(it.forecastTime) }
    val scale = TemperatureScale.from(points)
    val minimum = scale.minimum
    val maximum = scale.maximum
    val windMaximum = GraphLayout.niceMaximum(windValues.filter(Float::isFinite).maxOrNull() ?: MIN_VALUE_RANGE)
    val aqiMaximum =
        GraphLayout.niceMaximum(
            (aqiValues.filter(Float::isFinite).maxOrNull() ?: AQI_MIN_SCALE).coerceAtLeast(AQI_MIN_SCALE),
        )
    val scrollState = rememberScrollState()
    val headerHeight = dailyHeaderHeight()
    val textScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val rangeCelsius = (maximum - minimum) / if (options.isCelsius) 1f else FAHRENHEIT_PER_CELSIUS
    val chartHeight = GraphLayout.plotHeightDp(rangeCelsius, textScale).dp
    val nightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NIGHT_ALPHA)
    val overlayAxes =
        options.overlayOrder.distinct().mapNotNull { overlay ->
            when (overlay) {
                GraphOverlay.WIND ->
                    AxisSpec(options.units.wind.label, windMaximum, windColor).takeIf { options.showWind }
                GraphOverlay.AIR_QUALITY ->
                    AxisSpec("AQI", aqiMaximum, airQualityColor).takeIf { options.showAirQuality }
                GraphOverlay.UV -> AxisSpec("UV", MAX_UV_INDEX, uvColor).takeIf { options.showUv }
            }
        }
    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .width(AXIS_WIDTH_DP.dp * textScale)
                    .padding(top = headerHeight, start = AXIS_EDGE_PADDING_DP.dp)
                    .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            GraphLayout.ticks(minimum, maximum).forEachIndexed { index, value ->
                val unit = if (index == 0) options.units.temperature.label else "°"
                Text(
                    "${value.roundToInt()}$unit",
                    color = TemperaturePalette.color(toFahrenheit(value), isDark),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val visibleHours = GraphLayout.visibleHours(maxWidth.value, textScale, points.size)
            val hourWidth = maxWidth / visibleHours
            val contentWidth = hourWidth * points.size.coerceAtLeast(1)
            val density = androidx.compose.ui.platform.LocalDensity.current
            LaunchedEffect(selectedIndex, points.firstOrNull()?.forecastTime, hourWidth, options.navigationRevision) {
                selectedIndex?.let { index ->
                    val selectedX = with(density) { (hourWidth * index).roundToPx() }
                    val viewportWidth = with(density) { maxWidth.roundToPx() }
                    if (selectedX < scrollState.value || selectedX >= scrollState.value + viewportWidth) {
                        val dayStart = times.indexOfFirst { it.toLocalDate() == times[index].toLocalDate() }
                        val target = with(density) { (hourWidth * dayStart).roundToPx() }
                        scrollState.animateScrollTo(target.coerceAtLeast(0))
                    }
                }
            }
            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                Box(modifier = Modifier.width(contentWidth).height(headerHeight)) {
                    times.map { it.toLocalDate() }.distinct().forEach { date ->
                        val firstIndex = times.indexOfFirst { it.toLocalDate() == date }
                        val day = options.daily.firstOrNull { it.date == date.toString() }
                        GraphDailySummary(
                            date = date,
                            forecast = day,
                            points = points.filter { it.forecastTime.startsWith(date.toString()) },
                            options = options,
                            modifier =
                                Modifier
                                    .offset(x = hourWidth * firstIndex)
                                    .width(hourWidth * HOURS_PER_DAY)
                                    .clickable { onPointSelected(firstIndex) },
                        )
                    }
                }
                Spacer(
                    modifier =
                        Modifier
                            .width(contentWidth)
                            .height(chartHeight)
                            .pointerInput(points.size) {
                                detectTapGestures { offset ->
                                    onPointSelected(GraphLayout.hourIndex(offset.x, size.width.toFloat(), points.size))
                                }
                            }.drawWithCache {
                                val width = size.width
                                val height = size.height
                                val plotHeight = height
                                val xStep = GraphLayout.hourSpacing(width, points.size)

                                fun temperatureY(value: Float) = scale.y(value, plotHeight)

                                fun valueY(
                                    value: Float,
                                    maxValue: Float,
                                ): Float = plotHeight - value / maxValue.coerceAtLeast(MIN_VALUE_RANGE) * plotHeight

                                fun splitPath(
                                    values: List<Float>,
                                    yFor: (Float) -> Float,
                                ): Pair<Path, Path> {
                                    val earlierPath = Path()
                                    val forecastPath = Path()
                                    var earlierStarted = false
                                    var forecastStarted = false
                                    values.forEachIndexed { index, value ->
                                        if (!value.isFinite()) {
                                            earlierStarted = false
                                            forecastStarted = false
                                        } else {
                                            val isEarlier = options.currentIndex != null && index < options.currentIndex
                                            val path = if (isEarlier) earlierPath else forecastPath
                                            val started = if (isEarlier) earlierStarted else forecastStarted
                                            val pointX = index * xStep
                                            val pointY = yFor(value)
                                            if (started) path.lineTo(pointX, pointY) else path.moveTo(pointX, pointY)
                                            if (isEarlier) earlierStarted = true else forecastStarted = true
                                        }
                                    }
                                    return earlierPath to forecastPath
                                }

                                val (earlierTemperature, forecastTemperature) =
                                    splitPath(
                                        points.map { it.temperature },
                                        ::temperatureY,
                                    )
                                val dewPointPath =
                                    if (options.showDewPoint) {
                                        splitPath(points.map { it.dewPoint }, ::temperatureY)
                                    } else {
                                        null
                                    }
                                val feelsLikePath =
                                    if (options.showFeelsLike) {
                                        splitPath(points.map { it.feelsLike ?: Float.NaN }, ::temperatureY)
                                    } else {
                                        null
                                    }
                                val temperatureBrush =
                                    Brush.verticalGradient(
                                        colorStops =
                                            TemperaturePalette
                                                .gradientStops(toFahrenheit(minimum), toFahrenheit(maximum))
                                                .map { fahrenheit ->
                                                    val fraction =
                                                        (toFahrenheit(maximum) - fahrenheit) /
                                                            (toFahrenheit(maximum) - toFahrenheit(minimum))
                                                    fraction to TemperaturePalette.color(fahrenheit, isDark)
                                                }.toTypedArray(),
                                        startY = 0f,
                                        endY = plotHeight,
                                    )
                                val windMax = windMaximum
                                val windPaths =
                                    if (options.showWind) {
                                        splitPath(windValues) { value -> valueY(value, windMax) }
                                    } else {
                                        null
                                    }
                                val aqiMax = aqiMaximum
                                val aqiPaths =
                                    if (options.showAirQuality) {
                                        splitPath(aqiValues) { value -> valueY(value, aqiMax) }
                                    } else {
                                        null
                                    }
                                val precipitationValues = points.map { it.precipProb }
                                val precipitationMax = MAX_PRECIPITATION_PROBABILITY
                                val barWidth = xStep.coerceAtLeast(MIN_BAR_WIDTH)
                                val probabilityBands =
                                    precipitationValues.mapIndexed { index, value ->
                                        val startY = valueY(value, precipitationMax)
                                        val next = precipitationValues.getOrElse(index + 1) { value }
                                        val endY = valueY(next, precipitationMax)
                                        val startX = index * xStep
                                        val endX = (index + 1) * xStep
                                        Path().apply {
                                            moveTo(startX, plotHeight)
                                            lineTo(startX, startY)
                                            lineTo(endX, endY)
                                            lineTo(endX, plotHeight)
                                            close()
                                        }
                                    }
                                val dayBreakIndices =
                                    points.indices.drop(1).filter { index ->
                                        points[index].forecastTime.take(DATE_LENGTH) != points[index - 1].forecastTime.take(DATE_LENGTH)
                                    }

                                onDrawWithContent {
                                    options.daily.forEach { day ->
                                        val nights = ForecastTimeline.nights(day)
                                        nights.forEach { interval ->
                                            val startX = ForecastTimeline.position(times, interval.first) * xStep
                                            val endX = ForecastTimeline.position(times, interval.second) * xStep
                                            if (endX > startX) {
                                                drawRect(nightColor, Offset(startX, 0f), Size(endX - startX, plotHeight))
                                            }
                                        }
                                        listOfNotNull(nights.firstOrNull()?.second, nights.lastOrNull()?.first).forEach { time ->
                                            val x = ForecastTimeline.position(times, time) * xStep
                                            drawLine(separatorColor, Offset(x, 0f), Offset(x, plotHeight), strokeWidth = 1f)
                                        }
                                    }
                                    GraphLayout.ticks(0f, plotHeight).forEach { y ->
                                        drawLine(separatorColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                                    }
                                    probabilityBands.forEachIndexed { index, band ->
                                        val earlier = options.currentIndex != null && index < options.currentIndex
                                        val color = precipitationColors[index]
                                        drawPath(band, color.copy(alpha = PRECIPITATION_ALPHA))
                                        val startY = valueY(precipitationValues[index], precipitationMax)
                                        val start = Offset(index * xStep, startY)
                                        val next =
                                            precipitationValues.getOrElse(index + 1) { precipitationValues[index] }
                                        val end = Offset((index + 1) * xStep, valueY(next, precipitationMax))
                                        drawLine(
                                            color.copy(alpha = if (earlier) EARLIER_ALPHA else 1f),
                                            start,
                                            end,
                                            strokeWidth = 2f,
                                        )
                                    }

                                    if (options.showUv) {
                                        points.forEachIndexed { index, point ->
                                            if (point.uvIndex > 0) {
                                                val uvHeight = point.uvIndex / MAX_UV_INDEX * plotHeight * UV_BAND_RATIO
                                                drawRect(
                                                    color = uvColors[index].copy(alpha = UV_ALPHA),
                                                    topLeft = Offset(index * xStep - barWidth / 2f, plotHeight - uvHeight),
                                                    size = Size(barWidth, uvHeight),
                                                )
                                            }
                                        }
                                    }

                                    dewPointPath?.let { paths ->
                                        drawPath(paths.first, dewPointColor.copy(alpha = EARLIER_ALPHA), style = Stroke(width = 3f))
                                        drawPath(paths.second, dewPointColor, style = Stroke(width = 3f))
                                    }
                                    drawPath(
                                        earlierTemperature,
                                        temperatureBrush,
                                        alpha = EARLIER_ALPHA,
                                        style = Stroke(width = 4f, cap = StrokeCap.Round),
                                    )
                                    drawPath(
                                        forecastTemperature,
                                        temperatureBrush,
                                        style = Stroke(width = 6f, cap = StrokeCap.Round),
                                    )
                                    feelsLikePath?.let { paths ->
                                        val dotted =
                                            Stroke(
                                                width = 4f,
                                                cap = StrokeCap.Round,
                                                pathEffect =
                                                    PathEffect.dashPathEffect(floatArrayOf(FEELS_DOT, FEELS_GAP)),
                                            )
                                        drawPath(paths.first, temperatureBrush, alpha = EARLIER_ALPHA, style = dotted)
                                        drawPath(paths.second, temperatureBrush, style = dotted)
                                    }
                                    windPaths?.let { paths ->
                                        val windStroke =
                                            Stroke(
                                                width = 4f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(WIND_DASH, WIND_GAP)),
                                            )
                                        drawPath(paths.first, windColor.copy(alpha = EARLIER_ALPHA), style = windStroke)
                                        drawPath(paths.second, windColor, style = windStroke)
                                    }
                                    aqiPaths?.let { paths ->
                                        val aqiStroke =
                                            Stroke(
                                                width = 4f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(AQI_DASH, AQI_GAP)),
                                            )
                                        drawPath(paths.first, airQualityColor.copy(alpha = EARLIER_ALPHA), style = aqiStroke)
                                        drawPath(paths.second, airQualityColor, style = aqiStroke)
                                    }

                                    dayBreakIndices.forEach { index ->
                                        val x = index * xStep
                                        drawLine(
                                            color = separatorColor,
                                            start = Offset(x, 0f),
                                            end = Offset(x, plotHeight),
                                            strokeWidth = 2f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DAY_DIVIDER_DASH, DAY_DIVIDER_GAP)),
                                        )
                                    }

                                    options.currentIndex?.takeIf { it in points.indices }?.let { index ->
                                        val x = index * xStep
                                        drawLine(currentColor, Offset(x, 0f), Offset(x, plotHeight), strokeWidth = 3f)
                                        options.currentTemperature?.let { value ->
                                            drawCircle(currentColor, CURRENT_POINT_RADIUS, Offset(x, temperatureY(value)))
                                            drawCircle(surfaceColor, CURRENT_POINT_INNER_RADIUS, Offset(x, temperatureY(value)))
                                        }
                                    }
                                    selectedIndex?.takeIf { it in points.indices }?.let { index ->
                                        val x = index * xStep
                                        drawLine(
                                            selectedColor.copy(alpha = SELECTED_LINE_ALPHA),
                                            Offset(x, 0f),
                                            Offset(x, plotHeight),
                                            strokeWidth = 2f,
                                        )
                                        val y = temperatureY(points[index].temperature)
                                        drawCircle(selectedColor, SELECTED_POINT_RADIUS, Offset(x, y))
                                        drawCircle(surfaceColor, SELECTED_POINT_INNER_RADIUS, Offset(x, y))
                                    }
                                }
                            },
                )
            }
        }
        Row(modifier = Modifier.padding(start = AXIS_EDGE_PADDING_DP.dp, end = AXIS_EDGE_PADDING_DP.dp)) {
            (listOf(AxisSpec("%", MAX_PRECIPITATION_PROBABILITY, precipitationColor)) + overlayAxes).forEach { axis ->
                ValueAxis(axis, headerHeight, chartHeight, textScale)
            }
        }
    }
}

private const val AXIS_WIDTH_DP = 34
private const val AXIS_EDGE_PADDING_DP = 4
private const val HOURS_PER_DAY = 24
internal const val FAHRENHEIT_PER_CELSIUS = 1.8f
internal const val FREEZING_F = 32f
private const val FEELS_DOT = 1f
private const val FEELS_GAP = 9f
private const val NIGHT_ALPHA = 0.07f
private const val WIND_DASH = 12f
private const val WIND_GAP = 6f
private const val AQI_DASH = 3f
private const val AQI_GAP = 5f

private const val MIN_VALUE_RANGE = 1f
private const val AQI_MIN_SCALE = 50f
private const val MAX_PRECIPITATION_PROBABILITY = 100f
private const val MIN_BAR_WIDTH = 2f
private const val PRECIPITATION_ALPHA = 0.25f
private const val MAX_UV_INDEX = 12f
private const val UV_BAND_RATIO = 1f
private const val UV_ALPHA = 0.12f
private const val EARLIER_ALPHA = TemperaturePalette.PAST_ALPHA
private const val DAY_DIVIDER_DASH = 8f
private const val DAY_DIVIDER_GAP = 6f
private const val DATE_LENGTH = 10
private const val CURRENT_POINT_RADIUS = 12f
private const val CURRENT_POINT_INNER_RADIUS = 6f
private const val SELECTED_POINT_RADIUS = 10f
private const val SELECTED_POINT_INNER_RADIUS = 5f
private const val SELECTED_LINE_ALPHA = 0.5f
