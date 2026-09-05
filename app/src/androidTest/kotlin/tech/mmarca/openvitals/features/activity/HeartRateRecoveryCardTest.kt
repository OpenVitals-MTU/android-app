package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryIssue
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryMark
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryQuality
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * A wellness estimate presented as a number about the user's heart, so the unsure path matters:
 * a missing mark reads as "not measured", and a submaximal effort says why it is not comparable.
 */
class HeartRateRecoveryCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheFallAfterAGuidedTest() {
        setCard(
            reading(
                quality = HeartRateRecoveryQuality.CLEAN,
                marks = listOf(mark(Duration.ofMinutes(1), heartRate = 132, drop = 38)),
            ),
        )

        composeRule.onNodeWithText(string(R.string.heart_rate_recovery_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.heart_rate_recovery_headline, 38L))
            .assertIsDisplayed()
    }

    @Test
    fun aWatchThatStoppedRecordingSaysSoRatherThanShowingNumbers() {
        // No samples after the stop is not a recovery of zero.
        setCard(
            reading(
                quality = HeartRateRecoveryQuality.NO_DATA,
                marks = emptyList(),
                issues = setOf(HeartRateRecoveryIssue.NO_RECOVERY_SAMPLES),
            ),
        )

        composeRule
            .onNodeWithText(string(R.string.heart_rate_recovery_headline_unavailable))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.heart_rate_recovery_no_recovery_samples))
            .assertIsDisplayed()
    }

    @Test
    fun aSubmaximalGuidedTestStillShowsTheCardFlagged() {
        // The drop is real but not comparable. Hiding it loses a measurement; showing it unflagged misleads.
        setCard(
            reading(
                quality = HeartRateRecoveryQuality.NOT_COMPARABLE,
                marks = listOf(mark(Duration.ofMinutes(1), heartRate = 120, drop = 22)),
                issues = setOf(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT),
            ),
        )

        composeRule
            .onNodeWithText(string(R.string.heart_rate_recovery_headline, 22L))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.heart_rate_recovery_submaximal_effort))
            .assertIsDisplayed()
    }

    @Test
    fun withNoMaximumKnowableTheCardAppearsAndAsksForOne() {
        setCard(
            reading(
                quality = HeartRateRecoveryQuality.APPROXIMATE,
                marks = listOf(mark(Duration.ofMinutes(1), heartRate = 130, drop = 30)),
                issues = setOf(HeartRateRecoveryIssue.UNKNOWN_MAX_HEART_RATE),
            ),
        )

        composeRule.onNodeWithText(string(R.string.heart_rate_recovery_title)).assertIsDisplayed()
    }

    private fun reading(
        quality: HeartRateRecoveryQuality,
        marks: List<HeartRateRecoveryMark>,
        issues: Set<HeartRateRecoveryIssue> = emptySet(),
    ) = HeartRateRecoveryReading(
        recoveryStart = STOP,
        peakBpm = 170,
        peakTime = STOP,
        marks = marks,
        quality = quality,
        issues = issues,
        recoverySampleCount = marks.size,
    )

    private fun mark(offset: Duration, heartRate: Long, drop: Long) = HeartRateRecoveryMark(
        offset = offset,
        heartRateBpm = heartRate,
        dropBpm = drop,
        sampleTime = STOP.plus(offset),
        // Exactly on the mark: the skew label is a separate concern.
        sampleSkew = Duration.ZERO,
    )

    private fun setCard(reading: HeartRateRecoveryReading) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HeartRateRecoveryCard(reading = reading)
                }
            }
        }
    }

    private companion object {
        val STOP: Instant = Instant.parse("2026-06-23T08:45:00Z")
    }
}
