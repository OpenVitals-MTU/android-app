package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenInstantAt

/**
 * Port of Flutter's `test/goldens/charts/metric_session_chart_golden_test.dart`.
 *
 * The trace card the recorded session screens wear. The one thing it decides for
 * itself is whether to draw the DOTS: past 120 samples they merge into a smear and
 * are dropped. Both sides of that threshold are photographed here, because a sparse
 * trace losing its points and a dense trace growing them are both regressions no
 * existing test would notice.
 *
 * Kotlin has no single `MetricSessionChart`; the shared scaffold is private and each
 * metric has its own typed entry point, so these shoot the three entry points Flutter
 * shot through the one widget.
 */
class MetricSessionChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aRecordedTrace_denseEnoughThatTheDotsComeOff() {
        // One sample every 15 seconds for 45 minutes: 180 of them, which is what a
        // sensor actually writes and comfortably past the 120 the card will dot.
        // Rolling terrain, and no noise, so the picture changes only when the PAINTER
        // changes.
        val samples = (0 until 180).map { index ->
            SpeedSample(
                time = START.plusSeconds(index * 15L),
                metersPerSecond = 7.2 + 2.4 * sin(index / 14.0) + 0.8 * sin(index / 3.5),
                source = "golden",
            )
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 420.dp) {
                ActivitySpeedChartCard(
                    samples = samples,
                    sessionStart = START,
                    sessionEnd = END,
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_session_chart_dense")
    }

    @Test
    fun aTraceSteppedPerSplit_sparseEnoughToShowItsPoints() {
        // The watch that writes a distance and no speed: the shape of the run is
        // rebuilt from the splits, so the trace STEPS — two samples per split, at its
        // ends — and the card counts splits rather than samples.
        val splitSpeeds = listOf(3.05, 3.18, 3.11, 2.86, 3.22, 3.30)
        val samples = splitSpeeds.flatMapIndexed { index, speed ->
            listOf(
                ActivitySpeedTraceSample(START.plusSeconds(index * 7L * 60L), speed),
                ActivitySpeedTraceSample(START.plusSeconds((index + 1) * 7L * 60L), speed),
            )
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 420.dp) {
                ActivitySplitSpeedChartCard(
                    trace = ActivitySplitSpeedTrace(
                        samples = samples,
                        splitCount = splitSpeeds.size,
                        // The card's own mean would weigh the corners of the steps
                        // equally with the splits; only the caller knows the
                        // distances, so only the caller can state the average.
                        averageMetersPerSecond = 3.12,
                    ),
                    source = SplitSource.ROUTE,
                    splitDistanceMeters = 1_000.0,
                    sessionStart = START,
                    sessionEnd = END,
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_session_chart_splits")
    }

    @Test
    fun cadence_theOtherCardThatSharesThisScaffold() {
        // Cycling cadence over the same ride, once a minute — the second caller, and
        // the one whose only difference from speed used to be a whole duplicate file.
        val samples = (0..45).map { index ->
            ActivityCadenceSample(
                time = START.plusSeconds(index * 60L),
                rate = 84.0 + 8.0 * sin(index / 6.0),
                kind = ActivityCadenceKind.CYCLING,
                source = "golden",
            )
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 420.dp) {
                ActivityCadenceChartCard(
                    samples = samples,
                    kind = ActivityCadenceKind.CYCLING,
                    sessionStart = START,
                    sessionEnd = END,
                    unitFormatter = FORMATTER,
                    modifier = CARD,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_session_chart_cadence")
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val CARD: Modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        // A 45-minute ride, on the golden clock. Sessions are instants, not days: the
        // axis spans the RECORDING, so a trace that stops early stops early.
        val START: Instant = goldenInstantAt(9, 0)
        val END: Instant = START.plusSeconds(45L * 60L)
    }
}
