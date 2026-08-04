package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaffeineDistributionSlice
import tech.mmarca.openvitals.domain.model.CaffeineTimeBucket
import tech.mmarca.openvitals.domain.model.CaffeineTimeOfDayBucket
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden

/**
 * Port of Flutter's `test/goldens/charts/distribution_bars_golden_test.dart`.
 *
 * The "labelled proportional bar" rows: a label, a value on the right, and a bar
 * under it whose fraction is the row's share of the biggest row in the same card.
 *
 * The fixtures lean on the same two places a bar can lie: a row at essentially the
 * FULL width (where the rounded end cap either meets the track's end or overshoots
 * it) and a row at almost nothing (where a bar can round down to a bare stub, or to
 * nothing at all, and take the row's meaning with it).
 */
class DistributionBarsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun caffeineBySource() {
        // `caffeineDistributionBars` cuts to the top six and scales each against the
        // tallest of them, so the first row is always exactly 1.0 — the full-width
        // case is not an edge case here, it is EVERY card's first row. The last row is
        // 2% of the tallest: a row that renders as an empty track there is the bug,
        // and the number beside it would still be right.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                CaffeineDistributionCard(
                    title = "By source",
                    slices = listOf(
                        CaffeineDistributionSlice("Home espresso", 420.0),
                        CaffeineDistributionSlice("Office filter", 265.0),
                        CaffeineDistributionSlice("Corner café", 148.0),
                        CaffeineDistributionSlice("Green tea", 62.0),
                        CaffeineDistributionSlice("Cola", 34.0),
                        CaffeineDistributionSlice("Dark chocolate", 9.0),
                    ),
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("distribution_caffeine_sources")
    }

    @Test
    fun caffeineByCategory_longLabelsAgainstTheValue() {
        // The label takes the weight and the value does not, so a long name has to
        // yield rather than push the milligrams off the card.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 280.dp) {
                CaffeineDistributionCard(
                    title = "By category",
                    slices = listOf(
                        CaffeineDistributionSlice("Coffee", 685.0),
                        CaffeineDistributionSlice("Tea", 148.0),
                        CaffeineDistributionSlice("Energy drink (imported, 500 ml can)", 80.0),
                        CaffeineDistributionSlice("Soda", 34.0),
                    ),
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("distribution_caffeine_categories")
    }

    @Test
    fun caffeineWithNothingLogged() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 160.dp) {
                CaffeineDistributionCard(
                    title = "By source",
                    slices = emptyList(),
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("distribution_caffeine_empty")
    }

    @Test
    fun caffeineByTimeOfDay() {
        // Four fixed buckets, always all four, so an evening of nothing is a bucket at
        // zero rather than a missing row — the shape of the day is the point.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 280.dp) {
                CaffeineTimeBucketsCard(
                    buckets = listOf(
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.MORNING, 512.0),
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.AFTERNOON, 340.0),
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.EVENING, 48.0),
                        // Nothing at all after midnight. A zero-fraction bar and a
                        // missing bar are different claims, and this card makes the
                        // first one.
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.NIGHT, 0.0),
                    ),
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("distribution_caffeine_buckets")
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val CARD: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    }
}
