// The chart composables live under ui/charts/ but declare ui.components; this
// file mirrors that rather than adding an import that looks like a mistake.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.ui.theme.WorkoutColor

/**
 * Port of Flutter's `test/goldens/charts/sparkline_golden_test.dart`.
 *
 * [MetricSparklineChart] — the mini trend line on the activity summary rows. It has
 * no axis, no labels and no card: everything it says, it says with the line's shape
 * against its baseline. Which is precisely why it needs a picture — there is nothing
 * else in it to assert on.
 *
 * The one-point case is a real state, not a degenerate one: a person with a single
 * workout in the window still gets a row, and `singlePointLine` is what keeps that
 * row from rendering as one lonely dot in the middle of nowhere.
 */
class SparklineGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWeekOfBuckets() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 320.dp, height = 90.dp) {
                MetricSparklineChart(
                    // Minutes of activity per day. The zero is deliberate: a rest day
                    // has to reach the floor of the chart, not merely dip.
                    values = listOf(42.0, 0.0, 65.0, 30.0, 0.0, 88.0, 55.0),
                    accentColor = WorkoutColor,
                    singlePointLine = true,
                    modifier = SPARKLINE_BOX,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sparkline_week")
    }

    @Test
    fun onePoint_aFlatRunAcrossTheWholeWidth() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 320.dp, height = 90.dp) {
                MetricSparklineChart(
                    values = listOf(42.0),
                    accentColor = WorkoutColor,
                    singlePointLine = true,
                    modifier = SPARKLINE_BOX,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sparkline_single")
    }

    @Test
    fun onePoint_withSinglePointLineOff_theDotOnItsOwn() {
        // The default. Kept as a golden because the difference between this and the
        // one above is the entire reason the flag exists, and a refactor that flipped
        // the default would otherwise pass silently.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 320.dp, height = 90.dp) {
                MetricSparklineChart(
                    values = listOf(42.0),
                    accentColor = WorkoutColor,
                    modifier = SPARKLINE_BOX,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sparkline_single_dot")
    }

    private companion object {
        // The height the activity rows give it. A sparkline in an unbounded box paints
        // nothing, so the box is part of the fixture.
        val SPARKLINE_BOX: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(58.dp)
    }
}
