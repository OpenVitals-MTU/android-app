package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample

/*
 * Heart-rate recovery: how far the heart rate falls in the minutes after hard
 * effort stops. A fitter heart falls faster.
 *
 * It is only meaningful when the heart rate was driven near its maximum and effort
 * then stopped ABRUPTLY. Ease off gradually — slow down but keep moving — and the
 * number is a lie. So HRR is measured only for the app's guided recovery test, which
 * marks the instant of cessation with a trailing rest segment; an ordinary workout,
 * which carries no such mark, is not measured at all (see [heartRateRecoveryWindowFor]).
 *
 * Health Connect has no record type for it, and the app stores no health data of its
 * own, so nothing here is persisted: HRR is DERIVED, on read, from the heart-rate
 * samples Health Connect already holds — the same standing as cardio load or stress.
 * A mark for which no sample exists is reported as absent: it is never interpolated,
 * and the drop is never guessed from an average. A number that was not measured is
 * worse than a blank.
 */

/**
 * The marks, in the order they are always returned.
 *
 * No 10-second mark: optical sensors smooth over several seconds and even an
 * arm/chest strap is borderline that early, so a ten-second figure is unreliable
 * across the monitors people actually wear. The one-minute drop leads (it is the
 * mark with a body of normative literature behind it) and is fine for every
 * monitor type.
 */
val heartRateRecoveryOffsets: List<Duration> = listOf(
    Duration.ofSeconds(30),
    Duration.ofMinutes(1),
    Duration.ofMinutes(2),
    Duration.ofMinutes(3),
    Duration.ofMinutes(4),
    Duration.ofMinutes(5),
)

/**
 * The headline mark — the one-minute drop, the only mark with a body of normative
 * literature behind it.
 */
val heartRateRecoveryHeadlineOffset: Duration = Duration.ofMinutes(1)

/**
 * How far from a mark a sample may sit and still be taken as that mark.
 *
 * Kept tight: heart rate falls fast right after cessation (roughly 0.5-1.0 bpm/s
 * in the first half minute), so a loose window at 30s could cost several bpm — a
 * large fraction of the number reported. Monitors sample often enough while and
 * just after hard effort that a small window still finds a sample.
 */
val heartRateRecoveryTolerances: Map<Duration, Duration> = mapOf(
    Duration.ofSeconds(30) to Duration.ofSeconds(3),
    Duration.ofMinutes(1) to Duration.ofSeconds(5),
    Duration.ofMinutes(2) to Duration.ofSeconds(5),
    Duration.ofMinutes(3) to Duration.ofSeconds(5),
    Duration.ofMinutes(4) to Duration.ofSeconds(5),
    Duration.ofMinutes(5) to Duration.ofSeconds(5),
)

/**
 * The peak heart rate must come from a HARD window of the last ten seconds before
 * the stop. A wider window would let an effort that eased off earlier read a peak
 * from when it was still going, inflating the recovery. Monitors sample fast during
 * hard effort, so a sample is there.
 */
private val peakWindow: Duration = Duration.ofSeconds(10)

/**
 * How far either side of the recovery start a sample may sit and still count as "the
 * heart rate when they stopped", for the cool-down check.
 */
private val recoveryStartTolerance: Duration = Duration.ofSeconds(15)

/**
 * A fall of more than this between the last real high point and the stop means the
 * heart rate was ALREADY coming down before the "stop" — they eased off before they
 * pressed the button, which invalidates the recovery. Beat-to-beat noise is 3-4 bpm,
 * so 4 sits just above it: a genuine pre-stop cool-down of even a few beats matters.
 */
private const val cooldownBeforeStopDropBpm = 4

/** How far back the cool-down check looks for that high point. */
private val cooldownLookback: Duration = Duration.ofSeconds(60)

/**
 * How far below the maximum the peak may sit and the effort still count as
 * near-maximal (so the recovery is comparable with another). A fixed BAND, not a
 * fraction: HR-max estimates carry a roughly constant absolute uncertainty, so a
 * percentage floor is too low for the young and too high for the old.
 *
 * [estimatedMaxNearBandBpm] is the ~95% confidence interval of the age formula
 * (208 - 0.7*age): a peak within ~22 bpm of the estimate is consistent with a
 * near-maximal effort. When the maximum is KNOWN (the user stated it, or we have a
 * trustworthy observed max) there is no such uncertainty, so the band is tighter.
 */
