package io.github.erexer.tropo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.erexer.tropo.data.TemperatureConverter

object WeatherPalette {
    internal enum class Appearance { LIGHT, DARK }

    internal enum class Series { PRECIPITATION, UV, DEW_POINT, WIND, GUSTS, AQI, CLOUD_COVER }

    @Composable
    @ReadOnlyComposable
    fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < DARK_THRESHOLD

    @Composable
    @ReadOnlyComposable
    fun precipitation(): Color = reading(Series.PRECIPITATION)

    @Composable
    @ReadOnlyComposable
    fun uv(): Color = reading(Series.UV)

    @Composable
    @ReadOnlyComposable
    fun dewPoint(): Color = reading(Series.DEW_POINT)

    @Composable
    @ReadOnlyComposable
    fun wind(): Color = reading(Series.WIND)

    @Composable
    @ReadOnlyComposable
    fun airQuality(): Color = reading(Series.AQI)

    @Composable
    @ReadOnlyComposable
    internal fun reading(series: Series): Color = color(series, if (isDark()) Appearance.DARK else Appearance.LIGHT)

    @Composable
    @ReadOnlyComposable
    fun temperatureF(fahrenheit: Float): Color = TemperaturePalette.color(fahrenheit, isDark())

    @Composable
    @ReadOnlyComposable
    fun temperatureC(celsius: Double): Color = temperatureF(TemperatureConverter.celsiusToFahrenheit(celsius).toFloat())

    internal fun color(
        series: Series,
        appearance: Appearance,
    ): Color {
        val (dark, light) =
            when (series) {
                Series.PRECIPITATION -> DARK_RAIN_BLUE to LIGHT_RAIN_BLUE
                Series.UV -> DARK_UV_AMBER to LIGHT_UV_AMBER
                Series.DEW_POINT -> DARK_DEW_VIOLET to LIGHT_DEW_VIOLET
                Series.WIND -> DARK_WIND_SLATE to LIGHT_WIND_SLATE
                Series.GUSTS -> DARK_GUST_AMBER to LIGHT_GUST_AMBER
                Series.AQI -> DARK_AQI_GREEN to LIGHT_AQI_GREEN
                Series.CLOUD_COVER -> DARK_CLOUD_GREY to LIGHT_CLOUD_GREY
            }
        return Color(if (appearance == Appearance.DARK) dark else light)
    }

    private const val DARK_THRESHOLD = 0.5f
    private const val DARK_RAIN_BLUE = 0xFF54C7EA
    private const val LIGHT_RAIN_BLUE = 0xFF005D80
    private const val DARK_UV_AMBER = 0xFFFFB74D
    private const val LIGHT_UV_AMBER = 0xFF805000
    private const val DARK_DEW_VIOLET = 0xFFCFBCFF
    private const val LIGHT_DEW_VIOLET = 0xFF6750A4
    private const val DARK_WIND_SLATE = 0xFFB0BEC5
    private const val LIGHT_WIND_SLATE = 0xFF455A64
    private const val DARK_GUST_AMBER = 0xFFFFCC80
    private const val LIGHT_GUST_AMBER = 0xFF775200
    private const val DARK_AQI_GREEN = 0xFF81C784
    private const val LIGHT_AQI_GREEN = 0xFF216927
    private const val DARK_CLOUD_GREY = 0xFFCBD5E1
    private const val LIGHT_CLOUD_GREY = 0xFF465362
}
