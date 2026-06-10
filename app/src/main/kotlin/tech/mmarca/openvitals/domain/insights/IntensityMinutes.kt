package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.HeartRateSample

const val DefaultWeeklyIntensityMinutesTarget = 150

private const val ModerateHeartRateReserveThreshold = 0.40
private const val VigorousHeartRateReserveThreshold = 0.60
private const val GoodHeartRateCoverageMinutes = 10.0
private const val GoodHeartRateCoverageRatio = 0.60
private const val MaxHeartRateSampleGapMinutes = 5.0
private const val VigorousKcalPerMinute = 8.0
private const val ModerateKcalPerMinute = 3.0
private const val DailyActiveCaloriesModerateKcalPerMinute = 5.0
private const val CardioLoadToModerateEquivalentMinutes = 4.0
private const val ObservedMaxHeartRateMinimumBpm = 150L
private const val ObservedMaxHeartRateRestingDeltaBpm = 60L

enum class IntensityMinutesConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA,
}

enum class IntensityMinutesMethod {
    HEART_RATE_RESERVE,
    WORKOUT_ACTIVE_CALORIES,
    WORKOUT_DURATION,
    DAILY_ACTIVE_CALORIES,
    CARDIO_LOAD,
    NO_DATA,
}

data class IntensityWorkoutInput(
    val durationMinutes: Double,
    val activeCaloriesKcal: Double? = null,
)

data class IntensityMinutesEstimate(
    val moderateMinutes: Int = 0,
    val vigorousMinutes: Int = 0,
    val moderateEquivalentMinutes: Int = 0,
    val confidence: IntensityMinutesConfidence = IntensityMinutesConfidence.NO_DATA,
    val method: IntensityMinutesMethod = IntensityMinutesMethod.NO_DATA,
    val coveredHeartRateMinutes: Double = 0.0,
    val expectedHeartRateMinutes: Double = 0.0,
    val heartRateSampleCount: Int = 0,
) {
    companion object {
        val NoData = IntensityMinutesEstimate()
    }
}

private data class IntensityMaxHeartRateContext(
    val bpm: Long,
    val isObservedAvailable: Boolean,
)

private data class IntensityMinuteAccumulator(
    val moderateMinutes: Double,
    val vigorousMinutes: Double,
    val coveredHeartRateMinutes: Double,
    val expectedHeartRateMinutes: Double,
) {
    val moderateEquivalentMinutes: Double
        get() = moderateMinutes + (vigorousMinutes * 2.0)

    val hasGoodCoverage: Boolean
        get() = coveredHeartRateMinutes >= GoodHeartRateCoverageMinutes &&
            (expectedHeartRateMinutes <= 0.0 ||
                coveredHeartRateMinutes / expectedHeartRateMinutes >= GoodHeartRateCoverageRatio)
}

fun calculateIntensityMinutes(
    samples: List<HeartRateSample>,
    restingHeartRate: Long?,
    baselineRestingHeartRate: Long?,
    observedMaxHeartRate: Long?,
    activityWindows: List<CardioLoadTimeWindow>,
    workouts: List<IntensityWorkoutInput>,
    dailyActiveCaloriesKcal: Double?,
    cardioLoadScore: Int?,
): IntensityMinutesEstimate {
    val resting = restingHeartRate ?: baselineRestingHeartRate ?: samples.estimatedRestingHeartRate()
    val maxHeartRate = resting?.let { maxHeartRateContext(observedMaxHeartRate, samples, it) }
    if (resting != null && maxHeartRate != null) {
        val heartRateEstimate = calculateHeartRateReserveIntensity(
            samples = samples,
            restingHeartRate = resting,
            maxHeartRate = maxHeartRate.bpm,
            maxHeartRateObserved = maxHeartRate.isObservedAvailable,
            restingHeartRateObserved = restingHeartRate != null,
            activityWindows = activityWindows,
        )
        if (heartRateEstimate != null) return heartRateEstimate
    }

    workoutFallbackIntensity(workouts)?.let { return it }
    dailyActiveCaloriesFallbackIntensity(dailyActiveCaloriesKcal)?.let { return it }
    cardioLoadFallbackIntensity(cardioLoadScore)?.let { return it }
    return IntensityMinutesEstimate.NoData
}

