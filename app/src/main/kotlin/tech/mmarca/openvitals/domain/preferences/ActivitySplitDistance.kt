package tech.mmarca.openvitals.domain.preferences

import tech.mmarca.openvitals.domain.insights.DefaultSplitDistanceMeters

/**
 * The split distance preference: how far apart the activity detail screen cuts
 * derived splits ("every 1 km").
 *
 * STORED IN METERS, always — storage is metric in this codebase and imperial
 * exists only at the display boundary. The imperial presets below are exact
 * mile fractions converted to meters on the way in, so a user who picks
 * "1 mi" and later switches to metric sees 1.609 km worth of splits, not a
 * silently rounded 1600 m.
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

    /**
     * The preset closest to [meters], so the settings chips still show a
     * selection after the user switches unit systems (a stored 1000 m has no
     * exact imperial preset; the honest thing is to highlight the nearest one
     * rather than show nothing selected).
     */
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
