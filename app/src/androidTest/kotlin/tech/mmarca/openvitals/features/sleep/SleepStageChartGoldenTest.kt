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
 * Port of Flutter's `test/goldens/charts/sleep_stage_chart_golden_test.dart`.
 *
 * [SleepStagesLaneChart] — the hypnogram. The single most intricate painter in the
 * app: four lanes, one [androidx.compose.ui.graphics.Path] over every segment,
 * connectors wherever two stages touch, and a vertical gradient stretched across the
 * lane centres so a segment's colour comes from WHERE it is rather than from what it
 * is. Nothing about that survives a unit test. A regression here — a connector that
 * stops connecting, a gradient that collapses to one colour — is invisible except in
 * a picture.
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
        // The day card lists the same totals underneath, so repeating them in the lane
        // labels would say everything twice.
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
        // The cheap tracker: no stage detail at all, just SLEEPING and AWAKE. It still
        // gets four lanes — the standard set is fixed — and everything it recorded
        // lands in the Light lane, which is where SLEEPING is grouped. Two empty lanes
        // is the honest picture, and it is a picture worth having.
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
        // Two sessions of one night with a get-up between them. The week chart has always
        // drawn that stretch in translucent SleepColor, because it paints the in-bed span
        // as a base block and only overlays the stages it has; the day view painted the
        // same stretch in the Awake pink, because the gap was handed to it typed
        // STAGE_AWAKE and every segment took its colour from the lane gradient. This is
        // the picture of the two views finally agreeing — and the gap is the one segment
        // in the chart that does NOT come from the gradient, which is worth a photograph.
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

        // Pinned rather than localized: `ofLocalizedTime` follows the device, and the
        // scrub tooltip is not what these pictures are about.
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val CHART: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        // A night that straddles midnight, because every night does, and the chart's x
        // axis is the session rather than the day.
        val BEDTIME: Instant = goldenInstant(2026, 6, 21, 23, 15)
        val WAKE_UP: Instant = goldenInstant(2026, 6, 22, 7, 5)

        fun stage(type: Int, startMinute: Long, endMinute: Long): SleepStage = SleepStage(
            startTime = BEDTIME.plusSeconds(startMinute * 60L),
            endTime = BEDTIME.plusSeconds(endMinute * 60L),
            stageType = type,
        )

        /**
         * A plausible architecture: deep early, REM lengthening toward morning, a brief
         * wake before dawn. The stages are CONTIGUOUS on purpose — the connectors are
         * only drawn where one stage ends exactly where the next begins, so a fixture
         * with gaps in it would photograph a chart with no connectors and quietly stop
         * testing them.
         */
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

        /**
         * The same night, but slept in two goes with 90 minutes up in between — the shape
         * `combineNightStages` produces once it bridges two sessions less than three hours
         * apart. Contiguous like [night], so the connectors into and out of the gap are in
         * the picture too: they are the only ones that cross between the two paths the
         * chart now draws.
         */
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