private const val estimatedMaxNearBandBpm = 22
private const val knownMaxNearBandBpm = 10

/**
 * A trailing rest segment shorter than this is an inter-set breather, not a recovery.
 *
 * This matters concretely: the app already writes a rest segment after EVERY set of a
 * strength session, including the last. Without a floor, every set-based workout ever
 * recorded would be read as an HRR test whose "recovery" was a one-minute rest.
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
    /**
     * Near-maximal effort, a peak taken close to the stop, and at least the one-minute
     * mark present.
     */
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
    /**
     * No samples at all in the five minutes after the stop. Typically a watch that
     * stopped recording heart rate when the workout ended.
     */
    NO_RECOVERY_SAMPLES,

    /** Exactly one sample stood behind the peak; a single spurious reading would be it. */
    PEAK_FROM_SINGLE_SAMPLE,

    /**
     * The heart rate was already falling before the stop — they eased off first, so the
     * "drop" measures the cool-down and flatters them.
     */
    COOLDOWN_BEFORE_STOP,

    /** Hard, but not near-maximal. The drop is real; it is not comparable. */
    SUBMAXIMAL_EFFORT,

    /** No maximum heart rate could be resolved, so effort could not be judged. */
    UNKNOWN_MAX_HEART_RATE,

    /**
     * The heart rate did not fall after the "stop" — it was the same or higher at one of
     * the marks. Whatever ended, the effort did not: the recording stopped before the
     * person did. There is no recovery here, only a session boundary.
     */
    HEART_RATE_DID_NOT_FALL,
}

/**
 * One mark. [heartRateBpm] is null when no sample fell within tolerance — the mark
 * did not happen, and is never invented.
 */
