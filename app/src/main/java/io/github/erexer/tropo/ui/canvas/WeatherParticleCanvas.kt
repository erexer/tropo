package com.tropo.ui.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

enum class ParticleType { RAIN, SNOW, FOG, SUNBEAM }

fun wmoCodeToParticleType(code: Int): ParticleType = when (code) {
    0, 1 -> ParticleType.SUNBEAM
    45, 48 -> ParticleType.FOG
    71, 73, 75, 77, 85, 86 -> ParticleType.SNOW
    else -> ParticleType.RAIN
}

private data class Particle(
    var x: Float,
    var y: Float,
    var speed: Float,
    var size: Float,
    var alpha: Float
)

@Composable
fun WeatherParticleCanvas(
    weatherCode: Int,
    modifier: Modifier = Modifier
) {
    val particleType = remember(weatherCode) { wmoCodeToParticleType(weatherCode) }
    val particles = remember(particleType) {
        List(if (particleType == ParticleType.FOG) 8 else 60) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = when (particleType) {
                    ParticleType.RAIN -> Random.nextFloat() * 0.03f + 0.015f
                    ParticleType.SNOW -> Random.nextFloat() * 0.005f + 0.002f
                    ParticleType.FOG -> Random.nextFloat() * 0.001f + 0.0005f
                    ParticleType.SUNBEAM -> Random.nextFloat() * 0.002f + 0.001f
                },
                size = when (particleType) {
                    ParticleType.RAIN -> Random.nextFloat() * 25f + 15f
                    ParticleType.SNOW -> Random.nextFloat() * 8f + 4f
                    ParticleType.FOG -> Random.nextFloat() * 200f + 150f
                    ParticleType.SUNBEAM -> Random.nextFloat() * 60f + 30f
                },
                alpha = when (particleType) {
                    ParticleType.RAIN -> Random.nextFloat() * 0.4f + 0.3f
                    ParticleType.SNOW -> Random.nextFloat() * 0.6f + 0.3f
                    ParticleType.FOG -> Random.nextFloat() * 0.15f + 0.05f
                    ParticleType.SUNBEAM -> Random.nextFloat() * 0.2f + 0.1f
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_tick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        @Suppress("UNUSED_VARIABLE") val tick = time

        particles.forEach { p ->
            when (particleType) {
                ParticleType.RAIN -> {
                    p.y += p.speed
                    p.x -= p.speed * 0.2f
                    if (p.y > 1f) { p.y = 0f; p.x = Random.nextFloat() }
                    drawLine(
                        color = Color.White.copy(alpha = p.alpha),
                        start = Offset(p.x * width, p.y * height),
                        end = Offset((p.x - 0.05f) * width, (p.y + p.size / height) * height),
                        strokeWidth = 2f
                    )
                }
                ParticleType.SNOW -> {
                    p.y += p.speed
                    p.x += Math.sin(p.y * 10.0).toFloat() * 0.001f
                    if (p.y > 1f) { p.y = 0f; p.x = Random.nextFloat() }
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(p.x * width, p.y * height)
                    )
                }
                ParticleType.FOG -> {
                    p.x += p.speed
                    if (p.x > 1f) p.x = -0.3f
                    drawCircle(
                        color = Color.LightGray.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(p.x * width, p.y * height)
                    )
                }
                ParticleType.SUNBEAM -> {
                    p.alpha = (Math.sin(time * Math.PI * 2 + p.x * 10).toFloat() * 0.1f + 0.15f)
                    drawSunbeam(width, height, p)
                }
            }
        }
    }
}

private fun DrawScope.drawSunbeam(width: Float, height: Float, particle: Particle) {
    val path = Path().apply {
        moveTo(width * 0.2f, 0f)
        lineTo(particle.x * width, height)
        lineTo((particle.x + 0.1f) * width, height)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFE082).copy(alpha = particle.alpha),
                Color.Transparent
            )
        )
    )
}