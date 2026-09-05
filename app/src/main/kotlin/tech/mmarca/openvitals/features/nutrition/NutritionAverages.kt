package tech.mmarca.openvitals.features.nutrition

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis

/**
 * The daily average of [values] over [period] on [basis]. [values] is
 * sparse, so EVERY_DAY divides by the period's elapsed days, up to [today].
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

/** How many of this period's days have happened as of [today]. */
internal fun DatePeriod.elapsedDays(today: LocalDate): Int {
    if (start.isAfter(today)) return 0
    val last = if (end.isAfter(today)) today else end
    return (ChronoUnit.DAYS.between(start, last) + 1).toInt()
}
