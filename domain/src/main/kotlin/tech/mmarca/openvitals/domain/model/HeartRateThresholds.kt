package tech.mmarca.openvitals.domain.model

/** Stepping and clamping rules for the alert thresholds, shared by both surfaces. */
object HeartRateThresholds {
    const val STEP_BPM = 5

    const val DEFAULT_HIGH_BPM = 120
    const val DEFAULT_LOW_BPM = 50

    /** A stored threshold is clamped to these on write. */
    const val MIN_HIGH_BPM = 80
    const val MAX_HIGH_BPM = 220
    const val MIN_LOW_BPM = 30
    const val MAX_LOW_BPM = 100

    /** The high threshold may never come within this many bpm of the low one. */
    const val MINIMUM_GAP_BPM = 5
}
