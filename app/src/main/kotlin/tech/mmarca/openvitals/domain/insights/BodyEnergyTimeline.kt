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
 * The 5-minute-bucket Body Energy timeline algorithm.
 *
 * The original model derived drain purely from heart-rate zones, with no basal
 * cost and a daytime rest charge. That under-drained active days (a 20k-step,
 * low-heart-rate day read ~75 when it felt like 10%). V3 reframed it as an
 * energy balance — a basal waking floor plus an activity drain that is the
 * stronger of the heart-rate-zone estimate and an active-calorie estimate.
 *
 * Research: the activity-drain component is heart-rate-zone training load
 * (Banister TRIMP, https://pmc.ncbi.nlm.nih.gov/articles/PMC6561225/;
 * training-load monitoring review, https://pmc.ncbi.nlm.nih.gov/articles/PMC4213373/),
 * taken as the stronger of the zone estimate and an active-calorie estimate; the
 * basal floor is resting metabolism. The energy-balance framing is a documented
 * product design, not a single published model.
 */
const val BodyEnergyTimelineBucketMinutes = 5L

/**
 * v11: the manual resting and max heart rate inputs were removed, so the zone
 * ladder is derived from observed data for everyone. Anyone who had typed a
 * maximum gets a different ladder and a different score, and the confidence rule
 * changed with it, so every stored day has to be recomputed.
 */
const val BodyEnergyTimelineAlgorithmVersion = 12

/**
 * The score a day starts on when there is no previous day to carry from — a
 * brand-new install, or a chain gap too wide to close. Not a default for a day
 * that *does* have a predecessor: the whole point of the chain is that midnight
 * is not a reset.
 */
const val BodyEnergyNeutralStartScore = 50

/**
 * Floor applied to a *carried* seed, so one over-drawn day cannot pin the chain
 * at zero forever.
 *
 * A product guard, not a physiological derivation (the energy-balance framing
 * above is itself a design, not a published model). Charge is sleep-dominated
 * and caps near +45 across a full night, while drain is unbounded — a heavy day
 * can accumulate 200 points of it. Zero is therefore an absorbing state: once a
 * day ends there, every later day would start there too. 10 sits unambiguously
 * inside the "Low" band, so it never flatters a depleted user, and leaves ~7
 * hours of basal drain of headroom so the next morning is not already pinned
 * before breakfast. Applies only to a carried seed — never to
 * [BodyEnergyNeutralStartScore], and never to a score computed within a day.
 */
const val BodyEnergyCarryOverFloor = 10

/** Points of Body Energy charged per minute of sleep. */
private const val SleepPointsPerMinute = 0.10

/**
 * How far a night's QUALITY may scale its charge, either side of neutral.
 *
 * The charge already counted the minutes slept and already read overnight HRV;
 * what it never read was how well those minutes went. Two nights of the same
 * length — one unbroken with a full deep/REM share, one shallow and repeatedly
 * awake — charged within a couple of points of each other, which is not what
 * the following day feels like.
 *
 * ±20%: enough that a good night and a poor one of equal length land ten to
 * fifteen points apart, and bounded so a single night of broken staging cannot
 * undo eight hours actually slept. Duration stays the dominant term, which it
 * should be.
 */
private const val SleepQualitySwing = 0.20

/**
 * The quality fraction an ordinary night scores, and how far either side of it
 * the pillar realistically travels.
 *
 * NOT the pillar's arithmetic midpoint. Half of full marks sounds like the
 * neutral point and is not: efficiency alone earns its full share above 85%,
 * which most nights clear, so a genuinely broken night — an hour awake, almost
 * no deep sleep — still scores about 0.59 and would have been rewarded for it.
 * Centring on the theoretical middle would have charged nearly every night a
 * bonus and called it neutral.
 *
 * 0.85 is a decent night — good efficiency, little time awake, a reasonable
 * deep and REM share — and it charges exactly what it charged before this
 * factor existed. That matters beyond taste: [BodyEnergyCalibration.sleepChargeGain]
 * is fitted against watch readings, and a factor that sat above neutral on the
 * ordinary night would inflate every night rather than tell them apart, leaving
 * the fit to undo it for watch owners and nobody to undo it for everyone else.
 *
 * The ±0.35 span reaches full penalty at 0.5 — a night with an hour awake and
 * almost no deep sleep — while full marks land near +9%, so the best nights
 * stay distinguishable from the merely good instead of all clamping together.
 */
private const val TypicalSleepQualityFraction = 0.85
private const val SleepQualityFractionSpan = 0.35

/**
 * Points charged per minute of genuinely quiet waking time.
 *
 * Recovering while awake is real but slower than sleep. Lowered from 0.02 when
 * the gate widened from "within 8 bpm of resting" to a share of heart-rate
 * reserve: the diagnostic showed the charge over-shooting where it did fire
 * (quiet-rest observations ran 13.8 points ABOVE the watch) while firing on only
 * one day in seven. Widening the band and lowering the rate are the same
 * correction read from both ends.
 */
private const val RestPointsPerMinute = 0.012

/**
 * How much of the heart-rate reserve still counts as quiet enough to recover.
 *
 * Fifteen percent: comfortably above the resting-plus-8 band that measured out
 * at essentially zero over a real week, and comfortably below zone 1 (thirty
 * percent), which would charge at a heart rate nearly thirty beats above
 * resting. Being a fraction, it moves with the person.
 */
private const val RestChargeReserveCeiling = 0.15

/**
 * Points of Body Energy drained per minute of basal metabolism while awake.
 * ~0.022 accrues roughly 20 points across a 16-hour waking day, so the line
 * always trends gently down when nothing else is happening.
 */
private const val BasalPointsPerMinute = 0.022

/**
 * Reference BMR the basal drain is calibrated around; a higher measured BMR
 * drains proportionally faster (bounded).
 */
private const val ReferenceBmrKcalPerDay = 1600.0

/**
 * Points of Body Energy drained per kilocalorie of active energy expenditure.
 * Chosen so a heavy ~700 active-kcal day contributes ~40 points of drain.
 */
private const val ActiveKcalToPoints = 0.06

/**
 * Fallback conversion for buckets whose activity progress carries STEPS but no
 * active-calorie figure (a phone pedometer writing bare step counts into Health
 * Connect). ~0.04 kcal per step is the common walking approximation; it only
 * substitutes when the calorie series is silent, never adds to it.
 */
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

/**
 * Where a day's starting score came from. Surfaced in the UI so a reset is
 * always explicable rather than looking like lost data.
 */
enum class BodyEnergySeedSource {
    /**
     * No previous day to carry from — a first run, or every stored day
     * invalidated by a calibration or algorithm change.
     */
    NEUTRAL,

    /** The previous day's end score, chained across midnight. */
    CARRIED_OVER,

    /**
     * A stored day exists but too far back to chain within the read budget, so
     * this day starts neutral. A background chain sync closes the gap.
     */
    CHAIN_GAP,
}

enum class BodyEnergyCalibrationMode {
    AUTOMATIC,
    MANUAL_VALUES,
    MANUAL_ZONES,
}

/**
 * Which sentence [BodyEnergyTimeline.confidenceReason] is. The English string
 * stays canonical (and is what the store persists), the UI renders the code
 * through its string catalog. [LEGACY] marks a row stored before codes existed;
 * the UI falls back to the English text then.
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
    /** Heart-rate-zone estimate of the activity drain (the backstop signal). */
    val intensityDrain: Double = 0.0,
    /**
     * Active-calorie estimate of the activity drain. The drain actually applied
     * for activity is [appliedActivityDrain].
     */
    val activityEnergyDrain: Double = 0.0,
    /** Basal metabolic drain while awake. */
    val basalDrain: Double = 0.0,
    val stressDrain: Double = 0.0,
    val recoveryDebtDrain: Double = 0.0,
    val primaryInfluence: BodyEnergyPrimaryInfluence = BodyEnergyPrimaryInfluence.STEADY,
) {
    /**
     * The activity drain actually applied: the stronger of the heart-rate-zone
     * and active-calorie estimates, never their sum. The two describe the same
     * movement from different sensors, so adding them would bill it twice.
     */
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
    /**
     * Whether [previousEndScore] was raised to [BodyEnergyCarryOverFloor] before
     * seeding the day. Carried rather than recomputed by the UI so the stored
     * row records what actually happened.
     */
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
        /**
         * A day the model could not compute. It still carries the chain: a day
         * without heart-rate or sleep data is a day we know nothing about, not a
         * day the user's energy reset — returning a flat 50 here would break
         * every subsequent day's seed.
         */
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
 * The score a day starts on, given the previous day's end score.
 *
 * The single place the carry-over rules live: a missing predecessor means
 * [BodyEnergyNeutralStartScore], a present one is carried but floored at
 * [BodyEnergyCarryOverFloor]. Shared by the calculator and
 * [BodyEnergyTimeline.empty] so a data-less day seeds identically to a computed
 * one.
 */
/**
 * How much a night's quality scales its charge: 1.0 for a night of ordinary
 * quality, up to [SleepQualitySwing] either side of that.
 *
 * Reads the sleep score's QUALITY pillar alone — efficiency, continuity and
 * stage architecture. Not the whole sleep score: its other two pillars are
 * duration and overnight HRV, and the charge already counts both, so folding
 * the score in whole would count them twice and make this a duration
 * multiplier in disguise.
 *
 * Neutral (exactly 1.0) for a night this cannot honestly judge: under an hour
 * slept, or no deep/REM staging. A source that writes only a start and an end
 * would otherwise score a flawless night by construction — its sleep duration
 * IS its time in bed, so efficiency reads 100% and wake time zero — and would
 * be handed the full bonus for recording nothing. Those nights charge exactly
 * what they charged before this existed, which is the promise made to anyone
 * not wearing a staging device.
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
    val deviation = ((quality.fraction - TypicalSleepQualityFraction) / SleepQualityFractionSpan)
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
    /**
     * Hourly, cumulative activity progress (steps + active calories). Used to
     * estimate the active-calorie drain the heart-rate-zone signal misses.
     */
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    /** Latest basal metabolic rate in kcal/day, if the device reports it. */
    val basalMetabolicRateKcalPerDay: Double? = null,
    val restingHeartRateBpm: Long? = null,
    val baselineRestingHeartRateBpm: Long? = null,
    val observedMaxHeartRateBpm: Long? = null,
    val hrvBaselineRmssdMs: Double? = null,
    val respiratoryRateBaseline: Double? = null,
    val previousEndScore: Int? = null,
    /**
     * Left null by callers that cannot tell a cold start from a chain gap — see
     * [resolvedSeedSource]. Only the repository knows, so it passes this
     * explicitly when it matters.
     */
    val seedSource: BodyEnergySeedSource? = null,
    val calibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    /**
     * Only the birth year is read by v11 (Tanaka age max, and the calibration
     * mode reported in the summary). The manual resting/max heart rate fields
     * are deliberately ignored — see [resolveIntensityContext].
     */
    val bodyProfile: BodyProfile = BodyProfile(),
    val now: Instant = Instant.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    /**
     * A caller that supplies a previous score has, by definition, chained.
     */
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

    // Personal gains (clamped by normalized()); 1.0 leaves the objective model
    // untouched.
    val gains = inputs.calibration.normalized()

    // One factor per night, computed once rather than per five-minute bucket.
    val qualityFactors = inputs.sleepSessions.associate { it.id to sleepChargeQualityFactor(it) }

    // The day opens where the previous one closed (floored), not at a fixed 50 —
    // see [bodyEnergySeedScore].
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
            // The night this bucket falls in, so its charge is scaled by the
            // quality of THAT night. Weighted by overlap for the bucket that
            // straddles two sessions, which is the nap-then-night case.
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
            // Steps stand in for active calories only when the calorie series is
            // silent for the bucket — a phone pedometer writing bare step counts
            // (the 4k-step walk that used to move nothing because the watch was
            // off).
            val activeKcal = if (recordedKcal > 0.0) recordedKcal else bucketSteps * KcalPerStep
            // A day that has shown life keeps burning through its gaps: once any
            // signal (heart rate, sleep, activity) has been seen today, an
            // unmeasured awake bucket still pays the basal drain — a watch on the
            // charger does not pause the wearer's metabolism. BEFORE the first
            // signal the line stays frozen, which keeps two cases honest: a
            // device-less day holds its seed instead of sliding to zero with
            // nothing to ever charge it back, and an untracked night is not
            // billed as hours of wakefulness.
            daySignalSeen = daySignalSeen || avgHeartRate != null || sleepMinutes > 0.0 || activeKcal > 0.0
            // Awake-and-present: heart rate is being sampled, active energy was
            // spent, or the day's data has started and this is a mid-day gap.
            val awakePresent = notSleeping && (avgHeartRate != null || activeKcal > 0.0 || daySignalSeen)

            // Elevated heart rate while awake and not working out. Strengthened
            // from the original so ordinary sympathetic stress registers.
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
            // Heart-rate-zone estimate of the activity drain (the backstop signal).
            val rawIntensityDrain = if (avgHeartRate != null) {
                drainRateForZone(zone) * bucketMinutes * exerciseMultiplier * fatigueMultiplier
            } else if (workoutMinutes >= 2.0) {
                0.05 * workoutMinutes
            } else {
                0.0
            }
            // Active-calorie estimate — captures walking, chores and other
            // movement that never lifts heart rate out of the low zones.
            val rawActivityEnergyDrain = if (notSleeping) activeKcal * ActiveKcalToPoints else 0.0
            val rawRecoveryDebtDrain = if (recoveryDebtBuckets > 0) 0.015 * bucketMinutes else 0.0
            // Baseline metabolic cost of being awake — the floor that keeps the
            // line trending down when nothing else is happening.
            val rawBasalDrain = if (awakePresent) BasalPointsPerMinute * bucketMinutes * basalScale else 0.0
            val drainMultiplier = maxOf(hrvFactor.drainMultiplier, respirationFactor.drainMultiplier)
            val intensityDrain = rawIntensityDrain * drainMultiplier * gains.activityDrainGain
            val activityEnergyDrain = rawActivityEnergyDrain * drainMultiplier * gains.activityDrainGain
            val stressDrain = rawStressDrain * drainMultiplier * gains.stressDrainGain
            // Recovery debt is a CONSEQUENCE of hard activity — it is armed only
            // by `zone >= 3 && workoutMinutes > 0` below, and sized by the zone
            // reached — so it moves with the activity gain rather than a fifth
            // one of its own. "Hard days cost me more than this says" is one
            // claim, not two.
            val recoveryDebtDrain = rawRecoveryDebtDrain * drainMultiplier * gains.activityDrainGain
            // Basal is a metabolic constant, not a stress response — no
            // HRV/respiration modifier, just the personal gain.
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
            // Quiet enough to be recovering: a small fraction of heart-rate
            // reserve.
            //
            // A fraction rather than an absolute offset, because the offset does
            // not survive the profile changing — and zone 1 is far too generous
            // a ceiling: with resting 60 and max 190, 88 bpm is 28 beats above
            // resting and in the top stress tier, yet only 21% of reserve. Falls
            // back to the old resting-plus-8 band when there is no max to
            // measure reserve against, which is the conservative direction.
            val restReserve = avgHeartRate?.let { intensityContext.reserveFor(it) }
            val quietEnough = restReserve?.let { it <= RestChargeReserveCeiling } ?: restEligible
            // Sleep charges, and so — modestly — does genuinely quiet waking
            // time.
            //
            // V3 removed the waking charge outright because the old one fired on
            // any low-delta bucket and under-drained active days. Removing it
            // entirely overshot: with charge sleep-only a quiet day can never
            // recover, only decline at the basal rate. Measured against a real
            // week, the model lost ~10 points EVERY day and the chain sat pinned
            // on the floor.
            //
            // Deliberately NOT gated on "no activity drain in this bucket": the
            // activity series is hourly and cumulative and gets interpolated
            // across every 5-minute bucket, so a trickle lands almost
            // everywhere. The wrist decides whether you are resting; the drain
            // still applies and the two net out.
            //
            // Recovery debt is the one exception. Sitting quietly an hour after
            // a hard session is exactly the state that debt exists to model —
            // the body is not yet recovering, which is the whole point — so
            // charging through it would both overstate the recovery and, since
            // the rest rate is larger than the debt rate, hide recovery debt as
            // an influence entirely.
            // Quality scales the sleep charge and nothing else: quiet waking
            // rest has no night to judge.
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
            // A bucket only partly lands once the score reaches 0 or 100.
            // Attribute the part that landed proportionally across charge and
            // drain, so the day's totals — and the per-bucket components "What
            // moved it" breaks down — describe what actually moved the score
            // rather than what the model wanted to move it. That makes
            // `startScore + charged - drained == currentScore` hold exactly.
            //
            // Proportional rather than net-only because a bucket can carry both
            // — one straddling wake-up has sleep charge and basal drain — and
            // net-only would drop the drain from every charging bucket. Scaling
            // uniformly also preserves every magnitude comparison, so it cannot
            // change which influence wins.
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
                    // Scaled by `landed`, like the day totals, so the breakdown
                    // always sums to the headline. `primaryInfluence`
                    // deliberately stays computed from the raw magnitudes above:
                    // a fully clamped bucket scales every component to zero, and
                    // deriving the influence from those would report a hard
                    // workout as `STEADY`. The chart then draws a zero-height bar
                    // in the right colour, and the watch fit keeps a truthful
                    // driver.
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
 * The day's charge and drain totals as integers that still add up.
 *
 * The running doubles satisfy `start + charged - drained == end` exactly, but the
 * card shows three rounded integers and the score is rounded separately per
 * bucket — so rounding each independently can leave the row a point apart
 * (`Start 50 + 36 - 3` against an end score of 84). The residual is absorbed by
 * the larger of the two totals, where a single point is proportionally the least
 * misleading, so the summary always reads as one ledger.
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
        // v11 reads nothing else off the profile: the manual resting and max
        // heart rate fields no longer reach this model.
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

    // Basal drain is deliberately excluded from the competition: it is the
    // ever-present floor, reported as steady, never as the notable influence.
    val maxDrain = maxOf(appliedActivityDrain, stressDrain, recoveryDebtDrain)

    // Waking charge is reachable again as of the v8 rest charge, and it COMPETES
    // rather than short-circuiting. A bucket can now both charge and carry a
    // drain — resting quietly an hour after a hard workout charges while
    // recovery debt is still being billed — so the influence is whichever
    // actually moved the score more. Sleep is the exception above: nothing else
    // drains during it, so there is nothing to compete with.
    if (charge > 0.0 && charge >= maxDrain) return BodyEnergyPrimaryInfluence.QUIET_REST
    if (maxDrain <= 0.0) return BodyEnergyPrimaryInfluence.STEADY
    if (maxDrain == appliedActivityDrain) {
        // Low-heart-rate movement with no workout is everyday activity; anything
        // heart-rate- or workout-driven is exertion.
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
    /**
     * Fraction of heart-rate reserve at [heartRateBpm], or null when there is no
     * resting/max pair to measure it against.
     */
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
    // Tanaka (208 - 0.7*age), matching heart-rate recovery. The two used
    // different formulas off the same birth year, so the app disagreed with
    // itself about a person's max heart rate by several bpm — and this one feeds
    // the zone ladder that the whole drain model rests on.
    val ageMax = profile.ageYears(inputs.date)
        ?.let { maxOf(1L, (208.0 - 0.7 * it).roundToInt().toLong()) }
    val maxHeartRate = when {
        resting != null && observedMax != null && observedMax >= maxOf(150L, resting + 60L) -> observedMax
        ageMax != null -> ageMax
        resting != null && observedMax != null -> maxOf(observedMax + 10L, resting + 70L)
        resting != null -> resting + 70L
        else -> null
    }
    // A max the user TYPED used to be the only route to high, which had it
    // backwards: an observed max comes from their own heart rate and already has
    // to clear `max(150, resting + 60)` before it is used at all, while the typed
    // one cleared nothing. With the manual input gone, the measured value takes
    // the confidence it was always the better claim to.
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

/**
 * The code a PERSISTED English reason corresponds to, for rows stored before
 * codes existed — the sentences are fixed, so the mapping is exact; anything
 * unrecognised stays [BodyEnergyReasonCode.LEGACY] and renders as stored.
 */
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
 * A cumulative hourly series differenced into 5-minute buckets.
 *
 * [ActivityProgressPoint] is hourly and cumulative (each point's total is the
 * running figure at that hour's end). Treating the cumulative series as
 * piecewise-linear and differencing per bucket spreads each hour's burn evenly
 * across its buckets — the intended hourly→5-minute mapping.
 */
private fun List<ActivityProgressPoint>.cumulativePerBucket(
    bucketCount: Int,
    dayStart: Instant,
    value: (ActivityProgressPoint) -> Double?,
): DoubleArray {
    val result = DoubleArray(bucketCount.coerceAtLeast(0))
    if (bucketCount <= 0 || isEmpty()) return result

    // Cumulative knots, minutes-from-start → running value, starting at 0.
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
