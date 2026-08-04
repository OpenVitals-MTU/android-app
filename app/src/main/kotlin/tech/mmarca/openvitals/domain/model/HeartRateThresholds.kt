package tech.mmarca.openvitals.domain.model

/**
 * Shared stepping and clamping rules for the high/low heart-rate alert thresholds.
 * Used by both the heart screen and the settings Recovery section so the two
 * surfaces can never disagree about what a valid threshold pair looks like.
 */
object HeartRateThresholds {
    const val STEP_BPM = 5

    /** The high threshold may never come within this many bpm of the low one. */
    const val MINIMUM_GAP_BPM = 5
}
