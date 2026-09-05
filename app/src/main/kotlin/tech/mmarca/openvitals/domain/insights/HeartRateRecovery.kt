package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample

/*
 * Heart-rate recovery: how far the heart rate falls after hard effort stops.
 * Only meaningful after an abrupt stop, so it is measured only for the guided
 * recovery test, which marks the stop with a trailing rest segment. Derived
 * on read from Health Connect samples; nothing is persisted or interpolated.
 */

/**
 * The marks, in return order. No 10-second mark: optical sensors smooth over
 * several seconds. The one-minute drop has the normative literature.
 */
val heartRateRecoveryOffsets: List<Duration> = listOf(
    Duration.ofSeconds(30),
    Duration.ofMinutes(1),
    Duration.ofMinutes(2),
    Duration.ofMinutes(3),
    Duration.ofMinutes(4),
    Duration.ofMinutes(5),
)

/** The headline mark: the one-minute drop. */
val heartRateRecoveryHeadlineOffset: Duration = Duration.ofMinutes(1)

/**
 * How far from a mark a sample may sit. Tight, because heart rate falls
 * 0.5-1.0 bpm/s right after the stop.
 */
val heartRateRecoveryTolerances: Map<Duration, Duration> = mapOf(
    Duration.ofSeconds(30) to Duration.ofSeconds(3),
    Duration.ofMinutes(1) to Duration.ofSeconds(5),
    Duration.ofMinutes(2) to Duration.ofSeconds(5),
    Duration.ofMinutes(3) to Duration.ofSeconds(5),
    Duration.ofMinutes(4) to Duration.ofSeconds(5),
    Duration.ofMinutes(5) to Duration.ofSeconds(5),
)

/** The peak must come from the last ten seconds, or an early ease-off inflates the recovery. */
private val peakWindow: Duration = Duration.ofSeconds(10)

/** Tolerance for the sample taken as "the heart rate when they stopped". */
private val recoveryStartTolerance: Duration = Duration.ofSeconds(15)

/**
 * A larger fall between the last high point and the stop means they eased
 * off first. Beat-to-beat noise is 3-4 bpm, so 4 sits just above it.
 */
private const val cooldownBeforeStopDropBpm = 4

/** How far back the cool-down check looks for that high point. */
private val cooldownLookback: Duration = Duration.ofSeconds(60)

/**
 * How far below the maximum a peak may sit and still count as near-maximal.
 * A fixed band, not a fraction: max estimates carry a constant absolute
 * uncertainty. 22 bpm is the ~95% interval of the age formula; a known
 * maximum gets a tighter band.
 */
private const val estimatedMaxNearBandBpm = 22
private const val knownMaxNearBandBpm = 10

/**
 * A shorter trailing rest segment is an inter-set breather. The app writes a
 * rest segment after every strength set, the last one included.
 */
private val minimumRecoverySegmentDuration: Duration = Duration.ofSeconds(90)

/** How near the session end a rest segment must end to be the trailing one. */
private val trailingSegmentSlack: Duration = Duration.ofSeconds(30)

/** How long after the recovery start we keep reading, past the last mark. */
private val readTailPadding: Duration = Duration.ofSeconds(30)

/** How far before the recovery start we keep reading, to find the peak. */
private val readHeadPadding: Duration = Duration.ofSeconds(60)

/** One verdict on a reading, for the UI to lead with. */
enum class HeartRateRecoveryQuality {
    /** Near-maximal effort, a peak near the stop, and the one-minute mark present. */
    CLEAN,

    /** Usable, but something was estimated or coarse. See the issues. */
    APPROXIMATE,

    /** A real drop, from an effort too easy to compare against other readings. */
    NOT_COMPARABLE,

    /** The number would mislead. Do not chart it. */
    INVALID,

    /** There was nothing to measure. */
    NO_DATA,
}

enum class HeartRateRecoveryIssue {
    /** No samples in the five minutes after the stop. Usually a watch that stopped recording. */
    NO_RECOVERY_SAMPLES,

    /** Exactly one sample stood behind the peak; a single spurious reading would be it. */
    PEAK_FROM_SINGLE_SAMPLE,

    /** The heart rate was already falling before the stop. The drop flatters them. */
    COOLDOWN_BEFORE_STOP,

    /** Hard, but not near-maximal. The drop is real; it is not comparable. */
    SUBMAXIMAL_EFFORT,

    /** No maximum heart rate could be resolved, so effort could not be judged. */
    UNKNOWN_MAX_HEART_RATE,

    /** The heart rate did not fall after the stop. The recording ended, the effort did not. */
    HEART_RATE_DID_NOT_FALL,
}

