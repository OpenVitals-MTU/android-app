package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

const val BodyEnergyTimelineBucketMinutes = 5L
const val BodyEnergyTimelineAlgorithmVersion = 1

enum class BodyEnergyConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA,
}

enum class BodyEnergyBucketState {
    SLEEP,
    REST,
    ACTIVITY,
    STRESS,
    UNMEASURABLE,
}

data class BodyEnergyTimelinePoint(
    val time: Instant,
    val score: Int,
    val delta: Double,
    val state: BodyEnergyBucketState,
    val confidence: BodyEnergyConfidence,
)

data class BodyEnergyTimeline(
    val date: LocalDate,
    val startScore: Int,
    val currentScore: Int,
    val charged: Int,
    val drained: Int,
    val points: List<BodyEnergyTimelinePoint>,
    val confidence: BodyEnergyConfidence,
    val confidenceReason: String,
    val generatedAt: Instant = Instant.now(),
    val signature: String = "",
) {
    companion object {
        fun empty(date: LocalDate, reason: String): BodyEnergyTimeline =
            BodyEnergyTimeline(
                date = date,
                startScore = 50,
                currentScore = 50,
                charged = 0,
                drained = 0,
                points = emptyList(),
                confidence = BodyEnergyConfidence.NO_DATA,
                confidenceReason = reason,
            )
    }
}

data class BodyEnergyTimelineInputs(
    val date: LocalDate,
    val heartRateSamples: List<HeartRateSample>,
    val hrvSamples: List<HrvSample> = emptyList(),
    val sleepSessions: List<SleepData> = emptyList(),
    val workouts: List<ExerciseData> = emptyList(),
    val respiratoryRateSamples: List<RespiratoryRateEntry> = emptyList(),
    val restingHeartRateBpm: Long? = null,
    val baselineRestingHeartRateBpm: Long? = null,
    val observedMaxHeartRateBpm: Long? = null,
    val hrvBaselineRmssdMs: Double? = null,
    val respiratoryRateBaseline: Double? = null,
    val previousEndScore: Int? = null,
    val calibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    val now: Instant = Instant.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
)

