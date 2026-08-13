package tech.mmarca.openvitals.features.nutrition

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis

/**
 * The daily average of [values] over [period] on the given [basis].
 *
 * [values] is one figure per day the period carried data for, and it is SPARSE
 * — Health Connect returns no bucket for a day with no records — so the divisor
 * for [NutritionAverageBasis.EVERY_DAY] comes from the period, never from the
 * list's size.
 *
 * A period still running counts only the days that have happened: dividing this
 * month's food by 31 on the 13th would report a third of what the eater ate.
 * [today] is the boundary, and a period entirely in the future has no average
 * to give.
 */
fun nutritionDailyAverage(
    values: List<Double>,
    period: DatePeriod,
    basis: NutritionAverageBasis,
    today: LocalDate = LocalDate.now(),
): Double {
    val total = values.sum()
    if (total <= 0.0) return 0.0
    val divisor = when (basis) {
        NutritionAverageBasis.LOGGED_DAYS -> values.count { it > 0.0 }
        NutritionAverageBasis.EVERY_DAY -> period.elapsedDays(today)
    }
    return if (divisor > 0) total / divisor else 0.0
}

/**
 * How many of this period's days have happened, as of [today].
 *
 * Capped at the period's own end so a past period counts all of itself, and
 * floored at nothing for a period that has not started.
 */
internal fun DatePeriod.elapsedDays(today: LocalDate): Int {
    if (start.isAfter(today)) return 0
    val last = if (end.isAfter(today)) today else end
    return (ChronoUnit.DAYS.between(start, last) + 1).toInt()
}