/** One mark. [heartRateBpm] is null when no sample fell within tolerance. */
data class HeartRateRecoveryMark(
    val offset: Duration,
    val heartRateBpm: Long?,
    val dropBpm: Long?,
    val sampleTime: Instant?,

    /** How far the sample sat from the mark, for the UI. */
    val sampleSkew: Duration?,
)

data class HeartRateRecoveryReading(
    val recoveryStart: Instant?,
    val peakBpm: Long? = null,
    val peakTime: Instant? = null,
    val peakWindowSeconds: Int = 0,
    val peakWindowSampleCount: Int = 0,
    val marks: List<HeartRateRecoveryMark> = emptyList(),
    val maxHeartRateBpmUsed: Int? = null,
    val maxHeartRateEstimated: Boolean = false,
    val peakFractionOfMax: Double? = null,
    val recoverySampleCount: Int = 0,
    val quality: HeartRateRecoveryQuality = HeartRateRecoveryQuality.NO_DATA,
    val issues: Set<HeartRateRecoveryIssue> = emptySet(),
) {
    companion object {
        val NoData = HeartRateRecoveryReading(recoveryStart = null)
    }

    /** The one-minute drop, or null when that mark was not measured. */
    val headlineDropBpm: Long? get() = markAt(heartRateRecoveryHeadlineOffset)?.dropBpm

    fun markAt(offset: Duration): HeartRateRecoveryMark? {
        for (mark in marks) {
            if (mark.offset == offset) return mark
        }
        return null
    }

    /**
     * Whether this reading may be charted. The trend is of the one-minute
     * fall, so a reading without that mark has nothing to contribute.
     */
    val isComparable: Boolean
        get() = (quality == HeartRateRecoveryQuality.CLEAN ||
            quality == HeartRateRecoveryQuality.APPROXIMATE) &&
            headlineDropBpm != null
}

/** Where to measure from, and what to read, for one Health Connect session. */
data class HeartRateRecoveryWindow(
    val recoveryStart: Instant,
    val readStart: Instant,
    val readEnd: Instant,
)

/**
 * The instant effort stopped for [session] and the sample window to read, or
 * null when the session carries no mark of a deliberate stop.
 *
 * The recovery begins at a qualifying trailing rest segment: at least
 * [minimumRecoverySegmentDuration] long and ending within
 * [trailingSegmentSlack] of the session end. Strength sessions end with a
 * short rest segment too, hence the length floor.
 */
fun heartRateRecoveryWindowFor(session: ExerciseData): HeartRateRecoveryWindow? {
    val sessionEnd = session.endTime
    var recoveryStart: Instant? = null

    for (segment in session.segments) {
        if (segment.segmentType != ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST) continue
        if (!segment.startTime.isAfter(session.startTime)) continue
        if (Duration.between(segment.startTime, segment.endTime) <
            minimumRecoverySegmentDuration
        ) {
            continue
        }
        if (Duration.between(segment.endTime, sessionEnd).abs() > trailingSegmentSlack) {
            continue
        }
        // The last qualifying segment wins.
        val current = recoveryStart
        if (current == null || segment.startTime.isAfter(current)) {
            recoveryStart = segment.startTime
        }
    }

    val start = recoveryStart ?: return null

    return HeartRateRecoveryWindow(
        recoveryStart = start,
        readStart = start.minus(readHeadPadding),
        readEnd = start
            .plus(heartRateRecoveryOffsets.last())
            .plus(readTailPadding),
    )
}

/**
 * Measures the recovery from [recoveryStart] out of [samples]. Samples outside
 * the requested window are ignored. A mark with no sample comes back null.
 */
