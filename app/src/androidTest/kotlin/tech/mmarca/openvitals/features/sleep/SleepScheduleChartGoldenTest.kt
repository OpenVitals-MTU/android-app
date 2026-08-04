package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenInstant

/**
 * Port of Flutter's `test/goldens/charts/sleep_schedule_chart_golden_test.dart`.
 *
 * [SleepScheduleStageChart] — a week of nights on one clock. Its whole reason for
 * existing is the 18:00 anchor: a night that starts at 23:40 and ends at 07:10
 * crosses midnight, and on a plain 00:00-24:00 axis it would be drawn as two bars at
 * opposite ends of the chart. Anchored at six in the evening, it is one bar. Nothing
 * but a picture shows whether that still holds, and the fixture leans on it — every
 * night here straddles midnight, and one of them (Saturday) goes to bed AFTER it.
 *
 * This is also the one chart in the app whose label gutter is on the RIGHT, so its
 * x-axis row is padded on the right rather than inset on the left. The golden is the
 * only thing that would catch someone "fixing" that.
 */
class SleepScheduleChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWeekOfNights() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 400.dp) {
                ScheduleChart(days = week(), summary = "This week · 7h 24m avg")
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_schedule_chart_week")
    }

    @Test
    fun aPartlyStagedNightReadsAtItsFullDuration() {
        // A full night in bed (23:30-07:00) that the tracker staged only near the end —
        // a tail-only reading. The bar draws the whole time in bed as a base block with
        // stage colour overlaid on the staged tail, so it reads as its full duration:
        // not a tiny fragment floating in an empty slot, and not a uniform solid block
        // that hides the data. The middle night is fully staged.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 400.dp) {
                ScheduleChart(
                    days = listOf(
                        tailStaged(MONDAY),
                        night(MONDAY.plusDays(1), bedHour = 23, bedMinute = 5, minutes = 475),
                        tailStaged(MONDAY.plusDays(2)),
                    ),
                    summary = "This week · partly staged",
                    period = DatePeriod(MONDAY, MONDAY.plusDays(2)),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_schedule_chart_partly_staged")
    }

    @Test
    fun aWeekWithTheAverageBedtimeAndWakeUpMarked() {
        // Minutes of the day, not anchored minutes: 23:35 to bed, up at 07:20. The
        // chart anchors them itself, which is the only reason a bedtime of 1415 and a
        // wake-up of 440 can share one axis.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 400.dp) {
                ScheduleChart(
                    days = week(),
                    summary = "This week · 7h 24m avg",
                    averageSchedule = SleepOverviewSchedule(startMinute = 1_415, endMinute = 440),
                    selectedDate = MONDAY.plusDays(4),
                    onDateSelected = {},
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_schedule_chart_markers")
    }

    @androidx.compose.runtime.Composable
    private fun ScheduleChart(
        days: List<SleepScheduleDay>,
        summary: String,
        period: DatePeriod = WEEK,
        averageSchedule: SleepOverviewSchedule? = null,
        selectedDate: LocalDate? = null,
        onDateSelected: ((LocalDate) -> Unit)? = null,
    ) {
        SleepScheduleStageChart(
            title = "Sleep schedule",
            summaryText = summary,
            days = days,
            sleepWindow = SleepWindow.Default,
            selectedRange = TimeRange.WEEK,
            period = period,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            averageSchedule = averageSchedule,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    private companion object {
        // `date` is the night's DATE — the morning you woke up — which is why every
        // in-bed window starts on the day before.
        val MONDAY: LocalDate = LocalDate.of(2026, 6, 16)
        val WEEK = DatePeriod(MONDAY, LocalDate.of(2026, 6, 22))

        /** A night with plausible architecture, laid down from its bedtime. */
        fun stagesFrom(start: Instant, totalMinutes: Long): List<SleepStage> {
            val cycle = listOf(
                SleepStage.STAGE_AWAKE to 8L,
                SleepStage.STAGE_LIGHT to 45L,
                SleepStage.STAGE_DEEP to 50L,
                SleepStage.STAGE_LIGHT to 35L,
                SleepStage.STAGE_REM to 40L,
            )
            val end = start.plusSeconds(totalMinutes * 60L)
            val stages = mutableListOf<SleepStage>()
            var cursor = start
            var index = 0
            while (cursor.isBefore(end)) {
                val (type, minutes) = cycle[index % cycle.size]
                val stageEnd = minOf(cursor.plusSeconds(minutes * 60L), end)
                stages += SleepStage(startTime = cursor, endTime = stageEnd, stageType = type)
                cursor = stageEnd
                index++
            }
            return stages
        }

        fun night(
            date: LocalDate,
            bedHour: Int,
            bedMinute: Int,
            minutes: Long,
        ): SleepScheduleDay {
            // Bedtime belongs to the EVENING BEFORE the night's date, unless it is past
            // midnight already — the Saturday lie-in below.
            val bedDate = if (bedHour >= 12) date.minusDays(1) else date
            val start = goldenInstant(
                bedDate.year,
                bedDate.monthValue,
                bedDate.dayOfMonth,
                bedHour,
                bedMinute,
            )
            return SleepScheduleDay(
                date = date,
                inBedStart = start,
                inBedEnd = start.plusSeconds(minutes * 60L),
                stages = stagesFrom(start, minutes),
            )
        }

        fun tailStaged(date: LocalDate): SleepScheduleDay {
            val bed = goldenInstant(
                date.minusDays(1).year,
                date.minusDays(1).monthValue,
                date.minusDays(1).dayOfMonth,
                23,
                30,
            )
            val wake = bed.plusSeconds(450L * 60L)
            return SleepScheduleDay(
                date = date,
                inBedStart = bed,
                inBedEnd = wake,
                // ~50 min of stages at the tail of a 7h30m night — well under half.
                stages = stagesFrom(wake.minusSeconds(50L * 60L), 50L),
            )
        }

        fun week(): List<SleepScheduleDay> = listOf(
            night(MONDAY, bedHour = 23, bedMinute = 20, minutes = 460),
            night(MONDAY.plusDays(1), bedHour = 23, bedMinute = 5, minutes = 475),
            night(MONDAY.plusDays(2), bedHour = 0, bedMinute = 10, minutes = 410),
            // Thursday: the tracker recorded a window but no stages at all. The chart
            // draws a solid bar rather than an empty slot — "I know when, not what".
            SleepScheduleDay(
                date = MONDAY.plusDays(3),
                inBedStart = goldenInstant(2026, 6, 18, 23, 45),
                inBedEnd = goldenInstant(2026, 6, 19, 7, 0),
            ),
            night(MONDAY.plusDays(4), bedHour = 23, bedMinute = 55, minutes = 390),
            // Saturday: a late night out, in bed at 01:05, up at 09:40. Past the 18:00
            // anchor by more than a day's worth of minutes — this is the wrap case.
            night(MONDAY.plusDays(5), bedHour = 1, bedMinute = 5, minutes = 515),
            // Sunday: no session at all. A missing night is a gap, not a zero.
            SleepScheduleDay(
                date = LocalDate.of(2026, 6, 22),
                inBedStart = null,
                inBedEnd = null,
            ),
        )
    }
}
