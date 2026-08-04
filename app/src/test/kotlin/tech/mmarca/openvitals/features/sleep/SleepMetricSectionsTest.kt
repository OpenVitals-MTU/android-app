package tech.mmarca.openvitals.features.sleep

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.RecordingMethod
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage

/**
 * Port of the derivations test/features/sleep/sleep_display_test.dart asserts on
 * `SleepDisplay`: the stage shares, the newest-night-first entry lists, the
 * manual-entry confidence count, the schedule-chart rule and the period totals.
 * Kotlin computes these in the sections layer, so the test calls the pure
 * helpers the composables call.
 */
class SleepMetricSectionsTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val wednesday: LocalDate = LocalDate.of(2026, 3, 4)
    private val tuesday: LocalDate = LocalDate.of(2026, 3, 3)

    /** One night ending at 07:00 local on [date] and running [hours] backwards from it. */
    private fun night(
        date: LocalDate,
        hours: Double = 8.0,
        id: String = "night-$date",
        recordingMethod: Int? = null,
    ): SleepData {
        val end = date.atTime(7, 0).atZone(zone).toInstant()
        val durationMs = (hours * 3_600_000).toLong()
        val start = end.minusMillis(durationMs)
        return SleepData(
            id = id,
            startTime = start,
            endTime = end,
            durationMs = durationMs,
            source = "test",
            recordingMethod = recordingMethod,
            stages = listOf(
                SleepStage(start, start.plusMillis(durationMs / 2), SleepStage.STAGE_LIGHT),
                SleepStage(start.plusMillis(durationMs / 2), end, SleepStage.STAGE_DEEP),
            ),
        )
    }

    // ─── the stage shares split the recorded stage time, and only it ─────────

    @Test fun `the stage shares split the recorded stage time and only it`() {
        val fourHours = 4 * 3_600_000L
        val shares = sleepStageShares(
            SleepStageDurations(awakeMs = 0L, remMs = 0L, lightMs = fourHours, deepMs = fourHours),
        )

        // Half light, half deep — and awake/REM, never recorded, get no row.
        assertEquals(2, shares.size)
        assertEquals(
            setOf(SleepStage.STAGE_LIGHT, SleepStage.STAGE_DEEP),
            shares.map { it.stageType }.toSet(),
        )
        assertTrue(shares.all { it.percent == 50 })
        assertTrue(shares.all { it.fraction == 0.5f })
    }

    @Test fun `no recorded stage time hides the breakdown rather than drawing empty bars`() {
        val empty = SleepStageDurations(awakeMs = 0L, remMs = 0L, lightMs = 0L, deepMs = 0L)

        assertTrue(sleepStageShares(empty).isEmpty())
        assertEquals(0L, empty.totalMs)
    }

    @Test fun `the shares come from the overview summary the card is handed`() {
        val summary = SleepOverviewSummary(
            awakeDurationMs = 3_600_000L,
            remDurationMs = 3_600_000L,
            coreDurationMs = 3_600_000L,
            deepDurationMs = 3_600_000L,
        )

        val durations = sleepStageDurationsOf(summary)

        assertEquals(3_600_000L, durations.lightMs)
        assertEquals(4 * 3_600_000L, durations.totalMs)
        assertEquals(4, sleepStageShares(durations).size)
        assertTrue(sleepStageShares(durations).all { it.percent == 25 })
    }

    // ─── the entry lists come out newest night first ─────────────────────────

    @Test fun `the entry lists come out newest night first`() {
        val older = night(tuesday, hours = 6.0, id = "older")
        val newer = night(wednesday, hours = 8.0, id = "newer")

        val ordered = sleepEntriesNewestFirst(listOf(older, newer))

        assertEquals(listOf("newer", "older"), ordered.map { it.id })
        // …while the raw period list the schedule and confidence readings take is
        // left in the order it arrived.
        assertEquals(listOf("older", "newer"), listOf(older, newer).map { it.id })
    }

    // ─── manual-entry confidence ─────────────────────────────────────────────

    @Test fun `an actively-recorded night is not a manual entry`() {
        // Health Connect's RECORDING_METHOD_MANUAL_ENTRY is 3; 1 is
        // ACTIVELY_RECORDED, a night the watch recorded.
        val sessions = listOf(
            night(wednesday, recordingMethod = RecordingMethod.ACTIVELY_RECORDED),
        )

        assertEquals(0, sleepManualEntryCount(sessions))
    }

    @Test fun `a hand-typed night is a manual entry`() {
        val sessions = listOf(night(wednesday, recordingMethod = RecordingMethod.MANUAL_ENTRY))

        assertEquals(1, sleepManualEntryCount(sessions))
        // An unset recording method is not counted either.
        assertEquals(0, sleepManualEntryCount(listOf(night(wednesday))))
    }

    // ─── a week whose nights know their bedtimes gets the schedule chart ─────

    @Test fun `a week whose nights know their bedtimes gets the schedule chart`() {
        val days = (0L until 7L).map { offset ->
            val date = wednesday.minusDays(6 - offset)
            SleepOverviewDay(
                date = date,
                sessions = if (date == wednesday) listOf(night(wednesday)) else emptyList(),
            )
        }
        val scheduleDays = days.toSleepScheduleDays()
        val axis = SleepScheduleAxis.range(scheduleDays, zone, anchorMinute = 22 * 60)

        assertEquals(7, scheduleDays.size)
        assertEquals(1, scheduleDays.count { it.inBedStart != null })
        assertNotNull(axis)
        assertTrue(useSleepScheduleChart(TimeRange.WEEK, scheduleDays, axis))
        assertTrue(useSleepScheduleChart(TimeRange.MONTH, scheduleDays, axis))
        // …but the DAY view never does: it draws the night's own hypnogram.
        assertFalse(useSleepScheduleChart(TimeRange.DAY, scheduleDays, axis))
        assertFalse(useSleepScheduleChart(TimeRange.YEAR, scheduleDays, axis))
    }

    @Test fun `a week with no bedtimes has nothing to put on a clock axis`() {
        val scheduleDays = (0L until 7L)
            .map { SleepOverviewDay(date = wednesday.minusDays(it)) }
            .toSleepScheduleDays()

        assertTrue(scheduleDays.all { it.inBedStart == null })
        assertFalse(useSleepScheduleChart(TimeRange.WEEK, scheduleDays, scheduleAxis = null))
    }

    // ─── only the nights that recorded sleep count as nights ─────────────────

    @Test fun `only the nights that recorded sleep count as nights`() {
        val points = listOf(
            SleepDurationPoint(wednesday.minusDays(6), 0.0),
            SleepDurationPoint(wednesday.minusDays(5), 0.0),
            SleepDurationPoint(wednesday.minusDays(4), 0.0),
            SleepDurationPoint(wednesday.minusDays(3), 0.0),
            SleepDurationPoint(wednesday.minusDays(2), 0.0),
            SleepDurationPoint(tuesday, 6.0),
            SleepDurationPoint(wednesday, 8.0),
        )

        val totals = sleepPeriodTotals(points)

        assertEquals(7, points.size)
        assertEquals(2, totals.nights)
        assertEquals(14.0, totals.totalHours, 1e-9)
        assertEquals(7.0, totals.averageHours, 1e-9)
        assertEquals(8.0, totals.longestHours, 1e-9)
    }

    @Test fun `an empty period derives zeroes not NaN`() {
        val points = (0L until 7L).map { SleepDurationPoint(wednesday.minusDays(it), 0.0) }

        val totals = sleepPeriodTotals(points)

        assertEquals(0, totals.nights)
        assertEquals(0.0, totals.totalHours, 0.0)
        assertEquals(0.0, totals.averageHours, 0.0)
        assertEquals(0.0, totals.longestHours, 0.0)
        assertEquals(0, sleepPeriodTotals(emptyList()).nights)
    }
}
