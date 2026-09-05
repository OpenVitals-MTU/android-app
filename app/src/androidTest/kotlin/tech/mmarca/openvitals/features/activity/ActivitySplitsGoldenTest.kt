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
 * [ActivitySplitsCard]: the pace bars. The bar is not zero-based on purpose: the slowest
 * split fills the track and the fastest sits at the 25% floor. The estimated source has no bar,
 * because every estimated split has the same pace by construction.
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
        // A lap is whatever the watch called a lap: never re-cut, never marked partial.
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
        // Distance and duration only, so the pace is the evenly divided average. The card withholds the bar.
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

        /** Seconds per kilometre for a 5.42 km run: steady opening, a slow fourth, a fast finish. */
        val PACE_SECONDS = listOf(320.0, 312.0, 330.0, 345.0, 305.0)
        const val PARTIAL_PACE = 315.0
        const val PARTIAL_METERS = 420.0

        // Folded by the display layer. A ratio between splits, so seconds per km is right even for min/mi.
        const val SLOWEST = 345.0
        const val FASTEST = 305.0

        // The activity's own average pace: total time over total distance, not the mean of the split paces.
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
            // The trailing remainder keeps its real short distance and says so.
            splits += split(PACE_SECONDS.size + 1, PARTIAL_METERS, PARTIAL_PACE, cursor)
            return splits
        }
    }
}
