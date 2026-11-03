package tech.mmarca.openvitals.wearlink

/**
 * What the phone tells the watch after a sync, so the watch's tiles show
 * what the phone app shows: the settings that turn steps into distance and
 * walking energy, the daily goals, and Health Connect's totals for today.
 * Sent only to a watch that announces [WearLinkProtocol.CAP_PROFILE]; each
 * request is answered `END 0 0`.
 *
 * - `PROFILE <strideMm> <weightG|-> <M|I> <goalSteps> <goalDistanceM> <goalActiveKcal>`:
 *   the step length (heel to heel) in millimetres, the body weight in grams
 *   or `-` when the phone knows none, metric or imperial units, and the
 *   goals for steps, distance in metres and active kilocalories.
 * - `TOTALS <dayStartEpochMillis> <asOfEpochMillis> <steps|-> <distanceM|-> <activeKcal|->`:
 *   Health Connect's totals for the day from the local midnight at
 *   `dayStartEpochMillis`, every source together, `-` where the phone may not
 *   read one. `asOfEpochMillis` is the start of the first of the watch's own
 *   minutes they do not hold: the minute after the newest the phone wrote,
 *   or the midnight when it wrote none that day. The watch adds its minutes
 *   from there. The watch keeps them for that day only.
 *
 * Integers only, as the other lines; a line out of range is refused whole.
 */
object WearLinkProfile {

    enum class Units(val code: String) {
        METRIC("M"),
        IMPERIAL("I"),
        ;

        companion object {
            fun fromCode(code: String): Units? = entries.firstOrNull { it.code == code }
        }
    }

    data class Profile(
        val strideMillimetres: Int,
        val weightGrams: Int?,
        val units: Units,
        val goalSteps: Int,
        val goalDistanceMetres: Int,
        val goalActiveKcal: Int,
    )

    data class Totals(
        val dayStartEpochMillis: Long,
        val asOfEpochMillis: Long,
        val steps: Long?,
        val distanceMetres: Long?,
        val activeKcal: Long?,
    )

    fun formatProfile(profile: Profile): String = with(profile) {
        "${WearLinkProtocol.PROFILE} $strideMillimetres ${weightGrams ?: NO_VALUE} ${units.code} " +
            "$goalSteps $goalDistanceMetres $goalActiveKcal"
    }

    /** Null unless [line] is a well-formed `PROFILE` line with plausible values. */
    fun parseProfile(line: String): Profile? {
        val f = fields(line)
        if (f.size != PROFILE_FIELDS || f[0] != WearLinkProtocol.PROFILE) return null
        val stride = f[1].toIntOrNull()?.takeIf { it in STRIDE_MM } ?: return null
        val weight = if (f[2] == NO_VALUE) null else f[2].toIntOrNull()?.takeIf { it in WEIGHT_G } ?: return null
        val units = Units.fromCode(f[3]) ?: return null
        val goalSteps = f[4].toIntOrNull()?.takeIf { it in GOAL_STEPS } ?: return null
        val goalDistance = f[5].toIntOrNull()?.takeIf { it in GOAL_DISTANCE_M } ?: return null
        val goalKcal = f[6].toIntOrNull()?.takeIf { it in GOAL_ACTIVE_KCAL } ?: return null
        return Profile(stride, weight, units, goalSteps, goalDistance, goalKcal)
    }

    fun formatTotals(totals: Totals): String = with(totals) {
        "${WearLinkProtocol.TOTALS} $dayStartEpochMillis $asOfEpochMillis ${steps ?: NO_VALUE} " +
            "${distanceMetres ?: NO_VALUE} ${activeKcal ?: NO_VALUE}"
    }

    /** Null unless [line] is a well-formed `TOTALS` line: a day start before its time, counts that are none or not negative. */
    fun parseTotals(line: String): Totals? {
        val f = fields(line)
        if (f.size != TOTALS_FIELDS || f[0] != WearLinkProtocol.TOTALS) return null
        val dayStart = f[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val asOf = f[2].toLongOrNull()?.takeIf { it >= dayStart } ?: return null
        val steps = count(f[3]) ?: return null
        val distance = count(f[4]) ?: return null
        val kcal = count(f[5]) ?: return null
        return Totals(dayStart, asOf, steps.value, distance.value, kcal.value)
    }

    /** A count that is not negative, or `-`. Null for anything else, so the caller can refuse the line. */
    private class Count(val value: Long?)

    private fun count(field: String): Count? =
        if (field == NO_VALUE) Count(null) else field.toLongOrNull()?.takeIf { it >= 0 }?.let(::Count)

    private fun fields(line: String): List<String> = line.trim().split(' ').filter { it.isNotEmpty() }

    private const val NO_VALUE = "-"
    private const val PROFILE_FIELDS = 7
    private const val TOTALS_FIELDS = 6

    /** The phone's own bounds (`StrideLength`): 0.3 to 1.5 m. */
    private val STRIDE_MM = 300..1500

    /** 20 to 400 kg: anything else is a typo, not a body. */
    private val WEIGHT_G = 20_000..400_000

    /** Wider than the phone's goal pickers (`MetricDailyGoalKey`), so a later range change does not break the link. */
    private val GOAL_STEPS = 1..1_000_000
    private val GOAL_DISTANCE_M = 1..1_000_000
    private val GOAL_ACTIVE_KCAL = 1..100_000
}