fun calculateHeartRateRecovery(
    recoveryStart: Instant,
    samples: List<HeartRateSample>,
    restingHeartRateBpm: Int?,
    ageYears: Int?,
    observedMaxHeartRateBpm: Int?,
    explicitMaxHeartRateBpm: Int? = null,
): HeartRateRecoveryReading {
    val ordered = ordered(samples)
    if (ordered.isEmpty()) return HeartRateRecoveryReading.NoData

    val issues = mutableSetOf<HeartRateRecoveryIssue>()

    val peak = peak(ordered, recoveryStart) ?: return HeartRateRecoveryReading.NoData
    if (peak.sampleCount == 1) {
        issues.add(HeartRateRecoveryIssue.PEAK_FROM_SINGLE_SAMPLE)
    }

    // Strictly after the stop: a sample exactly on it is the reading at cessation.
    val recoveryEnd = recoveryStart.plus(heartRateRecoveryOffsets.last())
    val recoverySamples = ordered.filter { sample ->
        sample.time.isAfter(recoveryStart) && !sample.time.isAfter(recoveryEnd)
    }
    if (recoverySamples.isEmpty()) {
        return HeartRateRecoveryReading(
            recoveryStart = recoveryStart,
            peakBpm = peak.bpm,
            peakTime = peak.time,
            peakWindowSeconds = peak.windowSeconds,
            peakWindowSampleCount = peak.sampleCount,
            marks = heartRateRecoveryOffsets.map { offset ->
                HeartRateRecoveryMark(
                    offset = offset,
                    heartRateBpm = null,
                    dropBpm = null,
                    sampleTime = null,
                    sampleSkew = null,
                )
            },
            recoverySampleCount = 0,
            quality = HeartRateRecoveryQuality.NO_DATA,
            issues = issues + HeartRateRecoveryIssue.NO_RECOVERY_SAMPLES,
        )
    }

    val marks = heartRateRecoveryOffsets.map { offset ->
        markAt(ordered, recoveryStart, offset, peak.bpm)
    }

    val maxContext = resolveMaxHeartRate(
        explicitMaxHeartRateBpm = explicitMaxHeartRateBpm,
        observedMaxHeartRateBpm = observedMaxHeartRateBpm,
        restingHeartRateBpm = restingHeartRateBpm,
        ageYears = ageYears,
    )
    if (maxContext == null) {
        issues.add(HeartRateRecoveryIssue.UNKNOWN_MAX_HEART_RATE)
    }

    // Near-maximal effort as an absolute band below the maximum, wider when the
    // maximum was estimated. A lower peak is a real but not comparable recovery.
    val peakFraction = if (maxContext == null) null else peak.bpm.toDouble() / maxContext.bpm
    if (maxContext != null) {
        val band = if (maxContext.estimated) estimatedMaxNearBandBpm else knownMaxNearBandBpm
        if (peak.bpm < maxContext.bpm - band) {
            issues.add(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT)
        }
    }

    // Was the heart rate already falling before the stop? Compare against the
    // highest reading of the last minute, not [peak], which only covers ten seconds.
    val atStop = nearest(ordered, recoveryStart, recoveryStartTolerance)
    val recentHigh = maxBpmWithin(ordered, recoveryStart, cooldownLookback)
    if (atStop != null &&
        recentHigh != null &&
        recentHigh - atStop.beatsPerMinute > cooldownBeforeStopDropBpm
    ) {
        issues.add(HeartRateRecoveryIssue.COOLDOWN_BEFORE_STOP)
    }

    // If the heart rate was as high at any mark as at the peak, the recording
    // stopped but the effort did not. That is not a recovery.
    if (marks.any { mark -> mark.dropBpm != null && mark.dropBpm <= 0 }) {
        issues.add(HeartRateRecoveryIssue.HEART_RATE_DID_NOT_FALL)
    }

    return HeartRateRecoveryReading(
        recoveryStart = recoveryStart,
        peakBpm = peak.bpm,
        peakTime = peak.time,
        peakWindowSeconds = peak.windowSeconds,
        peakWindowSampleCount = peak.sampleCount,
        marks = marks,
        maxHeartRateBpmUsed = maxContext?.bpm,
        maxHeartRateEstimated = maxContext?.estimated ?: false,
        peakFractionOfMax = peakFraction,
        recoverySampleCount = recoverySamples.size,
        quality = quality(issues, marks),
        issues = issues,
    )
}

private fun quality(
    issues: Set<HeartRateRecoveryIssue>,
    marks: List<HeartRateRecoveryMark>,
): HeartRateRecoveryQuality {
    if (HeartRateRecoveryIssue.COOLDOWN_BEFORE_STOP in issues ||
        HeartRateRecoveryIssue.HEART_RATE_DID_NOT_FALL in issues
    ) {
        return HeartRateRecoveryQuality.INVALID
    }
    // Samples after the stop, but none near a mark: nothing measured.
    if (marks.all { mark -> mark.heartRateBpm == null }) {
        return HeartRateRecoveryQuality.NO_DATA
    }
    if (HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in issues) {
        return HeartRateRecoveryQuality.NOT_COMPARABLE
    }
    val headline = marks.firstOrNull { mark -> mark.offset == heartRateRecoveryHeadlineOffset }
    // Without the one-minute mark there is no anchor for a trend.
    val headlineMissing = headline?.heartRateBpm == null
    if (headlineMissing ||
        HeartRateRecoveryIssue.PEAK_FROM_SINGLE_SAMPLE in issues ||
        HeartRateRecoveryIssue.UNKNOWN_MAX_HEART_RATE in issues
    ) {
        return HeartRateRecoveryQuality.APPROXIMATE
    }
    return HeartRateRecoveryQuality.CLEAN
}