fun calculateBodyEnergyTimeline(inputs: BodyEnergyTimelineInputs): BodyEnergyTimeline {
    val dayStart = inputs.date.atStartOfDay(inputs.zone).toInstant()
    val dayEnd = inputs.date.plusDays(1).atStartOfDay(inputs.zone).toInstant()
    val usableEnd = minOf(dayEnd, inputs.now.takeIf { inputs.date == LocalDate.now(inputs.zone) } ?: dayEnd)
    val bucketCount = Duration.between(dayStart, usableEnd).toMinutes()
        .coerceAtLeast(0L)
        .let { ((it + BodyEnergyTimelineBucketMinutes - 1) / BodyEnergyTimelineBucketMinutes).toInt() }
    if (bucketCount <= 0) {
        return BodyEnergyTimeline.empty(inputs.date, "No timeline window is available.")
    }

    val sortedHeartRate = inputs.heartRateSamples
        .filter { it.time >= dayStart && it.time < dayEnd }
        .sortedBy { it.time }
    val heartRateAverages = sortedHeartRate.bucketedAverages(
        bucketCount = bucketCount,
        dayStart = dayStart,
        time = { it.time },
        value = { it.beatsPerMinute.toDouble() },
    )
    val hrvAverages = inputs.hrvSamples.bucketedAverages(
        bucketCount = bucketCount,
        dayStart = dayStart,
        time = { it.time },
        value = { it.rmssdMs },
    )
    val respiratoryAverages = inputs.respiratoryRateSamples.bucketedAverages(
        bucketCount = bucketCount,
        dayStart = dayStart,
        time = { it.time },
        value = { it.breathsPerMinute },
    )
    val intensityContext = resolveIntensityContext(inputs, sortedHeartRate)
    val hasSleep = inputs.sleepSessions.any { it.endTime > dayStart && it.startTime < dayEnd }
    if (sortedHeartRate.isEmpty() && !hasSleep) {
        return BodyEnergyTimeline.empty(inputs.date, "Heart rate or sleep data is needed for Body Energy.")
    }

    var score = (inputs.previousEndScore ?: 50).coerceIn(0, 100).toDouble()
    val startScore = score.roundToInt()
    var charged = 0.0
    var drained = 0.0
    var continuousActivityMinutes = 0.0
    var recoveryDebtBuckets = 0
    var highConfidenceBuckets = 0
    var mediumConfidenceBuckets = 0
    var lowConfidenceBuckets = 0

    val points = buildList {
        repeat(bucketCount) { index ->
            val bucketStart = dayStart.plus(Duration.ofMinutes(index * BodyEnergyTimelineBucketMinutes))
            val bucketEnd = minOf(bucketStart.plus(Duration.ofMinutes(BodyEnergyTimelineBucketMinutes)), usableEnd)
            val bucketMinutes = Duration.between(bucketStart, bucketEnd).seconds.toDouble() / 60.0
            if (bucketMinutes <= 0.0) return@repeat

            val avgHeartRate = heartRateAverages[index]
            val sleepMinutes = inputs.sleepSessions.sumOf { it.overlapMinutes(bucketStart, bucketEnd) }
            val workoutMinutes = inputs.workouts.sumOf { it.overlapMinutes(bucketStart, bucketEnd) }
            val hrvFactor = hrvRecoveryFactor(
                baseline = inputs.hrvBaselineRmssdMs,
                average = hrvAverages[index],
            )
            val respirationFactor = respiratoryStressFactor(
                baseline = inputs.respiratoryRateBaseline,
                average = respiratoryAverages[index],
            )
            val zone = avgHeartRate?.let { intensityContext.zoneFor(it) } ?: 0
            val activeByHeartRate = zone >= 2
            val active = workoutMinutes > 0.0 || activeByHeartRate

            continuousActivityMinutes = if (active) {
                continuousActivityMinutes + bucketMinutes
            } else {
                0.0
            }
            val fatigueMultiplier = when {
                continuousActivityMinutes >= 90.0 -> 1.5
                continuousActivityMinutes >= 45.0 -> 1.2
                else -> 1.0
            }
            val exerciseMultiplier = if (workoutMinutes > 0.0) 1.15 else 1.0
            val stressDrain = avgHeartRate?.let { bpm ->
                val resting = intensityContext.restingHeartRateBpm
                when {
                    resting == null || workoutMinutes > 0.0 || sleepMinutes > 0.0 -> 0.0
                    bpm >= resting + 25 -> 0.05 * bucketMinutes
                    bpm >= resting + 15 -> 0.025 * bucketMinutes
                    else -> 0.0
                }
            } ?: 0.0
            val intensityDrain = if (avgHeartRate != null) {
                drainRateForZone(zone) * bucketMinutes * exerciseMultiplier * fatigueMultiplier
            } else if (workoutMinutes >= 2.0) {
                0.05 * workoutMinutes
            } else {
                0.0
            }
            val recoveryDebtDrain = if (recoveryDebtBuckets > 0) 0.015 * bucketMinutes else 0.0
            val drain = (intensityDrain + stressDrain + recoveryDebtDrain) *
                maxOf(hrvFactor.drainMultiplier, respirationFactor.drainMultiplier)

            if (zone >= 3 && workoutMinutes > 0.0) {
                recoveryDebtBuckets = maxOf(recoveryDebtBuckets, (zone * 6).coerceAtMost(36))
            } else if (recoveryDebtBuckets > 0) {
                recoveryDebtBuckets -= 1
            }

            val restEligible = avgHeartRate?.let { bpm ->
                val resting = intensityContext.restingHeartRateBpm
                resting != null && bpm <= resting + 8
            } ?: false
            val charge = when {
                sleepMinutes > 0.0 -> 0.10 * sleepMinutes * hrvFactor.chargeMultiplier / respirationFactor.chargePenalty
                restEligible && recoveryDebtBuckets == 0 && drain <= 0.05 -> 0.015 * bucketMinutes
                else -> 0.0
            }

            val delta = charge - drain
            score = (score + delta).coerceIn(0.0, 100.0)
            if (delta > 0) charged += delta else drained += -delta

            val state = when {
                sleepMinutes > 0.0 -> BodyEnergyBucketState.SLEEP
                workoutMinutes > 0.0 || zone >= 2 -> BodyEnergyBucketState.ACTIVITY
                stressDrain > 0.0 -> BodyEnergyBucketState.STRESS
                restEligible -> BodyEnergyBucketState.REST
                avgHeartRate == null -> BodyEnergyBucketState.UNMEASURABLE
                else -> BodyEnergyBucketState.REST
            }
            val confidence = when {
                avgHeartRate == null && sleepMinutes <= 0.0 -> BodyEnergyConfidence.LOW
                intensityContext.confidence == BodyEnergyConfidence.HIGH -> BodyEnergyConfidence.HIGH
                intensityContext.confidence == BodyEnergyConfidence.MEDIUM -> BodyEnergyConfidence.MEDIUM
                else -> BodyEnergyConfidence.LOW
            }
            when (confidence) {
                BodyEnergyConfidence.HIGH -> highConfidenceBuckets += 1
                BodyEnergyConfidence.MEDIUM -> mediumConfidenceBuckets += 1
                BodyEnergyConfidence.LOW -> lowConfidenceBuckets += 1
                BodyEnergyConfidence.NO_DATA -> Unit
            }
            add(
                BodyEnergyTimelinePoint(
                    time = bucketStart,
                    score = score.roundToInt().coerceIn(0, 100),
                    delta = delta,
                    state = state,
                    confidence = confidence,
                )
            )
        }
    }

    val confidence = overallConfidence(
        high = highConfidenceBuckets,
        medium = mediumConfidenceBuckets,
        low = lowConfidenceBuckets,
        total = points.size,
    )
    return BodyEnergyTimeline(
        date = inputs.date,
        startScore = startScore,
        currentScore = points.lastOrNull()?.score ?: startScore,
        charged = charged.roundToInt(),
        drained = drained.roundToInt(),
        points = points,
        confidence = confidence,
        confidenceReason = confidenceReason(confidence, intensityContext),
    )
}

