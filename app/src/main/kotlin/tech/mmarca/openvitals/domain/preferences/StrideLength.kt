package tech.mmarca.openvitals.domain.preferences

/**
 * The user's step length for the "distance from steps" backfill. Stored in
 * meters; shown in cm or inches. Bounds reject absurd derived distances.
 */
internal object StrideLength {

    const val defaultMeters: Double = 0.7

    const val minMeters: Double = 0.3
    const val maxMeters: Double = 1.5

    fun normalize(meters: Double): Double {
        if (!meters.isFinite() || meters <= 0) return defaultMeters
        return meters.coerceIn(minMeters, maxMeters)
    }
}
