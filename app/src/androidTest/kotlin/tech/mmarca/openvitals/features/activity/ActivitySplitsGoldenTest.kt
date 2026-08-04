package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.math.roundToLong
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.ActivitySplit
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenInstantAt

/**
 * Port of Flutter's `test/goldens/charts/activity_splits_golden_test.dart`.
 *
 * [ActivitySplitsCard] — the pace bars. The bar is deliberately NOT zero-based, and
 * that is the whole point of photographing it: the interesting range of a run is the
 * thirty seconds between its fastest and its slowest kilometre, and a zero-based bar
 * squashes that into a row of identical stripes. The slowest split fills the track,
 * the fastest sits at the 25% floor so it still reads as a bar rather than as
 * nothing. A "fix" that re-based the scale at zero would leave every existing test
 * green.
 *
 * The estimated source has no bar at all, and that is not an oversight either: every
 * estimated split has the same pace by construction, so a bar chart of it would be a
 * flat line pretending to be a measurement.
 */
class ActivitySplitsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun splitsCutFromTheRoute() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 520.dp) {
                ActivitySplitsCard(
                    splits = ActivitySplits(source = SplitSource.ROUTE, splits = runSplits()),
                    splitDistanceMeters = 1_000.0,
                    slowestPaceSeconds = SLOWEST,
                    fastestPaceSeconds = FASTEST,
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("activity_splits_route")
    }

    @Test
    fun lapsTheDeviceRecordedItself() {
        // A lap is whatever the watch called a lap — never re-cut, never marked
        // partial, so the distances are uneven and none of them is an apology.
        val start = goldenInstantAt(7, 30)
        val laps = listOf(
            split(1, 400.0, 298.0, start),
            split(2, 400.0, 306.0, start.plusSeconds(120)),
            split(3, 800.0, 335.0, start.plusSeconds(245)),
            split(4, 400.0, 291.0, start.plusSeconds(515)),
        )

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 420.dp) {
                ActivitySplitsCard(
                    splits = ActivitySplits(source = SplitSource.DEVICE_LAPS, splits = laps),
                    splitDistanceMeters = 1_000.0,
                    slowestPaceSeconds = 335.0,
                    fastestPaceSeconds = 291.0,
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("activity_splits_laps")
    }

    @Test
    fun estimatedSplits_theNumbersAndNoBar() {
        // Distance and duration only: the pace is the activity average, evenly
        // divided. The card says so in words and withholds the bar.
        val start = goldenInstantAt(7, 30)
        val estimated = (0 until 4).map { index ->
            split(index + 1, 1_000.0, AVERAGE_PACE, start.plusSeconds(322L * index))
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 400.dp) {
                ActivitySplitsCard(
                    splits = ActivitySplits(source = SplitSource.ESTIMATED, splits = estimated),
                    splitDistanceMeters = 1_000.0,
                    slowestPaceSeconds = AVERAGE_PACE,
                    fastestPaceSeconds = AVERAGE_PACE,
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("activity_splits_estimated")
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val CARD: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        /**
         * Seconds per kilometre for each split of a real-feeling 5.42 km run: a steady
         * opening, a slow fourth (the hill), and a fast finish.
         */
        val PACE_SECONDS = listOf(320.0, 312.0, 330.0, 345.0, 305.0)
        const val PARTIAL_PACE = 315.0
        const val PARTIAL_METERS = 420.0

        // Folded by the display layer, not rescanned by the card — a ratio between
        // splits, so metric seconds-per-km is the right scale even for a user reading
        // min/mi.
        const val SLOWEST = 345.0
        const val FASTEST = 305.0

        // The activity's own average pace, which is what each split's delta is measured
        // against: total time over total distance, NOT the mean of the split paces.
        const val AVERAGE_PACE = 321.8

        fun split(index: Int, meters: Double, pace: Double, from: Instant): ActivitySplit {
            val elapsedMs = (pace * meters / 1000.0 * 1000).roundToLong()
            return ActivitySplit(
                index = index,
                distanceMeters = meters,
                elapsedMs = elapsedMs,
                startTime = from,
                endTime = from.plusMillis(elapsedMs),
                isPartial = meters < 1000.0,
                averageHeartRateBpm = 148.0 + index * 3,
                elevationGainMeters = 4.0 + index * 2,
                elevationLossMeters = 6.0 - index * 0.5,
                paceDeltaSecondsPerKilometer = pace - AVERAGE_PACE,
            )
        }

        fun runSplits(): List<ActivitySplit> {
            val splits = mutableListOf<ActivitySplit>()
            var cursor = goldenInstantAt(7, 30)
            PACE_SECONDS.forEachIndexed { index, pace ->
                val next = split(index + 1, 1_000.0, pace, cursor)
                splits += next
                cursor = next.endTime
            }
            // The trailing remainder. It keeps its real (short) distance and says so —
            // a partial split is short ON PURPOSE, and unlabelled it reads as a bug.
            splits += split(PACE_SECONDS.size + 1, PARTIAL_METERS, PARTIAL_PACE, cursor)
            return splits
        }
    }
}
