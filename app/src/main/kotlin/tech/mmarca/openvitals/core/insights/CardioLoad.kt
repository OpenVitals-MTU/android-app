package tech.mmarca.openvitals.core.insights

import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.HeartRateSample
import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.roundToInt

private const val MinimumTrimpMinutes = 5.0
private const val GoodHeartRateCoverageMinutes = 10.0
private const val GoodHeartRateCoverageRatio = 0.6
private const val MaxHeartRateSampleGapMinutes = 5.0
private const val ActiveHeartRateReserveThreshold = 0.3
private const val MinimumMovementFallbackLoad = 0.25
private const val ObservedMaxHeartRateMinimumBpm = 150L
private const val ObservedMaxHeartRateRestingDeltaBpm = 60L

enum class CardioLoadConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA,
}

enum class CardioLoadMethod {
    TRIMP_ACTIVITY_WINDOWS,
    TRIMP_ELEVATED_HEART_RATE,
    MOVEMENT_FALLBACK,
    NO_DATA,
}

data class CardioLoadEstimate(
    val score: Int = 0,
    val confidence: CardioLoadConfidence = CardioLoadConfidence.NO_DATA,
    val method: CardioLoadMethod = CardioLoadMethod.NO_DATA,
    val trimpScore: Double? = null,
    val coveredMinutes: Double = 0.0,
    val expectedMinutes: Double = 0.0,
    val restingHeartRateBpm: Long? = null,
    val restingHeartRateObserved: Boolean = false,
    val maxHeartRateBpm: Long? = null,
    val maxHeartRateObserved: Boolean = false,
    val heartRateSampleCount: Int = 0,
    val activityWindowCount: Int = 0,
    val activityWindowMinutes: Double = 0.0,
    val movementFallbackScore: Int = 0,
) {
    companion object {
        val NoData = CardioLoadEstimate()
    }
}

data class CardioLoadTimeWindow(
    val start: Instant,
    val end: Instant,
) {
    val durationMinutes: Double
        get() = Duration.between(start, end).seconds.coerceAtLeast(0L).toDouble() / 60.0
}

private data class MaxHeartRateContext(
    val bpm: Long,
    val isObservedAvailable: Boolean,
)

private data class TrimpResult(
    val score: Double,
    val coveredMinutes: Double,
    val expectedMinutes: Double,
) {
    val hasGoodCoverage: Boolean
        get() = coveredMinutes >= GoodHeartRateCoverageMinutes &&
            (expectedMinutes <= 0.0 || coveredMinutes / expectedMinutes >= GoodHeartRateCoverageRatio)
}

fun calculateCardioLoad(
    steps: DailySteps?,
    samples: List<HeartRateSample>,
    restingHeartRate: Long?,
    baselineRestingHeartRate: Long?,
    observedMaxHeartRate: Long?,
    activityWindows: List<CardioLoadTimeWindow>,
): CardioLoadEstimate {
    val fallback = movementFallbackCardioLoad(steps)
    val resting = restingHeartRate ?: baselineRestingHeartRate ?: samples.estimatedRestingHeartRate()
    val maxHeartRate = resting?.let { maxHeartRateContext(observedMaxHeartRate, samples, it) }
    val trimp = if (resting != null && maxHeartRate != null) {
        calculateTrimp(
            samples = samples,
            restingHeartRate = resting,
            maxHeartRate = maxHeartRate.bpm,
            activityWindows = activityWindows,
        )
    } else {
        null
    }

    val activityWindowMinutes = activityWindows.sumOf { it.durationMinutes }
    if (trimp != null && trimp.coveredMinutes >= MinimumTrimpMinutes && trimp.score > 0.0) {
        val confidence = when {
            trimp.hasGoodCoverage && restingHeartRate != null && maxHeartRate?.isObservedAvailable == true ->
                CardioLoadConfidence.HIGH
            trimp.hasGoodCoverage -> CardioLoadConfidence.MEDIUM
            else -> CardioLoadConfidence.LOW
        }
        return CardioLoadEstimate(
            score = trimp.score.roundToInt().coerceAtLeast(1),
            confidence = confidence,
            method = if (activityWindows.isNotEmpty()) {
                CardioLoadMethod.TRIMP_ACTIVITY_WINDOWS
            } else {
                CardioLoadMethod.TRIMP_ELEVATED_HEART_RATE
            },
            trimpScore = trimp.score,
            coveredMinutes = trimp.coveredMinutes,
            expectedMinutes = trimp.expectedMinutes,
            restingHeartRateBpm = resting,
            restingHeartRateObserved = restingHeartRate != null,
            maxHeartRateBpm = maxHeartRate?.bpm,
            maxHeartRateObserved = maxHeartRate?.isObservedAvailable == true,
            heartRateSampleCount = samples.size,
            activityWindowCount = activityWindows.size,
            activityWindowMinutes = activityWindowMinutes,
            movementFallbackScore = fallback,
        )
    }

    return when {
        fallback > 0 -> CardioLoadEstimate(
            score = fallback,
            confidence = CardioLoadConfidence.LOW,
            method = CardioLoadMethod.MOVEMENT_FALLBACK,
            restingHeartRateBpm = resting,
            restingHeartRateObserved = restingHeartRate != null,
            maxHeartRateBpm = maxHeartRate?.bpm,
            maxHeartRateObserved = maxHeartRate?.isObservedAvailable == true,
            heartRateSampleCount = samples.size,
            activityWindowCount = activityWindows.size,
            activityWindowMinutes = activityWindowMinutes,
            movementFallbackScore = fallback,
        )
        else -> CardioLoadEstimate.NoData
    }
}

