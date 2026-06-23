package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.AppCoroutineScope
import tech.mmarca.openvitals.core.performance.PerformanceTrace
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadEstimate
import tech.mmarca.openvitals.domain.insights.CardioLoadTimeWindow
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.insights.IntensityMinutesEstimate
import tech.mmarca.openvitals.domain.insights.IntensityWorkoutInput
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.insights.SleepScoreLookbackDays
import tech.mmarca.openvitals.domain.insights.calculateCardioLoad
import tech.mmarca.openvitals.domain.insights.calculateIntensityMinutes
import tech.mmarca.openvitals.domain.insights.calculateSleepScoreForDate
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.data.cache.CachedSummaryFreshness
import tech.mmarca.openvitals.data.cache.CachedSummaryKey
import tech.mmarca.openvitals.data.cache.DashboardDataSummaryCodec
import tech.mmarca.openvitals.data.cache.MetricSummaryCacheStore
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.dailySleepSummary
import tech.mmarca.openvitals.domain.model.sleepRangeWindowFor
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryCache
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryKey
import tech.mmarca.openvitals.healthconnect.currentDayTtlMillis
import tech.mmarca.openvitals.healthconnect.permissionFingerprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class HealthRepository @Inject constructor(
    private val hc: HealthConnectManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val queryCache: HealthConnectQueryCache = HealthConnectQueryCache(),
    private val preferencesRepository: PreferencesRepository? = null,
    private val metricSummaryCacheStore: MetricSummaryCacheStore? = null,
    @param:AppCoroutineScope private val appScope: CoroutineScope? = null,
) {

    companion object {
        private const val TAG = "HealthRepository"
        private const val DashboardCardioLoadHistoryPeriods = 4L
    }

    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val readDistancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val readExercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val readSleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val readHeartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val readRestingHRPermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val readWeightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val readBodyFatPermission = HealthPermission.getReadPermission(BodyFatRecord::class)
    private val readCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val readActiveCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val readBloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val readSpO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val readVo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)
    private val readRespiratoryRatePermission = HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val readBodyTemperaturePermission = HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    private val readBloodGlucosePermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    private val readSkinTemperaturePermission = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)
    private val readWheelchairPushesPermission = HealthPermission.getReadPermission(WheelchairPushesRecord::class)
    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    private val readHeightPermission = HealthPermission.getReadPermission(HeightRecord::class)
    private val readLeanMassPermission = HealthPermission.getReadPermission(LeanBodyMassRecord::class)
    private val readBmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val readBoneMassPermission = HealthPermission.getReadPermission(BoneMassRecord::class)
    private val readBodyWaterMassPermission = HealthPermission.getReadPermission(BodyWaterMassRecord::class)
    private val readMenstruationPeriodPermission = HealthPermission.getReadPermission(MenstruationPeriodRecord::class)
    private val readOvulationTestPermission = HealthPermission.getReadPermission(OvulationTestRecord::class)
    private val readBasalBodyTemperaturePermission = HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class)

    // ─── Availability + permissions ───────────────────────────────────────────

    fun availability(): HealthConnectAvailability = hc.availability()

    fun permissionContract() = hc.permissionContract()

    val phase1Permissions get() = hc.phase1Permissions
    val phase2Permissions get() = hc.phase2Permissions
    val phase3Permissions get() = hc.phase3Permissions
    val phase4Permissions get() = hc.phase4Permissions
    val corePermissions get() = hc.corePermissions
    val routePermissions get() = hc.routePermissions
    val activityWritePermissions get() = hc.activityWritePermissions
    val heartPermissions get() = hc.heartPermissions
    val bodyPermissions get() = hc.bodyPermissions
    val bodyWritePermissions get() = hc.bodyWritePermissions
    val activityExtrasPermissions get() = hc.activityExtrasPermissions
    val nutritionHydrationPermissions get() = hc.nutritionHydrationPermissions
    val hydrationWritePermissions get() = hc.hydrationWritePermissions
    val mindfulnessPermissions get() = hc.mindfulnessPermissions
    val mindfulnessWritePermissions get() = hc.mindfulnessWritePermissions
    val additionalDataAccessPermissions get() = hc.additionalDataAccessPermissions
    val vitalsPermissions get() = hc.vitalsPermissions
    val vitalsWritePermissions get() = hc.vitalsWritePermissions
    val dataImportWritePermissions get() = hc.dataImportWritePermissions
    val cyclePermissions get() = hc.cyclePermissions
    val manualOnlyPermissions get() = hc.manualOnlyPermissions
    val requestableWritePermissions get() = hc.requestableWritePermissions
    val onboardingPermissions get() = hc.onboardingRequestablePermissions
    val allPermissions get() = hc.allPermissions
    val managedPermissions get() = hc.managedPermissions
    fun grantModeFor(permission: String) = hc.grantModeFor(permission)

    fun isMindfulnessAvailable(): Boolean = hc.isMindfulnessSessionAvailable()

    suspend fun grantedPermissions(): Set<String> = hc.grantedPermissions()

    suspend fun missingPhase1(): Set<String> {
        val granted = hc.grantedPermissions()
        return hc.phase1Permissions.filterNot { it in granted }.toSet()
    }

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) {
            hc.grantedPermissions().also { granted ->
                Log.d(TAG, "grantedPermissionsIfAvailable count=${granted.size}")
            }
        } else {
            Log.w(TAG, "Health Connect unavailable, returning empty granted permissions")
            emptySet()
        }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    suspend fun loadDashboard(query: DashboardQuery): DashboardData =
        withContext(dispatchers.io) {
            val startedAt = System.currentTimeMillis()
            val granted = grantedPermissionsIfAvailable()
            val loadMetrics = query.visibleMetrics
            val showOpenVitalsCalculatedCalories = preferencesRepository?.showOpenVitalsCalculatedCalories == true
            val healthConnectCacheKey = dashboardHealthConnectCacheKey(
                query = query,
                loadMetrics = loadMetrics,
                granted = granted,
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
            )
            val summaryCacheKey = dashboardSummaryCacheKey(
                query = query,
                loadMetrics = loadMetrics,
                granted = granted,
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
            )

            val cached = metricSummaryCacheStore?.read(
                key = summaryCacheKey,
                referenceDate = query.date,
                refreshMode = query.refreshMode,
            )
            if (cached?.isUsable == true) {
                runCatching { DashboardDataSummaryCodec.decode(checkNotNull(cached.entry).payloadJson) }
                    .onSuccess { cachedData ->
                        if (cached.freshness == CachedSummaryFreshness.STALE) {
                            refreshDashboardSummaryInBackground(
                                query = query,
                                loadMetrics = loadMetrics,
                                granted = granted,
                                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                                healthConnectCacheKey = healthConnectCacheKey,
                                summaryCacheKey = summaryCacheKey,
                            )
                        }
                        Log.d(
                            TAG,
                            "loadDashboard cacheHit freshness=${cached.freshness} date=${query.date} metrics=${loadMetrics.size}",
                        )
                        return@withContext cachedData
                    }
                    .onFailure {
                        metricSummaryCacheStore?.invalidate(summaryCacheKey)
                    }
            }

            val data = loadDashboardFromHealthConnect(
                query = query,
                loadMetrics = loadMetrics,
                granted = granted,
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                healthConnectCacheKey = healthConnectCacheKey,
                refreshMode = query.refreshMode,
            )
            metricSummaryCacheStore?.write(summaryCacheKey, DashboardDataSummaryCodec.encode(data))
            Log.d(
                TAG,
                "loadDashboard completed date=${query.date} metrics=${loadMetrics.size} " +
                    "durationMs=${System.currentTimeMillis() - startedAt}",
            )
            data
        }

    private suspend fun loadDashboardFromHealthConnect(
        query: DashboardQuery,
        loadMetrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
        healthConnectCacheKey: HealthConnectQueryKey,
        refreshMode: tech.mmarca.openvitals.domain.model.RefreshMode,
    ): DashboardData =
        queryCache.getOrPut(
            key = healthConnectCacheKey,
            refreshMode = refreshMode,
            ttlMillis = currentDayTtlMillis(query.date),
        ) {
            PerformanceTrace.timed(
                name = "dashboard.healthConnect",
                attributes = mapOf(
                    "date" to query.date,
                    "metrics" to loadMetrics.size,
                ),
            ) {
                loadDashboardUncached(query, loadMetrics, granted, showOpenVitalsCalculatedCalories)
            }
        }

    private fun refreshDashboardSummaryInBackground(
        query: DashboardQuery,
        loadMetrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
        healthConnectCacheKey: HealthConnectQueryKey,
        summaryCacheKey: CachedSummaryKey,
    ) {
        val cacheStore = metricSummaryCacheStore ?: return
        val scope = appScope ?: return
        scope.launch(dispatchers.io) {
            runCatching {
                val refreshed = loadDashboardFromHealthConnect(
                    query = query,
                    loadMetrics = loadMetrics,
                    granted = granted,
                    showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                    healthConnectCacheKey = healthConnectCacheKey,
                    refreshMode = tech.mmarca.openvitals.domain.model.RefreshMode.FORCE,
                )
                cacheStore.write(summaryCacheKey, DashboardDataSummaryCodec.encode(refreshed))
            }.onFailure { error ->
                Log.w(TAG, "Background dashboard summary refresh failed date=${query.date}", error)
            }
        }
    }

    private fun dashboardHealthConnectCacheKey(
        query: DashboardQuery,
        loadMetrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
    ): HealthConnectQueryKey =
        HealthConnectQueryKey(
            operation = "dashboard",
            parts = dashboardCacheParts(query, loadMetrics, showOpenVitalsCalculatedCalories),
            permissions = granted.permissionFingerprint(),
        )

    private fun dashboardSummaryCacheKey(
        query: DashboardQuery,
        loadMetrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
    ): CachedSummaryKey =
        CachedSummaryKey(
            surface = DashboardDataSummaryCodec.Surface,
            startDate = query.date,
            endDate = query.date,
            metricSet = loadMetrics.sortedBy { it.name }.joinToString(separator = ",") { it.name },
            permissionFingerprint = granted.permissionFingerprint(),
            configHash = listOf(
                query.sleepRangeMode.name,
                query.activityWeekMode.name,
                showOpenVitalsCalculatedCalories.toString(),
            ).joinToString(separator = "|"),
            schemaVersion = DashboardDataSummaryCodec.SchemaVersion,
        )

    private fun dashboardCacheParts(
        query: DashboardQuery,
        loadMetrics: Set<DashboardMetric>,
        showOpenVitalsCalculatedCalories: Boolean,
    ): List<String> =
        listOf(
            query.date.toString(),
            query.sleepRangeMode.name,
            query.activityWeekMode.name,
            showOpenVitalsCalculatedCalories.toString(),
            loadMetrics.sortedBy { it.name }.joinToString(separator = ",") { it.name },
        )

    private suspend fun loadDashboardUncached(
        query: DashboardQuery,
        metrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
    ): DashboardData = coroutineScope {
        val date = query.date
        val sleepRangeMode = query.sleepRangeMode
        val activityWeekMode = query.activityWeekMode
        Log.d(
            TAG,
            "loadDashboard date=$date metrics=${metrics.sortedBy { it.name }} grantedCount=${granted.size}",
        )

        fun wants(metric: DashboardMetric): Boolean = metric in metrics
        fun wantsAny(vararg targets: DashboardMetric): Boolean = targets.any { it in metrics }

        fun <T> readIfNeeded(
            enabled: Boolean,
            permission: String,
            name: String,
            block: suspend () -> T,
        ) = if (enabled && permission in granted) {
            async { dashboardMetric(name, block) }
        } else {
            null
        }

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        val effectiveDayEnd = if (date == LocalDate.now(zone)) {
            minOf(dayEnd, Instant.now())
        } else {
            dayEnd
        }

        val steps = readIfNeeded(wants(DashboardMetric.STEPS), readStepsPermission, "steps") {
            hc.readSteps(date)
        }
        val distance = readIfNeeded(wants(DashboardMetric.DISTANCE), readDistancePermission, "distance") {
            hc.readDistanceMeters(date)
        }
        val workouts = readIfNeeded(wants(DashboardMetric.WORKOUT), readExercisePermission, "workouts") {
            hc.readExerciseSessions(dayStart, dayEnd)
        }
        val sleep = readIfNeeded(wants(DashboardMetric.SLEEP), readSleepPermission, "sleep") {
            readDashboardSleep(
                date = date,
                sleepRangeMode = sleepRangeMode,
            )
        }
        val calories = readIfNeeded(wants(DashboardMetric.CALORIES_OUT), readCaloriesPermission, "calories") {
            hc.readCaloriesBurned(
                date = date,
                includeEstimatedCalories = canEstimateTotalCalories(granted, showOpenVitalsCalculatedCalories),
            )
        }
        val activeCalories = if (
            wants(DashboardMetric.ACTIVE_CALORIES) &&
            readActiveCaloriesPermission in granted &&
            readStepsPermission in granted &&
            readDistancePermission in granted
        ) {
            async {
                dashboardMetric("active calories") {
                    hc.readDailySteps(date, date, includeActiveCalories = true)
                        .firstOrNull()
                        ?.activeCaloriesKcal
                }
            }
        } else {
            null
        }
        val caloriesIn = readIfNeeded(wants(DashboardMetric.CALORIES_IN), readNutritionPermission, "calories in") {
            hc.readCaloriesInKcal(date)
        }
        val macros = readIfNeeded(
            wantsAny(DashboardMetric.PROTEIN, DashboardMetric.CARBS, DashboardMetric.FAT),
            readNutritionPermission,
            "macros",
        ) {
            hc.readDailyMacros(date, date).firstOrNull()
        }
        val hydration = readIfNeeded(wants(DashboardMetric.HYDRATION), readHydrationPermission, "hydration") {
            hc.readHydrationLiters(date)
        }
        val weight = readIfNeeded(
            wantsAny(DashboardMetric.WEIGHT, DashboardMetric.BMI),
            readWeightPermission,
            "weight",
        ) {
            hc.readLatestWeight()
        }
        val height = readIfNeeded(
            wantsAny(DashboardMetric.HEIGHT, DashboardMetric.BMI),
            readHeightPermission,
            "height",
        ) {
            hc.readLatestHeightEntry()
        }
        val bodyFat = readIfNeeded(wants(DashboardMetric.BODY_FAT), readBodyFatPermission, "body fat") {
            hc.readLatestBodyFat()
        }
        val leanMass = readIfNeeded(wants(DashboardMetric.LEAN_MASS), readLeanMassPermission, "lean mass") {
            hc.readLatestLeanBodyMass()
        }
        val bmr = readIfNeeded(wants(DashboardMetric.BMR), readBmrPermission, "BMR") {
            hc.readLatestBMR()
        }
        val boneMass = readIfNeeded(wants(DashboardMetric.BONE_MASS), readBoneMassPermission, "bone mass") {
            hc.readLatestBoneMass()
        }
        val bodyWaterMass = readIfNeeded(
            wants(DashboardMetric.BODY_WATER_MASS),
            readBodyWaterMassPermission,
            "body water mass",
        ) {
            hc.readLatestBodyWaterMass()
        }
        val heartRate = readIfNeeded(wants(DashboardMetric.AVG_HEART_RATE), readHeartRatePermission, "heart rate") {
            hc.readAvgHeartRate(date)
        }
        val heartRateSamples = readIfNeeded(
            wants(DashboardMetric.AVG_HEART_RATE),
            readHeartRatePermission,
            "heart rate samples",
        ) {
            hc.readHeartRateSamples(dayStart, effectiveDayEnd)
        }
        val restingHR = readIfNeeded(
            wants(DashboardMetric.RESTING_HEART_RATE),
            readRestingHRPermission,
            "resting heart rate",
        ) {
            hc.readRestingHeartRate(date)
        }
        val restingHRBaseline = readIfNeeded(
            wants(DashboardMetric.RESTING_HEART_RATE),
            readRestingHRPermission,
            "resting heart rate baseline",
        ) {
            hc.readDailyRestingHR(date.minusDays(28), date.minusDays(1))
                .map { it.bpm }
                .filter { it > 0L }
                .medianLongOrNull()
        }
        val hrvSamples = readIfNeeded(wants(DashboardMetric.HRV), readHrvPermission, "HRV samples") {
            hc.readHrvSamples(dayStart, effectiveDayEnd)
        }
        val hrvBaseline = readIfNeeded(wants(DashboardMetric.HRV), readHrvPermission, "HRV baseline") {
            hc.readDailyHRV(date.minusDays(28), date.minusDays(1))
                .map { it.rmssdMs }
                .filter { it > 0.0 }
                .medianDoubleValuesOrNull()
        }
        val bloodPressure = readIfNeeded(
            wants(DashboardMetric.BLOOD_PRESSURE),
            readBloodPressurePermission,
            "blood pressure",
        ) {
            hc.readLatestBloodPressure(date)
        }
        val spO2 = readIfNeeded(wants(DashboardMetric.SPO2), readSpO2Permission, "SpO2") {
            hc.readLatestSpO2(date)
        }
        val vo2Max = readIfNeeded(wants(DashboardMetric.VO2_MAX), readVo2MaxPermission, "VO2 max") {
            hc.readLatestVo2Max(date)
        }
        val respiratoryRate = readIfNeeded(
            wants(DashboardMetric.RESPIRATORY_RATE),
            readRespiratoryRatePermission,
            "respiratory rate",
        ) {
            hc.readRespiratoryRateEntries(dayStart, dayEnd)
                .map { it.breathsPerMinute }
                .average()
                .takeUnless { it.isNaN() }
        }
        val bodyTemperature = readIfNeeded(
            wants(DashboardMetric.BODY_TEMPERATURE),
            readBodyTemperaturePermission,
            "body temperature",
        ) {
            hc.readBodyTemperatureEntries(dayStart, dayEnd)
                .maxByOrNull { it.time }
                ?.temperatureCelsius
        }
        val bloodGlucose = readIfNeeded(
            wants(DashboardMetric.BLOOD_GLUCOSE),
            readBloodGlucosePermission,
            "blood glucose",
        ) {
            hc.readBloodGlucoseEntries(dayStart, dayEnd)
                .maxByOrNull { it.time }
                ?.millimolesPerLiter
        }
        val skinTemperature = readIfNeeded(
            wants(DashboardMetric.SKIN_TEMPERATURE) && hc.isSkinTemperatureAvailable(),
            readSkinTemperaturePermission,
            "skin temperature",
        ) {
            hc.readSkinTemperatureEntries(dayStart, dayEnd)
                .maxByOrNull { it.time }
                ?.averageDeltaCelsius
        }
        val weeklyTrainingSignals = if (wantsAny(DashboardMetric.WEEKLY_CARDIO_LOAD, DashboardMetric.INTENSITY_MINUTES)) {
            async {
                dashboardMetric("weekly training signals") {
                    readDashboardWeeklyTrainingSignals(
                        date = date,
                        activityWeekMode = activityWeekMode,
                        granted = granted,
                    )
                }
            }
        } else {
            null
        }
        val floors = readIfNeeded(wants(DashboardMetric.FLOORS), readFloorsPermission, "floors") {
            hc.readFloorsClimbed(date)
        }
        val elevation = readIfNeeded(wants(DashboardMetric.ELEVATION), readElevationPermission, "elevation") {
            hc.readElevationGained(date)
        }
        val wheelchairPushes = readIfNeeded(
            wants(DashboardMetric.WHEELCHAIR_PUSHES),
            readWheelchairPushesPermission,
            "wheelchair pushes",
        ) {
            hc.readWheelchairPushes(date)
        }
        val mindfulnessMinutes = readIfNeeded(
            wants(DashboardMetric.MINDFULNESS),
            readMindfulnessPermission,
            "mindfulness",
        ) {
            hc.readMindfulnessMinutes(date)
        }
        val menstruationPeriods = readIfNeeded(
            wants(DashboardMetric.CYCLE),
            readMenstruationPeriodPermission,
            "menstruation periods",
        ) {
            hc.readMenstruationPeriods(dayStart, dayEnd)
        }
        val ovulationTests = readIfNeeded(wants(DashboardMetric.CYCLE), readOvulationTestPermission, "ovulation tests") {
            hc.readOvulationTests(dayStart, dayEnd)
        }
        val basalBodyTemperature = readIfNeeded(
            wants(DashboardMetric.CYCLE),
            readBasalBodyTemperaturePermission,
            "basal body temperature",
        ) {
            hc.readBasalBodyTemperatureEntries(dayStart, dayEnd)
                .maxByOrNull { it.time }
                ?.temperatureCelsius
        }

        val missingPerms = dashboardPermissionsFor(
            metrics = metrics,
            showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
        ).filterNot { it in granted }.toSet()
        val latestBloodPressure = bloodPressure?.await()
        val latestWeight = weight?.await()
        val latestHeight = height?.await()
        val dailyMacros = macros?.await()
        val caloriesBurned = calories?.await()
        val dayWorkouts = workouts?.await().orEmpty()
        val dashboardSleep = sleep?.await()
        val trainingSignals = weeklyTrainingSignals?.await()
        val dayHeartRateSamples = heartRateSamples?.await().orEmpty()
        val dayHrvSamples = hrvSamples?.await().orEmpty()
        val dayHrvRmssd = dayHrvSamples
            .takeIf { it.isNotEmpty() }
            ?.map { it.rmssdMs }
            ?.average()

        DashboardData(
            date = date,
            steps = steps?.await() ?: 0L,
            distanceMeters = distance?.await() ?: 0.0,
            caloriesKcal = caloriesBurned?.kcal ?: 0.0,
            caloriesKcalSource = caloriesBurned?.source ?: CaloriesBurnedSource.NO_DATA,
            activeCaloriesKcal = activeCalories?.await(),
            caloriesInKcal = caloriesIn?.await(),
            proteinGrams = dailyMacros?.proteinGrams,
            carbsGrams = dailyMacros?.carbsGrams,
            fatGrams = dailyMacros?.fatGrams,
            hydrationLiters = hydration?.await() ?: 0.0,
            workout = dayWorkouts.firstOrNull(),
            workouts = dayWorkouts,
            sleep = dashboardSleep?.sleep,
            sleepScore = dashboardSleep?.sleepScore ?: SleepScoreEstimate.NoData,
            weightKg = latestWeight?.weightKg,
            weightTime = latestWeight?.time,
            heightCm = latestHeight?.heightCm,
            heightTime = latestHeight?.time,
            bmi = latestWeight?.weightKg?.let { weightKg ->
                latestHeight?.heightCm
                    ?.takeIf { it > 0.0 }
                    ?.let { heightCm -> weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) }
            },
            bodyFatPercent = bodyFat?.await() ?: 0.0,
            leanMassKg = leanMass?.await(),
            bmrKcal = bmr?.await(),
            boneMassKg = boneMass?.await(),
            bodyWaterMassKg = bodyWaterMass?.await(),
            avgHeartRateBpm = heartRate?.await() ?: 0,
            heartRateSampleCount = dayHeartRateSamples.size,
            heartRateSampleStartTime = dayHeartRateSamples.firstOrNull()?.time,
            heartRateSampleEndTime = dayHeartRateSamples.lastOrNull()?.time,
            restingHeartRateBpm = restingHR?.await() ?: 0,
            restingHeartRateBaselineBpm = restingHRBaseline?.await(),
            hrvRmssdMs = dayHrvRmssd,
            hrvBaselineRmssdMs = hrvBaseline?.await(),
            hrvSampleCount = dayHrvSamples.size,
            hrvSampleStartTime = dayHrvSamples.firstOrNull()?.time,
            hrvSampleEndTime = dayHrvSamples.lastOrNull()?.time,
            latestSystolicMmHg = latestBloodPressure?.systolicMmHg,
            latestDiastolicMmHg = latestBloodPressure?.diastolicMmHg,
            latestSpO2Percent = spO2?.await()?.percent,
            latestVo2Max = vo2Max?.await()?.vo2MaxMlPerKgPerMin,
            avgRespiratoryRate = respiratoryRate?.await(),
            latestBodyTemperatureCelsius = bodyTemperature?.await(),
            latestBloodGlucoseMillimolesPerLiter = bloodGlucose?.await(),
            latestSkinTemperatureDeltaCelsius = skinTemperature?.await(),
            weeklyCardioLoad = if (wants(DashboardMetric.WEEKLY_CARDIO_LOAD)) {
                trainingSignals?.cardioLoad
            } else {
                null
            },
            weeklyIntensityMinutes = if (wants(DashboardMetric.INTENSITY_MINUTES)) {
                trainingSignals?.intensityMinutes
            } else {
                null
            },
            floorsClimbed = floors?.await(),
            elevationGainedMeters = elevation?.await(),
            wheelchairPushes = wheelchairPushes?.await(),
            mindfulnessMinutes = mindfulnessMinutes?.await(),
            menstruationPeriodDays = menstruationPeriods?.await()?.sumOf { period ->
                val startDate = period.startTime.atZone(zone).toLocalDate()
                val endDate = period.endTime.minusMillis(1).atZone(zone).toLocalDate()
                (Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toDays())
                    .toInt()
                    .coerceAtLeast(1)
            },
            ovulationTestCount = ovulationTests?.await()?.size,
            latestBasalBodyTemperatureCelsius = basalBodyTemperature?.await(),
            missingPermissions = missingPerms,
            loadedMetrics = metrics,
        )
    }

    private suspend fun readDashboardSleep(
        date: LocalDate,
        sleepRangeMode: SleepRangeMode,
    ): DashboardSleepData {
        val zone = ZoneId.systemDefault()
        val selectedWindow = sleepRangeWindowFor(date, sleepRangeMode, zone)
        val queryStart = sleepRangeWindowFor(date.minusDays(SleepScoreLookbackDays - 1), sleepRangeMode, zone)
            .start
            .minus(Duration.ofDays(1))
        val sessions = hc.readSleepSessions(queryStart, selectedWindow.end)
        val sleep = dailySleepSummary(
            sessions = sessions,
            selectedDate = date,
            sleepRangeMode = sleepRangeMode,
            zone = zone,
        )
        return DashboardSleepData(
            sleep = sleep,
            sleepScore = calculateSleepScoreForDate(
                selectedDate = date,
                sessions = sessions,
                sleepRangeMode = sleepRangeMode,
                zone = zone,
            ),
        )
    }

    private suspend fun readDashboardWeeklyTrainingSignals(
        date: LocalDate,
        activityWeekMode: ActivityWeekMode,
        granted: Set<String>,
    ): DashboardWeeklyTrainingSignals {
        val currentPeriod = dashboardCardioLoadPeriod(date, activityWeekMode)
        val rangeStart = currentPeriod.start.minusDays(DashboardCardioLoadHistoryPeriods * 7)
        val rangeEnd = currentPeriod.end
        val zone = ZoneId.systemDefault()
        val rangeStartInstant = rangeStart.atStartOfDay(zone).toInstant()
        val rangeEndInstant = rangeEnd.plusDays(1).atStartOfDay(zone).toInstant()

        val dailySteps = readDashboardCardioLoadSteps(rangeStart, rangeEnd, granted)
        val heartRateSamples = if (readHeartRatePermission in granted) {
            hc.readHeartRateSamples(rangeStartInstant, rangeEndInstant)
        } else {
            emptyList()
        }
        val restingHeartRates = if (readRestingHRPermission in granted) {
            hc.readDailyRestingHR(rangeStart, rangeEnd)
        } else {
            emptyList()
        }
        val workouts = if (readExercisePermission in granted) {
            hc.readExerciseSessions(rangeStartInstant, rangeEndInstant)
        } else {
            emptyList()
        }

        return withContext(dispatchers.default) {
            val stepsByDate = dailySteps.associateBy { it.date }
            val restingHeartRateByDate = restingHeartRates.associateBy { it.date }
            val baselineRestingHeartRate = restingHeartRates.map { it.bpm }.medianLongOrNull()
            val observedMaxHeartRate = heartRateSamples.maxOfOrNull { it.beatsPerMinute }
            val heartRateSamplesByDate = heartRateSamples
                .sortedBy { it.time }
                .groupBy { it.time.atZone(zone).toLocalDate() }

            val cardioEstimatesByDate = mutableMapOf<LocalDate, CardioLoadEstimate>()
            val intensityEstimatesByDate = mutableMapOf<LocalDate, IntensityMinutesEstimate>()
            datesInRange(rangeStart, rangeEnd).forEach { day ->
                val activityWindows = workouts.cardioLoadWindows(day, zone)
                val cardioLoad = calculateCardioLoad(
                    steps = stepsByDate[day],
                    samples = heartRateSamplesByDate[day].orEmpty(),
                    restingHeartRate = restingHeartRateByDate[day]?.bpm,
                    baselineRestingHeartRate = baselineRestingHeartRate,
                    observedMaxHeartRate = observedMaxHeartRate,
                    activityWindows = activityWindows,
                )
                val intensityMinutes = calculateIntensityMinutes(
                    samples = heartRateSamplesByDate[day].orEmpty(),
                    restingHeartRate = restingHeartRateByDate[day]?.bpm,
                    baselineRestingHeartRate = baselineRestingHeartRate,
                    observedMaxHeartRate = observedMaxHeartRate,
                    activityWindows = activityWindows,
                    workouts = workouts.intensityWorkoutInputs(day, zone),
                    dailyActiveCaloriesKcal = stepsByDate[day]?.activeCaloriesKcal,
                    cardioLoadScore = cardioLoad.score,
                )
                cardioEstimatesByDate[day] = cardioLoad
                intensityEstimatesByDate[day] = intensityMinutes
            }
            val currentPeriodDays = datesInRange(currentPeriod.start, currentPeriod.end).toList()
            val currentPeriodEstimates = datesInRange(currentPeriod.start, currentPeriod.end)
                .map { day -> cardioEstimatesByDate[day] ?: CardioLoadEstimate.NoData }
                .toList()
            val currentScore = currentPeriodEstimates.sumOf { it.score }
            val todayScore = cardioEstimatesByDate[date]?.score ?: 0
            val previousPeriodScores = (1L..DashboardCardioLoadHistoryPeriods).map { periodsAgo ->
                val periodStart = currentPeriod.start.minusDays(periodsAgo * 7)
                val periodEnd = periodStart.plusDays(6)
                datesInRange(periodStart, periodEnd).sumOf { day -> cardioEstimatesByDate[day]?.score ?: 0 }
            }
            val cardioTargetDays = ChronoUnit.DAYS.between(currentPeriod.start, currentPeriod.end).toInt() + 1
            val target = dashboardWeeklyCardioTarget(
                currentScore = currentScore,
                daysElapsed = cardioTargetDays,
                previousWeekScores = previousPeriodScores,
            )
            val cardioLoad = target?.let {
                DashboardWeeklyCardioLoad(
                    currentScore = currentScore,
                    targetScore = it.score,
                    todayScore = todayScore,
                    confidence = currentPeriodEstimates.weeklyCardioConfidence(),
                    targetSource = it.source,
                )
            }
            val currentIntensityEstimates = currentPeriodDays
                .map { day -> intensityEstimatesByDate[day] ?: IntensityMinutesEstimate.NoData }
            val intensityMinutes = DashboardWeeklyIntensityMinutes(
                moderateMinutes = currentIntensityEstimates.sumOf { it.moderateMinutes },
                vigorousMinutes = currentIntensityEstimates.sumOf { it.vigorousMinutes },
                moderateEquivalentMinutes = currentIntensityEstimates.sumOf { it.moderateEquivalentMinutes },
                todayModerateEquivalentMinutes = intensityEstimatesByDate[date]?.moderateEquivalentMinutes ?: 0,
                daysElapsed = (ChronoUnit.DAYS.between(currentPeriod.start, date).toInt() + 1).coerceIn(1, 7),
                confidence = currentIntensityEstimates.weeklyIntensityConfidence(),
            )

            DashboardWeeklyTrainingSignals(
                cardioLoad = cardioLoad,
                intensityMinutes = intensityMinutes,
            )
        }
    }

    private suspend fun readDashboardCardioLoadSteps(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailySteps> =
        when {
            readStepsPermission in granted && readDistancePermission in granted -> {
                hc.readDailySteps(
                    startDate = start,
                    endDate = end,
                    includeActiveCalories = readActiveCaloriesPermission in granted,
                )
            }
            readStepsPermission in granted -> {
                buildList {
                    datesInRange(start, end).forEach { date ->
                        add(
                            DailySteps(
                                date = date,
                                steps = hc.readSteps(date),
                                distanceMeters = 0.0,
                            )
                        )
                    }
                }
            }
            else -> emptyList()
        }

    private fun canEstimateTotalCalories(
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
    ): Boolean =
        showOpenVitalsCalculatedCalories && readActiveCaloriesPermission in granted && readBmrPermission in granted

    private suspend fun <T> dashboardMetric(name: String, block: suspend () -> T): T? =
        runCatching { block() }
            .onFailure { Log.w(TAG, "Skipping dashboard metric $name after Health Connect failure", it) }
            .getOrNull()

    private fun dashboardPermissionsFor(
        metrics: Set<DashboardMetric>,
        showOpenVitalsCalculatedCalories: Boolean,
    ): Set<String> =
        metrics.flatMapTo(mutableSetOf()) { metric ->
            when (metric) {
                DashboardMetric.STEPS -> setOf(readStepsPermission)
                DashboardMetric.DISTANCE -> setOf(readDistancePermission)
                DashboardMetric.CALORIES_OUT -> if (showOpenVitalsCalculatedCalories) {
                    setOf(readCaloriesPermission, readActiveCaloriesPermission, readBmrPermission)
                } else {
                    setOf(readCaloriesPermission)
                }
                DashboardMetric.ACTIVE_CALORIES -> setOf(
                    readActiveCaloriesPermission,
                    readStepsPermission,
                    readDistancePermission,
                )
                DashboardMetric.FLOORS -> setOf(readFloorsPermission)
                DashboardMetric.ELEVATION -> setOf(readElevationPermission)
                DashboardMetric.WHEELCHAIR_PUSHES -> setOf(readWheelchairPushesPermission)
                DashboardMetric.WORKOUT -> setOf(readExercisePermission)
                DashboardMetric.SLEEP -> setOf(readSleepPermission)
                DashboardMetric.HYDRATION -> setOf(readHydrationPermission)
                DashboardMetric.CALORIES_IN,
                DashboardMetric.PROTEIN,
                DashboardMetric.CARBS,
                DashboardMetric.FAT -> setOf(readNutritionPermission)
                DashboardMetric.WEIGHT -> setOf(readWeightPermission)
                DashboardMetric.HEIGHT -> setOf(readHeightPermission)
                DashboardMetric.BMI -> setOf(readWeightPermission, readHeightPermission)
                DashboardMetric.BODY_FAT -> setOf(readBodyFatPermission)
                DashboardMetric.LEAN_MASS -> setOf(readLeanMassPermission)
                DashboardMetric.BMR -> setOf(readBmrPermission)
                DashboardMetric.BONE_MASS -> setOf(readBoneMassPermission)
                DashboardMetric.BODY_WATER_MASS -> setOf(readBodyWaterMassPermission)
                DashboardMetric.AVG_HEART_RATE -> setOf(readHeartRatePermission)
                DashboardMetric.RESTING_HEART_RATE -> setOf(readRestingHRPermission)
                DashboardMetric.HRV -> setOf(readHrvPermission)
                DashboardMetric.BLOOD_PRESSURE -> setOf(readBloodPressurePermission)
                DashboardMetric.SPO2 -> setOf(readSpO2Permission)
                DashboardMetric.VO2_MAX -> setOf(readVo2MaxPermission)
                DashboardMetric.RESPIRATORY_RATE -> setOf(readRespiratoryRatePermission)
                DashboardMetric.BODY_TEMPERATURE -> setOf(readBodyTemperaturePermission)
                DashboardMetric.BLOOD_GLUCOSE -> setOf(readBloodGlucosePermission)
                DashboardMetric.SKIN_TEMPERATURE -> if (hc.isSkinTemperatureAvailable()) {
                    setOf(readSkinTemperaturePermission)
                } else {
                    emptySet()
                }
                DashboardMetric.WEEKLY_CARDIO_LOAD -> setOf(readStepsPermission)
                DashboardMetric.INTENSITY_MINUTES -> setOf(
                    readHeartRatePermission,
                    readRestingHRPermission,
                    readExercisePermission,
                    readActiveCaloriesPermission,
                    readStepsPermission,
                    readDistancePermission,
                )
                DashboardMetric.MINDFULNESS -> setOf(readMindfulnessPermission)
                DashboardMetric.CYCLE -> setOf(
                    readMenstruationPeriodPermission,
                    readOvulationTestPermission,
                    readBasalBodyTemperaturePermission,
                )
            }
        }
}

