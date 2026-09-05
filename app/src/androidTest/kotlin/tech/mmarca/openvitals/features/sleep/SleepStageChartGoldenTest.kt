package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenInstant

/**
 * [SleepStagesLaneChart], the hypnogram: four lanes, connectors where stages touch, and a
 * gradient across the lane centres. A regression here is invisible except in a picture.
 */
class SleepStageChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aNight_withTheLaneTotalsTheDetailScreenShows() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                SleepStagesLaneChart(
                    stages = night(),
                    unitFormatter = FORMATTER,
                    timeFormatter = TIME_FORMATTER,
                    timelineStart = BEDTIME,
                    timelineEnd = WAKE_UP,
                    modifier = CHART,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_stage_chart_night")
    }

    @Test
    fun theSameNightOnTheDayCard_labelsWithoutTotals() {
        // The day card lists the same totals underneath.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                SleepStagesLaneChart(
                    stages = night(),
                    unitFormatter = FORMATTER,
                    timeFormatter = TIME_FORMATTER,
                    timelineStart = BEDTIME,
                    timelineEnd = WAKE_UP,
                    showInlineLabels = false,
                    modifier = CHART,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_stage_chart_no_totals")
    }

    @Test
    fun aDeviceThatOnlySaysAsleep() {
        // A cheap tracker with only SLEEPING and AWAKE still gets four lanes;
        // everything lands in the Light lane and two lanes stay empty.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                SleepStagesLaneChart(
                    stages = listOf(
                        stage(SleepStage.STAGE_AWAKE, 0, 12),
                        stage(SleepStage.STAGE_SLEEPING, 12, 355),
                        stage(SleepStage.STAGE_AWAKE, 355, 363),
                        stage(SleepStage.STAGE_SLEEPING, 363, 470),
                    ),
                    unitFormatter = FORMATTER,
                    timeFormatter = TIME_FORMATTER,
                    timelineStart = BEDTIME,
                    timelineEnd = WAKE_UP,
                    modifier = CHART,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_stage_chart_sleeping_only")
    }

    @Test
    fun aSplitNight_theStretchOutOfBed() {
        // Two sessions with a get-up between them. The day view used to paint the gap in Awake pink
        // while the week chart used translucent SleepColor. The gap is the one segment not from the gradient.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                SleepStagesLaneChart(
                    stages = splitNight(),
                    unitFormatter = FORMATTER,
                    timeFormatter = TIME_FORMATTER,
                    timelineStart = BEDTIME,
                    timelineEnd = WAKE_UP,
                    showInlineLabels = false,
                    modifier = CHART,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_stage_chart_split_night")
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        // Pinned rather than localized: `ofLocalizedTime` follows the device.
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val CHART: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        // A night that straddles midnight; the x axis is the session, not the day.
        val BEDTIME: Instant = goldenInstant(2026, 6, 21, 23, 15)
        val WAKE_UP: Instant = goldenInstant(2026, 6, 22, 7, 5)

        fun stage(type: Int, startMinute: Long, endMinute: Long): SleepStage = SleepStage(
            startTime = BEDTIME.plusSeconds(startMinute * 60L),
            endTime = BEDTIME.plusSeconds(endMinute * 60L),
            stageType = type,
        )

        /** Deep early, REM lengthening toward morning, a brief wake before dawn. Contiguous on purpose, or the connectors go untested. */
        fun night(): List<SleepStage> = listOf(
            stage(SleepStage.STAGE_AWAKE, 0, 10),
            stage(SleepStage.STAGE_LIGHT, 10, 55),
            stage(SleepStage.STAGE_DEEP, 55, 110),
            stage(SleepStage.STAGE_LIGHT, 110, 145),
            stage(SleepStage.STAGE_REM, 145, 180),
            stage(SleepStage.STAGE_LIGHT, 180, 225),
            stage(SleepStage.STAGE_DEEP, 225, 275),
            stage(SleepStage.STAGE_LIGHT, 275, 315),
            stage(SleepStage.STAGE_REM, 315, 355),
            stage(SleepStage.STAGE_AWAKE, 355, 363),
            stage(SleepStage.STAGE_LIGHT, 363, 410),
            stage(SleepStage.STAGE_REM, 410, 450),
            stage(SleepStage.STAGE_LIGHT, 450, 470),
        )

        /** The same night slept in two goes, 90 minutes up in between, as `combineNightStages` produces. */
        fun splitNight(): List<SleepStage> = listOf(
            stage(SleepStage.STAGE_LIGHT, 0, 40),
            stage(SleepStage.STAGE_DEEP, 40, 95),
            stage(SleepStage.STAGE_LIGHT, 95, 140),
            stage(SleepStage.STAGE_REM, 140, 175),
            stage(SleepStage.STAGE_LIGHT, 175, 235),
            stage(SleepStage.STAGE_OUT_OF_BED, 235, 325),
            stage(SleepStage.STAGE_LIGHT, 325, 375),
            stage(SleepStage.STAGE_REM, 375, 425),
            stage(SleepStage.STAGE_LIGHT, 425, 470),
        )
    }
}
