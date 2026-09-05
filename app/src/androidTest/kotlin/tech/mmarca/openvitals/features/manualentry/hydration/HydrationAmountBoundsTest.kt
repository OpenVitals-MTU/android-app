package tech.mmarca.openvitals.features.manualentry.hydration

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The rejection message is in the field's own unit, so a user typing fluid ounces can act on it. */
class HydrationAmountBoundsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theAllowedAmountRangeIsStatedInTheFieldsOwnUnit() {
        var metricMessage = ""
        var imperialMessage = ""
        composeRule.setContent {
            OpenVitalsTheme {
                metricMessage = hydrationInputInvalidAmountText(
                    UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                )
                imperialMessage = hydrationInputInvalidAmountText(
                    UnitFormatter(unitSystemProvider = { UnitSystem.IMPERIAL }),
                )
            }
        }

        composeRule.runOnIdle {
            val metricUnit = hydrationInputUnitLabel(UnitSystem.METRIC)
            val imperialUnit = hydrationInputUnitLabel(UnitSystem.IMPERIAL)

            assertNotEquals(
                "the two unit systems must not share one message",
                metricMessage,
                imperialMessage,
            )
            assertTrue(
                "the metric bounds carry the metric unit",
                metricMessage.contains(metricUnit),
            )
            assertTrue(
                "the imperial bounds carry the imperial unit",
                imperialMessage.contains(imperialUnit),
            )
            assertFalse(
                "an imperial user is never quoted a millilitre bound",
                imperialMessage.contains(metricUnit),
            )
        }
    }
}
