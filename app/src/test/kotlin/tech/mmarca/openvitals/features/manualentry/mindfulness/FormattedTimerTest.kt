package tech.mmarca.openvitals.features.manualentry.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattedTimerTest {

    @Test fun `pads to mm ss and clamps below zero`() {
        assertEquals("00:00", formattedTimer(0))
        assertEquals("01:05", formattedTimer(65))
        assertEquals("10:00", formattedTimer(600))
        assertEquals("00:00", formattedTimer(-5))
    }
}
