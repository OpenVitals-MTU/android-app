package tech.mmarca.openvitals.data.repository.report

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.data.repository.dashboard.MetricReadPermissions
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.ReportDailyValue
import tech.mmarca.openvitals.domain.model.ReportData
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportMetricResult
import tech.mmarca.openvitals.domain.model.ReportMetricStatus
import tech.mmarca.openvitals.domain.model.ReportMetricDetail
import tech.mmarca.openvitals.domain.model.ReportRequest
import tech.mmarca.openvitals.domain.report.ReportRollup
import tech.mmarca.openvitals.domain.report.bloodGlucoseDetail
import tech.mmarca.openvitals.domain.report.bloodPressureDetail
import tech.mmarca.openvitals.domain.report.distinctBloodPressureReadings
import tech.mmarca.openvitals.domain.report.sleepDetail
import tech.mmarca.openvitals.domain.report.workoutsDetail
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/** A cooperative cancel flag the UI flips; checked between read groups. */
class ReportCancellation {
    @Volatile
    private var cancelled = false

    val isCancelled: Boolean get() = cancelled

    fun cancel() {
        cancelled = true
    }
}

/** Progress the builder screen shows while the reads run. */
data class ReportProgress(
    val completed: Int,
    val total: Int,
    val currentMetric: ReportMetric?,
)

/**
 * The report's data pass: every selected metric read as a daily series, rolled
 * up to the requested granularity. Reads run in coalesced GROUPS — one
 * `loadDailySteps` serves the whole steps family, one `loadDailyMacros` serves
 * the nutrition metrics — and the groups run SEQUENTIALLY on purpose: Health
 * Connect answers each read in one Binder parcel from a shared 1 MB buffer,
 * and parallel year-long reads are exactly how that buffer overflows.
 *
 * Never throws for data reasons. A group that fails or blows its budget marks
 * only its own metrics FAILED; a cancelled build marks the rest SKIPPED; an
 * ungranted metric is MISSING_PERMISSION without a read ever going out.
 */
