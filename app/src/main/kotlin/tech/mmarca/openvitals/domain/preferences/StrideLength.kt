package tech.mmarca.openvitals.domain.preferences

/**
 * The user's step length for the "distance from steps" backfill: one step,
 * heel to heel, not a two-step stride. Stored in meters; shown in cm or
 * inches. Bounds reject absurd derived distances.
 *
 * Distance = steps × this length, a rough estimate. The rule of thumb,
 * 0.413 × height for women and 0.415 for men, has no primary source. Barreira et al. 2010
 * (doi:10.70252/ESYD9554) tested 42 % of height in 130 young adults: 0.71 m
 * predicted, 0.78 m measured, r = 0.46 per person. Step length grows with
 * pace, so a measured length beats any formula.
 */
internal object StrideLength {

    /** Default, no source: about 42 % of a 1.67 m adult, below Barreira's measured 0.78 m. */
    const val defaultMeters: Double = 0.7

    const val minMeters: Double = 0.3
    const val maxMeters: Double = 1.5

    fun normalize(meters: Double): Double {
        if (!meters.isFinite() || meters <= 0) return defaultMeters
        return meters.coerceIn(minMeters, maxMeters)
    }
}
