package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.sleepDurationMsFromStages
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

/**
 * The 5-minute-bucket Body Energy timeline.
 *
 * An energy balance: a basal waking floor plus an activity drain, the stronger
 * of the heart-rate-zone estimate (Banister TRIMP,
 * https://pmc.ncbi.nlm.nih.gov/articles/PMC6561225/) and an active-calorie
 * estimate. The framing is a product design, not a published model.
 */
const val BodyEnergyTimelineBucketMinutes = 5L

/** Bump when a change means every stored day must be recomputed. */
const val BodyEnergyTimelineAlgorithmVersion = 12

/** Start score when there is no previous day to carry from. */
const val BodyEnergyNeutralStartScore = 50

/**
 * Floor for a carried seed. Charge caps near +45 a night while drain is
 * unbounded, so zero would be an absorbing state. 10 stays in the "Low" band
 * and leaves about 7 hours of basal headroom.
 */
const val BodyEnergyCarryOverFloor = 10

/** Points of Body Energy charged per minute of sleep. */
private const val SleepPointsPerMinute = 0.10

/**
 * How far a night's quality scales its charge. ±20% separates a good and a
 * poor night of equal length by 10-15 points without undoing the hours slept.
 */
private const val SleepQualitySwing = 0.20

/**
 * The quality fraction of an ordinary night, and the span to full effect.
 *
 * Not the arithmetic midpoint: efficiency alone clears its full share on most
 * nights, so a broken night still scores about 0.59. 0.72 is a decent night
 * and charges what it did before this factor existed, which keeps
 * [BodyEnergyCalibration.sleepChargeGain] honest. The span reaches full
 * penalty at 0.40 and full marks near +18%, under the clamp.
 */
private const val TypicalSleepQualityFraction = 0.72
private const val SleepQualityFractionSpan = 0.32

/**
 * Points charged per minute of quiet waking time. Lowered from 0.02 when the
 * gate widened to a share of heart-rate reserve.
 */
private const val RestPointsPerMinute = 0.012

/**
 * Share of heart-rate reserve that still counts as quiet. Above the old
 * resting-plus-8 band, below zone 1, and it moves with the person.
 */
private const val RestChargeReserveCeiling = 0.15

/** Basal drain per waking minute: about 20 points over a 16-hour day. */
private const val BasalPointsPerMinute = 0.022

/** Reference BMR; a higher measured BMR drains proportionally faster. */
private const val ReferenceBmrKcalPerDay = 1600.0

/** Drain per active kilocalorie: a heavy 700 kcal day is about 40 points. */
private const val ActiveKcalToPoints = 0.06

/** Fallback when a bucket has steps but no calories. Never added on top. */
private const val KcalPerStep = 0.04

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

enum class BodyEnergyPrimaryInfluence {
    SLEEP_RECOVERY,
    QUIET_REST,
    EVERYDAY_ACTIVITY,
    EXERTION,
    ELEVATED_HEART_RATE,
    RECOVERY_DEBT,
    NO_DATA,
    STEADY,
}

/** Where a day's starting score came from. Shown in the UI. */
enum class BodyEnergySeedSource {
    /** No previous day to carry from. */
    NEUTRAL,

    /** The previous day's end score, chained across midnight. */
    CARRIED_OVER,

    /** A stored day exists but is too far back to chain. A background sync closes the gap. */
    CHAIN_GAP,
}

enum class BodyEnergyCalibrationMode {
    AUTOMATIC,
    MANUAL_VALUES,
    MANUAL_ZONES,
}

/**
 * Which sentence [BodyEnergyTimeline.confidenceReason] is. The English string
 * is persisted; the UI renders the code. [LEGACY] marks rows stored before codes.
 */
enum class BodyEnergyReasonCode {
    LEGACY,
    STRONG_CALIBRATION,
    OBSERVED_OR_AGE_CALIBRATION,
    INCOMPLETE_CALIBRATION,
    SPARSE_BUCKETS,
    NO_USABLE_DATA,
    NO_TIMELINE_WINDOW,
    NEEDS_HEART_RATE_OR_SLEEP,
}