private data class IntensityContext(
    val restingHeartRateBpm: Long?,
    val maxHeartRateBpm: Long?,
    val manualZones: HeartZoneThresholds?,
    val confidence: BodyEnergyConfidence,
) {
    fun zoneFor(heartRateBpm: Double): Int {
        manualZones?.let { zones ->
            return when {
                heartRateBpm >= zones.zone5LowerBpm -> 5
                heartRateBpm >= zones.zone4LowerBpm -> 4
                heartRateBpm >= zones.zone3LowerBpm -> 3
                heartRateBpm >= zones.zone2LowerBpm -> 2
                heartRateBpm >= zones.zone1LowerBpm -> 1
                else -> 0
            }
        }
        val resting = restingHeartRateBpm ?: return 0
        val max = maxHeartRateBpm ?: return 0
        if (max <= resting) return 0
        val reserve = ((heartRateBpm - resting) / (max - resting).toDouble()).coerceIn(0.0, 1.0)
        return when {
            reserve >= 0.90 -> 5
            reserve >= 0.75 -> 4
            reserve >= 0.60 -> 3
            reserve >= 0.45 -> 2
            reserve >= 0.30 -> 1
            else -> 0
        }
    }
}

private fun resolveIntensityContext(
    inputs: BodyEnergyTimelineInputs,
    heartRateSamples: List<HeartRateSample>,
): IntensityContext {
    val calibration = inputs.calibration.normalized(inputs.date)
    if (calibration.useManualZones && calibration.manualZoneThresholdsBpm != null) {
        return IntensityContext(
            restingHeartRateBpm = calibration.manualRestingHeartRateBpm?.toLong()
                ?: inputs.restingHeartRateBpm
                ?: inputs.baselineRestingHeartRateBpm
                ?: heartRateSamples.estimatedRestingHeartRate(),
            maxHeartRateBpm = calibration.manualMaxHeartRateBpm?.toLong(),
            manualZones = calibration.manualZoneThresholdsBpm,
            confidence = BodyEnergyConfidence.HIGH,
        )
    }

    val resting = calibration.manualRestingHeartRateBpm?.toLong()
        ?: inputs.restingHeartRateBpm
        ?: inputs.baselineRestingHeartRateBpm
        ?: heartRateSamples.estimatedRestingHeartRate()
    val observedMax = listOfNotNull(
        calibration.manualMaxHeartRateBpm?.toLong(),
        inputs.observedMaxHeartRateBpm,
        heartRateSamples.maxOfOrNull { it.beatsPerMinute },
    ).maxOrNull()
    val ageMax = calibration.ageYears(inputs.date)?.let { 220L - it }
    val maxHeartRate = when {
        calibration.manualMaxHeartRateBpm != null -> calibration.manualMaxHeartRateBpm.toLong()
        resting != null && observedMax != null && observedMax >= maxOf(150L, resting + 60L) -> observedMax
        ageMax != null -> ageMax
        resting != null && observedMax != null -> maxOf(observedMax + 10L, resting + 70L)
        resting != null -> resting + 70L
        else -> null
    }
    val confidence = when {
        calibration.manualMaxHeartRateBpm != null && resting != null -> BodyEnergyConfidence.HIGH
        resting != null && observedMax != null && maxHeartRate == observedMax -> BodyEnergyConfidence.MEDIUM
        resting != null && ageMax != null -> BodyEnergyConfidence.MEDIUM
        resting != null && maxHeartRate != null -> BodyEnergyConfidence.LOW
        else -> BodyEnergyConfidence.LOW
    }
    return IntensityContext(
        restingHeartRateBpm = resting,
        maxHeartRateBpm = maxHeartRate,
        manualZones = null,
        confidence = confidence,
    )
}

private data class HrvFactor(
    val drainMultiplier: Double,
    val chargeMultiplier: Double,
)

private data class RespiratoryFactor(
    val drainMultiplier: Double,
    val chargePenalty: Double,
)