private fun calculateHeartRateReserveIntensity(
    samples: List<HeartRateSample>,
    restingHeartRate: Long,
    maxHeartRate: Long,
    maxHeartRateObserved: Boolean,
    restingHeartRateObserved: Boolean,
    activityWindows: List<CardioLoadTimeWindow>,
): IntensityMinutesEstimate? {
    val sortedSamples = samples
        .sortedBy { it.time }
        .distinctBy { it.time }
    if (sortedSamples.size < 2 || maxHeartRate <= restingHeartRate) return null

    var moderateMinutes = 0.0
    var vigorousMinutes = 0.0
    var coveredMinutes = 0.0
    val expectedMinutes = activityWindows
        .sumOf { it.durationMinutes }
        .takeIf { activityWindows.isNotEmpty() }

    sortedSamples.zipWithNext().forEach { (start, end) ->
        val rawMinutes = Duration.between(start.time, end.time).seconds.coerceAtLeast(0L).toDouble() / 60.0
        if (rawMinutes <= 0.0 || rawMinutes > MaxHeartRateSampleGapMinutes) return@forEach

        val interval = CardioLoadTimeWindow(start.time, end.time)
        val intervalMinutes = if (activityWindows.isNotEmpty()) {
            activityWindows.sumOf { interval.overlapMinutes(it) }
        } else {
            rawMinutes
        }
        if (intervalMinutes <= 0.0) return@forEach

        val averageBpm = (start.beatsPerMinute + end.beatsPerMinute) / 2.0
        val heartRateReserve = ((averageBpm - restingHeartRate) / (maxHeartRate - restingHeartRate).toDouble())
            .coerceIn(0.0, 1.0)
        when {
            heartRateReserve >= VigorousHeartRateReserveThreshold -> {
                vigorousMinutes += intervalMinutes
                coveredMinutes += intervalMinutes
            }
            heartRateReserve >= ModerateHeartRateReserveThreshold -> {
                moderateMinutes += intervalMinutes
                coveredMinutes += intervalMinutes
            }
        }
    }

    val accumulator = IntensityMinuteAccumulator(
        moderateMinutes = moderateMinutes,
        vigorousMinutes = vigorousMinutes,
        coveredHeartRateMinutes = coveredMinutes,
        expectedHeartRateMinutes = expectedMinutes ?: coveredMinutes,
    )
    if (accumulator.moderateEquivalentMinutes <= 0.0) return null

    val confidence = when {
        accumulator.hasGoodCoverage && restingHeartRateObserved && maxHeartRateObserved ->
            IntensityMinutesConfidence.HIGH
        accumulator.hasGoodCoverage -> IntensityMinutesConfidence.MEDIUM
        else -> IntensityMinutesConfidence.LOW
    }
    return IntensityMinutesEstimate(
        moderateMinutes = accumulator.moderateMinutes.roundToInt(),
        vigorousMinutes = accumulator.vigorousMinutes.roundToInt(),
        moderateEquivalentMinutes = accumulator.moderateEquivalentMinutes.roundToInt(),
        confidence = confidence,
        method = IntensityMinutesMethod.HEART_RATE_RESERVE,
        coveredHeartRateMinutes = accumulator.coveredHeartRateMinutes,
        expectedHeartRateMinutes = accumulator.expectedHeartRateMinutes,
        heartRateSampleCount = sortedSamples.size,
    )
}

private fun workoutFallbackIntensity(workouts: List<IntensityWorkoutInput>): IntensityMinutesEstimate? {
    var moderateMinutes = 0.0
    var vigorousMinutes = 0.0
    var durationOnlyMinutes = 0.0
    workouts.forEach { workout ->
        val duration = workout.durationMinutes.coerceAtLeast(0.0)
        if (duration <= 0.0) return@forEach
        val activeCalories = workout.activeCaloriesKcal
        if (activeCalories != null && activeCalories > 0.0) {
            val kcalPerMinute = activeCalories / duration
            when {
                kcalPerMinute >= VigorousKcalPerMinute -> vigorousMinutes += duration
                kcalPerMinute >= ModerateKcalPerMinute -> moderateMinutes += duration
            }
        } else {
            durationOnlyMinutes += duration * 0.5
        }
    }

    val moderateEquivalent = moderateMinutes + vigorousMinutes * 2.0
    if (moderateEquivalent > 0.0) {
        return IntensityMinutesEstimate(
            moderateMinutes = moderateMinutes.roundToInt(),
            vigorousMinutes = vigorousMinutes.roundToInt(),
            moderateEquivalentMinutes = moderateEquivalent.roundToInt(),
            confidence = IntensityMinutesConfidence.LOW,
            method = IntensityMinutesMethod.WORKOUT_ACTIVE_CALORIES,
        )
    }

    if (durationOnlyMinutes >= 5.0) {
        val minutes = durationOnlyMinutes.roundToInt()
        return IntensityMinutesEstimate(
            moderateMinutes = minutes,
            moderateEquivalentMinutes = minutes,
            confidence = IntensityMinutesConfidence.LOW,
            method = IntensityMinutesMethod.WORKOUT_DURATION,
        )
    }
    return null
}

private fun dailyActiveCaloriesFallbackIntensity(dailyActiveCaloriesKcal: Double?): IntensityMinutesEstimate? {
    val minutes = dailyActiveCaloriesKcal
        ?.takeIf { it > 0.0 }
        ?.let { it / DailyActiveCaloriesModerateKcalPerMinute }
        ?.takeIf { it >= 5.0 }
        ?.roundToInt()
        ?: return null
    return IntensityMinutesEstimate(
        moderateMinutes = minutes,
        moderateEquivalentMinutes = minutes,
        confidence = IntensityMinutesConfidence.LOW,
        method = IntensityMinutesMethod.DAILY_ACTIVE_CALORIES,
    )
}

private fun cardioLoadFallbackIntensity(cardioLoadScore: Int?): IntensityMinutesEstimate? {
    val minutes = cardioLoadScore
        ?.takeIf { it > 0 }
        ?.let { it * CardioLoadToModerateEquivalentMinutes }
        ?.takeIf { it >= 5.0 }
        ?.roundToInt()
        ?: return null
    return IntensityMinutesEstimate(
        moderateMinutes = minutes,
        moderateEquivalentMinutes = minutes,
        confidence = IntensityMinutesConfidence.LOW,
        method = IntensityMinutesMethod.CARDIO_LOAD,
    )
}

private fun maxHeartRateContext(
    observedMaxHeartRate: Long?,
    samples: List<HeartRateSample>,
    restingHeartRate: Long,
): IntensityMaxHeartRateContext? {
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
    return IntensityMaxHeartRateContext(
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
