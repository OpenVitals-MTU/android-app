package tech.mmarca.openvitals.data.repository

import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyBucketEntity
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyBucketRetentionDays
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyChainCursorKey
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyDayEntity
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyTimelineDao
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyCalibrationMode
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.insights.bodyEnergyReasonCodeForText

/**
 * How long a past day stays eligible for recomputation.
 *
 * Watches sync late and Health Connect back-fills, so a recent day can still
 * gain data and is worth recomputing. A day beyond this window cannot: nothing
 * new will arrive for it, recomputing costs ~8 Health Connect reads to reproduce
 * what is already stored, and — for a user who declined the history grant, which
 * Android cannot offer from the in-app dialog — Health Connect serves only ~30
 * days, so the recompute can come back empty and take the stored day with it.
 *
 * Seven days rather than two or three because a Garmin watch is often synced
 * only once a week, and one sync back-fills every day it covers.
 */
const val BodyEnergyChainSettlingDays = 7L

/**
 * Room-backed storage for the Body Energy chain: the day summaries whose end
 * scores seed each following day, and the 5-minute buckets behind them.
 *
 * Replaces the SharedPreferences timeline cache. That store encoded a whole day
 * as one delimited string, so answering "what did yesterday end on" — the
 * question the chain asks constantly — meant decoding 288 points, and a range
 * query for a multi-day view was impossible.
 *
 * The [BodyEnergyTimelineDao] speaks rows; the repository speaks
 * [BodyEnergyTimeline]. That mapping lives here so the repository stays about
 * the chain rather than about columns.
 */
@Singleton
open class BodyEnergyTimelineStore @Inject constructor(
    private val dao: BodyEnergyTimelineDao,
) {

    /**
     * The stored timeline for [date], or null when nothing is stored or the
     * stored row was computed under a different [signature].
     */
    open suspend fun load(date: LocalDate, signature: String): BodyEnergyTimeline? {
        val day = dao.day(date.toEpochDay()) ?: return null
        if (day.signature != signature) return null
        return day.toTimeline(dao.bucketsForDay(date.toEpochDay()))
    }

    /** Persists [timeline], replacing whatever was stored for its date. */
    open suspend fun save(timeline: BodyEnergyTimeline) {
        // An unsigned timeline cannot be validated on read, so storing it would
        // only produce a row every future read discards (the prefs store had the
        // same guard).
        if (timeline.signature.isBlank()) return
        val epochDay = timeline.date.toEpochDay()
        dao.upsertDay(
            timeline.toDayEntity(epochDay),
            timeline.points.map { it.toBucketEntity(epochDay) },
        )
    }

    /**
     * The chain-relevant facts for `[start, end]`, oldest first — one query, no
     * bucket decoding. The walk-back reads its whole lookback window with this.
     */
    open suspend fun storedDaysBetween(start: LocalDate, end: LocalDate): List<BodyEnergyStoredDay> =
        dao.daysBetween(start.toEpochDay(), end.toEpochDay()).map { row ->
            BodyEnergyStoredDay(
                date = LocalDate.ofEpochDay(row.epochDay),
                signature = row.signature,
                startScore = row.startScore,
                endScore = row.endScore,
                generatedAt = Instant.ofEpochMilli(row.generatedAtMillis),
            )
        }

    /**
     * Whether a stored day still has buckets behind it.
     *
     * Signature-independent on purpose: the caller is protecting the *shape* of
     * a day it may no longer be able to re-read, and a calibration edit is no
     * reason to discard that.
     */
    open suspend fun hasStoredPoints(date: LocalDate): Boolean =
        dao.countBucketsForDay(date.toEpochDay()) > 0

    /**
     * Forward ripple: every day in `[from, to]` was computed from a seed that no
     * longer holds, so drop them and let them be recomputed.
     */
    open suspend fun invalidateForward(from: LocalDate, to: LocalDate) {
        dao.deleteDays(from.toEpochDay(), to.toEpochDay())
    }

    /** Drops the whole chain and its cursor — the calibration/algorithm reset. */
    open suspend fun purgeAll() {
        dao.purgeAll()
    }

    /**
     * Drops buckets older than [BodyEnergyBucketRetentionDays], keeping the day
     * summaries so the chain stays walkable.
     */
    open suspend fun applyRetention(today: LocalDate) {
        dao.purgeBucketsBefore(today.minusDays(BodyEnergyBucketRetentionDays).toEpochDay())
    }

    /**
     * The global signature the stored chain was built under, or null when the
     * chain has never been synced.
     */
    open suspend fun storedGlobalSignature(): String? =
        dao.cursor(BodyEnergyChainCursorKey)?.changesToken

    open suspend fun writeGlobalSignature(signature: String) {
        dao.writeChainCursor(globalSignature = signature, lastPassMillis = null)
    }

    open suspend fun lastPassAt(): Instant? =
        dao.cursor(BodyEnergyChainCursorKey)?.lastFullSyncMillis?.let(Instant::ofEpochMilli)

    open suspend fun writeLastPassAt(at: Instant) {
        dao.writeChainCursor(globalSignature = null, lastPassMillis = at.toEpochMilli())
    }
}

/**
 * A stored day's chain-relevant facts.
 *
 * Deliberately not a [BodyEnergyTimeline]: the walk-back asks for a fortnight of
 * these on a cold screen open and must not pay a bucket read per day to learn an
 * end score.
 */
