package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CivilTwilightCalculatorTest {
    @Test
    fun calculatesCivilDawnAndDuskInTheLocationTimezone() {
        val date = LocalDate.of(2026, 6, 21)
        val timezone = ZoneId.of("America/New_York")
        val twilight = CivilTwilightCalculator.calculate(date, 40.7128, -74.0060, timezone.id)

        val firstLight = requireNotNull(twilight.firstLight)
        val lastLight = requireNotNull(twilight.lastLight)
        assertEquals(date, firstLight.toLocalDate())
        assertEquals(date, lastLight.toLocalDate())
        assertTrue(firstLight.isBefore(lastLight))
        assertTrue(firstLight.hour in 3..6)
        assertTrue(lastLight.hour in 19..22)
        assertEquals(timezone.rules.getOffset(firstLight), timezone.rules.getOffset(lastLight))
    }

    @Test
    fun returnsNoCivilTwilightWhenSunNeverSets() {
        val twilight =
            CivilTwilightCalculator.calculate(
                LocalDate.of(2026, 6, 21),
                78.2232,
                15.6469,
                "Arctic/Longyearbyen",
            )

        assertNull(twilight.firstLight)
        assertNull(twilight.lastLight)
    }
}
