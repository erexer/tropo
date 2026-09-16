package io.github.erexer.tropo.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import io.github.erexer.tropo.TropoApplication
import kotlinx.coroutines.runBlocking

class TropoWeatherWidget : GlanceAppWidget() {
    override async fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as TropoApplication).container
        val weather = runBlocking { container.weatherRepository.getCurrentWeather() }
        val location = runBlocking { container.weatherRepository.getSelectedLocation() }

        provideContent {
            WidgetContent(
                locationName = location?.name ?: "Tropo",
                temperature = weather?.temperature?.toInt()?.toString() ?: "--"
            )
        }
    }

    @Composable
    private fun WidgetContent(locationName: String, temperature: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = locationName)
            Text(text = "$temperature°")
        }
    }
}