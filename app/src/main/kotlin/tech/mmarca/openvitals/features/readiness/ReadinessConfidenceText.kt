package tech.mmarca.openvitals.features.readiness

import androidx.annotation.StringRes
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence

/**
 * The two halves of the readiness confidence line, as resource ids, so the
 * text reaches the string catalog and the fallback is testable.
 */
object ReadinessConfidenceText {

    @StringRes
    fun labelRes(confidence: ReadinessConfidence): Int = when (confidence) {
        ReadinessConfidence.HIGH -> R.string.data_confidence_high
        ReadinessConfidence.MEDIUM -> R.string.data_confidence_medium
        ReadinessConfidence.LOW -> R.string.data_confidence_low
    }

    /** Why the score carries its confidence. An unrecognised key reads as partial data. */
    @StringRes
    fun reasonRes(reason: String): Int = when (reason) {
        "complete_data" -> R.string.readiness_confidence_reason_complete
        "missing_sleep_data" -> R.string.readiness_confidence_reason_missing_sleep
        "missing_hrv_data" -> R.string.readiness_confidence_reason_missing_hrv
        "new_user_not_enough_baseline" -> R.string.readiness_confidence_reason_baseline
        else -> R.string.readiness_confidence_reason_partial
    }
}