private data class DashboardSleepData(
    val sleep: SleepData?,
    val sleepScore: SleepScoreEstimate,
)

private data class DashboardWeeklyTrainingSignals(
    val cardioLoad: DashboardWeeklyCardioLoad?,
    val intensityMinutes: DashboardWeeklyIntensityMinutes,
)

private data class DashboardWeeklyCardioTarget(
    val score: Int,
    val source: DashboardWeeklyCardioLoadTargetSource,
)

private fun dashboardCardioLoadPeriod(
    date: LocalDate,
    activityWeekMode: ActivityWeekMode,
): DatePeriod =
    periodFor(
        range = TimeRange.WEEK,
        anchorDate = date,
        today = date,
        weekPeriodMode = activityWeekMode.toWeekPeriodMode(),
    )

private fun dashboardWeeklyCardioTarget(
    currentScore: Int,
    daysElapsed: Int,
    previousWeekScores: List<Int>,
): DashboardWeeklyCardioTarget? {
    val previousBaseline = previousWeekScores
        .filter { it > 0 }
        .medianDoubleOrNull()
    if (previousBaseline != null) {
        return DashboardWeeklyCardioTarget(
            score = previousBaseline.roundCardioTarget(),
            source = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
        )
    }

    if (currentScore <= 0 || daysElapsed <= 0) return null
    return DashboardWeeklyCardioTarget(
        score = (currentScore * 7.0 / daysElapsed).roundCardioTarget(),
        source = DashboardWeeklyCardioLoadTargetSource.CURRENT_PACE,
    )
}

