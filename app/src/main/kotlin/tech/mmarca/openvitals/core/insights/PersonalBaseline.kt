package tech.mmarca.openvitals.core.insights

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val MinimumBaselineSamples = 3
private const val AnomalyZScoreThreshold = 2.0
private const val BaselineTolerance = 0.0001
val PersonalBaselineWindows = listOf(30, 60, 90)

data class BaselineValue(
    val date: LocalDate,
    val value: Double,
)

data class BaselineSummary(
    val windowDays: Int,
    val average: Double,
    val standardDeviation: Double,
    val sampleCount: Int,
) {
    val usualLow: Double get() = average - standardDeviation
    val usualHigh: Double get() = average + standardDeviation
}

enum class BaselineStatus {
    USUAL,
    ABOVE,
    BELOW,
    UNUSUAL_HIGH,
    UNUSUAL_LOW,
}

data class PersonalBaselineInsight(
    val currentValue: Double,
    val primarySummary: BaselineSummary,
    val summaries: List<BaselineSummary>,
    val status: BaselineStatus,
) {
    val deviation: Double get() = currentValue - primarySummary.average
    val absoluteDeviation: Double get() = abs(deviation)
    val percentDeviation: Double?
        get() = primarySummary.average.takeIf { abs(it) > BaselineTolerance }
            ?.let { deviation / it * 100.0 }
    val isAnomaly: Boolean
        get() = status == BaselineStatus.UNUSUAL_HIGH || status == BaselineStatus.UNUSUAL_LOW
}

fun personalBaselineInsight(
    currentValue: Double,
    values: List<BaselineValue>,
    referenceDate: LocalDate,
    windows: List<Int> = PersonalBaselineWindows,
): PersonalBaselineInsight? {
    val summaries = windows
        .sorted()
        .mapNotNull { windowDays ->
            baselineSummary(
                windowDays = windowDays,
                values = values,
                referenceDate = referenceDate,
            )
        }

    val primarySummary = summaries.lastOrNull() ?: return null
    val status = baselineStatus(currentValue, primarySummary)
    return PersonalBaselineInsight(
        currentValue = currentValue,
        primarySummary = primarySummary,
        summaries = summaries,
        status = status,
    )
}

private fun baselineSummary(
    windowDays: Int,
    values: List<BaselineValue>,
    referenceDate: LocalDate,
): BaselineSummary? {
    val start = referenceDate.minusDays(windowDays.toLong() - 1)
    val windowValues = values
        .asSequence()
        .filter { it.date in start..referenceDate }
        .map { it.value }
        .filter { it > 0.0 }
        .toList()

    if (windowValues.size < MinimumBaselineSamples) return null

    val average = windowValues.average()
    val standardDeviation = standardDeviation(windowValues, average)
    return BaselineSummary(
        windowDays = windowDays,
        average = average,
        standardDeviation = standardDeviation,
        sampleCount = windowValues.size,
    )
}

private fun baselineStatus(
    currentValue: Double,
    summary: BaselineSummary,
): BaselineStatus {
    val standardDeviation = summary.standardDeviation
    if (standardDeviation <= BaselineTolerance) {
        return when {
            currentValue > summary.average + BaselineTolerance -> BaselineStatus.ABOVE
            currentValue < summary.average - BaselineTolerance -> BaselineStatus.BELOW
            else -> BaselineStatus.USUAL
        }
    }

    val zScore = (currentValue - summary.average) / standardDeviation
    return when {
        zScore >= AnomalyZScoreThreshold -> BaselineStatus.UNUSUAL_HIGH
        zScore <= -AnomalyZScoreThreshold -> BaselineStatus.UNUSUAL_LOW
        currentValue > summary.usualHigh -> BaselineStatus.ABOVE
        currentValue < summary.usualLow -> BaselineStatus.BELOW
        else -> BaselineStatus.USUAL
    }
}

private fun standardDeviation(values: List<Double>, average: Double): Double {
    val variance = values.sumOf { (it - average).pow(2) } / values.size
    return sqrt(variance)
}