data class BodyEnergyTimelinePoint(
    val time: Instant,
    val score: Int,
    val delta: Double,
    val state: BodyEnergyBucketState,
    val confidence: BodyEnergyConfidence,
    val charge: Double = delta.coerceAtLeast(0.0),
    /** Heart-rate-zone estimate of the activity drain. */
    val intensityDrain: Double = 0.0,
    /** Active-calorie estimate of the activity drain. See [appliedActivityDrain]. */
    val activityEnergyDrain: Double = 0.0,
    /** Basal metabolic drain while awake. */
    val basalDrain: Double = 0.0,
    val stressDrain: Double = 0.0,
    val recoveryDebtDrain: Double = 0.0,
    val primaryInfluence: BodyEnergyPrimaryInfluence = BodyEnergyPrimaryInfluence.STEADY,
) {
    /** The stronger of the two activity estimates, never their sum. */
    val appliedActivityDrain: Double
        get() = maxOf(intensityDrain, activityEnergyDrain)
}

data class BodyEnergyInputSummary(
    val algorithmVersion: Int = BodyEnergyTimelineAlgorithmVersion,
    val bucketMinutes: Long = BodyEnergyTimelineBucketMinutes,
    val heartRateSampleCount: Int = 0,
    val hrvSampleCount: Int = 0,
    val sleepSessionCount: Int = 0,
    val workoutCount: Int = 0,
    val respiratorySampleCount: Int = 0,
    val hasRestingHeartRate: Boolean = false,
    val hasBaselineRestingHeartRate: Boolean = false,
    val hasObservedMaxHeartRate: Boolean = false,
    val hasHrvBaseline: Boolean = false,
    val hasRespiratoryBaseline: Boolean = false,
    val previousEndScore: Int? = null,
    /** Whether [previousEndScore] was raised to [BodyEnergyCarryOverFloor]. */
    val carryOverFloorApplied: Boolean = false,
    val seedSource: BodyEnergySeedSource = BodyEnergySeedSource.NEUTRAL,
    val calibrationMode: BodyEnergyCalibrationMode = BodyEnergyCalibrationMode.AUTOMATIC,
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
    val confidenceReasonCode: BodyEnergyReasonCode = BodyEnergyReasonCode.LEGACY,
    val inputSummary: BodyEnergyInputSummary = BodyEnergyInputSummary(),
    val generatedAt: Instant = Instant.now(),
    val signature: String = "",
) {
    companion object {
        /** A day the model could not compute. It still carries the chain. */
        fun empty(
            date: LocalDate,
            reason: String,
            reasonCode: BodyEnergyReasonCode = BodyEnergyReasonCode.LEGACY,
            inputSummary: BodyEnergyInputSummary = BodyEnergyInputSummary(),
        ): BodyEnergyTimeline {
            val seed = bodyEnergySeedScore(inputSummary.previousEndScore)
            return BodyEnergyTimeline(
                date = date,
                startScore = seed,
                currentScore = seed,
                charged = 0,
                drained = 0,
                points = emptyList(),
                confidence = BodyEnergyConfidence.NO_DATA,
                confidenceReason = reason,
                confidenceReasonCode = reasonCode,
                inputSummary = inputSummary,
            )
        }
    }
}

/**
 * How much a night's quality scales its charge, up to [SleepQualitySwing]
 * either side of 1.0.
 *
 * Reads only the QUALITY pillar; duration and HRV are already counted.
 * Neutral for a night it cannot judge: under an hour, or no staging.
 */
