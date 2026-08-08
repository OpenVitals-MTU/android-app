package tech.mmarca.openvitals.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.ui.components.ChartViewport
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.SessionAxis
import tech.mmarca.openvitals.ui.components.SessionPause
import tech.mmarca.openvitals.ui.components.axisFractionOf
import tech.mmarca.openvitals.ui.components.cullPlotPoints
import tech.mmarca.openvitals.ui.components.cumulativeDayPlotPoints
import tech.mmarca.openvitals.ui.components.dayAxisLabelsFor
import tech.mmarca.openvitals.ui.components.dayEndFraction
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.components.isDayToday
import tech.mmarca.openvitals.ui.components.rawDayPlotPoints
import tech.mmarca.openvitals.ui.components.timeAxisInstantsFor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ChartTimeAxesTest {

    // ── Day axis labels ─────────────────────────────────────────────────────

    @Test
    fun `full viewport gives the classic five hour labels`() {
        assertEquals(
            listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
            dayAxisLabelsFor(ChartViewport.Full),
        )
    }

    @Test
    fun `zoomed viewport labels the hours actually under the plot`() {
        // 06:00..12:00 on show: quarter ticks land every 90 minutes.
        assertEquals(
            listOf("06:00", "07:30", "09:00", "10:30", "12:00"),
            dayAxisLabelsFor(ChartViewport(start = 0.25f, end = 0.5f)),
        )
    }

    @Test
    fun `zoomed labels round to whole minutes`() {
        val labels = dayAxisLabelsFor(ChartViewport(start = 0.1f, end = 0.9f))
        // 0.1 * 1440 = 144 min = 02:24; 0.9 * 1440 = 1296 min = 21:36.
        assertEquals("02:24", labels.first())
        assertEquals("21:36", labels.last())
    }

    // ── The day axis itself ─────────────────────────────────────────────────
    //
    // The rule every intraday chart obeys, pinned in one place. This is the
    // regression for a bug that shipped on five screens at once: each card scaled
    // x by the time ELAPSED so far, then drew a fixed 00:00 / 06:00 / 12:00 /
    // 18:00 row underneath. Opened at 12:49, a drink at 09:29 was drawn at 74% of
    // the width — under the label that says quarter past five.

    private val day = LocalDate.of(2026, 6, 22)
    private val dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant()
    private val dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    private fun at(hour: Int, minute: Int = 0): Instant =
        day.atTime(hour, minute).toInstant(ZoneOffset.UTC)

    @Test
    fun `places a time at its real hour, not at its share of the elapsed day`() {
        // The chart is open at 12:49 — the exact case from the bug report. 09:29
        // is 39.5% of the way through the DAY; the old maths made it 74% of the
        // way through the part of the day that had happened.
        assertEquals(0.395f, axisFractionOf(dayStart, dayEnd, at(9, 29)), 0.001f)
    }

    @Test
    fun `the day axis spans the whole day, so the labels under it are true`() {
        assertEquals(0f, axisFractionOf(dayStart, dayEnd, at(0)), 0f)
        assertEquals(0.25f, axisFractionOf(dayStart, dayEnd, at(6)), 1e-6f)
        assertEquals(0.5f, axisFractionOf(dayStart, dayEnd, at(12)), 1e-6f)
        assertEquals(0.75f, axisFractionOf(dayStart, dayEnd, at(18)), 1e-6f)
    }

    @Test
    fun `today's series stops at now, rather than claiming the rest of the day`() {
        assertEquals(0.5f, dayEndFraction(dayStart, dayEnd, at(12)), 1e-6f)
        assertEquals(0.25f, dayEndFraction(dayStart, dayEnd, at(6)), 1e-6f)
    }

    @Test
    fun `a past day runs to its right edge`() {
        val nextMorning = day.plusDays(1).atTime(4, 0).toInstant(ZoneOffset.UTC)
        assertFalse(isDayToday(dayStart, dayEnd, nextMorning))
        assertEquals(1f, dayEndFraction(dayStart, dayEnd, nextMorning), 0f)
    }

    @Test
    fun `clamps a time from outside the day onto it`() {
        val yesterdayEvening = day.minusDays(1).atTime(22, 0).toInstant(ZoneOffset.UTC)
        val tomorrowMorning = day.plusDays(1).atTime(2, 0).toInstant(ZoneOffset.UTC)
        assertEquals(0f, axisFractionOf(dayStart, dayEnd, yesterdayEvening), 0f)
        assertEquals(1f, axisFractionOf(dayStart, dayEnd, tomorrowMorning), 0f)
    }

    @Test
    fun `honours the injected clock rather than the wall clock`() {
        // `now` in the future: the day under test is not today, whatever the
        // machine running the test happens to think.
        val distantFuture = LocalDate.of(2030, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()
        assertFalse(isDayToday(dayStart, dayEnd, distantFuture))
        assertTrue(isDayToday(dayStart, dayEnd, at(9)))
        // Midnight belongs to the day it opens; the next midnight does not.
        assertTrue(isDayToday(dayStart, dayEnd, dayStart))
        assertFalse(isDayToday(dayStart, dayEnd, dayEnd))
    }

    // ── Session axis ────────────────────────────────────────────────────────

    private val start = Instant.parse("2026-08-01T10:00:00Z")

    @Test
    fun `duration is the moving time`() {
        val axis = SessionAxis(
            start = start,
            end = start.plusSeconds(1260), // 21 min session
            pauses = listOf(
                SessionPause(start.plusSeconds(300), start.plusSeconds(930)), // 10.5 min pause
            ),
        )
        assertEquals(Duration.ofSeconds(630).toMillis(), axis.durationMs)
    }

    @Test
    fun `places a sample at its elapsed position in the session`() {
        val axis = SessionAxis(start = start, end = start.plusSeconds(3600))
        assertEquals(0f, axis.fractionOf(start), 0f)
        assertEquals(0.25f, axis.fractionOf(start.plusSeconds(900)), 1e-6f)
        assertEquals(0.5f, axis.fractionOf(start.plusSeconds(1800)), 1e-6f)
        assertEquals(1f, axis.fractionOf(start.plusSeconds(3600)), 0f)
    }

    @Test
    fun `spans the recorded session, not the samples that exist`() {
        // A trace whose sensor died twenty minutes into an hour-long ride must
        // stop a third of the way across. Normalizing against the samples instead
        // would stretch it to the right edge and imply an hour of readings.
        val axis = SessionAxis(start = start, end = start.plusSeconds(3600))
        assertEquals(1f / 3f, axis.fractionOf(start.plusSeconds(1200)), 1e-6f)
    }

    @Test
    fun `clamps a sample from outside the session onto it`() {
        val axis = SessionAxis(start = start, end = start.plusSeconds(3600))
        assertEquals(0f, axis.fractionOf(start.minusSeconds(3600)), 0f)
        assertEquals(1f, axis.fractionOf(start.plusSeconds(7200)), 0f)
    }

    @Test
    fun `a zero length session still has a positive axis`() {
        val axis = SessionAxis(start = start, end = start)
        assertEquals(1L, axis.durationMs)
        assertEquals(0f, axis.fractionOf(start), 0f)
    }

    @Test
    fun `an entirely paused session does not divide by zero`() {
        val end = start.plusSeconds(3600)
        val axis = SessionAxis(
            start = start,
            end = end,
            pauses = listOf(SessionPause(start, end)),
        )
        assertEquals(1L, axis.durationMs)
        assertEquals(0f, axis.fractionOf(start.plusSeconds(1800)), 0f)
    }

    @Test
    fun `the scrubber and the labels agree, both in moving time`() {
        // 60 wall-clock minutes with a 30-minute pause in the middle: half the
        // axis is 15 moving minutes, and the row underneath says so too.
        val axis = SessionAxis(
            start = start,
            end = start.plusSeconds(3600),
            pauses = listOf(SessionPause(start.plusSeconds(600), start.plusSeconds(2400))),
        )
        assertEquals(Duration.ofMinutes(15), axis.elapsedAt(0.5f))
        assertEquals(
            listOf("0:00", "7:30", "15:00", "22:30", "30:00"),
            axis.elapsedLabelsFor(ChartViewport.Full),
        )
    }

    @Test
    fun `an instant inside a pause resolves to the moment the pause began`() {
        val axis = SessionAxis(
            start = start,
            end = start.plusSeconds(600),
            pauses = listOf(SessionPause(start.plusSeconds(120), start.plusSeconds(240))),
        )
        val pauseStartFraction = axis.fractionOf(start.plusSeconds(120))
        assertEquals(pauseStartFraction, axis.fractionOf(start.plusSeconds(180)), 1e-6f)
        assertEquals(pauseStartFraction, axis.fractionOf(start.plusSeconds(239)), 1e-3f)
        // 120 moving seconds of a 480-moving-second session.
        assertEquals(0.25f, pauseStartFraction, 1e-6f)
    }

    @Test
    fun `overlapping pauses are merged so shared time is not subtracted twice`() {
        val axis = SessionAxis(
            start = start,
            end = start.plusSeconds(600),
            pauses = listOf(
                SessionPause(start.plusSeconds(100), start.plusSeconds(200)),
                SessionPause(start.plusSeconds(150), start.plusSeconds(250)),
            ),
        )
        assertEquals(1, axis.pauses.size)
        // 600 - merged 150s pause.
        assertEquals(450_000L, axis.durationMs)
    }

    @Test
    fun `pauses are clipped to the session`() {
        val axis = SessionAxis(
            start = start,
            end = start.plusSeconds(600),
            pauses = listOf(
                SessionPause(start.minusSeconds(60), start.plusSeconds(60)),
                SessionPause(start.plusSeconds(700), start.plusSeconds(800)),
            ),
        )
        assertEquals(1, axis.pauses.size)
        assertEquals(540_000L, axis.durationMs)
    }

    @Test
    fun `elapsedAt inverts fractionOf on moving time`() {
        val axis = SessionAxis(
            start = start,
            end = start.plusSeconds(600),
            pauses = listOf(SessionPause(start.plusSeconds(120), start.plusSeconds(240))),
        )
        // The sample at 300s wall clock is 180s moving.
        val fraction = axis.fractionOf(start.plusSeconds(300))
        assertEquals(Duration.ofSeconds(180), axis.elapsedAt(fraction))
    }

    @Test
    fun `elapsed labels at full zoom span the whole session`() {
        val axis = SessionAxis(start = start, end = start.plusSeconds(3600))
        assertEquals(
            listOf("0:00", "15:00", "30:00", "45:00", "1:00:00"),
            axis.elapsedLabelsFor(ChartViewport.Full),
        )
    }

    @Test
    fun `elapsed labels under zoom describe the visible slice`() {
        val axis = SessionAxis(start = start, end = start.plusSeconds(3600))
        assertEquals(
            listOf("30:00", "37:30", "45:00", "52:30", "1:00:00"),
            axis.elapsedLabelsFor(ChartViewport(start = 0.5f, end = 1f)),
        )
    }

    @Test
    fun `elapsed format switches to hours past sixty minutes`() {
        assertEquals("0:00", formatElapsedChartLabel(Duration.ZERO))
        assertEquals("59:59", formatElapsedChartLabel(Duration.ofSeconds(3599)))
        assertEquals("1:00:00", formatElapsedChartLabel(Duration.ofSeconds(3600)))
        assertEquals("2:05:07", formatElapsedChartLabel(Duration.ofSeconds(7507)))
    }

    // ── Clock axis ──────────────────────────────────────────────────────────

    @Test
    fun `time axis instants at full zoom are start, middle and end`() {
        val end = start.plusSeconds(7200)
        assertEquals(
            listOf(start, start.plusSeconds(3600), end),
            timeAxisInstantsFor(start, end, ChartViewport.Full),
        )
    }

    @Test
    fun `time axis instants under zoom follow the visible slice`() {
        val end = start.plusSeconds(7200)
        assertEquals(
            listOf(start.plusSeconds(1800), start.plusSeconds(2700), start.plusSeconds(3600)),
            timeAxisInstantsFor(start, end, ChartViewport(start = 0.25f, end = 0.5f)),
        )
    }

    // ── Day fractions and the cumulative shape ──────────────────────────────

    @Test
    fun `axisFractionOf places a moment against the whole span and clamps outside it`() {
        val end = start.plusSeconds(86_400)
        assertEquals(0f, axisFractionOf(start, end, start.minusSeconds(60)), 0f)
        assertEquals(0.5f, axisFractionOf(start, end, start.plusSeconds(43_200)), 1e-6f)
        assertEquals(1f, axisFractionOf(start, end, end.plusSeconds(60)), 0f)
    }

    @Test
    fun `cumulative shape anchors at zero and plateaus out to the end fraction`() {
        val points = cumulativeDayPlotPoints(
            fractions = listOf(0.2f to 1.0, 0.4f to 3.0),
            endFraction = 0.55f,
        )
        assertEquals(
            listOf(
                MetricLinePlotPoint(0f, 0.0, synthetic = true),
                MetricLinePlotPoint(0.2f, 1.0),
                MetricLinePlotPoint(0.4f, 3.0),
                MetricLinePlotPoint(0.55f, 3.0, synthetic = true),
            ),
            points,
        )
    }

    @Test
    fun `only the real entries carry dots — the anchor and hold are synthetic`() {
        // The dot at the end of the hydration day line read as an entry nobody
        // made (#250): the trailing hold at "now" and the midnight anchor shape
        // the line but must not be marked like the drinks are.
        val points = cumulativeDayPlotPoints(
            fractions = listOf(0.2f to 1.0, 0.4f to 3.0),
            endFraction = 0.55f,
        )
        assertEquals(listOf(true, false, false, true), points.map { it.synthetic })
    }

    @Test
    fun `cumulative on today stops at now, not at the right edge`() {
        // 400 ml logged at 06:00, the chart open at noon. Held out to midday —
        // the afternoon has not happened, and a line drawn across it would be a
        // claim about the future.
        val points = cumulativeDayPlotPoints(
            fractions = listOf(axisFractionOf(dayStart, dayEnd, at(6)) to 400.0),
            endFraction = dayEndFraction(dayStart, dayEnd, at(12)),
        )
        assertEquals(0.5f, points.last().xFraction, 1e-6f)
        assertEquals(400.0, points.last().value, 0.0)
    }

    @Test
    fun `raw plots the readings and invents nothing`() {
        val points = rawDayPlotPoints(
            samples = listOf(at(6) to 70.0, at(18) to 80.0),
            dayStart = dayStart,
            dayEnd = dayEnd,
            time = { it.first },
            value = { it.second },
        )

        // No midnight anchor, no trailing hold: a weight at 06:00 says nothing
        // about midnight, and nothing about tonight.
        assertEquals(2, points.size)
        assertEquals(0.25f, points.first().xFraction, 1e-6f)
        assertEquals(70.0, points.first().value, 0.0)
        assertEquals(0.75f, points.last().xFraction, 1e-6f)
        assertEquals(80.0, points.last().value, 0.0)
    }

    @Test
    fun `every shape survives an empty day`() {
        assertTrue(cumulativeDayPlotPoints(emptyList(), 0.5f).isEmpty())
        assertTrue(
            rawDayPlotPoints(
                samples = emptyList<Pair<Instant, Double>>(),
                dayStart = dayStart,
                dayEnd = dayEnd,
                time = { it.first },
                value = { it.second },
            ).isEmpty(),
        )
    }

    @Test
    fun `cumulative shape of an empty day is empty`() {
        assertTrue(cumulativeDayPlotPoints(emptyList(), 0.5f).isEmpty())
    }

    // ── Viewport culling ────────────────────────────────────────────────────

    private fun plotPoints(vararg fractions: Float) =
        fractions.map { MetricLinePlotPoint(it, 1.0) }

    @Test
    fun `an unzoomed viewport culls nothing`() {
        val points = plotPoints(0f, 0.5f, 1f)
        assertEquals(points, cullPlotPoints(points, ChartViewport.Full))
    }

    @Test
    fun `culling keeps one point past each edge so the line reaches the border`() {
        val points = plotPoints(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
        val culled = cullPlotPoints(points, ChartViewport(start = 0.35f, end = 0.65f))
        assertEquals(plotPoints(0.2f, 0.4f, 0.6f, 0.8f), culled)
    }

    @Test
    fun `a window inside a gap between samples culls to nothing`() {
        val points = plotPoints(0f, 0.1f, 0.9f, 1f)
        // 0.4..0.5 falls entirely between 0.1 and 0.9: outside-left points fail
        // the right test and vice versa, so the plot draws nothing there.
        assertTrue(cullPlotPoints(points, ChartViewport(start = 0.4f, end = 0.5f)).isEmpty())
    }
}
