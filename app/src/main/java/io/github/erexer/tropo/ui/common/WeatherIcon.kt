package io.github.erexer.tropo.ui.common

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.github.erexer.tropo.data.PrecipitationType
import io.github.erexer.tropo.data.WeatherCondition
import io.github.erexer.tropo.ui.theme.WeatherPalette
import io.github.erexer.tropo.ui.theme.WeatherValuePalette

@Suppress("MagicNumber")
private data class IconColors(
    val sun: Color,
    val moon: Color,
    val cloud: Color,
    val stormCloud: Color,
    val rain: Color,
    val snow: Color,
    val bolt: Color,
    val fog: Color,
) {
    companion object {
        val light =
            IconColors(
                sun = Color(0xFFF2A900),
                moon = Color(0xFFD4A72C),
                cloud = Color(0xFF94A3B8),
                stormCloud = Color(0xFF64748B),
                rain = Color(0xFF006E96),
                snow = Color(0xFF3B9AD9),
                bolt = Color(0xFFF5B800),
                fog = Color(0xFF8A96A6),
            )
        val dark =
            IconColors(
                sun = Color(0xFFFFC94A),
                moon = Color(0xFFF3D98B),
                cloud = Color(0xFFCBD5E1),
                stormCloud = Color(0xFF94A3B8),
                rain = Color(0xFF54C7EA),
                snow = Color(0xFF9AD8FF),
                bolt = Color(0xFFFFD54F),
                fog = Color(0xFFAAB6C4),
            )
    }
}

@Composable
@Suppress("FunctionNaming")
fun WeatherIcon(
    code: Int,
    modifier: Modifier = Modifier,
    light: WeatherCondition.Light = WeatherCondition.Light.DAY,
) {
    val colors =
        (if (WeatherPalette.isDark()) IconColors.dark else IconColors.light).copy(
            rain = WeatherValuePalette.precipitation(PrecipitationType.fromCode(code)),
            snow = WeatherValuePalette.precipitation(PrecipitationType.SNOW),
        )
    val kind = WeatherCondition.kind(code)
    val vector = remember(kind, light, colors) { weatherVector(kind, light == WeatherCondition.Light.DAY, colors) }
    Icon(vector, WeatherCondition.label(code), modifier = modifier, tint = Color.Unspecified)
}

@Suppress("MagicNumber")
private fun weatherVector(
    kind: WeatherCondition.Kind,
    isDay: Boolean,
    colors: IconColors,
): ImageVector {
    val builder = ImageVector.Builder("weather_${kind.name}_$isDay", 24.dp, 24.dp, 24f, 24f)

    fun fill(
        path: String,
        color: Color,
    ) = builder.addPath(addPathNodes(path), fill = SolidColor(color))

    fun stroke(
        path: String,
        color: Color,
        width: Float = 1.8f,
    ) = builder.addPath(
        addPathNodes(path),
        stroke = SolidColor(color),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
    when (kind) {
        WeatherCondition.Kind.CLEAR ->
            if (isDay) {
                fill(SUN_DISC, colors.sun)
                stroke(SUN_RAYS, colors.sun)
            } else {
                fill(MOON, colors.moon)
            }
        WeatherCondition.Kind.CLOUDY -> {
            if (isDay) {
                fill(SMALL_SUN_DISC, colors.sun)
                stroke(SMALL_SUN_RAYS, colors.sun, 1.6f)
            } else {
                fill(SMALL_MOON, colors.moon)
            }
            fill(LOW_CLOUD, colors.cloud)
        }
        WeatherCondition.Kind.FOG -> stroke(FOG_BANDS, colors.fog, 2f)
        WeatherCondition.Kind.RAIN -> {
            fill(HIGH_CLOUD, colors.cloud)
            stroke(RAIN_DROPS, colors.rain, 2f)
        }
        WeatherCondition.Kind.SNOW -> stroke(SNOWFLAKE, colors.snow, 2f)
        WeatherCondition.Kind.STORM -> {
            fill(HIGH_CLOUD, colors.stormCloud)
            fill(BOLT, colors.bolt)
        }
        WeatherCondition.Kind.OVERCAST, WeatherCondition.Kind.UNKNOWN -> fill(FULL_CLOUD, colors.cloud)
    }
    return builder.build()
}

private const val SUN_DISC = "M12,7A5,5 0,1 0,12 17A5,5 0,1 0,12 7Z"
private const val SUN_RAYS = "M12,1V4M12,20V23M1,12H4M20,12H23M4,4L6,6M18,18L20,20M4,20L6,18M18,6L20,4"
private const val MOON = "M20,15C12,18 7,10 11,3C0,5 0,19 10,21C15,22 19,19 20,15Z"
private const val SMALL_SUN_DISC = "M8,4A4,4 0,1 0,8 12A4,4 0,1 0,8 4Z"
private const val SMALL_SUN_RAYS = "M8,0.8V2M0.8,8H2M2.9,2.9L3.8,3.8M13.1,2.9L12.2,3.8M2.9,13.1L3.8,12.2"
private const val SMALL_MOON = "M12,9C8,10.5 5,7 7,2C2,3.5 1.5,10.5 6.5,12C9,12.7 11.2,11.2 12,9Z"
private const val LOW_CLOUD = "M10,21H19C24,21 24,13 19,13C18,7 9,7 8,14C3,14 4,21 10,21Z"
private const val HIGH_CLOUD = "M6,15H18C24,15 24,7 18,7C17,1 7,1 6,8C0,8 0,15 6,15Z"
private const val FULL_CLOUD = "M6,18H18C24,18 24,9 18,9C17,2 6,2 5,10C-1,10 -1,18 6,18Z"
private const val FOG_BANDS = "M2,6H22M4,10H20M2,14H22M4,18H20"
private const val RAIN_DROPS = "M7,18L5,22M13,18L11,22M19,18L17,22"
private const val SNOWFLAKE =
    "M12,2V22M3,7L21,17M3,17L21,7M8,4L12,8L16,4M8,20L12,16L16,20" +
        "M3,11L7,9L7,5M17,19L17,15L21,13M3,13L7,15L7,19M17,5L17,9L21,11"
private const val BOLT = "M13,14L8.5,19.5H11.5L10,24L15.5,17.5H12.5L14,14Z"
