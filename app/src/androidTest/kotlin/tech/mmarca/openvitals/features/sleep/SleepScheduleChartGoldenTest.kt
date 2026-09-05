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
 * [SleepScheduleStageChart]: a week of nights on one clock anchored at 18:00, so a night
 * crossing midnight is one bar. Every night here straddles midnight and Saturday goes to bed after it.
 * This is the one chart with its label gutter on the right.
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
        // A full night the tracker staged only near the end. The bar draws the whole time in bed
        // as a base block with the staged tail overlaid. The middle night is fully staged.
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
        // Minutes of the day, not anchored minutes. The chart anchors them itself.
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
        // `date` is the morning you woke up, so every in-bed window starts the day before.
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
            // Bedtime belongs to the evening before, unless it is past midnight already.
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
            // Thursday: a window but no stages. The chart draws a solid bar.
            SleepScheduleDay(
                date = MONDAY.plusDays(3),
                inBedStart = goldenInstant(2026, 6, 18, 23, 45),
                inBedEnd = goldenInstant(2026, 6, 19, 7, 0),
            ),
            night(MONDAY.plusDays(4), bedHour = 23, bedMinute = 55, minutes = 390),
            // Saturday: in bed at 01:05, up at 09:40. The wrap case.
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
