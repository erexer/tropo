package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WindDirectionFormatterTest {
    @Test
    fun arrowShowsWindFlowDirectionAndCompassShowsWhereItComesFrom() {
        assertEquals("↓ from N", WindDirectionFormatter.describeWindFrom(0f))
        assertEquals("← from E", WindDirectionFormatter.describeWindFrom(90f))
        assertEquals("↗ from SW", WindDirectionFormatter.describeWindFrom(225f))
        assertEquals("↓ from N", WindDirectionFormatter.describeWindFrom(360f))
    }
}
