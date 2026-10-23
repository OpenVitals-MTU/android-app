package tech.mmarca.openvitals.domain.preferences

/** How the intraday vitals charts summarise their data. */
enum class ChartAggregationMode {
    OFF,
    MIN5,
    MIN10,
    MIN30,
    ;

    /** Bucket width in minutes, or null when [OFF] (raw data). */
    val bucketMinutes: Int?
        get() = when (this) {
            OFF -> null
            MIN5 -> 5
            MIN10 -> 10
            MIN30 -> 30
        }
}