internal fun sleepChargeQualityFactor(session: SleepData): Double {
    val timeInBedMs = Duration.between(session.startTime, session.endTime).toMillis()
    if (timeInBedMs <= 0L) return 1.0
    val sleepDurationMs = sleepDurationMsFromStages(session.stages, session.durationMs).coerceAtLeast(0L)
    if (sleepDurationMs < Duration.ofMinutes(MinimumQualityScoredMinutes).toMillis()) return 1.0

    val efficiencyPercent = (sleepDurationMs / timeInBedMs.toDouble() * 100.0).coerceIn(0.0, 100.0)
    val wakeMinutes = (session.wakeAfterSleepOnsetMs() ?: (timeInBedMs - sleepDurationMs).coerceAtLeast(0L))
        .toDouble() / 60_000.0
    val quality = sleepQualityPillar(
        session = session,
        sleepDurationMs = sleepDurationMs,
        sleepEfficiencyPercent = efficiencyPercent,
        wakeAfterSleepOnsetMinutes = wakeMinutes,
    )
    if (!quality.staged) return 1.0
    // The continuous read: the scored points stop distinguishing good nights.
    val deviation = ((quality.continuousFraction - TypicalSleepQualityFraction) / SleepQualityFractionSpan)
        .coerceIn(-1.0, 1.0)
    return 1.0 + deviation * SleepQualitySwing
}

/** Below this a night is a nap or a fragment, and its quality says nothing. */
private const val MinimumQualityScoredMinutes = 60L

fun bodyEnergySeedScore(previousEndScore: Int?): Int =
    previousEndScore?.coerceIn(BodyEnergyCarryOverFloor, 100) ?: BodyEnergyNeutralStartScore

/** Whether [bodyEnergySeedScore] had to raise [previousEndScore] to the floor. */
fun bodyEnergyCarryOverFloorApplies(previousEndScore: Int?): Boolean =
    previousEndScore != null && previousEndScore < BodyEnergyCarryOverFloor

data class BodyEnergyTimelineInputs(
    val date: LocalDate,
    val heartRateSamples: List<HeartRateSample>,
    val hrvSamples: List<HrvSample> = emptyList(),
    val sleepSessions: List<SleepData> = emptyList(),
    val workouts: List<ExerciseData> = emptyList(),
    val respiratoryRateSamples: List<RespiratoryRateEntry> = emptyList(),
    /** Hourly, cumulative steps and active calories. */
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    /** Latest basal metabolic rate in kcal/day, if the device reports it. */
    val basalMetabolicRateKcalPerDay: Double? = null,
    val restingHeartRateBpm: Long? = null,
    val baselineRestingHeartRateBpm: Long? = null,
    val observedMaxHeartRateBpm: Long? = null,
    val hrvBaselineRmssdMs: Double? = null,
    val respiratoryRateBaseline: Double? = null,
    val previousEndScore: Int? = null,
    /** Null when the caller cannot tell a cold start from a chain gap. */
    val seedSource: BodyEnergySeedSource? = null,
    val calibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    /** Only the birth year is read. Manual heart-rate fields are ignored. */
    val bodyProfile: BodyProfile = BodyProfile(),
    val now: Instant = Instant.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** A caller that supplies a previous score has chained. */
    val resolvedSeedSource: BodyEnergySeedSource
        get() = seedSource ?: if (previousEndScore != null) {
            BodyEnergySeedSource.CARRIED_OVER
        } else {
            BodyEnergySeedSource.NEUTRAL
        }
}