data class BodyEnergyStoredDay(
    val date: LocalDate,
    val signature: String,
    val startScore: Int,
    val endScore: Int,
    val generatedAt: Instant,
)

private fun BodyEnergyDayEntity.toTimeline(
    buckets: List<BodyEnergyBucketEntity>,
): BodyEnergyTimeline =
    BodyEnergyTimeline(
        date = LocalDate.ofEpochDay(epochDay),
        startScore = startScore,
        currentScore = endScore,
        charged = charged,
        drained = drained,
        points = buckets.map { it.toPoint() },
        confidence = confidence.toEnumOrNull<BodyEnergyConfidence>() ?: BodyEnergyConfidence.NO_DATA,
        confidenceReason = confidenceReason,
        // Rows written before reason codes existed carry only the English
        // sentence; the mapping back is exact, and anything unrecognised stays
        // LEGACY and renders as stored.
        confidenceReasonCode = bodyEnergyReasonCodeForText(confidenceReason),
        inputSummary = BodyEnergyInputSummary(
            algorithmVersion = algorithmVersion,
            bucketMinutes = bucketMinutes,
            heartRateSampleCount = heartRateSampleCount,
            hrvSampleCount = hrvSampleCount,
            sleepSessionCount = sleepSessionCount,
            workoutCount = workoutCount,
            respiratorySampleCount = respiratorySampleCount,
            hasRestingHeartRate = hasRestingHeartRate,
            hasBaselineRestingHeartRate = hasBaselineRestingHeartRate,
            hasObservedMaxHeartRate = hasObservedMaxHeartRate,
            hasHrvBaseline = hasHrvBaseline,
            hasRespiratoryBaseline = hasRespiratoryBaseline,
            previousEndScore = previousEndScore,
            carryOverFloorApplied = carryOverFloorApplied,
            seedSource = seedSource.toEnumOrNull<BodyEnergySeedSource>() ?: BodyEnergySeedSource.NEUTRAL,
            calibrationMode = calibrationMode.toEnumOrNull<BodyEnergyCalibrationMode>() ?: BodyEnergyCalibrationMode.AUTOMATIC,
        ),
        generatedAt = Instant.ofEpochMilli(generatedAtMillis),
        signature = signature,
    )

private fun BodyEnergyBucketEntity.toPoint(): BodyEnergyTimelinePoint =
    BodyEnergyTimelinePoint(
        time = Instant.ofEpochMilli(timeMillis),
        score = score,
        delta = delta,
        state = state.toEnumOrNull<BodyEnergyBucketState>() ?: BodyEnergyBucketState.UNMEASURABLE,
        confidence = confidence.toEnumOrNull<BodyEnergyConfidence>() ?: BodyEnergyConfidence.NO_DATA,
        charge = charge,
        intensityDrain = intensityDrain,
        activityEnergyDrain = activityEnergyDrain,
        basalDrain = basalDrain,
        stressDrain = stressDrain,
        recoveryDebtDrain = recoveryDebtDrain,
        primaryInfluence = primaryInfluence.toEnumOrNull<BodyEnergyPrimaryInfluence>() ?: BodyEnergyPrimaryInfluence.STEADY,
    )

private fun BodyEnergyTimeline.toDayEntity(epochDay: Long): BodyEnergyDayEntity =
    BodyEnergyDayEntity(
        epochDay = epochDay,
        signature = signature,
        startScore = startScore,
        endScore = currentScore,
        charged = charged,
        drained = drained,
        confidence = confidence.name,
        confidenceReason = confidenceReason,
        generatedAtMillis = generatedAt.toEpochMilli(),
        algorithmVersion = inputSummary.algorithmVersion,
        bucketMinutes = inputSummary.bucketMinutes,
        heartRateSampleCount = inputSummary.heartRateSampleCount,
        hrvSampleCount = inputSummary.hrvSampleCount,
        sleepSessionCount = inputSummary.sleepSessionCount,
        workoutCount = inputSummary.workoutCount,
        respiratorySampleCount = inputSummary.respiratorySampleCount,
        hasRestingHeartRate = inputSummary.hasRestingHeartRate,
        hasBaselineRestingHeartRate = inputSummary.hasBaselineRestingHeartRate,
        hasObservedMaxHeartRate = inputSummary.hasObservedMaxHeartRate,
        hasHrvBaseline = inputSummary.hasHrvBaseline,
        hasRespiratoryBaseline = inputSummary.hasRespiratoryBaseline,
        previousEndScore = inputSummary.previousEndScore,
        carryOverFloorApplied = inputSummary.carryOverFloorApplied,
        seedSource = inputSummary.seedSource.name,
        calibrationMode = inputSummary.calibrationMode.name,
    )

private fun BodyEnergyTimelinePoint.toBucketEntity(epochDay: Long): BodyEnergyBucketEntity =
    BodyEnergyBucketEntity(
        epochDay = epochDay,
        timeMillis = time.toEpochMilli(),
        score = score,
        delta = delta,
        state = state.name,
        confidence = confidence.name,
        charge = charge,
        intensityDrain = intensityDrain,
        activityEnergyDrain = activityEnergyDrain,
        basalDrain = basalDrain,
        stressDrain = stressDrain,
        recoveryDebtDrain = recoveryDebtDrain,
        primaryInfluence = primaryInfluence.name,
    )

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(this) }.getOrNull()