private fun List<CardioLoadEstimate>.weeklyCardioConfidence(): CardioLoadConfidence {
    val tracked = filter { it.score > 0 && it.confidence != CardioLoadConfidence.NO_DATA }
    return when {
        tracked.isEmpty() -> CardioLoadConfidence.NO_DATA
        tracked.any { it.confidence == CardioLoadConfidence.HIGH } -> CardioLoadConfidence.HIGH
        tracked.any { it.confidence == CardioLoadConfidence.MEDIUM } -> CardioLoadConfidence.MEDIUM
        else -> CardioLoadConfidence.LOW
    }
}

private fun List<IntensityMinutesEstimate>.weeklyIntensityConfidence(): IntensityMinutesConfidence {
    val tracked = filter {
        it.moderateEquivalentMinutes > 0 && it.confidence != IntensityMinutesConfidence.NO_DATA
    }
    return when {
        tracked.isEmpty() -> IntensityMinutesConfidence.NO_DATA
        tracked.any { it.confidence == IntensityMinutesConfidence.HIGH } -> IntensityMinutesConfidence.HIGH
        tracked.any { it.confidence == IntensityMinutesConfidence.MEDIUM } -> IntensityMinutesConfidence.MEDIUM
        else -> IntensityMinutesConfidence.LOW
    }
}