fun calculateBodyEnergyTimeline(inputs: BodyEnergyTimelineInputs): BodyEnergyTimeline {
    val dayStart = inputs.date.atStartOfDay(inputs.zone).toInstant()
    val dayEnd = inputs.date.plusDays(1).atStartOfDay(inputs.zone).toInstant()
    val usableEnd = minOf(dayEnd, inputs.now.takeIf { inputs.date == LocalDate.now(inputs.zone) } ?: dayEnd)
    val inputSummary = inputs.inputSummary(
        heartRateSampleCount = inputs.heartRateSamples.count { it.time >= dayStart && it.time < dayEnd },
    )
    val bucketCount = Duration.between(dayStart, usableEnd).toMinutes()
        .coerceAtLeast(0L)
        .let { ((it + BodyEnergyTimelineBucketMinutes - 1) / BodyEnergyTimelineBucketMinutes).toInt() }
    if (bucketCount <= 0) {
        return BodyEnergyTimeline.empty(
            date = inputs.date,
            reason = "No timeline window is available.",
            reasonCode = BodyEnergyReasonCode.NO_TIMELINE_WINDOW,
            inputSummary = inputSummary,
        )
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
        return BodyEnergyTimeline.empty(
            date = inputs.date,
            reason = "Heart rate or sleep data is needed for Body Energy.",
            reasonCode = BodyEnergyReasonCode.NEEDS_HEART_RATE_OR_SLEEP,
            inputSummary = inputSummary,
        )
    }

    val activeKcalPerBucket = inputs.activityProgress.cumulativePerBucket(
        bucketCount = bucketCount,
        dayStart = dayStart,
        value = { it.totalActiveCaloriesKcal },
    )
    val stepsPerBucket = inputs.activityProgress.cumulativePerBucket(
        bucketCount = bucketCount,
        dayStart = dayStart,
        value = { it.totalSteps.toDouble() },
    )
    val basalScale = inputs.basalMetabolicRateKcalPerDay
        ?.let { (it / ReferenceBmrKcalPerDay).coerceIn(0.5, 2.0) }
        ?: 1.0

    // Personal gains; 1.0 leaves the model untouched.
    val gains = inputs.calibration.normalized()

    // One factor per night, not per bucket.
    val qualityFactors = inputs.sleepSessions.associate { it.id to sleepChargeQualityFactor(it) }

    // The day opens where the previous one closed, floored.
    var score = bodyEnergySeedScore(inputs.previousEndScore).toDouble()
    val startScore = score.roundToInt()
    var charged = 0.0
    var drained = 0.0
    var continuousActivityMinutes = 0.0
    var recoveryDebtBuckets = 0
    var daySignalSeen = false
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
            // Scale charge by the quality of the night this bucket falls in.
            // Weighted by overlap when the bucket straddles two sessions.
            val sleepQualityFactor = if (sleepMinutes > 0.0) {
                inputs.sleepSessions.sumOf { session ->
                    val minutes = session.overlapMinutes(bucketStart, bucketEnd)
                    if (minutes <= 0.0) 0.0 else minutes * qualityFactors.getValue(session.id)
                } / sleepMinutes
            } else {
                1.0
            }
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
            val notSleeping = sleepMinutes <= 0.0
            val recordedKcal = activeKcalPerBucket.getOrElse(index) { 0.0 }
            val bucketSteps = stepsPerBucket.getOrElse(index) { 0.0 }
            // Steps stand in for calories only when the calorie series is silent.
            val activeKcal = if (recordedKcal > 0.0) recordedKcal else bucketSteps * KcalPerStep
            // Once any signal is seen today, unmeasured awake buckets still pay basal
            // drain. Before the first signal the line stays frozen.
            daySignalSeen = daySignalSeen || avgHeartRate != null || sleepMinutes > 0.0 || activeKcal > 0.0
            val awakePresent = notSleeping && (avgHeartRate != null || activeKcal > 0.0 || daySignalSeen)

            // Elevated heart rate while awake and not working out.
            val rawStressDrain = if (avgHeartRate == null) {
                0.0
            } else {
                val resting = intensityContext.restingHeartRateBpm
                when {
                    resting == null || workoutMinutes > 0.0 || sleepMinutes > 0.0 -> 0.0
                    avgHeartRate >= resting + 25 -> 0.07 * bucketMinutes
                    avgHeartRate >= resting + 15 -> 0.04 * bucketMinutes
                    avgHeartRate >= resting + 8 -> 0.02 * bucketMinutes
                    else -> 0.0
                }
            }
            val rawIntensityDrain = if (avgHeartRate != null) {
                drainRateForZone(zone) * bucketMinutes * exerciseMultiplier * fatigueMultiplier
            } else if (workoutMinutes >= 2.0) {
                0.05 * workoutMinutes
            } else {
                0.0
            }
            // Captures movement that never lifts heart rate out of the low zones.
            val rawActivityEnergyDrain = if (notSleeping) activeKcal * ActiveKcalToPoints else 0.0
            val rawRecoveryDebtDrain = if (recoveryDebtBuckets > 0) 0.015 * bucketMinutes else 0.0
            val rawBasalDrain = if (awakePresent) BasalPointsPerMinute * bucketMinutes * basalScale else 0.0
            val drainMultiplier = maxOf(hrvFactor.drainMultiplier, respirationFactor.drainMultiplier)
            val intensityDrain = rawIntensityDrain * drainMultiplier * gains.activityDrainGain
            val activityEnergyDrain = rawActivityEnergyDrain * drainMultiplier * gains.activityDrainGain
            val stressDrain = rawStressDrain * drainMultiplier * gains.stressDrainGain
            // Recovery debt follows hard activity, so it shares the activity gain.
            val recoveryDebtDrain = rawRecoveryDebtDrain * drainMultiplier * gains.activityDrainGain
            // Basal is a metabolic constant: no HRV or respiration modifier.
            val basalDrain = rawBasalDrain * gains.basalDrainGain
            // Activity is the stronger of the two estimates, never their sum.
            val appliedActivityDrain = maxOf(intensityDrain, activityEnergyDrain)
            val drain = basalDrain + appliedActivityDrain + stressDrain + recoveryDebtDrain

            if (zone >= 3 && workoutMinutes > 0.0) {
                recoveryDebtBuckets = maxOf(recoveryDebtBuckets, (zone * 6).coerceAtMost(36))
            } else if (recoveryDebtBuckets > 0) {
                recoveryDebtBuckets -= 1
            }

            val restEligible = if (avgHeartRate == null) {
                false
            } else {
                val resting = intensityContext.restingHeartRateBpm
                resting != null && avgHeartRate <= resting + 8
            }
            // A fraction of reserve, not an offset, so it survives profile changes.
            // Falls back to resting-plus-8 when there is no max.
            val restReserve = avgHeartRate?.let { intensityContext.reserveFor(it) }
            val quietEnough = restReserve?.let { it <= RestChargeReserveCeiling } ?: restEligible
            // Quiet waking time charges modestly. Not gated on activity drain: the
            // activity series is interpolated everywhere, so the two net out.
            // Recovery debt is the exception: the body is not yet recovering.
            // Quality scales the sleep charge only.
            val chargeModifier = hrvFactor.chargeMultiplier / respirationFactor.chargePenalty
            val charge = when {
                sleepMinutes > 0.0 ->
                    SleepPointsPerMinute * sleepMinutes * sleepQualityFactor *
                        chargeModifier * gains.sleepChargeGain
                quietEnough && awakePresent && recoveryDebtDrain <= 0.0 ->
                    RestPointsPerMinute * bucketMinutes * chargeModifier * gains.sleepChargeGain
                else -> 0.0
            }

            val delta = charge - drain
            val scoreBefore = score
            score = (score + delta).coerceIn(0.0, 100.0)
            val applied = score - scoreBefore
            // Once the score hits 0 or 100 only part of a bucket lands. Scale charge
            // and drain proportionally so start + charged - drained == score holds.
            val landed = if (delta == 0.0) 1.0 else (applied / delta).coerceIn(0.0, 1.0)
            charged += charge * landed
            drained += drain * landed

            val state = when {
                sleepMinutes > 0.0 -> BodyEnergyBucketState.SLEEP
                workoutMinutes > 0.0 || zone >= 2 -> BodyEnergyBucketState.ACTIVITY
                stressDrain > 0.0 -> BodyEnergyBucketState.STRESS
                restEligible -> BodyEnergyBucketState.REST
                avgHeartRate == null -> BodyEnergyBucketState.UNMEASURABLE
                else -> BodyEnergyBucketState.REST
            }
            val primaryInfluence = primaryInfluence(
                charge = charge,
                appliedActivityDrain = appliedActivityDrain,
                energyDriven = activityEnergyDrain >= intensityDrain,
                stressDrain = stressDrain,
                recoveryDebtDrain = recoveryDebtDrain,
                sleepMinutes = sleepMinutes,
                workoutMinutes = workoutMinutes,
                zone = zone,
                state = state,
            )
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
                    // Scaled by `landed` so the breakdown sums to the headline.
                    // `primaryInfluence` keeps the raw magnitudes: a clamped bucket
                    // would otherwise report a hard workout as STEADY.
                    delta = applied,
                    state = state,
                    confidence = confidence,
                    charge = charge * landed,
                    intensityDrain = intensityDrain * landed,
                    activityEnergyDrain = activityEnergyDrain * landed,
                    basalDrain = basalDrain * landed,
                    stressDrain = stressDrain * landed,
                    recoveryDebtDrain = recoveryDebtDrain * landed,
                    primaryInfluence = primaryInfluence,
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
    val endScore = points.lastOrNull()?.score ?: startScore
    val (chargedTotal, drainedTotal) = reconciledTotals(
        charged = charged,
        drained = drained,
        startScore = startScore,
        endScore = endScore,
    )
    return BodyEnergyTimeline(
        date = inputs.date,
        startScore = startScore,
        currentScore = endScore,
        charged = chargedTotal,
        drained = drainedTotal,
        points = points,
        confidence = confidence,
        confidenceReason = confidenceReason(confidence, intensityContext),
        confidenceReasonCode = confidenceReasonCode(confidence, intensityContext),
        inputSummary = inputSummary,
    )
}

