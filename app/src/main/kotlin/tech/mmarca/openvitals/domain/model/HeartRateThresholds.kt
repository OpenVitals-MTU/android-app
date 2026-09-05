package tech.mmarca.openvitals.domain.model

/** Stepping and clamping rules for the alert thresholds, shared by both surfaces. */
object HeartRateThresholds {
    const val STEP_BPM = 5

    /** The high threshold may never come within this many bpm of the low one. */
    const val MINIMUM_GAP_BPM = 5
}
