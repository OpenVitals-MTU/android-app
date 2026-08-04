package tech.mmarca.openvitals.features.readiness

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence

/**
 * The readiness confidence line.
 *
 * Dart counterpart: the `daily readiness panel` and `training readiness detail`
 * confidence cases of test/features/readiness/. Those were unportable while
 * the mapping lived as a private `@Composable` inside each screen — twice, and
 * built from English literals rather than resources.
 */
class ReadinessConfidenceTextTest {

    @Test
    fun `each confidence level names itself`() {
        assertThat(ReadinessConfidenceText.labelRes(ReadinessConfidence.HIGH))
            .isEqualTo(R.string.data_confidence_high)
        assertThat(ReadinessConfidenceText.labelRes(ReadinessConfidence.MEDIUM))
            .isEqualTo(R.string.data_confidence_medium)
        assertThat(ReadinessConfidenceText.labelRes(ReadinessConfidence.LOW))
            .isEqualTo(R.string.data_confidence_low)
    }

    @Test
    fun `each known reason has its own wording`() {
        val byReason = listOf(
            "complete_data" to R.string.readiness_confidence_reason_complete,
            "missing_sleep_data" to R.string.readiness_confidence_reason_missing_sleep,
            "missing_hrv_data" to R.string.readiness_confidence_reason_missing_hrv,
            "new_user_not_enough_baseline" to R.string.readiness_confidence_reason_baseline,
        )
        byReason.forEach { (reason, expected) ->
            assertThat(ReadinessConfidenceText.reasonRes(reason)).isEqualTo(expected)
        }
        // Distinct wording, not four aliases of one string.
        assertThat(byReason.map { it.second }.toSet()).hasSize(4)
    }

    @Test
    fun `an unrecognised confidence reason falls back to partial data`() {
        // The reason is a raw key from the insight, so a new one on the domain
        // side — or one read back from an older stored insight — must still
        // render a whole line rather than half of one.
        listOf("", "something_new", "MISSING_SLEEP_DATA", "missing_stress_data").forEach { reason ->
            assertThat(ReadinessConfidenceText.reasonRes(reason))
                .isEqualTo(R.string.readiness_confidence_reason_partial)
        }
    }
}
