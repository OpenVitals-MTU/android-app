package tech.mmarca.openvitals.data.repository

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineAlgorithmVersion
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineInputs
import tech.mmarca.openvitals.domain.insights.BodyEnergyWatchFitEpoch
import tech.mmarca.openvitals.domain.insights.calculateBodyEnergyTimeline
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/**
 * Builds the per-day Body Energy timeline from the heart, sleep, activity and
 * vitals repositories.
 *
 * Body Energy is a chain: each day opens where the previous one closed, so
 * the stored end score is an input to the next day. [resolveSeed] makes that
 * true. Baselines live in SharedPreferences, day timelines in Room.
 */
@Singleton
class BodyEnergyRepositoryImpl(
    private val heartRepository: HeartRepository,
    private val sleepRepository: SleepRepository,
    private val activityRepository: ActivityRepository,
    private val vitalsRepository: VitalsRepository,
    private val bodyRepository: BodyRepository,
    private val healthRepository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val baselineCacheStore: BodyEnergyBaselineCacheStore,
    /** Nullable for contexts that must not open Room. Then the chain uses the prefs mirror. */
    private val timelineStore: BodyEnergyTimelineStore?,
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : BodyEnergyRepository {

    @Inject
    constructor(
        heartRepository: HeartRepository,
        sleepRepository: SleepRepository,
        activityRepository: ActivityRepository,
        vitalsRepository: VitalsRepository,
        bodyRepository: BodyRepository,
        healthRepository: HealthRepository,
        preferencesRepository: PreferencesRepository,
        baselineCacheStore: BodyEnergyBaselineCacheStore,
        timelineStore: BodyEnergyTimelineStore,
    ) : this(
        heartRepository = heartRepository,
        sleepRepository = sleepRepository,
        activityRepository = activityRepository,
        vitalsRepository = vitalsRepository,
        bodyRepository = bodyRepository,
        healthRepository = healthRepository,
        preferencesRepository = preferencesRepository,
        baselineCacheStore = baselineCacheStore,
        timelineStore = timelineStore,
        now = Instant::now,
    )

    override suspend fun loadTimeline(query: BodyEnergyTimelineQuery): BodyEnergyTimelineResult {
        // Here, not in the chain sync service: this is the one path every computation takes.
        resetGainsIfAlgorithmChanged()

        val context = ChainContext(
            calibration = preferencesRepository.bodyEnergyCalibration(),
            bodyProfile = preferencesRepository.bodyProfile(),
            permissionSignature = permissionSignature(),
        )

        // Sequential: each day threads its end score forward. Within-day reads run concurrently.
        val days = mutableListOf<BodyEnergyTimeline>()
        var date = query.period.start
        var carried: ChainSeed? = null
        while (!date.isAfter(query.period.end)) {
            val day = loadDay(
                date = date,
                refreshMode = query.refreshMode,
                context = context,
                seedOverride = carried,
            )
            days += day
            carried = ChainSeed.carried(day.currentScore)
            date = date.plusDays(1)
        }
        return BodyEnergyTimelineResult(query = query, days = days)
    }

    private suspend fun loadDay(
        date: LocalDate,
        refreshMode: RefreshMode,
        context: ChainContext,
        seedOverride: ChainSeed?,
    ): BodyEnergyTimeline {
        val signature = signatureFor(date, context)

        val cached = timelineStore?.load(date, signature)
        if (cached != null &&
            refreshMode == RefreshMode.NORMAL &&
            cacheIsUsable(cached) &&
            !timelineIsStale(cached, date)
        ) {
            return cached
        }

        // A forced refresh applies to the requested day only.
        val seed = seedOverride ?: resolveSeed(date, context)
        return computeDay(
            date = date,
            context = context,
            seed = seed,
            // Only a past day can invalidate the days after it.
            rippleForward = date.isBefore(today()),
        )
    }

    /** The score [date] opens on: the previous day's end. One SQLite query on the warm path. */
    private suspend fun resolveSeed(date: LocalDate, context: ChainContext): ChainSeed {
        val store = timelineStore ?: return seedFromMirror(date)

        val window = store.storedDaysBetween(
            date.minusDays(ChainLookbackDays),
            date.minusDays(1),
        )
        // No history at all degrades to the prefs mirror. Distinct from anchor == null
        // below, which is a deliberate chain break the mirror must not undo.
        if (window.isEmpty()) return seedFromMirror(date)
        val byEpochDay = window.associateBy { it.date.toEpochDay() }

        // The newest stored day before `date` whose CHAIN signature validates against
        // its own date. Not the full signature: nudged gains still make an honest carry.
        var anchor: BodyEnergyStoredDay? = null
        for (back in 1..ChainLookbackDays) {
            val candidate = date.minusDays(back)
            val stored = byEpochDay[candidate.toEpochDay()]
            if (stored != null &&
                chainPartOf(stored.signature) == chainSignatureFor(candidate, context)
            ) {
                anchor = stored
                break
            }
        }

        // Rows exist but none validates: a deliberate chain break. Seed neutral.
        if (anchor == null) return ChainSeed.Neutral

        val gap = date.toEpochDay() - anchor.date.toEpochDay() - 1
        if (gap == 0L) return ChainSeed.carried(anchor.endScore)

        if (gap > ChainForegroundFillDays) {
            // Too wide to close in budget: start neutral and say so. The mirror can
            // still rescue the seed when it holds exactly yesterday's score.
            return mirrorSeedOr(date, ChainSeed.ChainGap)
        }

        // Close the gap forward, oldest first. A failing gap day must not fail the
        // requested day.
        val filled = try {
            withTimeoutOrNull(ChainFillBudgetMillis) {
                fillGap(
                    from = anchor.date.plusDays(1),
                    until = date,
                    seed = ChainSeed.carried(anchor.endScore),
                    context = context,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }
        // A failed fill degrades to the mirror before it degrades to a gap.
        return filled ?: mirrorSeedOr(date, ChainSeed.ChainGap)
    }

    /** [seedFromMirror] when it genuinely carries a score, else [fallback]. */
    private fun mirrorSeedOr(date: LocalDate, fallback: ChainSeed): ChainSeed {
        val mirrored = seedFromMirror(date)
        return if (mirrored.score != null) mirrored else fallback
    }

    /** Computes and persists `[from, until)`, returning the seed the requested day opens on. */
    private suspend fun fillGap(
        from: LocalDate,
        until: LocalDate,
        seed: ChainSeed,
        context: ChainContext,
    ): ChainSeed {
        var carried = seed
        var date = from
        while (date.isBefore(until)) {
            val day = computeDay(
                date = date,
                context = context,
                seed = carried,
                rippleForward = false,
            )
            carried = ChainSeed.carried(day.currentScore)
            date = date.plusDays(1)
        }
        return carried
    }

    /**
     * Resets the learned gains once when the algorithm changes. A gain fitted
     * under an older model can suppress what the new one introduced. Manual
     * zones, the profile and the setup flag are untouched.
     */
    private fun resetGainsIfAlgorithmChanged() {
        rewindWatchFitIfEpochChanged()
        if (preferencesRepository.bodyEnergyGainsAlgorithmVersion == BodyEnergyTimelineAlgorithmVersion) {
            return
        }
        val current = preferencesRepository.bodyEnergyCalibration()
        if (current.hasPersonalGains) {
            preferencesRepository.setBodyEnergyCalibration(
                current.copy(
                    sleepChargeGain = 1.0,
                    activityDrainGain = 1.0,
                    basalDrainGain = 1.0,
                    stressDrainGain = 1.0,
                    watchObservationCount = 0,
                )
            )
        }
        preferencesRepository.bodyEnergyGainsAlgorithmVersion = BodyEnergyTimelineAlgorithmVersion
    }

    /**
     * Rewinds the watch fit watermark once per [BodyEnergyWatchFitEpoch], so a
     * relearn has evidence to read. Separate from the algorithm version, which
     * would discard the stored chain.
     */
    private fun rewindWatchFitIfEpochChanged() {
        if (preferencesRepository.bodyEnergyWatchFitEpoch == BodyEnergyWatchFitEpoch) return
        preferencesRepository.bodyEnergyWatchFitWatermarkMillis = 0L
        preferencesRepository.bodyEnergyWatchFitEpoch = BodyEnergyWatchFitEpoch
    }

    /**
     * The store-less fallback: the mirrored scores, accepted for the day before
     * [date] or for [date] itself, so a rescue survives its own recompute.
     * A neutrally opened day reopens as a fresh Neutral.
     */
    private fun seedFromMirror(date: LocalDate): ChainSeed {
        val encoded = preferencesRepository.bodyEnergyChainSeedMirror ?: return ChainSeed.Neutral
        val parts = encoded.split("|")
        if (parts.size < 2) return ChainSeed.Neutral
        val epochDay = parts[0].toLongOrNull() ?: return ChainSeed.Neutral
        val endScore = parts[1].toIntOrNull() ?: return ChainSeed.Neutral
        if (epochDay == date.minusDays(1).toEpochDay()) return ChainSeed.carried(endScore)
        if (epochDay == date.toEpochDay() && parts.size >= 4 && parts[3] == "1") {
            val startScore = parts[2].toIntOrNull() ?: return ChainSeed.Neutral
            return ChainSeed.carried(startScore)
        }
        return ChainSeed.Neutral
    }

    private suspend fun computeDay(
        date: LocalDate,
        context: ChainContext,
        seed: ChainSeed,
        rippleForward: Boolean,
    ): BodyEnergyTimeline = coroutineScope {
        val signature = signatureFor(date, context)
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        val baselineStart = date.minusDays(BaselineDays)
        val baselineEnd = date.minusDays(1)

        // Independent reads run concurrently. Only respiratory depends on the baseline.
        val baselinesJob = async {
            loadBaselines(
                date = date,
                baselineStart = baselineStart,
                baselineEnd = baselineEnd,
                dayStart = dayStart,
                signature = baselineSignature(context.permissionSignature),
            )
        }
        val heartRateJob = async { heartRepository.loadRawHeartRateSamplesForDayGraph(date) }
        val hrvJob = async { heartRepository.loadHrvSamples(dayStart, dayEnd) }
        val sleepJob = async { sleepRepository.loadSleepSessions(date.minusDays(1), date) }
        val workoutsJob = async { activityRepository.loadWorkouts(date, date) }
        // Energy-balance inputs the heart-rate-zone model alone was missing.
        val activityProgressJob = async { activityRepository.loadActivityProgress(date) }
        val basalMetabolicRateJob = async { bodyRepository.loadLatestBMR() }
        val restingJob = async { heartRepository.loadRestingHeartRate(date) }

        val baselines = baselinesJob.await()
        // Respiratory is only loaded when a respiratory baseline exists.
        val respiratoryJob: Deferred<List<RespiratoryRateEntry>>? =
            if (baselines.respiratoryRateBaseline != null) {
                async { vitalsRepository.loadRespiratoryRate(date, date) }
            } else {
                null
            }

        val heartRateSamples = heartRateJob.await()
        val hrvSamples = hrvJob.await()
        val sleepSessions = sleepJob.await()
        val workouts = workoutsJob.await()
        val activityProgress = activityProgressJob.await()
        val basalMetabolicRate = basalMetabolicRateJob.await()
        val restingHr = restingJob.await()
        val respiratory = respiratoryJob?.await().orEmpty()

        val timeline = withContext(dispatchers.default) {
            calculateBodyEnergyTimeline(
                BodyEnergyTimelineInputs(
                    date = date,
                    heartRateSamples = heartRateSamples,
                    hrvSamples = hrvSamples,
                    sleepSessions = sleepSessions,
                    workouts = workouts,
                    respiratoryRateSamples = respiratory,
                    activityProgress = activityProgress,
                    basalMetabolicRateKcalPerDay = basalMetabolicRate,
                    restingHeartRateBpm = restingHr,
                    baselineRestingHeartRateBpm = baselines.baselineRestingHeartRateBpm,
                    observedMaxHeartRateBpm = baselines.observedMaxHeartRateBpm,
                    hrvBaselineRmssdMs = baselines.hrvBaselineRmssdMs,
                    respiratoryRateBaseline = baselines.respiratoryRateBaseline,
                    previousEndScore = seed.score,
                    seedSource = seed.source,
                    calibration = context.calibration,
                    bodyProfile = context.bodyProfile,
                    now = now(),
                    zone = zone,
                )
            )
        }.copy(signature = signature, generatedAt = now())

        val store = timelineStore
        if (store != null) {
            // A recompute that found nothing must not replace a stored day: without
            // the history grant Health Connect serves only ~30 days, and save deletes
            // the day's buckets first.
            if (timeline.points.isEmpty() && store.hasStoredPoints(date)) {
                return@coroutineScope store.load(date, signature) ?: timeline
            }

            val stored = if (rippleForward) store.storedDaysBetween(date, date) else emptyList()
            val previousEnd = stored.firstOrNull()?.endScore
            store.save(timeline)
            // Ripple only when the end score moved.
            if (rippleForward && previousEnd != null && previousEnd != timeline.currentScore) {
                store.invalidateForward(date.plusDays(1), today())
            }
        }
        writeSeedMirror(timeline)
        timeline
    }

    /**
     * Mirrors the newest computed day, today included, as
     * `epochDay|endScore|startScore|chained`. Only moves forward.
     */
    private fun writeSeedMirror(timeline: BodyEnergyTimeline) {
        if (timeline.date.isAfter(today())) return
        val existing = preferencesRepository.bodyEnergyChainSeedMirror
        val existingEpochDay = existing?.substringBefore("|")?.toLongOrNull()
        if (existingEpochDay != null && existingEpochDay > timeline.date.toEpochDay()) return
        val chained = if (timeline.inputSummary.seedSource == BodyEnergySeedSource.CARRIED_OVER) 1 else 0
        preferencesRepository.bodyEnergyChainSeedMirror =
            "${timeline.date.toEpochDay()}|${timeline.currentScore}|${timeline.startScore}|$chained"
    }

    /** Reuse a fresh cached baseline (this or an adjacent day), else recompute and cache. */
    private suspend fun loadBaselines(
        date: LocalDate,
        baselineStart: LocalDate,
        baselineEnd: LocalDate,
        dayStart: Instant,
        signature: String,
    ): BodyEnergyBaselineCacheEntry = coroutineScope {
        val reusable = loadReusableBaseline(date, signature)
        if (reusable != null && !baselineIsStale(reusable)) return@coroutineScope reusable

        val baselineStartInstant = baselineStart.atStartOfDay(zone).toInstant()
        val baselineResting = async {
            heartRepository.loadDailyRestingHR(baselineStart, baselineEnd)
                .map { it.bpm }
                .filter { it > 0L }
                .medianLongOrNull()
        }
        // Observed max is taken over the whole baseline window.
        val observedMax = async {
            heartRepository.loadHeartRateSamples(baselineStartInstant, dayStart)
                .maxOfOrNull { it.beatsPerMinute }
        }
        val hrvBaseline = async {
            heartRepository.loadDailyHRV(baselineStart, baselineEnd)
                .map { it.rmssdMs }
                .filter { it > 0.0 }
                .medianDoubleOrNull()
        }
        val baseline = BodyEnergyBaselineCacheEntry(
            baselineRestingHeartRateBpm = baselineResting.await(),
            observedMaxHeartRateBpm = observedMax.await(),
            hrvBaselineRmssdMs = hrvBaseline.await(),
            respiratoryRateBaseline = reusable?.respiratoryRateBaseline,
            generatedAt = now(),
        )
        baselineCacheStore.saveBaseline(date, signature, baseline)
        baseline
    }

    private fun loadReusableBaseline(
        date: LocalDate,
        signature: String,
    ): BodyEnergyBaselineCacheEntry? {
        val exact = baselineCacheStore.loadBaseline(date, signature)
        if (exact != null && !baselineIsStale(exact)) return exact

        val adjacent = listOf(date.minusDays(1), date.plusDays(1))
            .firstNotNullOfOrNull { adjacentDate ->
                baselineCacheStore.loadBaseline(adjacentDate, signature)
                    ?.takeUnless { baselineIsStale(it) }
            }
        if (adjacent != null) {
            baselineCacheStore.saveBaseline(date, signature, adjacent)
        }
        return adjacent
    }

    /**
     * The granted-permission hash for chain signatures, or the last successful
     * one when the read fails. A failed read says nothing about permissions.
     */
    private suspend fun permissionSignature(): Int =
        runCatching {
            if (healthRepository.availability() == HealthConnectAvailability.AVAILABLE) {
                healthRepository.grantedPermissions()
                    .sorted()
                    .joinToString(",")
                    .hashCode()
                    .also { signature ->
                        if (preferencesRepository.bodyEnergyPermissionSignature != signature) {
                            preferencesRepository.bodyEnergyPermissionSignature = signature
                        }
                    }
            } else {
                null
            }
        }.getOrNull()
            ?: preferencesRepository.bodyEnergyPermissionSignature
            ?: 0

    /**
     * The signature a row for [date] is stored under. Always from the row's own
     * date: the profile signature varies by date.
     */
    private fun signatureFor(date: LocalDate, context: ChainContext): String =
        "${chainSignatureFor(date, context)}|${context.calibration.gainSignature().hashCode()}"

    /**
     * Everything a carry-over score depends on, gains left out. Folding gains
     * in invalidated every stored day on each learner nudge. Serving a cached
     * timeline still needs the full signature.
     */
    private fun chainSignatureFor(date: LocalDate, context: ChainContext): String {
        val combined = "${context.calibration.zoneSignature()}|${context.bodyProfile.signature(date)}"
        return "v$BodyEnergyTimelineAlgorithmVersion|${combined.hashCode()}|${context.permissionSignature}"
    }

    /** The chain part of a stored signature — everything before the gain hash. */
    private fun chainPartOf(signature: String): String {
        val cut = signature.lastIndexOf('|')
        return if (cut < 0) signature else signature.substring(0, cut)
    }

    private fun baselineSignature(permissionSignature: Int): String =
        "v$BodyEnergyTimelineAlgorithmVersion|baseline|$permissionSignature"

    /**
     * Whether [timeline] should be recomputed. Today re-reads every 15 minutes,
     * a day inside [BodyEnergyChainSettlingDays] daily, a settled day never.
     * A signature mismatch or [RefreshMode.FORCE] still rebuilds a settled day.
     */
    private fun timelineIsStale(timeline: BodyEnergyTimeline, date: LocalDate): Boolean {
        val instant = now()
        val age = Duration.between(timeline.generatedAt, instant)
        val today = instant.atZone(zone).toLocalDate()
        if (date == today) return age.toMinutes() >= CurrentDayCacheMinutes
        val daysOld = today.toEpochDay() - date.toEpochDay()
        if (daysOld > BodyEnergyChainSettlingDays) return false
        return age.toHours() >= PastDayCacheHours
    }

    /**
     * Whether a cached timeline still has its points. Retention purges buckets
     * but keeps the summary row. NO_DATA with no points is the exception.
     */
    private fun cacheIsUsable(cached: BodyEnergyTimeline): Boolean =
        cached.points.isNotEmpty() || cached.confidence == BodyEnergyConfidence.NO_DATA

    private fun baselineIsStale(baseline: BodyEnergyBaselineCacheEntry): Boolean =
        Duration.between(baseline.generatedAt, now()).toHours() >= BaselineCacheHours

    private fun today(): LocalDate = now().atZone(zone).toLocalDate()

    private companion object {
        const val BaselineDays = 28L
        const val CurrentDayCacheMinutes = 15L
        const val PastDayCacheHours = 24L
        const val BaselineCacheHours = 24L

        /** How far back a chain anchor is looked for. One SQLite query, cheap to raise. */
        const val ChainLookbackDays = 14L

        /**
         * Missing days the foreground load recomputes. Each day is ~8 Health
         * Connect reads; wider gaps are the chain sync service's job.
         */
        const val ChainForegroundFillDays = 2L

        /** Budget for the gap fill. A slow fill degrades to a neutral seed. */
        const val ChainFillBudgetMillis = 12_000L
    }
}

/** The per-load inputs every day in a chain walk shares. Only the date varies. */
private data class ChainContext(
    val calibration: BodyEnergyCalibration,
    val bodyProfile: BodyProfile,
    val permissionSignature: Int,
)

/** A resolved starting score, and where it came from. */
private data class ChainSeed(
    /** The previous day's end score, or null when there is nothing to carry. */
    val score: Int?,
    val source: BodyEnergySeedSource,
) {
    companion object {
        val Neutral = ChainSeed(null, BodyEnergySeedSource.NEUTRAL)
        val ChainGap = ChainSeed(null, BodyEnergySeedSource.CHAIN_GAP)
        fun carried(score: Int) = ChainSeed(score, BodyEnergySeedSource.CARRIED_OVER)
    }
}

private fun List<Long>.medianLongOrNull(): Long? {
    if (isEmpty()) return null
    val sorted = sorted()
    return sorted[sorted.lastIndex / 2]
}

private fun List<Double>.medianDoubleOrNull(): Double? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middle = sorted.lastIndex / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle] + sorted[middle + 1]) / 2.0
    } else {
        sorted[middle]
    }
}
