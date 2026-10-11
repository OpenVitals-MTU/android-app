package tech.mmarca.openvitals.domain.preferences

/**
 * The user's step length for the "distance from steps" backfill: one step,
 * heel to heel, not a two-step stride. Stored in meters; shown in cm or
 * inches. Bounds reject absurd derived distances.
 *
 * The distance is steps × this length, a rough estimate. The common rule of
 * thumb, step length ≈ 0.413–0.415 × height, has no primary paper behind it.
 * The one peer-reviewed check found, Barreira, Rowe and Kang, "Parameters of
 * walking and jogging in healthy young adults", Int J Exerc Sci
 * 2010;3(1):4–13, doi:10.70252/ESYD9554, tested 42 % of height on 130 young
 * adults at a moderate pace: it came out at 0.71 m against 0.78 m measured,
 * about 9 % short, and predicted the individual poorly (r = 0.46). Step
 * length also grows with pace, so a length the user measures beats any
 * formula.
 */
internal object StrideLength {

    /** A plain default, not from a study: about 42 % of a 1.67 m adult, the low side of Barreira's measured 0.78 m. */
    const val defaultMeters: Double = 0.7

    const val minMeters: Double = 0.3
    const val maxMeters: Double = 1.5

    fun normalize(meters: Double): Double {
        if (!meters.isFinite() || meters <= 0) return defaultMeters
        return meters.coerceIn(minMeters, maxMeters)
    }
}