private fun List<ExerciseData>.cardioLoadWindows(date: LocalDate, zone: ZoneId): List<CardioLoadTimeWindow> {
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    return mapNotNull { workout ->
        if (!workout.endTime.isAfter(dayStart) || !workout.startTime.isBefore(dayEnd)) return@mapNotNull null
        CardioLoadTimeWindow(
            start = maxOf(workout.startTime, dayStart),
            end = minOf(workout.endTime, dayEnd),
        ).takeIf { it.durationMinutes > 0.0 }
    }
}

private fun List<ExerciseData>.intensityWorkoutInputs(date: LocalDate, zone: ZoneId): List<IntensityWorkoutInput> {
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    return mapNotNull { workout ->
        if (!workout.endTime.isAfter(dayStart) || !workout.startTime.isBefore(dayEnd)) return@mapNotNull null
        val overlapStart = maxOf(workout.startTime, dayStart)
        val overlapEnd = minOf(workout.endTime, dayEnd)
        if (!overlapEnd.isAfter(overlapStart)) return@mapNotNull null
        val overlapMinutes = Duration.between(overlapStart, overlapEnd).seconds.toDouble() / 60.0
        if (overlapMinutes <= 0.0) return@mapNotNull null
        val totalMinutes = workout.durationMs.coerceAtLeast(0L).toDouble() / 60_000.0
        val activeCalories = workout.activeCaloriesKcal?.takeIf { totalMinutes > 0.0 }?.let { calories ->
            calories * (overlapMinutes / totalMinutes)
        }
        IntensityWorkoutInput(
            durationMinutes = overlapMinutes,
            activeCaloriesKcal = activeCalories,
        )
    }
}

private fun datesInRange(start: LocalDate, end: LocalDate): Sequence<LocalDate> =
    if (start.isAfter(end)) {
        emptySequence()
    } else {
        generateSequence(start) { date ->
            date.plusDays(1).takeUnless { it.isAfter(end) }
        }
    }

private fun List<Long>.medianLongOrNull(): Long? {
    if (isEmpty()) return null
    val sorted = sorted()
    return sorted[sorted.lastIndex / 2]
}

private fun List<Int>.medianDoubleOrNull(): Double? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle].toDouble()
    }
}

private fun List<Double>.medianDoubleValuesOrNull(): Double? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle]
    }
}

private fun Double.roundCardioTarget(): Int =
    ((this / 5.0).roundToInt() * 5).coerceAtLeast(5)
