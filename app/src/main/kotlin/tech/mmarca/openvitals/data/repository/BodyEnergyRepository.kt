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
 * Composes the heart / sleep / activity / vitals repositories to build a per-day
 * body-energy timeline via [calculateBodyEnergyTimeline].
 *
 * Body Energy is a *chain*: each day opens where the previous one closed, so the
 * stored end score is an input to the next computation, not just a cache entry.
 * [resolveSeed] is what makes that true — the original port only ever read a
 * cached predecessor under TODAY's signature and, because the detail screen asks
 * for one day at a time, rarely found one, so days silently restarted at 50.
 *
 * The expensive 28-day baselines stay in SharedPreferences
 * ([BodyEnergyBaselineCacheStore]); the day timelines live in Room
 * ([BodyEnergyTimelineStore]).
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
    /**
     * Nullable so a context that must not open Room can still build a
     * repository. Without it the chain degrades to the prefs seed mirror (see
     * [seedFromMirror]).
     */
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
        // Here rather than in the chain sync service, which is only kicked by
        // the Body Energy screen — the dashboard, the widgets and the
        // diagnostics all reach the model without it, so the reset silently
        // never ran for them. This is the one path every computation goes
        // through.
        resetGainsIfAlgorithmChanged()

        val context = ChainContext(
            calibration = preferencesRepository.bodyEnergyCalibration(),
            bodyProfile = preferencesRepository.bodyProfile(),
            permissionSignature = permissionSignature(),
        )

        // The day loop stays sequential and threads the previous day's freshly
        // computed end score forward, so only `period.start` pays the walk-back.
        // The within-day reads are what run concurrently (see computeDay).
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

        // A forced refresh applies to the requested day only — the chain fill
        // below always uses the normal staleness rules. Recomputing a fortnight
        // of days because the user pulled to refresh is exactly the runaway the
        // fill bound exists to prevent.
        val seed = seedOverride ?: resolveSeed(date, context)
        return computeDay(
            date = date,
            context = context,
            seed = seed,
            // Only a past day can invalidate days after it; today has none.
            rippleForward = date.isBefore(today()),
        )
    }

    /**
     * The score [date] opens on: the previous day's end, chained.
     *
     * Costs ONE SQLite query on the warm path — yesterday stored and valid — and
     * no Health Connect read at all. That is what the day-summary table buys.
     */
    private suspend fun resolveSeed(date: LocalDate, context: ChainContext): ChainSeed {
        val store = timelineStore ?: return seedFromMirror(date)

        val window = store.storedDaysBetween(
            date.minusDays(ChainLookbackDays),
            date.minusDays(1),
        )
        val byEpochDay = window.associateBy { it.date.toEpochDay() }

        // The newest stored day strictly before `date` whose CHAIN signature
        // still validates against ITS OWN date's. Deliberately not the full
        // signature: a row computed under gains the learner has since nudged is
        // still an honest carry-over, and rejecting it strands the day on the
        // neutral 50.
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

        // Nothing stored in the window. Seeding neutral is correct here: there
        // is no previous day to be continuous with. The warm service builds
        // history so the next open is chained.
        if (anchor == null) return ChainSeed.Neutral

        val gap = date.toEpochDay() - anchor.date.toEpochDay() - 1
        if (gap == 0L) return ChainSeed.carried(anchor.endScore)

        if (gap > ChainForegroundFillDays) {
            // Too wide to close inside the read budget. Carrying a score from
            // over a week ago through a field the screen labels as the previous
            // day's would be a worse lie than an honest reset, so the day starts
            // neutral and says so. The warm service closes the gap for next
            // time.
            return ChainSeed.ChainGap
        }

        // Close the gap forward, oldest first, persisting each day so the next
        // open is warm. No forward ripple inside the fill: the days after each
        // one are exactly the days being written next.
        //
        // A gap day that times out or fails must not fail the day the user asked
        // for; it just leaves the chain broken until the warm service retries.
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
        return filled ?: ChainSeed.ChainGap
    }

    /**
     * Computes and persists `[from, until)` so the requested day has a stored
     * predecessor, returning the seed it should open on.
     */
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
     * Returns the learned gains to neutral once, when the algorithm they were
     * fitted against has been replaced.
     *
     * A gain is a multiplier on a component, so it only means anything relative
     * to the model that produced the errors it came from. A `sleepChargeGain` of
     * 0.80 fitted under an older model can suppress exactly the charge the new
     * one introduced, leaving the model fighting its own correction while the
     * watch fit crawls back at 0.1 per observation.
     *
     * Only the four multipliers and the observation count reset — manual heart
     * zones, the body profile and the setup flag are untouched.
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
     * Rewinds the watch fit watermark once per [BodyEnergyWatchFitEpoch].
     *
     * The watermark records how far the watch evidence has already been
     * consumed, so leaving it ahead while the gains go back to 1.0 means the
     * model is told to relearn and then denied everything it would relearn from.
     * An epoch of its own says what is meant — "the fit machinery changed,
     * re-read the evidence" — without claiming a model change or discarding the
     * stored chain the way an algorithm bump would. Hanging it off the
     * algorithm-version reset made it dead code on every install already at the
     * current version, which is all of them.
     */
    private fun rewindWatchFitIfEpochChanged() {
        if (preferencesRepository.bodyEnergyWatchFitEpoch == BodyEnergyWatchFitEpoch) return
        preferencesRepository.bodyEnergyWatchFitWatermarkMillis = 0L
        preferencesRepository.bodyEnergyWatchFitEpoch = BodyEnergyWatchFitEpoch
    }

    /**
     * The store-less fallback: the mirrored end score, accepted only when it
     * belongs to the day immediately before [date].
     */
    private fun seedFromMirror(date: LocalDate): ChainSeed {
        val encoded = preferencesRepository.bodyEnergyChainSeedMirror ?: return ChainSeed.Neutral
        val parts = encoded.split("|")
        if (parts.size != 2) return ChainSeed.Neutral
        val epochDay = parts[0].toLongOrNull() ?: return ChainSeed.Neutral
        val endScore = parts[1].toIntOrNull() ?: return ChainSeed.Neutral
        return if (epochDay == date.minusDays(1).toEpochDay()) {
            ChainSeed.carried(endScore)
        } else {
            ChainSeed.Neutral
        }
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

        // Independent reads run concurrently: start every job, then await. The
        // baseline runs alongside them; only respiratory depends on it.
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
        // Hourly steps + active calories, and the basal rate — the
        // energy-balance inputs the heart-rate-zone model alone was missing.
        val activityProgressJob = async { activityRepository.loadActivityProgress(date) }
        val basalMetabolicRateJob = async { bodyRepository.loadLatestBMR() }
        val restingJob = async { heartRepository.loadRestingHeartRate(date) }

        val baselines = baselinesJob.await()
        // Respiratory is loaded only when a respiratory baseline exists (the
        // stress factor is inert without one), so it can only start after the
        // baseline resolves.
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
            // A recompute that found nothing must not replace a day we already
            // have. Without the (user-optional, dialog-ungrantable) history
            // grant Health Connect serves only ~30 days, so an old day can come
            // back empty purely because its data is out of reach — and `save`
            // deletes that day's buckets before writing, so the stored timeline
            // would be the thing lost. Skip the ripple too: nothing downstream
            // changed, because nothing here did.
            if (timeline.points.isEmpty() && store.hasStoredPoints(date)) {
                return@coroutineScope store.load(date, signature) ?: timeline
            }

            val stored = if (rippleForward) store.storedDaysBetween(date, date) else emptyList()
            val previousEnd = stored.firstOrNull()?.endScore
            store.save(timeline)
            // Ripple only when the end score actually moved. A routine recompute
            // that lands on the same number changes nothing downstream, and
            // wiping a week of stored days for a no-op would guarantee a chain
            // gap on the next open.
            if (rippleForward && previousEnd != null && previousEnd != timeline.currentScore) {
                store.invalidateForward(date.plusDays(1), today())
            }
        }
        writeSeedMirror(timeline)
        timeline
    }

    /**
     * Mirrors the newest completed day's end score for a store-less context.
     * Only moves forward, so an old day being backfilled cannot overwrite it.
     */
    private fun writeSeedMirror(timeline: BodyEnergyTimeline) {
        if (!timeline.date.isBefore(today())) return
        val existing = preferencesRepository.bodyEnergyChainSeedMirror
        val existingEpochDay = existing?.substringBefore("|")?.toLongOrNull()
        if (existingEpochDay != null && existingEpochDay > timeline.date.toEpochDay()) return
        preferencesRepository.bodyEnergyChainSeedMirror =
            "${timeline.date.toEpochDay()}|${timeline.currentScore}"
    }

    /**
     * Reuse a fresh cached baseline (this day or an adjacent one), else
     * recompute the 28-day medians + observed max and cache.
     */
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
        // Observed max is taken over the whole baseline window, not just the
        // current day's samples.
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

    private suspend fun permissionSignature(): Int =
        runCatching {
            if (healthRepository.availability() == HealthConnectAvailability.AVAILABLE) {
                healthRepository.grantedPermissions()
                    .sorted()
                    .joinToString(",")
                    .hashCode()
            } else {
                0
            }
        }.getOrDefault(0)

    /**
     * The signature a row for [date] is stored under: the chain part, plus the
     * learned gains the row was actually computed with.
     *
     * Always computed from the row's OWN date. The body profile's signature
     * varies by date (its age gate is relative to the day being asked about), so
     * validating yesterday's row against today's signature — what the original
     * seed lookup did — silently breaks the chain across a birthday.
     */
    private fun signatureFor(date: LocalDate, context: ChainContext): String =
        "${chainSignatureFor(date, context)}|${context.calibration.gainSignature().hashCode()}"

    /**
     * Everything a CARRY-OVER SCORE depends on, with the learned gains left out.
     *
     * A seed is one number from the previous day, not a timeline, and it has to
     * survive the watch fit nudging a gain by a fraction of a percent. Folding
     * the gains in meant every observation the learner absorbed invalidated all
     * fourteen stored days at once, so the next load found no valid predecessor
     * and fell back to the neutral 50 — turning a sub-percent model change into
     * a visible 40-point jump, which is the exact discontinuity the chain exists
     * to remove.
     *
     * Serving a cached timeline still requires the full signature. This is only
     * for deciding whether a stored day is a legitimate ANCHOR to continue from.
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
     * Whether [timeline] should be recomputed rather than served.
     *
     * Three tiers. Today is volatile and re-reads every 15 minutes. A day inside
     * [BodyEnergyChainSettlingDays] can still gain late-arriving watch data, so
     * it re-reads daily. A settled day never does: nothing new will arrive for
     * it, and recomputing would spend ~8 Health Connect reads to reproduce what
     * is already stored — which is what made the whole bucket table write-only,
     * since retention keeps 120 days but nothing read one older than a day.
     *
     * "Never stale" is not "never updated": [BodyEnergyTimelineStore.load] still
     * requires a signature match, so a calibration edit, a permission change or
     * an algorithm-version bump all rebuild a settled day, and
     * [RefreshMode.FORCE] bypasses this entirely.
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
     * Whether a cached timeline still has what it claims.
     *
     * Retention purges buckets past `BodyEnergyBucketRetentionDays` but keeps
     * the summary row, so such a day still carries a score and a confidence with
     * nothing to draw. Serving it would put a headline above a blank chart — a
     * hole that only stayed hidden while every old day was recomputed anyway. A
     * genuinely data-less day is the exception: NO_DATA with no points is the
     * whole truth about it.
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

        /**
         * How far back a stored chain anchor is looked for. Pure SQLite — one
         * query covers the whole window — so this bound costs nothing to raise.
         */
        const val ChainLookbackDays = 14L

        /**
         * How many missing days the FOREGROUND load will recompute to close a
         * gap. Deliberately far smaller than [ChainLookbackDays]: each day is ~8
         * Health Connect reads. Two days covers "I last opened the app the day
         * before yesterday", which is the common gap; anything wider is
         * [tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService]'s job.
         */
        const val ChainForegroundFillDays = 2L

        /**
         * Sub-budget for the gap fill. A slow fill must degrade to a neutral
         * seed, never fail the whole timeline load.
         */
        const val ChainFillBudgetMillis = 12_000L
    }
}

/**
 * The per-load inputs every day in a chain walk shares. Only the date varies,
 * which is what lets the signature be computed for an arbitrary day rather than
 * just the requested one.
 */
private data class ChainContext(
    val calibration: BodyEnergyCalibration,
    val bodyProfile: BodyProfile,
    val permissionSignature: Int,
)

/** A resolved starting score, and where it came from. */
private data class ChainSeed(
    /**
     * The previous day's end score, or null when there is nothing to carry — in
     * which case the calculator falls back to the neutral start score.
     */
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