private fun hrvRecoveryFactor(
    baseline: Double?,
    average: Double?,
): HrvFactor {
    if (baseline == null || average == null) return HrvFactor(1.0, 1.0)
    return when {
        average < baseline * 0.75 -> HrvFactor(drainMultiplier = 1.18, chargeMultiplier = 0.75)
        average < baseline * 0.90 -> HrvFactor(drainMultiplier = 1.08, chargeMultiplier = 0.90)
        average > baseline * 1.10 -> HrvFactor(drainMultiplier = 0.96, chargeMultiplier = 1.12)
        else -> HrvFactor(1.0, 1.0)
    }
}

private fun respiratoryStressFactor(
    baseline: Double?,
    average: Double?,
): RespiratoryFactor {
    if (baseline == null || average == null) return RespiratoryFactor(1.0, 1.0)
    return when {
        average >= baseline + 3.0 -> RespiratoryFactor(drainMultiplier = 1.12, chargePenalty = 1.15)
        average >= baseline + 1.5 -> RespiratoryFactor(drainMultiplier = 1.05, chargePenalty = 1.06)
        else -> RespiratoryFactor(1.0, 1.0)
    }
}

private fun drainRateForZone(zone: Int): Double =
    when (zone) {
        1 -> 0.03
        2 -> 0.07
        3 -> 0.14
        4 -> 0.25
        5 -> 0.40
        else -> 0.0
    }

private fun overallConfidence(
    high: Int,
    medium: Int,
    low: Int,
    total: Int,
): BodyEnergyConfidence {
    if (total == 0) return BodyEnergyConfidence.NO_DATA
    val covered = high + medium + low
    if (covered == 0) return BodyEnergyConfidence.NO_DATA
    val highRatio = high / total.toDouble()
    val mediumOrHighRatio = (high + medium) / total.toDouble()
    return when {
        highRatio >= 0.55 -> BodyEnergyConfidence.HIGH
        mediumOrHighRatio >= 0.55 -> BodyEnergyConfidence.MEDIUM
        else -> BodyEnergyConfidence.LOW
    }
}

private fun confidenceReason(
    confidence: BodyEnergyConfidence,
    context: IntensityContext,
): String = when (confidence) {
    BodyEnergyConfidence.HIGH -> "Heart-rate intensity has strong calibration."
    BodyEnergyConfidence.MEDIUM -> "Heart-rate intensity uses observed or age-based calibration."
    BodyEnergyConfidence.LOW -> if (context.restingHeartRateBpm == null || context.maxHeartRateBpm == null) {
        "Calibration is incomplete, so automatic estimates are conservative."
    } else {
        "Some timeline buckets have sparse Health Connect data."
    }
    BodyEnergyConfidence.NO_DATA -> "No usable Health Connect data was available."
}

private fun List<HeartRateSample>.estimatedRestingHeartRate(): Long? {
    if (isEmpty()) return null
    val sorted = map { it.beatsPerMinute }.sorted()
    val index = (sorted.lastIndex * 0.1).roundToInt().coerceIn(sorted.indices)
    return sorted[index].coerceIn(40L, 100L)
}

private fun SleepData.overlapMinutes(start: Instant, end: Instant): Double =
    overlapMinutes(startTime, endTime, start, end)

private fun ExerciseData.overlapMinutes(start: Instant, end: Instant): Double =
    overlapMinutes(startTime, endTime, start, end)

private fun overlapMinutes(
    sourceStart: Instant,
    sourceEnd: Instant,
    start: Instant,
    end: Instant,
): Double {
    val overlapStart = maxOf(sourceStart, start)
    val overlapEnd = minOf(sourceEnd, end)
    if (!overlapEnd.isAfter(overlapStart)) return 0.0
    return Duration.between(overlapStart, overlapEnd).seconds.toDouble() / 60.0
}

private inline fun <T> List<T>.bucketedAverages(
    bucketCount: Int,
    dayStart: Instant,
    crossinline time: (T) -> Instant,
    crossinline value: (T) -> Double,
): Array<Double?> {
    if (bucketCount <= 0 || isEmpty()) return arrayOfNulls(bucketCount.coerceAtLeast(0))
    val sums = DoubleArray(bucketCount)
    val counts = IntArray(bucketCount)
    forEach { sample ->
        val minutesFromStart = Duration.between(dayStart, time(sample)).toMinutes()
        if (minutesFromStart < 0) return@forEach
        val bucketIndex = (minutesFromStart / BodyEnergyTimelineBucketMinutes).toInt()
        if (bucketIndex in 0 until bucketCount) {
            val sampleValue = value(sample)
            if (sampleValue.isFinite()) {
                sums[bucketIndex] += sampleValue
                counts[bucketIndex] += 1
            }
        }
    }
    return Array(bucketCount) { index ->
        counts[index].takeIf { it > 0 }?.let { count -> sums[index] / count }
    }
}
