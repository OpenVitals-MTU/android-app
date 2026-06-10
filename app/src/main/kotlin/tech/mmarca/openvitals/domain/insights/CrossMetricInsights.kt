package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val MinimumCrossMetricPairs = 3
private const val CorrelationTolerance = 0.0001
private const val ModerateCorrelationThreshold = 0.35
private const val StrongCorrelationThreshold = 0.7

data class CrossMetricValue(
    val date: LocalDate,
    val value: Double,
)

enum class CrossMetricDirection {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

enum class CrossMetricStrength {
    WEAK,
    MODERATE,
    STRONG,
}

data class CrossMetricInsight(
    val correlation: Double,
    val pairedDays: Int,
) {
    val direction: CrossMetricDirection
        get() = when {
            correlation > CorrelationTolerance -> CrossMetricDirection.POSITIVE
            correlation < -CorrelationTolerance -> CrossMetricDirection.NEGATIVE
            else -> CrossMetricDirection.NEUTRAL
        }

    val strength: CrossMetricStrength
        get() = when {
            abs(correlation) >= StrongCorrelationThreshold -> CrossMetricStrength.STRONG
            abs(correlation) >= ModerateCorrelationThreshold -> CrossMetricStrength.MODERATE
            else -> CrossMetricStrength.WEAK
        }
}

fun crossMetricInsight(
    primaryValues: List<CrossMetricValue>,
    secondaryValues: List<CrossMetricValue>,
): CrossMetricInsight? {
    val primaryByDate = primaryValues
        .filter { it.value > 0.0 }
        .associateBy { it.date }
    val pairs = secondaryValues
        .filter { it.value > 0.0 }
        .mapNotNull { secondary ->
            primaryByDate[secondary.date]?.let { primary -> primary.value to secondary.value }
        }

    if (pairs.size < MinimumCrossMetricPairs) return null

    val primaryAverage = pairs.map { it.first }.average()
    val secondaryAverage = pairs.map { it.second }.average()
    val primaryVariance = pairs.sumOf { (it.first - primaryAverage).pow(2) }
    val secondaryVariance = pairs.sumOf { (it.second - secondaryAverage).pow(2) }
    if (primaryVariance <= CorrelationTolerance || secondaryVariance <= CorrelationTolerance) {
        return CrossMetricInsight(correlation = 0.0, pairedDays = pairs.size)
    }

    val covariance = pairs.sumOf { (it.first - primaryAverage) * (it.second - secondaryAverage) }
    val correlation = covariance / sqrt(primaryVariance * secondaryVariance)
    return CrossMetricInsight(
        correlation = correlation.coerceIn(-1.0, 1.0),
        pairedDays = pairs.size,
    )
}
