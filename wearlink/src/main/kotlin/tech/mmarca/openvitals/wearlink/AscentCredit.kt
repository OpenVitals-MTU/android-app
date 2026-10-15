package tech.mmarca.openvitals.wearlink

/**
 * How much of a minute's climb counts, the same on the watch and the phone.
 *
 * The barometer counts every rise, an elevator ride and a drift of the
 * weather too. Height counts only with at least [MIN_STEPS_PER_FLOOR] steps
 * per floor's height in that minute and the one before; stairs take about 16
 * per floor. The threshold is an app heuristic, no published source.
 */
object AscentCredit {

    /** One floor: 3 m, about 10 ft, the common definition wearables use. */
    const val METRES_PER_FLOOR: Double = 3.0

    const val MIN_STEPS_PER_FLOOR: Int = 10

    /** The metres of [climbedMetres] that [steps] (this minute and the one before) carry. */
    fun creditedMetres(climbedMetres: Double, steps: Int): Double =
        minOf(climbedMetres, steps * METRES_PER_FLOOR / MIN_STEPS_PER_FLOOR).coerceAtLeast(0.0)
}
