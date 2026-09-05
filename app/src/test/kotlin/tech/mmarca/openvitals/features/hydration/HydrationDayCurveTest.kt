package tech.mmarca.openvitals.features.hydration

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HydrationEntry

/** The day curve accumulates, in time order, skipping empty drinks. */
class HydrationDayCurveTest {

    private val morning: Instant = Instant.parse("2026-03-02T08:00:00Z")

    private fun drink(time: Instant, liters: Double) = HydrationEntry(
        startTime = time,
        endTime = time,
        liters = liters,
        source = "Test",
    )

    @Test fun `the day curve accumulates, in time order, skipping empty drinks`() {
        val points = listOf(
            drink(morning.plus(Duration.ofHours(5)), 0.5),
            drink(morning, 0.3),
            drink(morning.plus(Duration.ofHours(2)), 0.0),
        ).cumulativeHydrationPoints()

        assertEquals(2, points.size)
        assertEquals(0.3, points[0].second, 0.0001)
        assertEquals(0.8, points[1].second, 0.0001)
        assertEquals(morning, points.first().first)
    }
}
