package com.tropo.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tropo.data.local.TropoDatabase
import com.tropo.data.preferences.TemperatureUnit
import com.tropo.data.preferences.UserPreferencesRepository
import com.tropo.ui.theme.TropoColors
import kotlinx.coroutines.flow.first

class TropoWeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TropoDatabase.getInstance(context)
        val prefsRepository = UserPreferencesRepository(context)

        val locationWithWeather = database.weatherDao().getPrimaryLocationWithWeather()
        val prefs = prefsRepository.userPreferencesFlow.first()

        val tempText = locationWithWeather?.weather?.let { weather ->
            val tempC = weather.currentTemperature
            if (prefs.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                "${((tempC * 9 / 5) + 32).toInt()}°F"
            } else {
                "${tempC.toInt()}°C"
            }
        } ?: "--°"

        val locationName = locationWithWeather?.location?.name ?: "No Location"
        val conditionText = locationWithWeather?.weather?.conditionDescription ?: "Tap to sync"

        provideContent {
            WidgetContent(
                locationName = locationName,
                temperature = tempText,
                condition = conditionText
            )
        }
    }

    @Composable
    private fun WidgetContent(
        locationName: String,
        temperature: String,
        condition: String
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(TropoColors.WidgetBackground))
                .padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text(
                text = locationName,
                style = TextStyle(
                    color = ColorProvider(TropoColors.WidgetTextPrimary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = temperature,
                style = TextStyle(
                    color = ColorProvider(TropoColors.WidgetTextAccent),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = condition,
                style = TextStyle(
                    color = ColorProvider(TropoColors.WidgetTextSecondary),
                    fontSize = 12.sp
                )
            )
        }
    }
}

class TropoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TropoWeatherWidget()
}