package tech.mmarca.openvitals.domain.preferences

import tech.mmarca.openvitals.domain.insights.DefaultSplitDistanceMeters

/**
 * The split distance preference, stored in meters. Imperial presets are
 * exact mile fractions, so "1 mi" survives a switch to metric as 1.609 km.
 */
internal object ActivitySplitDistance {

    /** One kilometer, the default a runner expects. */
    const val defaultMeters: Double = DefaultSplitDistanceMeters

    const val minMeters: Double = 100.0
    const val maxMeters: Double = 50000.0

    private const val MetersPerMile = 1609.344

    /** Metric presets: 0.5 / 1 / 2 / 5 km. */
    val metricPresetMeters: List<Double> = listOf(
        500.0,
        1000.0,
        2000.0,
        5000.0,
    )

    /** Imperial presets: 0.25 / 0.5 / 1 / 5 mi, in meters. */
    val imperialPresetMeters: List<Double> = listOf(
        0.25 * MetersPerMile,
        0.5 * MetersPerMile,
        MetersPerMile,
        5 * MetersPerMile,
    )

    fun presetsFor(unitSystem: UnitSystem): List<Double> =
        when (unitSystem) {
            UnitSystem.METRIC -> metricPresetMeters
            UnitSystem.IMPERIAL -> imperialPresetMeters
        }

    fun normalize(meters: Double): Double {
        if (!meters.isFinite() || meters <= 0) return defaultMeters
        return meters.coerceIn(minMeters, maxMeters)
    }

    /** The preset closest to [meters], so the chips keep a selection after a unit switch. */
    fun nearestPreset(meters: Double, presets: List<Double>): Double {
        var best = presets.first()
        var bestDelta = kotlin.math.abs(presets.first() - meters)
        for (preset in presets.drop(1)) {
            val delta = kotlin.math.abs(preset - meters)
            if (delta < bestDelta) {
                best = preset
                bestDelta = delta
            }
        }
        return best
    }

    fun nearestPreset(meters: Double, unitSystem: UnitSystem): Double =
        nearestPreset(meters, presetsFor(unitSystem))
}
