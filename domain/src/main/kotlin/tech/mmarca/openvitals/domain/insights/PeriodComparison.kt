package tech.mmarca.openvitals.domain.insights

import kotlin.math.abs

private const val ComparisonTolerance = 0.0001

enum class PeriodComparisonDirection {
    UP,
    DOWN,
    SAME,
}

data class PeriodComparison(
    val currentValue: Double,
    val previousValue: Double,
) {
    val change: Double get() = currentValue - previousValue
    val absoluteChange: Double get() = abs(change)
    val percentChange: Double?
        get() = previousValue.takeIf { abs(it) > ComparisonTolerance }
            ?.let { change / it * 100.0 }
    val direction: PeriodComparisonDirection
        get() = when {
            change > ComparisonTolerance -> PeriodComparisonDirection.UP
            change < -ComparisonTolerance -> PeriodComparisonDirection.DOWN
            else -> PeriodComparisonDirection.SAME
        }
}

fun periodComparison(
    currentValue: Double,
    previousValue: Double,
): PeriodComparison =
    PeriodComparison(
        currentValue = currentValue,
        previousValue = previousValue,
    )
