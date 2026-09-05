package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden

/**
 * [BodyEnergyTimelineChart]: the score line and the charge/drain strip. Both must agree
 * with the hour row, or the card puts the workout at the wrong time. The fixture is what
 * `buildBodyEnergyDisplay` produces: xFractions across the whole day, one bar per bucket.
 */
class BodyEnergyTimelineGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aDayUpToTheGoldenClock() {
        // 14:30, so the line stops past halfway. A line to the right edge would claim hours not yet happened.
        val (points, bars) = day(buckets = 174)

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 320.dp) {
                BodyEnergyTimelineChart(
                    points = points,
                    influenceBars = bars,
                    // The tallest bar the strip must fit, folded by the mapper. It is the workout hour.
                    maxMagnitude = 1.6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("body_energy_timeline_day")
    }

    @Test
    fun earlyMorning_fewEnoughPointsThatTheLineGrowsDots() {
        // 36 buckets. Past forty the dots are dropped, so this is the only shape with point markers.
        val (points, bars) = day(buckets = 36)

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 320.dp) {
                BodyEnergyTimelineChart(
                    points = points,
                    influenceBars = bars,
                    // Floored at 1.0 by the mapper.
                    maxMagnitude = 1.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("body_energy_timeline_morning")
    }

    private companion object {
        // The bucket the algorithm actually uses, expressed the way the display does.
        const val BUCKET_MINUTES = 5
        const val BUCKETS_PER_DAY = 24 * 60 / BUCKET_MINUTES
        const val WIDTH_FRACTION = BUCKET_MINUTES / (24f * 60f)

        /** A night of sleep, a quiet breakfast, an hour off the wrist, a hard hour on the bike, a slow afternoon. */
        fun bucket(index: Int): Triple<Double, Double, BodyEnergyPrimaryInfluence> = when {
            index < 84 -> Triple(0.4, 0.0, BodyEnergyPrimaryInfluence.SLEEP_RECOVERY)
            index < 96 -> Triple(0.08, 0.0, BodyEnergyPrimaryInfluence.QUIET_REST)
            // Watch off the wrist. NO_DATA draws a low-emphasis tick, not a blank gap.
            index < 108 -> Triple(0.0, 0.0, BodyEnergyPrimaryInfluence.NO_DATA)
            index < 120 -> Triple(0.0, 1.6, BodyEnergyPrimaryInfluence.EXERTION)
            index < 150 -> Triple(0.0, 0.12, BodyEnergyPrimaryInfluence.STEADY)
            else -> Triple(0.0, 0.3, BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE)
        }

        /** The day up to [buckets], accumulating the score the way the timeline does. */
        fun day(buckets: Int): Pair<List<BodyEnergyChartPoint>, List<BodyEnergyInfluenceBar>> {
            val points = mutableListOf<BodyEnergyChartPoint>()
            val bars = mutableListOf<BodyEnergyInfluenceBar>()
            var score = 62.0
            repeat(buckets) { index ->
                val (charge, drain, influence) = bucket(index)
                score = (score + charge - drain).coerceIn(0.0, 100.0)
                val xFraction = index.toFloat() / BUCKETS_PER_DAY
                points += BodyEnergyChartPoint(xFraction = xFraction, score = kotlin.math.round(score))
                bars += BodyEnergyInfluenceBar(
                    xFraction = xFraction,
                    widthFraction = WIDTH_FRACTION,
                    charge = charge,
                    drain = drain,
                    influence = influence,
                )
            }
            return points to bars
        }
    }
}
