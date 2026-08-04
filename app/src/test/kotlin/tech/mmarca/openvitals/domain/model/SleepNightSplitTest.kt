package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Splitting a night window's sessions into the one night and its naps, the
 * wall-clock duration of that night, and the union sweep the duration rests on.
 */
class SleepNightSplitTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun t(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()

    private fun s(
        id: String,
        start: Instant,
        end: Instant,
        durationMs: Long? = null,
    ): SleepData = SleepData(
        id = id,
        startTime = start,
        endTime = end,
        durationMs = durationMs ?: Duration.between(start, end).toMillis(),
        source = "fitbit",
    )

    // --- splitNightAndNaps ---

    @Test fun `splitNightAndNaps keeps a night broken by a short early-morning wake together`() {
        // 23:43->05:37 then 07:17->09:33: a 1h40m wake, one night.
        val a = s("a", t(2026, 7, 11, 23, 43), t(2026, 7, 12, 5, 37))
        val b = s("b", t(2026, 7, 12, 7, 17), t(2026, 7, 12, 9, 33))

        val split = splitNightAndNaps(listOf(b, a))

        assertEquals(listOf("a", "b"), split.night.map { it.id })
        assertEquals(emptyList<String>(), split.naps.map { it.id })
    }

    @Test fun `splitNightAndNaps splits an afternoon nap from the night`() {
        val night = s("night", t(2026, 7, 10, 23, 18), t(2026, 7, 11, 7, 25))
        val nap = s("nap", t(2026, 7, 11, 16, 10), t(2026, 7, 11, 16, 45))

        val split = splitNightAndNaps(listOf(nap, night))

        assertEquals(listOf("night"), split.night.map { it.id })
        assertEquals(listOf("nap"), split.naps.map { it.id })
    }

    @Test fun `splitNightAndNaps treats a single session as the night`() {
        val split = splitNightAndNaps(
            listOf(s("x", t(2026, 7, 11, 23, 0), t(2026, 7, 12, 6, 0))),
        )
        assertEquals("x", split.night.single().id)
        assertEquals(emptyList<String>(), split.naps.map { it.id })
    }

    @Test fun `splitNightAndNaps on empty input yields no night and no naps`() {
        val split = splitNightAndNaps(emptyList())
        assertEquals(emptyList<String>(), split.night.map { it.id })
        assertEquals(emptyList<String>(), split.naps.map { it.id })
    }

    // --- dailySleepSummary: night only, wall-clock ---

    @Test fun `dailySleepSummary sums the night segments in wall-clock and excludes a nap`() {
        val a = s("a", t(2026, 7, 11, 23, 43), t(2026, 7, 12, 5, 37)) // 5h54
        val b = s("b", t(2026, 7, 12, 7, 17), t(2026, 7, 12, 9, 33)) // 2h16
        val nap = s("nap", t(2026, 7, 12, 15, 0), t(2026, 7, 12, 15, 40))

        val summary = dailySleepSummary(
            listOf(a, b, nap),
            LocalDate.of(2026, 7, 12),
            zone = zone,
        )

        // The bug reported 1h43m here; the night is 5h54 + 2h16 = 8h10.
        assertNotNull(summary)
        assertEquals(
            Duration.ofHours(8).plusMinutes(10).toMillis(),
            summary!!.durationMs,
        )
        assertEquals(
            listOf("nap"),
            dailyNaps(listOf(a, b, nap), LocalDate.of(2026, 7, 12), zone = zone).map { it.id },
        )
    }

    @Test fun `dailySleepSummary duration is wall-clock, not the stored time-asleep durationMs`() {
        // Stored durationMs (1h) differs from the 7h span; the summary uses the span.
        val only = s(
            "x",
            t(2026, 7, 11, 23, 0),
            t(2026, 7, 12, 6, 0),
            durationMs = Duration.ofHours(1).toMillis(),
        )

        val summary = dailySleepSummary(listOf(only), LocalDate.of(2026, 7, 12), zone = zone)

        assertEquals(Duration.ofHours(7).toMillis(), summary!!.durationMs)
    }

    @Test fun `dailySleepSummary counts overlapping night sessions once (union, not sum)`() {
        // The reported pair, had it slipped past dedup: 1:15-6:40 and 1:16-7:28.
        // Sum would be 11h37m; the union (1:15-7:28) is 6h13m.
        val a = s("a", t(2026, 7, 14, 1, 15), t(2026, 7, 14, 6, 40))
        val b = s("b", t(2026, 7, 14, 1, 16), t(2026, 7, 14, 7, 28))

        val summary = dailySleepSummary(listOf(a, b), LocalDate.of(2026, 7, 14), zone = zone)

        assertEquals(Duration.ofHours(6).plusMinutes(13).toMillis(), summary!!.durationMs)
    }

    // --- sleepSessionsUnionMs ---

    @Test fun `sleepSessionsUnionMs counts overlapping intervals shared time once`() {
        val a = s("a", t(2026, 7, 14, 1, 15), t(2026, 7, 14, 6, 40))
        val b = s("b", t(2026, 7, 14, 1, 16), t(2026, 7, 14, 7, 28))
        assertEquals(
            Duration.ofHours(6).plusMinutes(13).toMillis(),
            sleepSessionsUnionMs(listOf(a, b)),
        )
    }

    @Test fun `sleepSessionsUnionMs of disjoint intervals equals the sum of their spans`() {
        val a = s("a", t(2026, 7, 14, 1, 0), t(2026, 7, 14, 3, 0)) // 2h
        val b = s("b", t(2026, 7, 14, 5, 0), t(2026, 7, 14, 6, 30)) // 1h30
        assertEquals(
            Duration.ofHours(3).plusMinutes(30).toMillis(),
            sleepSessionsUnionMs(listOf(a, b)),
        )
    }

    @Test fun `sleepSessionsUnionMs merges adjacent (touching) intervals without a gap`() {
        val a = s("a", t(2026, 7, 14, 1, 0), t(2026, 7, 14, 3, 0))
        val b = s("b", t(2026, 7, 14, 3, 0), t(2026, 7, 14, 4, 0))
        assertEquals(Duration.ofHours(3).toMillis(), sleepSessionsUnionMs(listOf(a, b)))
    }

    @Test fun `sleepSessionsUnionMs adds nothing for a fully-contained interval`() {
        val outer = s("outer", t(2026, 7, 14, 1, 0), t(2026, 7, 14, 9, 0))
        val inner = s("inner", t(2026, 7, 14, 3, 0), t(2026, 7, 14, 4, 0))
        assertEquals(Duration.ofHours(8).toMillis(), sleepSessionsUnionMs(listOf(outer, inner)))
    }

    @Test fun `sleepSessionsUnionMs of empty input is zero`() {
        assertEquals(0L, sleepSessionsUnionMs(emptyList()))
        assertTrue(sleepSessionsUnionMs(emptyList()) == 0L)
    }
}