@Singleton
class ReportDataLoader @Inject constructor(
    private val hc: HealthConnectManager,
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val nutritionRepository: NutritionRepository,
    private val hydrationRepository: HydrationRepository,
    private val bodyRepository: BodyRepository,
    private val heartRepository: HeartRepository,
    private val vitalsRepository: VitalsRepository,
    private val mindfulnessRepository: MindfulnessRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) {
    companion object {
        private const val TAG = "ReportDataLoader"

        /**
         * Per-group budget. A dense year-long raw read (HRV, uncached vitals)
         * can be slow; past this it costs its own section, not the report.
         */
        private const val GroupBudgetMillis = 60_000L

        /** What the range shrinks to when the history permission is missing. */
        private const val HistoryClampDays = 30

        /**
         * Past this many raw glucose readings the range is CGM territory —
         * a per-reading table would run to hundreds of pages, so the section
         * falls back to the generic daily chart.
         */
        private const val MaxGlucoseDetailReadings = 500

        /** Same idea for body temperature: wearables stream it continuously. */
        private const val MaxTemperatureDetailReadings = 200
    }

    private val readHealthDataHistoryPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    /**
     * The raw permissions [metric]'s report read needs. On top of the
     * dashboard mapping, the steps-family metrics add the steps permission:
     * their only range read is `loadDailySteps`, which hard-requires it.
     */
    fun rawPermissionsFor(metric: ReportMetric): Set<String> {
        val base = MetricReadPermissions.forMetric(
            metric.dashboardMetric,
            showOpenVitalsCalculatedCalories = false,
        )
        return when (metric) {
            ReportMetric.DISTANCE,
            ReportMetric.FLOORS,
            ReportMetric.ELEVATION,
            ReportMetric.WHEELCHAIR_PUSHES,
            -> base + MetricReadPermissions.readStepsPermission
            else -> base
        }
    }

    /** [rawPermissionsFor] cut down to what the provider can actually grant. */
    fun requestablePermissionsFor(metrics: Set<ReportMetric>): Set<String> =
        metrics.flatMapTo(mutableSetOf()) { rawPermissionsFor(it) }
            .intersect(hc.managedPermissions)

    /**
     * The metrics the installed provider can serve at all — the picker hides
     * the rest. Mirrors the dashboard's `supportedMetrics`.
     */
    fun supportedReportMetrics(): Set<ReportMetric> {
        val managed = hc.managedPermissions
        return ReportMetric.entries.filterTo(mutableSetOf()) { metric ->
            val permissions = rawPermissionsFor(metric)
            permissions.isNotEmpty() && permissions.all { it in managed }
        }
    }

    suspend fun load(
        request: ReportRequest,
        onProgress: (ReportProgress) -> Unit = {},
        cancellation: ReportCancellation = ReportCancellation(),
    ): ReportData = withContext(dispatchers.io) {
        val granted = grantedPermissionsIfAvailable()

        val historyDefined = readHealthDataHistoryPermission in hc.additionalDataAccessPermissions
        val historyMissing = historyDefined && readHealthDataHistoryPermission !in granted
        val clampStart = request.end.minusDays(HistoryClampDays - 1L)
        val truncated = historyMissing && request.start.isBefore(clampStart)
        val effectiveStart = if (truncated) clampStart else request.start

        val requested = request.metrics
        // A metric is readable only when the provider manages every permission
        // its read needs AND all of them are granted — a permission outside
        // `managedPermissions` can never be granted, so the metric can never fill.
        val missing = requested.filterTo(mutableSetOf()) { metric ->
            val permissions = rawPermissionsFor(metric)
            permissions.any { it !in hc.managedPermissions } || permissions.any { it !in granted }
        }
        val readable = requested - missing

        val groups = readGroups(readable, effectiveStart, request)
        val total = readable.size
        var completed = 0
        val resultsByMetric = mutableMapOf<ReportMetric, ReportMetricResult>()
        var cancelled = false

        for (group in groups) {
            if (cancellation.isCancelled) {
                cancelled = true
                group.metrics.forEach { metric ->
                    resultsByMetric[metric] = ReportMetricResult(metric, ReportMetricStatus.SKIPPED)
                }
                continue
            }
            onProgress(ReportProgress(completed, total, group.metrics.first()))
            val series = try {
                withTimeoutOrNull(GroupBudgetMillis) { group.read() }
                    ?: run {
                        Log.w(TAG, "Report read group ${group.metrics} blew its ${GroupBudgetMillis}ms budget")
                        null
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Report read group ${group.metrics} failed", error)
                null
            }
            group.metrics.forEach { metric ->
                resultsByMetric[metric] = if (series == null) {
                    ReportMetricResult(metric, ReportMetricStatus.FAILED)
                } else {
                    resultFor(metric, series[metric] ?: MetricSeries(emptyList()), effectiveStart, request)
                }
            }
            completed += group.metrics.size
            onProgress(ReportProgress(completed, total, null))
        }

        val results = requested.sortedWith(compareBy({ it.section.ordinal }, { it.ordinal })).map { metric ->
            resultsByMetric[metric]
                ?: ReportMetricResult(metric, ReportMetricStatus.MISSING_PERMISSION)
        }

        ReportData(
            request = request,
            effectiveStart = effectiveStart,
            truncatedToDays = HistoryClampDays.takeIf { truncated },
            missingPermissions = missing,
            historyPermissionMissing = truncated,
            cancelled = cancelled,
            results = results,
            generatedAt = Instant.now(),
        )
    }

    private fun resultFor(
        metric: ReportMetric,
        series: MetricSeries,
        effectiveStart: LocalDate,
        request: ReportRequest,
    ): ReportMetricResult {
        val daily = series.daily
        if (daily.isEmpty()) return ReportMetricResult(metric, ReportMetricStatus.EMPTY)
        val sorted = daily.sortedBy { it.date }
        return ReportMetricResult(
            metric = metric,
            status = ReportMetricStatus.OK,
            points = ReportRollup.rollup(
                daily = sorted,
                valueKind = metric.valueKind,
                granularity = request.granularity,
                weekMode = request.weekMode,
                rangeStart = effectiveStart,
                rangeEnd = request.end,
            ),
            summary = ReportRollup.summarize(sorted, metric.valueKind),
            detail = series.detail,
        )
    }

    /**
     * One metric's read output: the daily series every metric has, plus the
     * blood-pressure extras only BP carries.
     */
    private class MetricSeries(
        val daily: List<ReportDailyValue>,
        val detail: ReportMetricDetail? = null,
    )

    /** One coalesced read serving one or more metrics. */
    private class ReadGroup(
        val metrics: List<ReportMetric>,
        val read: suspend () -> Map<ReportMetric, MetricSeries>,
    )

    private fun readGroups(
        readable: Set<ReportMetric>,
        start: LocalDate,
        request: ReportRequest,
    ): List<ReadGroup> {
        val end = request.end
        val groups = mutableListOf<ReadGroup>()

        fun group(vararg metrics: ReportMetric, read: suspend () -> Map<ReportMetric, List<ReportDailyValue>>) {
            val wanted = metrics.filter { it in readable }
            if (wanted.isNotEmpty()) {
                groups += ReadGroup(wanted) { read().mapValues { (_, daily) -> MetricSeries(daily) } }
            }
        }

        fun singleGroup(metric: ReportMetric, read: suspend () -> List<ReportDailyValue>) =
            group(metric) { mapOf(metric to read()) }

        group(
            ReportMetric.STEPS,
            ReportMetric.DISTANCE,
            ReportMetric.ACTIVE_CALORIES,
            ReportMetric.FLOORS,
            ReportMetric.ELEVATION,
            ReportMetric.WHEELCHAIR_PUSHES,
        ) {
            val daily = activityRepository.loadDailySteps(
                start,
                end,
                includeWheelchairPushes = ReportMetric.WHEELCHAIR_PUSHES in readable,
            )
            mapOf(
                ReportMetric.STEPS to daily.mapNotNull { day ->
                    day.steps.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it.toDouble()) }
                },
                ReportMetric.DISTANCE to daily.mapNotNull { day ->
                    day.distanceMeters.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it) }
                },
                ReportMetric.ACTIVE_CALORIES to daily.mapNotNull { day ->
                    day.activeCaloriesKcal?.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it) }
                },
                ReportMetric.FLOORS to daily.mapNotNull { day ->
                    day.floorsClimbed?.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it.toDouble()) }
                },
                ReportMetric.ELEVATION to daily.mapNotNull { day ->
                    day.elevationGainedMeters?.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it) }
                },
                ReportMetric.WHEELCHAIR_PUSHES to daily.mapNotNull { day ->
                    day.wheelchairPushes?.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it.toDouble()) }
                },
            )
        }

        singleGroup(ReportMetric.CALORIES_OUT) {
            activityRepository.loadDailyNutrition(start, end).mapNotNull { day ->
                day.caloriesBurnedKcal.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it) }
            }
        }

        if (ReportMetric.WORKOUT in readable) {
            groups += ReadGroup(listOf(ReportMetric.WORKOUT)) {
                val zone = ZoneId.systemDefault()
                // The metrics variant adds per-session distance (with route
                // backfill) — the same read the Activities screens use.
                val workouts = activityRepository.loadWorkoutsWithMetrics(start, end)
                    .filter {
                        val date = it.startTime.atZone(zone).toLocalDate()
                        !date.isBefore(start) && !date.isAfter(end)
                    }
                val daily = workouts
                    .groupBy { it.startTime.atZone(zone).toLocalDate() }
                    .map { (date, dayWorkouts) ->
                        ReportDailyValue(date, dayWorkouts.sumOf { it.durationMs } / 60_000.0)
                    }
                mapOf(ReportMetric.WORKOUT to MetricSeries(daily, workoutsDetail(workouts)))
            }
        }

        if (ReportMetric.SLEEP in readable) {
            groups += ReadGroup(listOf(ReportMetric.SLEEP)) {
                val daily = sleepRepository.loadDailySleepDurations(start, end, request.sleepWindow)
                    .filter { it.durationMs > 0 }
                    .map { ReportDailyValue(it.date, it.durationMs / 60_000.0) }
                val sessions = sleepRepository.loadSleepSessions(start, end)
                mapOf(ReportMetric.SLEEP to MetricSeries(daily, sleepDetail(sessions, ZoneId.systemDefault())))
            }
        }

        singleGroup(ReportMetric.HYDRATION) {
            hydrationRepository.loadDailyHydration(start, end).mapNotNull { day ->
                day.liters.takeIf { it > 0 }?.let { ReportDailyValue(day.date, it) }
            }
        }

        group(
            ReportMetric.CALORIES_IN,
            ReportMetric.PROTEIN,
            ReportMetric.CARBS,
            ReportMetric.FAT,
            ReportMetric.CAFFEINE,
        ) {
            val daily = nutritionRepository.loadDailyMacros(start, end)
            fun series(value: (tech.mmarca.openvitals.domain.model.DailyMacros) -> Double) =
                daily.mapNotNull { day -> value(day).takeIf { it > 0 }?.let { ReportDailyValue(day.date, it) } }
            mapOf(
                ReportMetric.CALORIES_IN to series { it.energyKcal },
                ReportMetric.PROTEIN to series { it.proteinGrams },
                ReportMetric.CARBS to series { it.carbsGrams },
                ReportMetric.FAT to series { it.fatGrams },
                // Stored in grams; the app talks about caffeine in milligrams.
                ReportMetric.CAFFEINE to series { (it.nutrientValues[NutritionNutrient.CAFFEINE] ?: 0.0) * 1_000.0 },
            )
        }

        val zone = ZoneId.systemDefault()
        fun dailyMean(entries: List<Pair<Instant, Double>>): List<ReportDailyValue> =
            entries
                .groupBy({ it.first.atZone(zone).toLocalDate() }, { it.second })
                .filterKeys { !it.isBefore(start) && !it.isAfter(end) }
                .map { (date, values) -> ReportDailyValue(date, values.average()) }

        singleGroup(ReportMetric.WEIGHT) {
            dailyMean(bodyRepository.loadWeightEntries(start, end).map { it.time to it.weightKg })
        }
        singleGroup(ReportMetric.HEIGHT) {
            dailyMean(bodyRepository.loadHeightEntries(start, end).map { it.time to it.heightCm })
        }
        singleGroup(ReportMetric.BODY_FAT) {
            dailyMean(bodyRepository.loadBodyFatEntries(start, end).map { it.time to it.percent })
        }
        singleGroup(ReportMetric.LEAN_MASS) {
            dailyMean(bodyRepository.loadLeanBodyMassEntries(start, end).map { it.time to it.massKg })
        }
        singleGroup(ReportMetric.BMR) {
            dailyMean(bodyRepository.loadBmrEntries(start, end).map { it.time to it.kcalPerDay })
        }
        singleGroup(ReportMetric.BONE_MASS) {
            dailyMean(bodyRepository.loadBoneMassEntries(start, end).map { it.time to it.massKg })
        }
        singleGroup(ReportMetric.BODY_WATER_MASS) {
            dailyMean(bodyRepository.loadBodyWaterMassEntries(start, end).map { it.time to it.massKg })
        }

        singleGroup(ReportMetric.AVG_HEART_RATE) {
            heartRepository.loadDailyHeartRateSummaries(start, end).map { day ->
                ReportDailyValue(
                    date = day.date,
                    value = day.avgBpm.toDouble(),
                    min = day.minBpm.toDouble(),
                    max = day.maxBpm.toDouble(),
                )
            }
        }
        singleGroup(ReportMetric.RESTING_HEART_RATE) {
            heartRepository.loadDailyRestingHR(start, end).map { ReportDailyValue(it.date, it.bpm.toDouble()) }
        }
        singleGroup(ReportMetric.HRV) {
            heartRepository.loadDailyHRV(start, end).map { ReportDailyValue(it.date, it.rmssdMs) }
        }

        // Raw readings rather than the daily-average read: the section lists
        // every measurement and averages by time-of-day slot, and the chart's
        // daily min/max should be real extremes, not a mean repeated.
        if (ReportMetric.BLOOD_PRESSURE in readable) {
            groups += ReadGroup(listOf(ReportMetric.BLOOD_PRESSURE)) {
                val bpZone = ZoneId.systemDefault()
                val entries = distinctBloodPressureReadings(vitalsRepository.loadBloodPressure(start, end))
                val daily = entries
                    .groupBy { it.time.atZone(bpZone).toLocalDate() }
                    .filterKeys { !it.isBefore(start) && !it.isAfter(end) }
                    .map { (date, dayEntries) ->
                        ReportDailyValue(
                            date = date,
                            value = dayEntries.map { it.systolicMmHg }.average(),
                            min = dayEntries.minOf { it.systolicMmHg }.toDouble(),
                            max = dayEntries.maxOf { it.systolicMmHg }.toDouble(),
                            secondaryValue = dayEntries.map { it.diastolicMmHg }.average(),
                        )
                    }
                mapOf(
                    ReportMetric.BLOOD_PRESSURE to MetricSeries(
                        daily = daily,
                        detail = bloodPressureDetail(entries, bpZone),
                    ),
                )
            }
        }
        fun vitalsGroup(metric: ReportMetric, vitalsMetric: VitalsPeriodMetric) =
            singleGroup(metric) {
                vitalsRepository.loadDailyVitals(vitalsMetric, start, end)
                    .map { ReportDailyValue(it.date, it.value) }
            }
        vitalsGroup(ReportMetric.SPO2, VitalsPeriodMetric.SPO2)
        vitalsGroup(ReportMetric.VO2_MAX, VitalsPeriodMetric.VO2_MAX)
        vitalsGroup(ReportMetric.RESPIRATORY_RATE, VitalsPeriodMetric.RESPIRATORY_RATE)
        vitalsGroup(ReportMetric.SKIN_TEMPERATURE, VitalsPeriodMetric.SKIN_TEMPERATURE)

        if (ReportMetric.BODY_TEMPERATURE in readable) {
            groups += ReadGroup(listOf(ReportMetric.BODY_TEMPERATURE)) {
                val daily = vitalsRepository.loadDailyVitals(VitalsPeriodMetric.BODY_TEMPERATURE, start, end)
                    .map { ReportDailyValue(it.date, it.value) }
                val entries = vitalsRepository.loadBodyTemperature(start, end)
                val detail = entries
                    .takeIf { it.isNotEmpty() && it.size <= MaxTemperatureDetailReadings }
                    ?.let { raw ->
                        tech.mmarca.openvitals.domain.model.ReportReadingsDetail(
                            tech.mmarca.openvitals.domain.report.dedupeReadings(
                                raw,
                                time = { it.time },
                                key = { it.temperatureCelsius },
                            ).map { tech.mmarca.openvitals.domain.model.ReportReading(it.time, it.temperatureCelsius) },
                        )
                    }
                mapOf(ReportMetric.BODY_TEMPERATURE to MetricSeries(daily, detail))
            }
        }

        if (ReportMetric.BLOOD_GLUCOSE in readable) {
            groups += ReadGroup(listOf(ReportMetric.BLOOD_GLUCOSE)) {
                val glucoseZone = ZoneId.systemDefault()
                val entries = vitalsRepository.loadBloodGlucose(start, end)
                if (entries.size > MaxGlucoseDetailReadings) {
                    // CGM territory: keep the cached daily chart, skip the detail.
                    val daily = vitalsRepository.loadDailyVitals(VitalsPeriodMetric.BLOOD_GLUCOSE, start, end)
                        .map { ReportDailyValue(it.date, it.value) }
                    mapOf(ReportMetric.BLOOD_GLUCOSE to MetricSeries(daily))
                } else {
                    val daily = entries
                        .groupBy { it.time.atZone(glucoseZone).toLocalDate() }
                        .filterKeys { !it.isBefore(start) && !it.isAfter(end) }
                        .map { (date, dayEntries) ->
                            ReportDailyValue(
                                date = date,
                                value = dayEntries.map { it.millimolesPerLiter }.average(),
                                min = dayEntries.minOf { it.millimolesPerLiter },
                                max = dayEntries.maxOf { it.millimolesPerLiter },
                            )
                        }
                    mapOf(
                        ReportMetric.BLOOD_GLUCOSE to
                            MetricSeries(daily, bloodGlucoseDetail(entries, glucoseZone)),
                    )
                }
            }
        }

        singleGroup(ReportMetric.MINDFULNESS) {
            val sessionZone = ZoneId.systemDefault()
            mindfulnessRepository.loadMindfulnessSessions(start, end)
                .groupBy { it.startTime.atZone(sessionZone).toLocalDate() }
                .filterKeys { !it.isBefore(start) && !it.isAfter(end) }
                .map { (date, sessions) ->
                    ReportDailyValue(date, sessions.sumOf { it.durationMs } / 60_000.0)
                }
        }

        return groups
    }

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()
}
