package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One morning temperature. [disturbed] readings are drawn but never used for the shift. */
data class BbtReading(
    val date: LocalDate,
    val celsius: Double,
    val disturbed: Boolean = false,
)

sealed interface ThermalShiftResult {
    data object None : ThermalShiftResult

    /** A sustained rise was found. It is retrospective, never a fertility prediction. */
    data class Confirmed(
        val coverlineCelsius: Double,
        val firstHighDay: LocalDate,
        val baselineLowTemps: List<Double>,
        val highTemps: List<Double>,
    ) : ThermalShiftResult
}

/**
 * The six-low, three-high rule: the coverline is the
 * highest of six undisturbed lows; three following readings must all sit above
 * it, the third by at least 0.2 °C. Readings must be at most three days apart,
 * and the sixth low and first high at most two.
 */
object ThermalShiftCalculator {
    private const val MIN_SHIFT_DELTA_CELSIUS = 0.20
    private const val LOWS = 6
    private const val HIGHS = 3
    private const val WINDOW = LOWS + HIGHS

    fun evaluateCycle(cycleStartDate: LocalDate, readings: List<BbtReading>): ThermalShiftResult {
        val valid = readings
            .filter { !it.date.isBefore(cycleStartDate) && !it.disturbed }
            .sortedBy { it.date }
        if (valid.size < WINDOW) return ThermalShiftResult.None

        for (index in 0..(valid.size - WINDOW)) {
            val transitionOk = daysBetween(valid[index + LOWS - 1], valid[index + LOWS]) <= 2
            if (!transitionOk) continue
            val continuous = (index until index + WINDOW - 1).all { i -> daysBetween(valid[i], valid[i + 1]) <= 3 }
            if (!continuous) continue

            val lows = valid.subList(index, index + LOWS).map { it.celsius }
            val highs = valid.subList(index + LOWS, index + WINDOW).map { it.celsius }
            val maxLow = lows.max()
            val allHigher = highs.all { it > maxLow }
            val thirdClearsDelta = highs[HIGHS - 1] >= maxLow + MIN_SHIFT_DELTA_CELSIUS
            if (allHigher && thirdClearsDelta) {
                return ThermalShiftResult.Confirmed(
                    coverlineCelsius = maxLow,
                    firstHighDay = valid[index + LOWS].date,
                    baselineLowTemps = lows,
                    highTemps = highs,
                )
            }
        }
        return ThermalShiftResult.None
    }

    private fun daysBetween(from: BbtReading, to: BbtReading): Long = ChronoUnit.DAYS.between(from.date, to.date)
}
