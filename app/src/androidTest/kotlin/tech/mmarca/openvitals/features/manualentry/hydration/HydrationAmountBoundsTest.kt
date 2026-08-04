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

/**
 * Port of the `volume > bounds are rendered in the field's unit` case of
 * Flutter's `test/core/presentation/measurement_input_test.dart`.
 *
 * The amount field is labelled in the user's own unit, so the rejection message
 * has to be too. Telling someone typing fluid ounces that the allowed range is
 * "1 to 100,000 ml" is an instruction they cannot act on without doing the
 * conversion the app exists to do for them.
 */
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
