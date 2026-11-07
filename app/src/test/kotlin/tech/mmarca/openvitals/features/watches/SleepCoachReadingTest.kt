package tech.mmarca.openvitals.features.watches

import org.junit.Assert.assertEquals
import org.junit.Test

/** Sleep Coach reads as a comparison against the usual need, not a bare number. */
class SleepCoachReadingTest {

    @Test
    fun `Sleep Coach reads as a comparison, not a bare number`() {
        val reading = sleepCoachReading(neededMinutes = 520, usualMinutes = 470)

        assertEquals("8h 40m", reading.neededText)
        // "8h 40m needed" alone says nothing; against the usual 7h 50m it says what the strain cost.
        assertEquals(
            SleepCoachComparison.Above(extraText = "50 min", usualText = "7h 50m"),
            reading.comparison,
        )
    }

    @Test
    fun `a need no higher than usual reads as the same, not as a negative`() {
        val reading = sleepCoachReading(neededMinutes = 470, usualMinutes = 470)

        assertEquals(SleepCoachComparison.Same(usualText = "7h 50m"), reading.comparison)
    }

    @Test
    fun `a watch that never sent a usual need offers no comparison`() {
        val reading = sleepCoachReading(neededMinutes = 520, usualMinutes = null)

        assertEquals("8h 40m", reading.neededText)
        assertEquals(SleepCoachComparison.Unknown, reading.comparison)
    }
}