data class HeartRateRecoveryMark(
    val offset: Duration,
    val heartRateBpm: Long?,
    val dropBpm: Long?,
    val sampleTime: Instant?,

    /** How far the sample actually sat from the mark. Lets the UI be honest: "+58s". */
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

    /**
     * The drop one minute after the stop — the figure to lead with, null when that mark
     * was not measured.
     */
    val headlineDropBpm: Long? get() = markAt(heartRateRecoveryHeadlineOffset)?.dropBpm

    fun markAt(offset: Duration): HeartRateRecoveryMark? {
        for (mark in marks) {
            if (mark.offset == offset) return mark
        }
        return null
    }

    /**
     * Whether this reading may be charted as a point in a trend.
     *
     * Being merely "not invalid" is not enough. The trend is of the one-minute fall, so a
     * reading that never measured it has nothing to contribute — and on watch data, which
     * commonly samples once a minute, that is most of them. Charting them would be
     * charting the gaps.
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
 * The instant effort stopped, for [session], and the window of heart-rate samples that
 * has to be read to measure the recovery from it — or null when the session carries no
 * mark of a deliberate stop.
 *
 * Heart-rate recovery is only meaningful when the person drove their heart rate near
 * its maximum and then ABRUPTLY stopped and rested. An ordinary recorded session gives
 * no such guarantee — you slow down but keep moving — so the session's end cannot be
 * taken as the moment effort stopped. The recovery therefore begins only at a
 * qualifying trailing rest segment, which the app's guided test writes at the true
 * instant of cessation (a watch that genuinely recorded a trailing rest qualifies too).
 * No segment, no reading.
 *
 * "Qualifying" is doing real work. The app writes a rest segment after every set of a
 * strength session, the last one included, so a bare "ends with a rest segment" test
 * would read every set-based workout as an HRR test with a one-minute recovery. A
 * segment therefore qualifies only if it is at least [minimumRecoverySegmentDuration]
 * long AND ends within [trailingSegmentSlack] of the session end.
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
        // The last qualifying one wins, so a session that somehow carries two takes the
        // one nearest the end.
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
 * Measures the recovery from [recoveryStart] out of [samples].
 *
 * [samples] should span the window [heartRateRecoveryWindowFor] asked for; anything
 * outside it is ignored. Nothing is invented: a mark with no sample within tolerance
 * comes back null.
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

    // Strictly AFTER the stop. A sample landing exactly on it is the reading at cessation
    // — the thing we measure the fall FROM — not part of the fall. Counting it would let a
    // watch that quits the moment the workout ends look as though it had recorded a
    // recovery, when it recorded nothing at all.
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

    // Near-maximal effort, judged as an absolute distance below the maximum, not a
    // fraction of it — a fixed band, wider when the maximum was estimated from age
    // (which carries that much uncertainty) than when it is known. A peak more than the
    // band below the maximum is a real recovery from a submaximal effort: shown, but not
    // comparable across days.
    val peakFraction = if (maxContext == null) null else peak.bpm.toDouble() / maxContext.bpm
    if (maxContext != null) {
        val band = if (maxContext.estimated) estimatedMaxNearBandBpm else knownMaxNearBandBpm
        if (peak.bpm < maxContext.bpm - band) {
            issues.add(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT)
        }
    }

    // Was the heart rate already coming down before they "stopped"?
    //
    // Compare against the highest reading of the last MINUTE, not against [peak]. When the
    // peak window is the default ten seconds, peak is drawn from those ten seconds alone —
    // and someone who eased off forty seconds before pressing stop has nothing but decayed
    // values in there, so peak would sit just above the reading at the stop and the check
    // could never fire. It is the fall from the last real high point that gives them away.
    val atStop = nearest(ordered, recoveryStart, recoveryStartTolerance)
    val recentHigh = maxBpmWithin(ordered, recoveryStart, cooldownLookback)
    if (atStop != null &&
        recentHigh != null &&
        recentHigh - atStop.beatsPerMinute > cooldownBeforeStopDropBpm
    ) {
        issues.add(HeartRateRecoveryIssue.COOLDOWN_BEFORE_STOP)
    }

    // Did the heart rate fall at all? If it was as high or higher at any mark than it was
    // at the peak, then whatever the session end was, it was not the end of the effort —
    // the recording stopped while the rider kept riding. A "recovery" of MINUS four beats
    // is not a small recovery, it is not a recovery, and reporting it as one would be the
    // worst thing this code could do.
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
    // Samples after the stop, but none of them near enough to any mark to be one. Nothing
    // was measured, so the verdict is nothing measured — not "approximate".
    if (marks.all { mark -> mark.heartRateBpm == null }) {
        return HeartRateRecoveryQuality.NO_DATA
    }
    if (HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in issues) {
        return HeartRateRecoveryQuality.NOT_COMPARABLE
    }
    val headline = marks.firstOrNull { mark -> mark.offset == heartRateRecoveryHeadlineOffset }
    // Without the one-minute mark there is no anchor for a trend, so the reading must not
    // be dressed up as authoritative however good the rest of it looks.
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
 * Samples in time order, at most one per instant.
 *
 * Two sources (a strap and a watch, both recording) can land a sample on the same
 * instant. Keeping the higher of the two is the conservative choice in both directions:
 * a higher peak is harder to clear the vigour gate with, and a higher recovery reading
 * means a SMALLER reported drop. It also keeps the median gap honest — left in, the
 * duplicates read as zero-second gaps and would mask coarse sampling.
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

/**
 * The highest heart rate in the hard [peakWindow] before the stop, or null if nothing
 * sits there.
 *
 * A hard ten-second window on purpose: a wider one would let an effort that eased off
 * earlier draw its "peak" from when it was still going, inflating the recovery. The
 * guided test is the only thing that reaches this code now, and monitors sample fast
 * during hard effort, so a sample is there.
 */
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

/**
 * The highest reading in the [lookback] before [recoveryStart], or null if there is
 * nothing there.
 */
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
 * The sample nearest [target], or null if the nearest is further than [tolerance].
 *
 * A tie goes to the EARLIER sample: deterministic, and the conservative call, since the
 * earlier sample of a falling curve is the higher one and so reports the smaller drop.
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
 * What to measure the effort against.
 *
 * In order: what the user told us; then the highest we have actually seen, but only if
 * it clears the bar for being a real maximum rather than the ceiling of an easy week
 * ([isObservedMaxHeartRateTrustworthy]); then the age formula; then nothing.
 *
 * Nothing is a legitimate outcome, and it must not blank the screen: a user who never
 * filled in a birth year still gets every mark, and only loses the judgement of whether
 * the effort was hard enough to compare.
 *
 * Kotlin adaptation: unlike the Dart source, this app kept the manual max-heart-rate
 * field on BodyProfile, so an explicit user-stated maximum resolves first. It is taken
 * as KNOWN (non-estimated) without the trustworthy check — the user stated it, it is
 * not the ceiling of an easy week.
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
        // Tanaka (208 - 0.7*age): more accurate across ages than the old 220 - age.
        return MaxHeartRate(max(1, (208 - 0.7 * age).roundToInt()), true)
    }

    return null
}
