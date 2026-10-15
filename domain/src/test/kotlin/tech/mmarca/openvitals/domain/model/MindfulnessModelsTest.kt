package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

/** The one mindfulness day value: the dashboard, the screen, the reminder and the report all use it. */
class MindfulnessModelsTest {

    private fun session(start: String, durationMs: Long) = MindfulnessSession(
        id = start,
        title = null,
        startTime = Instant.parse(start),
        endTime = Instant.parse(start).plusMillis(durationMs.coerceAtLeast(0L)),
        durationMs = durationMs,
        source = "test",
    )

    @Test
    fun `minutes are summed on the day a session starts`() {
        val sessions = listOf(
            // Starts before midnight, so it belongs to the first day in full.
            session("2026-06-01T23:50:00Z", 20 * 60_000L),
            session("2026-06-02T08:00:00Z", 10 * 60_000L),
            session("2026-06-02T18:00:00Z", 5 * 60_000L),
        )

        val minutes = sessions.minutesByStartDate(ZoneOffset.UTC)

        assertEquals(mapOf(LocalDate.of(2026, 6, 1) to 20.0, LocalDate.of(2026, 6, 2) to 15.0), minutes)
    }

    @Test
    fun `a negative duration counts as nothing`() {
        val minutes = listOf(session("2026-06-02T08:00:00Z", -60_000L)).minutesByStartDate(ZoneOffset.UTC)

        assertEquals(mapOf(LocalDate.of(2026, 6, 2) to 0.0), minutes)
    }
}