/**
 * Charge and drain totals as integers that still add up. Rounding each
 * separately can leave the row a point off; the larger total absorbs it.
 */
private fun reconciledTotals(
    charged: Double,
    drained: Double,
    startScore: Int,
    endScore: Int,
): Pair<Int, Int> {
    var chargedTotal = charged.roundToInt()
    var drainedTotal = drained.roundToInt()
    val residual = (startScore + chargedTotal - drainedTotal) - endScore
    if (residual == 0) return chargedTotal to drainedTotal
    if (chargedTotal >= drainedTotal) {
        chargedTotal -= residual
    } else {
        drainedTotal += residual
    }
    return chargedTotal to drainedTotal
}

private fun BodyEnergyTimelineInputs.inputSummary(heartRateSampleCount: Int): BodyEnergyInputSummary =
    BodyEnergyInputSummary(
        algorithmVersion = BodyEnergyTimelineAlgorithmVersion,
        bucketMinutes = BodyEnergyTimelineBucketMinutes,
        heartRateSampleCount = heartRateSampleCount,
        hrvSampleCount = hrvSamples.size,
        sleepSessionCount = sleepSessions.size,
        workoutCount = workouts.size,
        respiratorySampleCount = respiratoryRateSamples.size,
        hasRestingHeartRate = restingHeartRateBpm != null,
        hasBaselineRestingHeartRate = baselineRestingHeartRateBpm != null,
        hasObservedMaxHeartRate = observedMaxHeartRateBpm != null,
        hasHrvBaseline = hrvBaselineRmssdMs != null,
        hasRespiratoryBaseline = respiratoryRateBaseline != null,
        previousEndScore = previousEndScore,
        carryOverFloorApplied = bodyEnergyCarryOverFloorApplies(previousEndScore),
        seedSource = resolvedSeedSource,
        calibrationMode = calibrationMode(calibration, bodyProfile, date),
    )

