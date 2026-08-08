package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.SleepWindow

/**
 * A night is classified by when you FELL ASLEEP (its start), against the
 * configurable night window (default 18:00 -> 10:00). Start-based keeps a night
 * on the wake-up date (bed 22:40 -> next morning) as before, AND keeps a
 * sleep-in past the morning hour with its night instead of misfiling it as a
 * nap. Sessions that begin outside the window are daytime naps, reported
 * apart — never dropped.
 */
class SleepDayAttributionTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun t(day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(2026, 7, day, hour, minute).atZone(zone).toInstant()

    private fun session(id: String, start: Instant, end: Instant): SleepData = SleepData(
        id = id,
        startTime = start,
        endTime = end,
        durationMs = Duration.between(start, end).toMillis(),
        source = "test",
        stages = listOf(SleepStage(start, end, SleepStage.STAGE_LIGHT)),
    )

    // Bed 22:40 on the 16th, up 06:17 on the 17th — a cross-midnight night.
    private val crossMidnight = session("x", t(16, 22, 40), t(17, 6, 17))

    // Bed 01:38 on the 16th, up 07:32 on the 16th — begins and ends the same day.
    private val sameDay = session("s", t(16, 1, 38), t(16, 7, 32))

    private fun forDay(day: Int): List<SleepData> = sleepSessionsForRange(
        listOf(crossMidnight, sameDay),
        LocalDate.of(2026, 7, day),
        SleepWindow.Default,
        zone,
    )

    @Test fun `a cross-midnight night is filed under the wake-up date`() {
        assertEquals("begins early the 16th", listOf("s"), forDay(16).map { it.id })
        assertEquals("begins the 16th evening", listOf("x"), forDay(17).map { it.id })
    }

    @Test fun `a sleep-in past the morning hour stays that night, not a nap`() {
        // Bed 01:00, up 11:00 — wakes an hour after the 10:00 end. End-based
        // attribution would call it a daytime nap; start-based keeps it the night.
        val sleepIn = session("in", t(18, 1, 0), t(18, 11, 0))
        val night = sleepSessionsForRange(
            listOf(sleepIn),
            LocalDate.of(2026, 7, 18),
            SleepWindow.Default,
            zone,
        )
        assertEquals(listOf("in"), night.map { it.id })
        assertEquals(
            emptyList<String>(),
            dailyNaps(listOf(sleepIn), LocalDate.of(2026, 7, 18), zone = zone).map { it.id },
        )
    }

    @Test fun `a daytime session becomes a nap on its date and is not dropped`() {
        // Begins 14:00 — in the daytime gap [10:00, 18:00), so a nap, not the night.
        val nap = session("nap", t(18, 14, 0), t(18, 15, 0))
        assertEquals(
            emptyList<String>(),
            sleepSessionsForRange(
                listOf(nap),
                LocalDate.of(2026, 7, 18),
                SleepWindow.Default,
                zone,
            ).map { it.id },
        )
        assertEquals(
            listOf("nap"),
            dailyNaps(listOf(nap), LocalDate.of(2026, 7, 18), zone = zone).map { it.id },
        )
    }

    @Test fun `a same-day window keeps each night on its own date`() {
        // A 00:00 -> 12:00 window lies wholly within the wake-up date. Anchored
        // on D-1 it spanned 36 hours, so each date's window swallowed the
        // neighbouring night too: "today" showed whichever adjacent night was
        // longer, and the loser was filed as a full-length "nap".
        val window = SleepWindow(startHour = 0, endHour = 12)
        val nights = listOf(
            session("n6", t(6, 2, 8), t(6, 11, 43)),
            session("n7", t(7, 1, 23), t(7, 11, 0)),
            session("n8", t(8, 2, 1), t(8, 11, 13)),
        )
        for (day in 6..8) {
            val date = LocalDate.of(2026, 7, day)
            assertEquals(
                "night of the ${day}th",
                "n$day",
                dailySleepSummary(nights, date, window, zone)?.id,
            )
            assertEquals(
                "no phantom naps on the ${day}th",
                emptyList<String>(),
                dailyNaps(nights, date, window, zone).map { it.id },
            )
        }
    }

    @Test fun `a same-day window files an afternoon nap on its own date`() {
        val window = SleepWindow(startHour = 0, endHour = 12)
        val night = session("n", t(6, 2, 8), t(6, 11, 43))
        val nap = session("nap", t(6, 16, 50), t(6, 16, 55))
        assertEquals(
            listOf("nap"),
            dailyNaps(listOf(night, nap), LocalDate.of(2026, 7, 6), window, zone).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            dailyNaps(listOf(night, nap), LocalDate.of(2026, 7, 7), window, zone).map { it.id },
        )
    }

    @Test fun `custom window hours move the night boundary`() {
        // 20:00 -> 09:00. A session begun at 09:30 now falls in the daytime gap.
        val window = SleepWindow(startHour = 20, endHour = 9)
        val late = session("l", t(18, 9, 30), t(18, 10, 30))
        assertEquals(
            emptyList<String>(),
            sleepSessionsForRange(listOf(late), LocalDate.of(2026, 7, 18), window, zone)
                .map { it.id },
        )
        assertEquals(
            listOf("l"),
            dailyNaps(listOf(late), LocalDate.of(2026, 7, 18), sleepWindow = window, zone = zone)
                .map { it.id },
        )
    }
}
