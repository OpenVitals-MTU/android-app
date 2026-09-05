package tech.mmarca.openvitals.features.mindfulness

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MindfulnessSession

/** The mindfulness intraday chart points. */
class MindfulnessIntradayChartTest {

    private val dayStart: Instant = Instant.parse("2026-03-04T00:00:00Z")

    private fun session(start: Instant, duration: Duration) = MindfulnessSession(
        id = start.toString(),
        title = null,
        startTime = start,
        endTime = start.plus(duration),
        durationMs = duration.toMillis(),
        source = "Test",
    )

    @Test fun `cumulativeMindfulness banks the minutes when a session ENDS`() {
        // A session lands on the chart when it ends: a 30-minute sit begun at 06:00 lands at 06:30.
        val points = listOf(
            session(dayStart.plus(Duration.ofHours(6)), Duration.ofMinutes(30)),
            session(dayStart.plus(Duration.ofHours(18)), Duration.ofMinutes(10)),
        ).cumulativeMindfulnessPoints()

        assertEquals(listOf(30.0, 40.0), points.map { it.second })
        val first = points.first().first.atZone(ZoneOffset.UTC)
        assertEquals(6, first.hour)
        assertEquals(30, first.minute)
    }
}