private fun calibrationMode(
    calibration: BodyEnergyCalibration,
    bodyProfile: BodyProfile,
    date: LocalDate,
): BodyEnergyCalibrationMode {
    val normalizedCalibration = calibration.normalized()
    val normalizedProfile = bodyProfile.normalized(date)
    return when {
        normalizedCalibration.useManualZones && normalizedCalibration.manualZoneThresholdsBpm != null ->
            BodyEnergyCalibrationMode.MANUAL_ZONES
        // Only the birth year is read from the profile.
        normalizedProfile.birthYear != null -> BodyEnergyCalibrationMode.MANUAL_VALUES
        else -> BodyEnergyCalibrationMode.AUTOMATIC
    }
}

private fun primaryInfluence(
    charge: Double,
    appliedActivityDrain: Double,
    energyDriven: Boolean,
    stressDrain: Double,
    recoveryDebtDrain: Double,
    sleepMinutes: Double,
    workoutMinutes: Double,
    zone: Int,
    state: BodyEnergyBucketState,
): BodyEnergyPrimaryInfluence {
    if (state == BodyEnergyBucketState.UNMEASURABLE) return BodyEnergyPrimaryInfluence.NO_DATA
    if (charge > 0.0 && sleepMinutes > 0.0) return BodyEnergyPrimaryInfluence.SLEEP_RECOVERY

    // Basal is the ever-present floor, never the notable influence.
    val maxDrain = maxOf(appliedActivityDrain, stressDrain, recoveryDebtDrain)

    // Waking charge competes with drain rather than short-circuiting. Sleep is
    // the exception: nothing else drains during it.
    if (charge > 0.0 && charge >= maxDrain) return BodyEnergyPrimaryInfluence.QUIET_REST
    if (maxDrain <= 0.0) return BodyEnergyPrimaryInfluence.STEADY
    if (maxDrain == appliedActivityDrain) {
        // Low-heart-rate movement with no workout is everyday activity.
        val everyday = energyDriven && zone < 2 && workoutMinutes <= 0.0
        return if (everyday) {
            BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY
        } else {
            BodyEnergyPrimaryInfluence.EXERTION
        }
    }
    if (maxDrain == stressDrain) return BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE
    return BodyEnergyPrimaryInfluence.RECOVERY_DEBT
}