/**
 * Samples in time order, at most one per instant. Two sources can share an
 * instant; the higher reading is the conservative choice both ways.
 */
private fun ordered(samples: List<HeartRateSample>): List<HeartRateSample> {
    val byInstant = mutableMapOf<Long, HeartRateSample>()
    for (sample in samples) {
        val key = sample.time.toEpochMilli()
        val existing = byInstant[key]
        if (existing == null || sample.beatsPerMinute > existing.beatsPerMinute) {
            byInstant[key] = sample
        }
    }
    return byInstant.values.sortedBy { it.time }
}

private class Peak(
    val bpm: Long,
    val time: Instant,
    val windowSeconds: Int,
    val sampleCount: Int,
)

/** The highest heart rate in the [peakWindow] before the stop, or null. */
private fun peak(ordered: List<HeartRateSample>, recoveryStart: Instant): Peak? {
    val start = recoveryStart.minus(peakWindow)
    val inWindow = ordered.filter { sample ->
        !sample.time.isBefore(start) && !sample.time.isAfter(recoveryStart)
    }
    if (inWindow.isEmpty()) return null
    var best = inWindow.first()
    for (sample in inWindow) {
        if (sample.beatsPerMinute > best.beatsPerMinute) best = sample
    }
    return Peak(
        best.beatsPerMinute,
        best.time,
        peakWindow.seconds.toInt(),
        inWindow.size,
    )
}

/** The highest reading in the [lookback] before [recoveryStart], or null. */
private fun maxBpmWithin(
    ordered: List<HeartRateSample>,
    recoveryStart: Instant,
    lookback: Duration,
): Long? {
    val start = recoveryStart.minus(lookback)
    var best: Long? = null
    for (sample in ordered) {
        if (sample.time.isBefore(start) || sample.time.isAfter(recoveryStart)) {
            continue
        }
        val current = best
        if (current == null || sample.beatsPerMinute > current) {
            best = sample.beatsPerMinute
        }
    }
    return best
}

private fun markAt(
    ordered: List<HeartRateSample>,
    recoveryStart: Instant,
    offset: Duration,
    peakBpm: Long,
): HeartRateRecoveryMark {
    val target = recoveryStart.plus(offset)
    val tolerance = heartRateRecoveryTolerances.getValue(offset)
    val sample = nearest(ordered, target, tolerance)
        ?: return HeartRateRecoveryMark(
            offset = offset,
            heartRateBpm = null,
            dropBpm = null,
            sampleTime = null,
            sampleSkew = null,
        )
    return HeartRateRecoveryMark(
        offset = offset,
        heartRateBpm = sample.beatsPerMinute,
        dropBpm = peakBpm - sample.beatsPerMinute,
        sampleTime = sample.time,
        sampleSkew = Duration.between(target, sample.time).abs(),
    )
}

/**
 * The sample nearest [target], or null beyond [tolerance]. A tie goes to the
 * earlier sample, which reports the smaller drop.
 */
private fun nearest(
    ordered: List<HeartRateSample>,
    target: Instant,
    tolerance: Duration,
): HeartRateSample? {
    var best: HeartRateSample? = null
    var bestSkew: Duration? = null
    for (sample in ordered) {
        val skew = Duration.between(target, sample.time).abs()
        if (skew > tolerance) continue
        if (bestSkew == null || skew < bestSkew) {
            best = sample
            bestSkew = skew
        }
    }
    return best
}

private class MaxHeartRate(
    val bpm: Int,
    val estimated: Boolean,
)

/**
 * What to measure the effort against, in order: the user's stated maximum,
 * a trustworthy observed maximum, the age formula, nothing. Nothing still
 * yields every mark; only the effort judgement is lost.
 */
private fun resolveMaxHeartRate(
    explicitMaxHeartRateBpm: Int?,
    observedMaxHeartRateBpm: Int?,
    restingHeartRateBpm: Int?,
    ageYears: Int?,
): MaxHeartRate? {
    if (explicitMaxHeartRateBpm != null) {
        return MaxHeartRate(explicitMaxHeartRateBpm, false)
    }

    val observed = observedMaxHeartRateBpm
    if (observed != null) {
        val trustworthy = if (restingHeartRateBpm != null) {
            isObservedMaxHeartRateTrustworthy(observed, restingHeartRateBpm)
        } else {
            observed >= observedMaxHeartRateMinimumBpm
        }
        if (trustworthy) return MaxHeartRate(observed, false)
    }

    val age = ageYears
    if (age != null) {
        // Tanaka (208 - 0.7*age).
        return MaxHeartRate(max(1, (208 - 0.7 * age).roundToInt()), true)
    }

    return null
}
