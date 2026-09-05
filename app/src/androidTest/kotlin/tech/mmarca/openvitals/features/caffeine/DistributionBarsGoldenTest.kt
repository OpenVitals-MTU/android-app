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
 * Labelled proportional bar rows. The fixtures lean on the two places a bar can lie:
 * a row at full width and a row at almost nothing.
 */
class DistributionBarsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun caffeineBySource() {
        // The first row is always exactly 1.0 and the last is 2% of the tallest; an empty track there is the bug.
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
        // The label takes the weight, so a long name yields rather than pushing the value off the card.
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
        // Four fixed buckets, always all four: an evening of nothing is a bucket at zero.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 280.dp) {
                CaffeineTimeBucketsCard(
                    buckets = listOf(
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.MORNING, 512.0),
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.AFTERNOON, 340.0),
                        CaffeineTimeBucket(CaffeineTimeOfDayBucket.EVENING, 48.0),
                        // A zero-fraction bar and a missing bar are different claims.
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
