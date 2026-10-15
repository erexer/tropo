package io.github.erexer.tropo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperaturePaletteTest {
    @Test
    fun snowUsesIcyBlueNotGreenInBothThemes() {
        WeatherPalette.Appearance.entries.forEach { appearance ->
            val snow = WeatherValuePalette.precipitation(io.github.erexer.tropo.data.PrecipitationType.SNOW, appearance)
            val rain = WeatherValuePalette.precipitation(io.github.erexer.tropo.data.PrecipitationType.RAIN, appearance)
            assertTrue(snow.blue > snow.green && snow.blue > snow.red)
            assertTrue(snow != rain)
        }
    }

    @Test
    fun uvAndPrecipitationColorsAreDistinctAndReadable() {
        WeatherPalette.Appearance.entries.forEach { appearance ->
            val background = if (appearance == WeatherPalette.Appearance.DARK) Color(0xFF33353A) else Color(0xFFE1E2E8)
            val uvColors = listOf(0f, 3f, 6f, 8f, 11f).map { WeatherValuePalette.uv(it, appearance) }
            assertEquals(5, uvColors.distinct().size)
            val types = io.github.erexer.tropo.data.PrecipitationType.entries
            (uvColors + types.map { WeatherValuePalette.precipitation(it, appearance) }).forEach {
                assertTrue(contrast(it, background) >= 4.5f)
            }
        }
    }

    @Test
    fun anchorsAndClampingMatchScale() {
        assertEquals(Color(0xFF1F5FC9), TemperaturePalette.color(32f, isDark = false))
        assertEquals(Color(0xFF174A8B), TemperaturePalette.color(-20f, isDark = false))
        assertEquals(Color(0xFFFF8A80), TemperaturePalette.color(110f, isDark = true))
        assertEquals(listOf(100f, 95f, 85f, 75f, 65f, 50f, 32f, 10f, 0f), TemperaturePalette.gradientStops(0f, 100f))
        assertEquals(listOf(50f, 32f), TemperaturePalette.gradientStops(32f, 50f))
    }

    @Test
    fun lightColorsReadOnWhiteAndDarkColorsReadOnDarkSurface() {
        listOf(0f, 20f, 40f, 55f, 70f, 80f, 90f, 100f).forEach { f ->
            assertTrue("light $f", contrast(TemperaturePalette.color(f, false), Color.White) >= 4.5f)
            assertTrue("dark $f", contrast(TemperaturePalette.color(f, true), Color(0xFF1C2027)) >= 4.5f)
        }
    }

    @Test
    fun interpolatedTextAndPastLinesReadOnTonalSurfaces() {
        val surfaces =
            mapOf(
                false to listOf(LightSurface, LightBackground, Color(0xFFECEEF4), Color(0xFFE1E2E8)),
                true to listOf(DarkBackground, SurfaceCard, Color(0xFF282A2F), Color(0xFF33353A)),
            )
        surfaces.forEach { (dark, backgrounds) ->
            (-80..480).forEach { step ->
                val fahrenheit = step / 4f
                val color = TemperaturePalette.color(fahrenheit, dark)
                backgrounds.forEach { background ->
                    assertTrue("text $fahrenheit dark=$dark on $background", contrast(color, background) >= 4.5f)
                    assertTrue(
                        "past line $fahrenheit dark=$dark on $background",
                        contrast(
                            color.copy(alpha = TemperaturePalette.PAST_ALPHA).compositeOver(background),
                            background,
                        ) >= 3f,
                    )
                }
            }
        }
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val (high, low) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (high + 0.05f) / (low + 0.05f)
    }

    @Test
    fun weatherSeriesReadOnTonalSurfaces() {
        WeatherPalette.Appearance.entries.forEach { appearance ->
            val backgrounds =
                if (appearance == WeatherPalette.Appearance.DARK) {
                    listOf(DarkBackground, SurfaceCard, Color(0xFF33353A))
                } else {
                    listOf(LightSurface, LightBackground, Color(0xFFE1E2E8))
                }
            WeatherPalette.Series.entries.forEach { series ->
                val color = WeatherPalette.color(series, appearance)
                backgrounds.forEach { background ->
                    assertTrue("$appearance $series text", contrast(color, background) >= 4.5f)
                    assertTrue(
                        "$appearance $series past line",
                        contrast(
                            color.copy(alpha = TemperaturePalette.PAST_ALPHA).compositeOver(background),
                            background,
                        ) >= 3f,
                    )
                }
            }
        }
    }
}
