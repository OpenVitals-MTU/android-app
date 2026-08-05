package tech.mmarca.openvitals.domain.preferences

/**
 * The user's step length for the opt-in "distance from steps" backfill.
 *
 * STORED IN METERS, always — storage is metric in this codebase and imperial
 * exists only at the display boundary (the settings card shows cm or inches).
 * The default is a typical adult walking stride; the bounds are generous
 * enough for short and tall walkers while rejecting values that would derive
 * absurd distances from a day of steps.
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
