package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.math.pow
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeineEntryInsight
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenInstantAt

/**
 * Port of Flutter's `test/goldens/charts/caffeine_curve_golden_test.dart`.
 *
 * [CaffeineCurveCard] — the decay curve, on the shared line plot. Three things have
 * to survive any change to it: the SAWTOOTH (a dose lands instantly and then decays,
 * so the curve jumps and slides, it does not swell), the DASHED sleep threshold, and
 * the per-drink markers on the baseline. The fixture is built to make all three
 * unmissable.
 */
class CaffeineCurveGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aDayOfDrinking_sawtoothThresholdMarkers() {
        val curve = (0..24 * 4).map { quarter ->
            val at = goldenInstantAt(0).plusSeconds(quarter * 15L * 60L)
            CaffeinePoint(time = at, valueMg = activeMg(at))
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                CaffeineCurveCard(
                    insights = CaffeineInsights(
                        currentMg = activeMg(goldenInstantAt(14, 30)),
                        todayTotalMg = 270.0,
                        sleepThresholdMg = THRESHOLD_MG,
                        bedtimeMg = activeMg(goldenInstantAt(23)),
                        curvePoints = curve,
                        entryInsights = DOSES.mapIndexed { index, (at, mg) ->
                            entryInsight(id = "dose_$index", at = at, mg = mg)
                        },
                    ),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    onSelectEntry = {},
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("caffeine_curve_day", allowedDifferentPixelRatio = SmallFeatureTolerance)
    }

    @Test
    fun aSingleDose_oneRiseOneLongDecay() {
        // The shape a light day actually has, and the one where the curve never
        // reaches the threshold: the dashed line sits ABOVE the whole trace, because
        // `caffeineCurveMaxMg` takes the threshold rather than the peak. That branch
        // has no other picture.
        val at = goldenInstantAt(8)
        val curve = (0..24 * 4).map { quarter ->
            val time = goldenInstantAt(0).plusSeconds(quarter * 15L * 60L)
            CaffeinePoint(time = time, valueMg = decayed(mg = 40.0, from = at, to = time))
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                CaffeineCurveCard(
                    insights = CaffeineInsights(
                        currentMg = 18.0,
                        todayTotalMg = 40.0,
                        sleepThresholdMg = THRESHOLD_MG,
                        curvePoints = curve,
                        entryInsights = listOf(entryInsight(id = "dose_0", at = at, mg = 40.0)),
                    ),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    onSelectEntry = {},
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("caffeine_curve_single_dose", allowedDifferentPixelRatio = SmallFeatureTolerance)
    }

    @Test
    fun aDayWithNothingInIt() {
        // Fewer than two points and the card refuses to draw a line at all — one point
        // is not a trend, and placing it would divide by a zero-wide time span. The
        // empty state is what it shows instead.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                CaffeineCurveCard(
                    insights = CaffeineInsights(sleepThresholdMg = THRESHOLD_MG),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    onSelectEntry = {},
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("caffeine_curve_empty")
    }

    private companion object {
        /**
         * Tighter than the 0.5% default, which would let this pass without the
         * sleep-threshold guide line at all: the dashed line is ~236 of 122,400
         * pixels, well inside the default budget. A golden that tolerates more
         * than the feature it exists to photograph is not a golden.
         */
        const val SmallFeatureTolerance = 0.0005

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val CARD: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        /** Caffeine's half-life, which is what makes the curve a decay rather than a line. */
        const val HALF_LIFE_HOURS = 5.0

        /**
         * 50 mg is the default, and the day deliberately crosses it — a threshold line
         * that nothing ever touches proves nothing about where it was drawn.
         */
        const val THRESHOLD_MG = 50

        /**
         * A day's drinking, on the golden clock. Four doses, spaced so their decays
         * overlap — a curve of one dose is just an exponential, and would photograph
         * none of the interesting behaviour.
         */
        val DOSES = listOf(
            goldenInstantAt(7, 30) to 95.0,
            goldenInstantAt(10, 15) to 65.0,
            goldenInstantAt(14, 0) to 80.0,
            goldenInstantAt(17, 30) to 30.0,
        )

        fun decayed(mg: Double, from: Instant, to: Instant): Double {
            if (to.isBefore(from)) return 0.0
            val hours = (to.toEpochMilli() - from.toEpochMilli()) / 3_600_000.0
            return mg * 0.5.pow(hours / HALF_LIFE_HOURS)
        }

        /** Active mg at [time]: every dose already taken, each halved every five hours. */
        fun activeMg(time: Instant): Double =
            DOSES.sumOf { (at, mg) -> decayed(mg = mg, from = at, to = time) }

        fun entryInsight(id: String, at: Instant, mg: Double): CaffeineEntryInsight =
            CaffeineEntryInsight(
                entry = CaffeineEntry(
                    id = id,
                    startTime = at,
                    endTime = at,
                    caffeineMg = mg,
                    name = "Coffee",
                    source = "openvitals",
                    mealType = 0,
                    isOpenVitalsEntry = true,
                ),
                currentContributionMg = mg,
                peakTime = at,
                peakMg = mg,
                contributionPoints = emptyList(),
                inferredCategory = CaffeineSourceCategory.COFFEE,
            )
    }
}
