package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetProgress
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

/**
 * [DashboardSummaryCard]: the hero gauges. At zero the fill must not be drawn, because a
 * round cap on a zero-length arc paints a dot. Over goal the progress is clamped.
 */
class SummaryRingGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nothingYet_theTrackAndNoFill() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = RING_WIDTH, height = RING_WIDTH) {
                Ring(
                    title = "Steps",
                    value = DisplayValue(value = "0", unit = ""),
                    accentColor = StepsColor,
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    progress = DashboardWidgetProgress(fraction = 0f, label = "of 10,000"),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("summary_ring_zero")
    }

    @Test
    fun partWayRound() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = RING_WIDTH, height = RING_WIDTH) {
                Ring(
                    title = "Steps",
                    value = DisplayValue(value = "4,512", unit = ""),
                    accentColor = StepsColor,
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    progress = DashboardWidgetProgress(fraction = 0.45f, label = "of 10,000"),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("summary_ring_partial")
    }

    @Test
    fun pastTheGoal_theArcClosesTheNumberKeepsGoing() {
        // Clamped to 1.0 inside the card, so 135% and 100% draw the same arc.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = RING_WIDTH, height = RING_WIDTH) {
                Ring(
                    title = "Weekly cardio",
                    value = DisplayValue(value = "203", unit = "min"),
                    accentColor = WorkoutColor,
                    icon = Icons.Outlined.FitnessCenter,
                    progress = DashboardWidgetProgress(fraction = 1.35f, label = "of 150"),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("summary_ring_over")
    }

    @Test
    fun aValueLongEnoughToFightTheRingForRoom() {
        // The centre text shrinks rather than wraps or clips. A six-figure count on a 174dp ring exercises it.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = RING_WIDTH, height = RING_WIDTH) {
                Ring(
                    title = "Steps",
                    value = DisplayValue(value = "128,540", unit = ""),
                    accentColor = StepsColor,
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    progress = DashboardWidgetProgress(
                        fraction = 1f,
                        label = "of 10,000 · goal smashed",
                    ),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("summary_ring_long_value")
    }

    @Composable
    private fun Ring(
        title: String,
        value: DisplayValue,
        accentColor: Color,
        icon: ImageVector,
        progress: DashboardWidgetProgress,
    ) {
        DashboardSummaryCard(
            title = title,
            value = value,
            icon = icon,
            accentColor = accentColor,
            progress = progress,
            modifier = Modifier.size(RING_WIDTH),
        )
    }

    private companion object {
        // Half a phone's content width, less the gap. The gauge scales its stroke off its side, so the width is part of the fixture.
        val RING_WIDTH = 174.dp
    }
}
