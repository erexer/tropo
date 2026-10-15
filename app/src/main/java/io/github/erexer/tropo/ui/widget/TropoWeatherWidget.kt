package io.github.erexer.tropo.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.erexer.tropo.TropoApplication
import io.github.erexer.tropo.data.LocationLabel
import io.github.erexer.tropo.data.TemperatureConverter
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WidgetSnapshot
import io.github.erexer.tropo.ui.common.AppLinks
import io.github.erexer.tropo.ui.theme.LightSurface
import io.github.erexer.tropo.ui.theme.LightTextPrimary
import io.github.erexer.tropo.ui.theme.SurfaceCard
import io.github.erexer.tropo.ui.theme.TemperaturePalette
import io.github.erexer.tropo.ui.theme.TextPrimary
import kotlinx.coroutines.flow.first
import androidx.glance.color.ColorProvider as DayNightColorProvider

class TropoWeatherWidget(
    private val refresh: WeatherRefresh = WeatherRefresh.FORCE,
) : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as TropoApplication).container
        val reading = container.weatherRepository.currentReading(refresh)
        val location = container.weatherRepository.getSelectedLocation()
        val weather = reading.weather?.takeIf { it.locationId == location?.id }
        val units = container.weatherRepository.units().first()

        provideContent {
            val snapshot by container.weatherRepository.widgetState().collectAsState(
                initial = WidgetSnapshot(location, weather, units),
            )
            val current = snapshot.weather
            val temperature =
                current?.temperature?.let {
                    snapshot.units
                        .currentTemperature(it)
                        .toInt()
                        .toString() + snapshot.units.temperature.label
                } ?: "--"
            val temperatureFahrenheit =
                current?.let {
                    TemperatureConverter.celsiusToFahrenheit(it.temperature).toFloat()
                }
            WidgetContent(
                locationName = snapshot.location?.let(LocationLabel::of) ?: "Tropo",
                temperature = temperature,
                temperatureColor =
                    temperatureFahrenheit?.let {
                        DayNightColorProvider(
                            day = TemperaturePalette.color(it, isDark = false),
                            night = TemperaturePalette.color(it, isDark = true),
                        )
                    },
                wind = current?.let { snapshot.units.currentWindText(it.windSpeed) }.orEmpty(),
            )
        }
    }

    @Composable
    @Suppress("FunctionNaming")
    private fun WidgetContent(
        locationName: String,
        temperature: String,
        temperatureColor: ColorProvider?,
        wind: String,
    ) {
        val textStyle = TextStyle(color = DayNightColorProvider(day = LightTextPrimary, night = TextPrimary))
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(DayNightColorProvider(day = LightSurface, night = SurfaceCard))
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = locationName, style = textStyle)
            if (temperatureColor != null) {
                Text(text = temperature, style = TextStyle(color = temperatureColor))
            } else {
                Text(text = temperature, style = textStyle)
            }
            if (wind.isNotEmpty()) {
                Text(text = wind, style = textStyle)
            }
            Text(
                text = "Open-Meteo",
                modifier =
                    GlanceModifier.clickable(
                        actionStartActivity(Intent(Intent.ACTION_VIEW, AppLinks.WEATHER.toUri())),
                    ),
                style = TextStyle(color = textStyle.color, fontSize = 10.sp),
            )
        }
    }
}
