package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's `test/ui/components/metric_card_test.dart`.
 *
 * These are the small shared pieces every metric screen is assembled from, so
 * they are worth pinning once here rather than through each screen that draws
 * them.
 */
class MetricCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun metricCard_showsTitleValueUnitSubtitleAndSource() {
        composeRule.setContent {
            OpenVitalsTheme {
                MetricCard(
                    title = "Steps",
                    value = "8,432",
                    unit = "steps",
                    icon = Icons.Outlined.DirectionsWalk,
                    accentColor = Accent,
                    subtitle = "Goal 10,000",
                    source = "Fitbit",
                )
            }
        }

        listOf("Steps", "8,432", "steps", "Goal 10,000", "Fitbit").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun metricCard_onClickFires() {
        var clicked = false

        composeRule.setContent {
            OpenVitalsTheme {
                MetricCard(
                    title = "Steps",
                    value = "8,432",
                    unit = "steps",
                    icon = Icons.Outlined.DirectionsWalk,
                    accentColor = Accent,
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("8,432").performClick()

        assertTrue("tapping the card's value must trigger its click", clicked)
    }

    @Test
    fun metricCardPlaceholder_showsItsMessage() {
        composeRule.setContent {
            OpenVitalsTheme {
                MetricCardPlaceholder(
                    title = "Sleep",
                    icon = Icons.Outlined.Bedtime,
                    accentColor = Accent,
                    message = "No data for this period",
                )
            }
        }

        composeRule.onNodeWithText("Sleep").assertIsDisplayed()
        composeRule.onNodeWithText("No data for this period").assertIsDisplayed()
    }

    @Test
    fun sectionHeader_rendersItsText() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column { SectionHeader("Trends") }
            }
        }

        composeRule.onNodeWithText("Trends").assertIsDisplayed()
    }

    private companion object {
        val Accent = Color(0xFF4CAF50)
    }
}
