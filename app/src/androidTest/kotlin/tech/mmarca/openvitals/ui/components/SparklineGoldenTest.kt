// The chart composables live under ui/charts/ but declare ui.components; this file mirrors that.
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
 * [MetricSparklineChart]: no axis, no labels, no card, so only a picture can assert on it.
 * The one-point case is real: `singlePointLine` keeps a single workout from rendering as a lone dot.
 */
class SparklineGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWeekOfBuckets() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 320.dp, height = 90.dp) {
                MetricSparklineChart(
                    // Minutes per day. The zero is deliberate: a rest day reaches the floor.
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
        // The default, kept because a refactor that flipped it would otherwise pass silently.
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
        // The height the activity rows give it. A sparkline in an unbounded box paints nothing.
        val SPARKLINE_BOX: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(58.dp)
    }
}
