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
 * Port of Flutter's `test/goldens/charts/body_energy_timeline_golden_test.dart`.
 *
 * [BodyEnergyTimelineChart] — the 0-100 score line, and the charge/drain strip under
 * it. Two painters that have to agree with each other and with the hour row beneath
 * them: the line's x, the bar's x and the axis's 12:00 all come from the same
 * fraction of the same day, and if any one of them drifts the card says the workout
 * happened at a time it did not.
 *
 * Everything here arrives precomputed from the presentation mapper, so the fixture is
 * what `buildBodyEnergyDisplay` produces: xFractions across the WHOLE day, one bar
 * per five-minute bucket.
 */
class BodyEnergyTimelineGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aDayUpToTheGoldenClock() {
        // 14:30, so the line stops just past halfway and the rest of the day is empty —
        // the same honesty the day charts owe: a line held out to the right edge would
        // be a claim about hours that have not happened.
        val (points, bars) = day(buckets = 174)

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 320.dp) {
                BodyEnergyTimelineChart(
                    points = points,
                    influenceBars = bars,
                    // The tallest bar the strip must fit, folded by the mapper rather
                    // than rescanned on every repaint. It is the workout hour.
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
        // 36 buckets (three hours in). Past forty the dots are dropped, so this is the
        // only shape in which the point markers are ever drawn at all.
        val (points, bars) = day(buckets = 36)

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 320.dp) {
                BodyEnergyTimelineChart(
                    points = points,
                    influenceBars = bars,
                    // Floored at 1.0 by the mapper, so an all-quiet night still divides
                    // by something.
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

        /**
         * What each bucket of a plausible day is doing: a night of sleep recovering the
         * score, a quiet breakfast, an hour the watch was off the wrist, a hard hour on
         * the bike, then a slow afternoon drain.
         */
        fun bucket(index: Int): Triple<Double, Double, BodyEnergyPrimaryInfluence> = when {
            index < 84 -> Triple(0.4, 0.0, BodyEnergyPrimaryInfluence.SLEEP_RECOVERY)
            index < 96 -> Triple(0.08, 0.0, BodyEnergyPrimaryInfluence.QUIET_REST)
            // Watch off the wrist. NO_DATA with nothing to draw is not a blank gap — it
            // is a low-emphasis tick spanning the strip, which is the only way the card
            // can say "I do not know" instead of "nothing happened".
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
