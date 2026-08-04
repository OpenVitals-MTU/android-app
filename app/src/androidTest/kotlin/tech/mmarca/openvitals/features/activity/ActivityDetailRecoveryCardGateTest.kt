package tech.mmarca.openvitals.features.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryMark
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryQuality
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports `ActivityHeartRateRecoveryCard > an ordinary workout shows no card at
 * all` from Flutter's `test/features/activity/activity_heart_rate_recovery_test.dart`.
 *
 * `HeartRateRecoveryCardTest` covers what the card says once it is on screen.
 * This covers the decision one level up — whether it is on screen at all — and
 * that decision is the one with the sharper edge. A recovery figure carries an
 * implicit claim that a recovery was measured; printed over an ordinary ride
 * that never stopped for one, it is a number about the user's heart that came
 * from nowhere. The gate is `heartRateRecovery != null && peakBpm != null`, so
 * both halves are exercised here.
 *
 * The card lives in a `LazyColumn`, where a plain "does not exist" would pass
 * just as happily for a card that is merely below the fold. So absence is
 * asserted by scrolling for it and requiring the scroll to fail, against a
 * positive control that scrolls to it and finds it.
 */
class ActivityDetailRecoveryCardGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aGuidedRecoveryTestGetsItsCard() {
        // The control: the same screen, the same scroll, a reading with a
        // measured peak — so a failure below means "no card", not "no scroll".
        setDetail(
            HeartRateRecoveryReading(
                recoveryStart = STOP,
                peakBpm = 170,
                peakTime = STOP,
                marks = listOf(
                    HeartRateRecoveryMark(
                        offset = Duration.ofMinutes(1),
                        heartRateBpm = 132,
                        dropBpm = 38,
                        sampleTime = STOP.plusSeconds(60),
                        sampleSkew = Duration.ZERO,
                    ),
                ),
                quality = HeartRateRecoveryQuality.CLEAN,
                recoverySampleCount = 1,
            ),
        )

        composeRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(TITLE))
        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun anOrdinaryWorkoutShowsNoCardAtAll() {
        setDetail(recovery = null)

        assertNoRecoveryCard()
    }

    @Test
    fun aRecoveryWindowWithNoMeasuredPeakShowsNoCardEither() {
        // A window with nothing in it is not a recovery of zero. The card would
        // open on a blank where the peak belongs, which reads as a bug rather
        // than as "this was not measured".
        setDetail(HeartRateRecoveryReading.NoData)

        assertNoRecoveryCard()
    }

    private fun assertNoRecoveryCard() {
        val found = runCatching {
            composeRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(TITLE))
        }
        assertTrue(
            "scrolling the whole detail screen must never reach a recovery card",
            found.isFailure,
        )
    }

    private fun setDetail(recovery: HeartRateRecoveryReading?) {
        composeRule.setContent {
            OpenVitalsTheme { DetailContent(recovery) }
        }
    }

    @Composable
    private fun DetailContent(recovery: HeartRateRecoveryReading?) {
        ActivityDetailContent(
            workout = WORKOUT,
            heartRateSamples = emptyList(),
            heartRateRecovery = recovery,
            speedSamples = emptyList(),
            cadenceSamples = emptyList(),
            markers = emptyList(),
            splits = ActivitySplits.none(),
            splitDistanceMeters = 1_000.0,
            slowestSplitPaceSeconds = null,
            fastestSplitPaceSeconds = null,
            splitSpeedTrace = null,
            elevationSamples = emptyList(),
            isDeleting = false,
            unitFormatter = testUnitFormatter(),
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            onEditActivity = {},
            onDeleteActivity = {},
            onOpenRouteInMap = {},
            onSaveRouteAsGpx = {},
            onSaveRouteAsKmz = {},
            onShareRouteAsGpx = {},
            onShareRouteAsKmz = {},
        )
    }

    private companion object {
        val TITLE: String = string(R.string.heart_rate_recovery_title)

        /** A fixed past morning, so nothing here depends on when the suite runs. */
        val START: Instant = Instant.parse("2026-06-23T08:00:00Z")
        val STOP: Instant = Instant.parse("2026-06-23T08:45:00Z")

        val WORKOUT = ExerciseData(
            id = "workout-1",
            title = "Morning ride",
            exerciseType = 8,
            startTime = START,
            endTime = STOP,
            durationMs = Duration.between(START, STOP).toMillis(),
            source = "tech.mmarca.openvitals",
            totalDistanceMeters = 21_400.0,
            averageHeartRateBpm = 141,
        )
    }
}