private data class IntensityContext(
    val restingHeartRateBpm: Long?,
    val maxHeartRateBpm: Long?,
    val manualZones: HeartZoneThresholds?,
    val confidence: BodyEnergyConfidence,
) {
    /** Fraction of heart-rate reserve at [heartRateBpm], or null without a resting/max pair. */
    fun reserveFor(heartRateBpm: Double): Double? {
        val resting = restingHeartRateBpm ?: return null
        val max = maxHeartRateBpm ?: return null
        if (max <= resting) return null
        return ((heartRateBpm - resting) / (max - resting).toDouble()).coerceIn(0.0, 1.0)
    }

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
        val reserve = reserveFor(heartRateBpm) ?: return 0
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
    val calibration = inputs.calibration.normalized()
    val profile = inputs.bodyProfile.normalized(inputs.date)
    if (calibration.useManualZones && calibration.manualZoneThresholdsBpm != null) {
        return IntensityContext(
            restingHeartRateBpm = inputs.restingHeartRateBpm
                ?: inputs.baselineRestingHeartRateBpm
                ?: heartRateSamples.estimatedRestingHeartRate(),
            maxHeartRateBpm = null,
            manualZones = calibration.manualZoneThresholdsBpm,
            confidence = BodyEnergyConfidence.HIGH,
        )
    }

    val resting = inputs.restingHeartRateBpm
        ?: inputs.baselineRestingHeartRateBpm
        ?: heartRateSamples.estimatedRestingHeartRate()
    val observedMax = listOfNotNull(
        inputs.observedMaxHeartRateBpm,
        heartRateSamples.maxOfOrNull { it.beatsPerMinute },
    ).maxOrNull()
    // Tanaka (208 - 0.7*age), matching heart-rate recovery.
    val ageMax = profile.ageYears(inputs.date)
        ?.let { maxOf(1L, (208.0 - 0.7 * it).roundToInt().toLong()) }
    val maxHeartRate = when {
        resting != null && observedMax != null && observedMax >= maxOf(150L, resting + 60L) -> observedMax
        ageMax != null -> ageMax
        resting != null && observedMax != null -> maxOf(observedMax + 10L, resting + 70L)
        resting != null -> resting + 70L
        else -> null
    }
    // An observed max already cleared max(150, resting + 60), so it earns high confidence.
    val confidence = when {
        resting != null && observedMax != null && maxHeartRate == observedMax -> BodyEnergyConfidence.HIGH
        resting != null && ageMax != null -> BodyEnergyConfidence.MEDIUM
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

private fun confidenceReasonCode(
    confidence: BodyEnergyConfidence,
    context: IntensityContext,
): BodyEnergyReasonCode = when (confidence) {
    BodyEnergyConfidence.HIGH -> BodyEnergyReasonCode.STRONG_CALIBRATION
    BodyEnergyConfidence.MEDIUM -> BodyEnergyReasonCode.OBSERVED_OR_AGE_CALIBRATION
    BodyEnergyConfidence.LOW ->
        if (context.restingHeartRateBpm == null || context.maxHeartRateBpm == null) {
            BodyEnergyReasonCode.INCOMPLETE_CALIBRATION
        } else {
            BodyEnergyReasonCode.SPARSE_BUCKETS
        }
    BodyEnergyConfidence.NO_DATA -> BodyEnergyReasonCode.NO_USABLE_DATA
}

/** The code for a persisted English reason. Unrecognised text stays [BodyEnergyReasonCode.LEGACY]. */
fun bodyEnergyReasonCodeForText(reason: String): BodyEnergyReasonCode = when (reason) {
    "Heart-rate intensity has strong calibration." -> BodyEnergyReasonCode.STRONG_CALIBRATION
    "Heart-rate intensity uses observed or age-based calibration." ->
        BodyEnergyReasonCode.OBSERVED_OR_AGE_CALIBRATION
    "Calibration is incomplete, so automatic estimates are conservative." ->
        BodyEnergyReasonCode.INCOMPLETE_CALIBRATION
    "Some timeline buckets have sparse Health Connect data." -> BodyEnergyReasonCode.SPARSE_BUCKETS
    "No usable Health Connect data was available." -> BodyEnergyReasonCode.NO_USABLE_DATA
    "No timeline window is available." -> BodyEnergyReasonCode.NO_TIMELINE_WINDOW
    "Heart rate or sleep data is needed for Body Energy." -> BodyEnergyReasonCode.NEEDS_HEART_RATE_OR_SLEEP
    else -> BodyEnergyReasonCode.LEGACY
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

/**
 * A cumulative hourly series differenced into 5-minute buckets. Each hour's
 * burn is spread evenly across its buckets.
 */
private fun List<ActivityProgressPoint>.cumulativePerBucket(
    bucketCount: Int,
    dayStart: Instant,
    value: (ActivityProgressPoint) -> Double?,
): DoubleArray {
    val result = DoubleArray(bucketCount.coerceAtLeast(0))
    if (bucketCount <= 0 || isEmpty()) return result

    // Cumulative knots: minutes from start to running value.
    val knotMinutes = mutableListOf(0.0)
    val knotValues = mutableListOf(0.0)
    filter { value(it) != null }
        .sortedBy { it.time }
        .forEach { point ->
            val minute = Duration.between(dayStart, point.time).seconds / 60.0
            if (minute <= 0.0) return@forEach
            var v = maxOf(0.0, value(point)!!)
            // Guard against a non-monotonic cumulative series.
            if (v < knotValues.last()) v = knotValues.last()
            knotMinutes += minute
            knotValues += v
        }
    if (knotMinutes.size < 2) return result

    fun cumulativeAt(minute: Double): Double {
        if (minute <= knotMinutes.first()) return knotValues.first()
        if (minute >= knotMinutes.last()) return knotValues.last()
        for (i in 1 until knotMinutes.size) {
            if (minute <= knotMinutes[i]) {
                val t0 = knotMinutes[i - 1]
                val v0 = knotValues[i - 1]
                val t1 = knotMinutes[i]
                val v1 = knotValues[i]
                if (t1 <= t0) return v1
                return v0 + (v1 - v0) * ((minute - t0) / (t1 - t0))
            }
        }
        return knotValues.last()
    }

    for (i in 0 until bucketCount) {
        val start = (i * BodyEnergyTimelineBucketMinutes).toDouble()
        val end = ((i + 1) * BodyEnergyTimelineBucketMinutes).toDouble()
        result[i] = maxOf(0.0, cumulativeAt(end) - cumulativeAt(start))
    }
    return result
}
