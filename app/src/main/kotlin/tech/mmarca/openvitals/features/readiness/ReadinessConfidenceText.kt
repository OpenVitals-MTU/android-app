package tech.mmarca.openvitals.features.readiness

import androidx.annotation.StringRes
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence

/**
 * The two halves of the readiness confidence line: how sure the score is, and
 * why.
 *
 * Split out of the screens because the daily panel and the details screen each
 * drew it from their own copy of the same function, and both copies built the
 * text from hardcoded English literals. Nothing reached `strings.xml`, so
 * nothing reached Weblate, so the line stayed English in all sixteen
 * translations.
 *
 * Keeping the decision here as resource ids leaves the composables doing
 * nothing but resolving them, and lets the fallback be tested off a device.
 */
object ReadinessConfidenceText {

    @StringRes
    fun labelRes(confidence: ReadinessConfidence): Int = when (confidence) {
        ReadinessConfidence.HIGH -> R.string.data_confidence_high
        ReadinessConfidence.MEDIUM -> R.string.data_confidence_medium
        ReadinessConfidence.LOW -> R.string.data_confidence_low
    }

    /**
     * Why the score carries the confidence it does.
     *
     * The reason arrives as a raw key from the insight rather than an enum, so
     * an unrecognised one is always reachable — a reason added on the domain
     * side, or one read back from an older stored insight. It reads as partial
     * data, because a confidence line with a missing half is worse than a
     * vague one.
     */
    @StringRes
    fun reasonRes(reason: String): Int = when (reason) {
        "complete_data" -> R.string.readiness_confidence_reason_complete
        "missing_sleep_data" -> R.string.readiness_confidence_reason_missing_sleep
        "missing_hrv_data" -> R.string.readiness_confidence_reason_missing_hrv
        "new_user_not_enough_baseline" -> R.string.readiness_confidence_reason_baseline
        else -> R.string.readiness_confidence_reason_partial
    }
}
