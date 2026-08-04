package tech.mmarca.openvitals.features.mindfulness

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MindfulnessSession

/**
 * Ported from mobile-app test/features/mindfulness/mindfulness_intraday_chart_test.dart.
 */
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
        // Not when it starts: a session you are still sitting is not minutes you have
        // done. A 30-minute sit begun at 06:00 lands on the chart at 06:30.
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
