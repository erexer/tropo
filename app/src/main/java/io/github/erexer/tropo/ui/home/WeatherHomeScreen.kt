package com.tropo.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tropo.ui.canvas.WeatherParticleCanvas
import com.tropo.ui.common.shimmerLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherHomeScreen(
    isRefreshing: Boolean,
    isLoading: Boolean,
    weatherCode: Int,
    temperatureText: String,
    locationName: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        WeatherParticleCanvas(weatherCode = weatherCode)

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(32.dp)
                            .shimmerLoading()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(64.dp)
                            .shimmerLoading()
                    )
                } else {
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = temperatureText,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }
        }
    }
}