private fun calculateTrimp(
    samples: List<HeartRateSample>,
    restingHeartRate: Long,
    maxHeartRate: Long,
    activityWindows: List<CardioLoadTimeWindow>,
): TrimpResult? {
    val sortedSamples = samples
        .sortedBy { it.time }
        .distinctBy { it.time }
    if (sortedSamples.size < 2 || maxHeartRate <= restingHeartRate) return null

    var score = 0.0
    var coveredMinutes = 0.0
    val expectedMinutes = activityWindows
        .sumOf { it.durationMinutes }
        .takeIf { activityWindows.isNotEmpty() }

    sortedSamples.zipWithNext().forEach { (start, end) ->
        val interval = CardioLoadTimeWindow(start.time, end.time)
        val rawMinutes = interval.durationMinutes
        if (rawMinutes <= 0.0 || rawMinutes > MaxHeartRateSampleGapMinutes) return@forEach

        val intervalMinutes = if (activityWindows.isNotEmpty()) {
            activityWindows.sumOf { interval.overlapMinutes(it) }
        } else {
            rawMinutes
        }
        if (intervalMinutes <= 0.0) return@forEach

        val averageBpm = (start.beatsPerMinute + end.beatsPerMinute) / 2.0
        val heartRateReserve = ((averageBpm - restingHeartRate) / (maxHeartRate - restingHeartRate).toDouble())
            .coerceIn(0.0, 1.0)
        if (activityWindows.isEmpty() && heartRateReserve < ActiveHeartRateReserveThreshold) {
            return@forEach
        }

        coveredMinutes += intervalMinutes
        score += intervalMinutes * heartRateReserve * 0.64 * exp(1.92 * heartRateReserve)
    }

    if (coveredMinutes <= 0.0) return null
    return TrimpResult(
        score = score,
        coveredMinutes = coveredMinutes,
        expectedMinutes = expectedMinutes ?: coveredMinutes,
    )
}

private fun movementFallbackCardioLoad(steps: DailySteps?): Int {
    steps ?: return 0
    val rawLoad = maxOf(
        steps.steps.toDouble() / 3_000.0,
        steps.distanceMeters / 1_500.0,
        steps.activeCaloriesKcal.orZero() / 75.0,
    )
    return if (rawLoad >= MinimumMovementFallbackLoad) {
        rawLoad.roundToInt().coerceAtLeast(1)
    } else {
        0
    }
}

private fun maxHeartRateContext(
    observedMaxHeartRate: Long?,
    samples: List<HeartRateSample>,
    restingHeartRate: Long,
): MaxHeartRateContext? {
    val sampleMax = samples.maxOfOrNull { it.beatsPerMinute }
    val observedMax = listOfNotNull(observedMaxHeartRate, sampleMax).maxOrNull() ?: return null
    val observedAvailable = observedMax >= maxOf(
        ObservedMaxHeartRateMinimumBpm,
        restingHeartRate + ObservedMaxHeartRateRestingDeltaBpm,
    )
    val estimatedMax = maxOf(
        observedMax + 10L,
        restingHeartRate + 70L,
    )
    return MaxHeartRateContext(
        bpm = if (observedAvailable) observedMax else estimatedMax,
        isObservedAvailable = observedAvailable,
    )
}

private fun List<HeartRateSample>.estimatedRestingHeartRate(): Long? {
    if (isEmpty()) return null
    val sorted = map { it.beatsPerMinute }.sorted()
    val index = (sorted.lastIndex * 0.1).roundToInt().coerceIn(sorted.indices)
    return sorted[index].coerceIn(40L, 100L)
}

private fun CardioLoadTimeWindow.overlapMinutes(other: CardioLoadTimeWindow): Double {
    val overlapStart = maxOf(start, other.start)
    val overlapEnd = minOf(end, other.end)
    if (!overlapEnd.isAfter(overlapStart)) return 0.0
    return Duration.between(overlapStart, overlapEnd).seconds.toDouble() / 60.0
}

private fun Double?.orZero(): Double = this ?: 0.0
