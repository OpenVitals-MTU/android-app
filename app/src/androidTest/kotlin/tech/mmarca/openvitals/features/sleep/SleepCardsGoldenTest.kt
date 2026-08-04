package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden

/**
 * Port of Flutter's `test/goldens/charts/sleep_cards_golden_test.dart`.
 *
 * [SleepStageShareCard] — the proportional stage-share bars. In Flutter these shipped
 * as empty grey tracks: the coloured fill was a non-positioned child of a Stack, took
 * loose constraints, and a childless box under loose constraints is zero pixels tall.
 * Every test passed, because every test asserted on the numbers beside the bars — and
 * the numbers were right the whole time. This is the assertion nobody thought to
 * write.
 *
 * Kotlin takes the DURATIONS and folds the shares itself (`sleepStageShares`), where
 * Flutter is handed pre-clamped fractions, so the fixture is four totals rather than
 * four fractions. Same card, same bars.
 */
class SleepCardsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aNightBrokenDownByStage() {
        // A 7h 50m night, in the proportions a staged tracker actually reports.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                SleepStageShareCard(
                    durations = SleepStageDurations(
                        awakeMs = minutes(18),
                        remMs = minutes(115),
                        lightMs = minutes(227),
                        deepMs = minutes(110),
                    ),
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_stage_share_card")
    }

    @Test
    fun aDeviceThatOnlySaysAsleep() {
        // The cheap tracker: no stage detail, so everything it recorded is grouped into
        // the Light row and only two rows survive. One at nearly the full track, one at
        // almost none of it — the two ends of the bar's range in a single shot. A 2%
        // bar that renders as nothing is the same class of bug as a 100% bar that does.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                SleepStageShareCard(
                    durations = SleepStageDurations(
                        awakeMs = minutes(9),
                        remMs = 0L,
                        lightMs = minutes(461),
                        deepMs = 0L,
                    ),
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("sleep_stage_share_card_sleeping_only")
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val CARD: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        fun minutes(count: Long): Long = count * 60_000L
    }
}
