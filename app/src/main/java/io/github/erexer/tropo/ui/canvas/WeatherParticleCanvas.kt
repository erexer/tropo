package io.github.erexer.tropo.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlin.random.Random

private data class Particle(var x: Float, var y: Float, val speed: Float, val radius: Float)

@Composable
fun WeatherParticleCanvas(modifier: Modifier = Modifier) {
    val particles = remember {
        List(40) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.005f + 0.002f,
                radius = Random.nextFloat() * 3f + 1f
            )
        }
    }
    val frameState = remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                particles.forEach { p ->
                    p.y += p.speed
                    if (p.y > 1f) p.y = 0f
                }
                frameState.value = time
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        frameState.value // trigger recomposition
        particles.forEach { p ->
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = p.radius,
                center = Offset(p.x * width, p.y * height)
            )
        }
    }
}