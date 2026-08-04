package tech.mmarca.openvitals.domain.model

/**
 * Health Connect recording-method values. Never compare to a literal — these
 * names are the contract.
 */
object RecordingMethod {
    const val UNKNOWN = 0
    const val ACTIVELY_RECORDED = 1
    const val AUTOMATICALLY_RECORDED = 2
    const val MANUAL_ENTRY = 3
}
