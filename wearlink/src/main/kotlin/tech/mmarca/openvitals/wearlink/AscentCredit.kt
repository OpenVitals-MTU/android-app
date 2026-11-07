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

    /**
     * Per minute start (epoch milliseconds), the metres of that minute's climb
     * that count: [creditedMetres] of the minute's climb and the steps of that
     * minute and the one before. A minute listed twice keeps its larger climb
     * and its larger step count. Minutes that credit nothing are left out.
     * The phone writes these as floors and elevation; the watch's floors tile
     * shows them, so both count alike.
     */
    fun creditedPerMinute(climbedMetres: List<Pair<Long, Double>>, steps: List<Pair<Long, Int>>): List<Pair<Long, Double>> {
        val stepsAt = HashMap<Long, Int>()
        for ((start, count) in steps) stepsAt.merge(start, count, ::maxOf)
        return climbedMetres.groupBy { it.first }.mapNotNull { (start, same) ->
            val climbed = same.maxOf { it.second }
            val walked = (stepsAt[start - MINUTE_MILLIS] ?: 0) + (stepsAt[start] ?: 0)
            val credited = creditedMetres(climbed, walked)
            if (credited > 0.0) start to credited else null
        }
    }

    private const val MINUTE_MILLIS: Long = 60_000L
}
