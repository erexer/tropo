package io.github.erexer.tropo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.erexer.tropo.data.HourlyPoint

@Composable
fun WeatherChart(
    points: List<HourlyPoint>,
    showUv: Boolean,
    showDewPoint: Boolean,
    showWind: Boolean,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val tempColor = MaterialTheme.colorScheme.error
    val dewPointColor = MaterialTheme.colorScheme.tertiary
    val precipColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nodeBgColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .pointerInput(points.size) {
                detectTapGestures { offset ->
                    val xStep = size.width / (points.size - 1).coerceAtLeast(1)
                    val clickedIdx = (offset.x / xStep).toInt().coerceIn(0, points.size - 1)
                    onPointSelected(clickedIdx)
                }
            }
            .drawWithCache {
                val width = size.width
                val height = size.height
                val paddingBottom = 40f
                val chartHeight = height - paddingBottom
                val xStep = width / (points.size - 1).coerceAtLeast(1)

                val minTemp = points.minOf { it.temperature }.coerceAtMost(points.minOf { it.dewPoint }) - 5f
                val maxTemp = points.maxOf { it.temperature }.coerceAtLeast(points.maxOf { it.dewPoint }) + 5f
                val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)

                fun getTempY(temp: Float): Float =
                    chartHeight - ((temp - minTemp) / tempRange * chartHeight)

                fun getPrecipY(prob: Float): Float =
                    chartHeight - ((prob / 100f).coerceIn(0f, 1f) * (chartHeight * 0.6f))

                val precipPath = Path().apply {
                    moveTo(0f, chartHeight)
                    points.forEachIndexed { i, pt ->
                        lineTo(i * xStep, getPrecipY(pt.precipProb))
                    }
                    lineTo(width, chartHeight)
                    close()
                }

                val tempPath = Path().apply {
                    points.forEachIndexed { i, pt ->
                        val x = i * xStep
                        val y = getTempY(pt.temperature)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                val dewPath = if (showDewPoint) {
                    Path().apply {
                        points.forEachIndexed { i, pt ->
                            val x = i * xStep
                            val y = getTempY(pt.dewPoint)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                } else null

                onDrawWithContent {
                    drawPath(path = precipPath, color = precipColor)

                    if (showUv) {
                        val maxUv = 12f
                        points.forEachIndexed { i, pt ->
                            if (pt.uvIndex > 0) {
                                val barHeight = (pt.uvIndex / maxUv) * (chartHeight * 0.35f)
                                val barColor = when {
                                    pt.uvIndex < 3 -> Color(0xFF4CAF50)
                                    pt.uvIndex < 6 -> Color(0xFFFFEB3B)
                                    pt.uvIndex < 8 -> Color(0xFFFF9800)
                                    pt.uvIndex < 11 -> Color(0xFFF44336)
                                    else -> Color(0xFF9C27B0)
                                }
                                drawRect(
                                    color = barColor.copy(alpha = 0.5f),
                                    topLeft = Offset(i * xStep - (xStep * 0.3f), chartHeight - barHeight),
                                    size = Size(xStep * 0.6f, barHeight)
                                )
                            }
                        }
                    }

                    dewPath?.let { path ->
                        drawPath(path = path, color = dewPointColor, style = Stroke(width = 3f))
                    }

                    drawPath(path = tempPath, color = tempColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

                    selectedIndex?.let { idx ->
                        if (idx in points.indices) {
                            val cx = idx * xStep
                            val cy = getTempY(points[idx].temperature)

                            drawLine(
                                color = crosshairColor.copy(alpha = 0.5f),
                                start = Offset(cx, 0f),
                                end = Offset(cx, height),
                                strokeWidth = 2f
                            )
                            drawCircle(color = tempColor, radius = 10f, center = Offset(cx, cy))
                            drawCircle(color = nodeBgColor, radius = 5f, center = Offset(cx, cy))
                        }
                    }
                }
            }
    )
}