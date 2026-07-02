package tech.mmarca.openvitals.data.repository.dashboard

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
import tech.mmarca.openvitals.core.performance.DashboardLoadCoalesceKey
import tech.mmarca.openvitals.core.performance.DashboardLoadCoalescer
import tech.mmarca.openvitals.core.performance.DispatcherProvider
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
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodFor
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
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.dailySleepSummary
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.cardioLoadWindows
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.datesInRange
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.intensityWorkoutInputs
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.medianDoubleOrNull
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.medianDoubleValuesOrNull
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.medianLongOrNull
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.roundCardioTarget
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.weeklyCardioConfidence
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.weeklyIntensityConfidence
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

@Singleton
class DashboardDataLoader @Inject constructor(
    private val hc: HealthConnectManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val preferencesRepository: PreferencesRepository? = null,
) {
    companion object {
        private const val TAG = "DashboardDataLoader"
        private const val DashboardCardioLoadHistoryPeriods = 4L
        private const val DashboardWeeklyCardioHeartRateSampleWeeks = 2L
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
    private val dashboardLoadCoalescer = DashboardLoadCoalescer()

    suspend fun grantedPermissionsIfAvailable(): Set<String> =
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
            val inputs = dashboardLoadInputs()
            if (query.refreshMode == RefreshMode.NORMAL) {
                val coalesceKey = DashboardLoadCoalesceKey.from(
                    query = query,
                    granted = inputs.granted,
                    showOpenVitalsCalculatedCalories = inputs.showOpenVitalsCalculatedCalories,
                )
                dashboardLoadCoalescer.getOrPut(coalesceKey) {
                    loadDashboardInternal(query, inputs)
                }
            } else {
                loadDashboardInternal(query, inputs)
            }
        }

    private suspend fun dashboardLoadInputs(): DashboardLoadInputs =
        DashboardLoadInputs(
            granted = grantedPermissionsIfAvailable(),
            showOpenVitalsCalculatedCalories = preferencesRepository?.showOpenVitalsCalculatedCalories == true,
        )

    private suspend fun loadDashboardInternal(
        query: DashboardQuery,
        inputs: DashboardLoadInputs,
    ): DashboardData {
        val startedAt = System.currentTimeMillis()
        val granted = inputs.granted
        val loadMetrics = query.visibleMetrics
        val showOpenVitalsCalculatedCalories = inputs.showOpenVitalsCalculatedCalories
        val data = loadDashboardFromHealthConnect(
            query = query,
            loadMetrics = loadMetrics,
            granted = granted,
            showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
            calculateDerivedMetrics = true,
        )
        Log.d(
            TAG,
            "loadDashboard completed metrics=${loadMetrics.size} " +
                "durationMs=${System.currentTimeMillis() - startedAt}",
        )
        return data
    }

    private suspend fun loadDashboardFromHealthConnect(
        query: DashboardQuery,
        loadMetrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
        calculateDerivedMetrics: Boolean = true,
    ): DashboardData =
        PerformanceTrace.timed(
            name = "dashboard.healthConnect",
            attributes = mapOf(
                "date" to query.date,
                "metrics" to loadMetrics.size,
            ),
        ) {
            loadDashboardUncached(
                query = query,
                metrics = loadMetrics,
                granted = granted,
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                calculateDerivedMetrics = calculateDerivedMetrics,
            )
        }

    private suspend fun loadDashboardUncached(
        query: DashboardQuery,
        metrics: Set<DashboardMetric>,
        granted: Set<String>,
        showOpenVitalsCalculatedCalories: Boolean,
        calculateDerivedMetrics: Boolean = true,
    ): DashboardData = coroutineScope {
        val date = query.date
        val sleepRangeMode = query.sleepRangeMode
        val activityWeekMode = query.activityWeekMode
        Log.d(
            TAG,
            "loadDashboard metrics=${metrics.sortedBy { it.name }} grantedCount=${granted.size}",
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
                calculateSleepScore = calculateDerivedMetrics,
            )
        }
        val calories = readIfNeeded(wants(DashboardMetric.CALORIES_OUT), readCaloriesPermission, "calories") {
            hc.readCaloriesBurned(
                date = date,
                includeEstimatedCalories = calculateDerivedMetrics &&
                    canEstimateTotalCalories(granted, showOpenVitalsCalculatedCalories),
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
            wantsAny(DashboardMetric.PROTEIN, DashboardMetric.CARBS, DashboardMetric.FAT, DashboardMetric.CAFFEINE),
            readNutritionPermission,
            "macros",
        ) {
            hc.readDailyMacros(date, date).firstOrNull()
        }
        val hydration = readIfNeeded(wants(DashboardMetric.HYDRATION), readHydrationPermission, "hydration") {
            hc.readHydrationLiters(date)
        }
        val weight = readIfNeeded(
            wantsAny(DashboardMetric.WEIGHT, DashboardMetric.BMI, DashboardMetric.FFMI),
            readWeightPermission,
            "weight",
        ) {
            hc.readLatestWeight()
        }
        val height = readIfNeeded(
            wantsAny(DashboardMetric.HEIGHT, DashboardMetric.BMI, DashboardMetric.FFMI),
            readHeightPermission,
            "height",
        ) {
            hc.readLatestHeightEntry()
        }
        val bodyFat = readIfNeeded(
            wantsAny(DashboardMetric.BODY_FAT, DashboardMetric.FFMI),
            readBodyFatPermission,
            "body fat",
        ) {
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
        val restingHR = readIfNeeded(
            wants(DashboardMetric.RESTING_HEART_RATE),
            readRestingHRPermission,
            "resting heart rate",
        ) {
            hc.readRestingHeartRate(date)
        }
        val restingHRBaseline = readIfNeeded(
            calculateDerivedMetrics && query.includeHistoricalBaselines && wants(DashboardMetric.RESTING_HEART_RATE),
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
        val hrvBaseline = readIfNeeded(
            calculateDerivedMetrics && query.includeHistoricalBaselines && wants(DashboardMetric.HRV),
            readHrvPermission,
            "HRV baseline",
        ) {
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
        val weeklyTrainingSignals = if (
            calculateDerivedMetrics &&
            query.includeWeeklyTrainingSignals &&
            wantsAny(DashboardMetric.WEEKLY_CARDIO_LOAD, DashboardMetric.INTENSITY_MINUTES)
        ) {
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
        val dayHrvSamples = hrvSamples?.await().orEmpty()
        val dayHrvRmssd = dayHrvSamples
            .takeIf { it.isNotEmpty() }
            ?.map { it.rmssdMs }
            ?.average()
        val latestBodyFatPercent = bodyFat?.await()

        val metricSourcePackages = buildMap {
            fun putSource(metric: DashboardMetric, source: String?) {
                source?.takeIf { it.isNotBlank() }?.let { put(metric, it) }
            }
            if (wants(DashboardMetric.SLEEP)) {
                putSource(DashboardMetric.SLEEP, dashboardSleep?.sleep?.source)
            }
            if (wantsAny(DashboardMetric.WEIGHT, DashboardMetric.BMI, DashboardMetric.FFMI)) {
                putSource(DashboardMetric.WEIGHT, latestWeight?.source)
            }
            if (wantsAny(DashboardMetric.HEIGHT, DashboardMetric.BMI, DashboardMetric.FFMI)) {
                putSource(DashboardMetric.HEIGHT, latestHeight?.source)
            }
            if (wants(DashboardMetric.BLOOD_PRESSURE)) {
                putSource(DashboardMetric.BLOOD_PRESSURE, latestBloodPressure?.source)
            }
            if (wants(DashboardMetric.SPO2)) {
                putSource(DashboardMetric.SPO2, spO2?.await()?.source)
            }
            if (wants(DashboardMetric.VO2_MAX)) {
                putSource(DashboardMetric.VO2_MAX, vo2Max?.await()?.source)
            }
            if (wants(DashboardMetric.WORKOUT)) {
                dayWorkouts.firstOrNull()?.source?.let { put(DashboardMetric.WORKOUT, it) }
            }
        }

        DashboardData(
            date = date,
            steps = steps?.await() ?: 0L,
            distanceMeters = distance?.await() ?: 0.0,
            caloriesKcal = caloriesBurned?.kcal ?: 0.0,
            caloriesKcalSource = caloriesBurned?.source ?: CaloriesBurnedSource.NO_DATA,
            activeCaloriesKcal = activeCalories?.await(),
            caloriesInKcal = caloriesIn?.await()?.takeIf { it > 0.0 },
            proteinGrams = dailyMacros?.proteinGrams?.takeIf { it > 0.0 },
            carbsGrams = dailyMacros?.carbsGrams?.takeIf { it > 0.0 },
            fatGrams = dailyMacros?.fatGrams?.takeIf { it > 0.0 },
            caffeineGrams = dailyMacros
                ?.nutrientValues
                ?.get(NutritionNutrient.CAFFEINE)
                ?.takeIf { it > 0.0 },
            hydrationLiters = hydration?.await() ?: 0.0,
            workout = dayWorkouts.firstOrNull(),
            workouts = dayWorkouts,
            sleep = dashboardSleep?.sleep,
            sleepScore = dashboardSleep?.sleepScore ?: SleepScoreEstimate.NoData,
            weightKg = latestWeight?.weightKg,
            weightTime = latestWeight?.time,
            heightCm = latestHeight?.heightCm,
            heightTime = latestHeight?.time,
            bmi = if (calculateDerivedMetrics && wants(DashboardMetric.BMI)) {
                latestWeight?.weightKg?.let { weightKg ->
                    latestHeight?.heightCm
                        ?.takeIf { it > 0.0 }
                        ?.let { heightCm -> weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) }
                }
            } else {
                null
            },
            ffmi = if (calculateDerivedMetrics && wants(DashboardMetric.FFMI)) {
                calculateAdjustedFfmi(
                    weightKg = latestWeight?.weightKg,
                    heightCm = latestHeight?.heightCm,
                    bodyFatPercent = latestBodyFatPercent,
                )
            } else {
                null
            },
            bodyFatPercent = latestBodyFatPercent ?: 0.0,
            leanMassKg = leanMass?.await(),
            bmrKcal = bmr?.await(),
            boneMassKg = boneMass?.await(),
            bodyWaterMassKg = bodyWaterMass?.await(),
            avgHeartRateBpm = heartRate?.await() ?: 0,
            restingHeartRateBpm = restingHR?.await() ?: 0,
            restingHeartRateBaselineBpm = restingHRBaseline?.await(),
            hrvRmssdMs = if (calculateDerivedMetrics) dayHrvRmssd else null,
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
            metricSourcePackages = metricSourcePackages,
        )
    }

    private fun calculateAdjustedFfmi(
        weightKg: Double?,
        heightCm: Double?,
        bodyFatPercent: Double?,
    ): Double? {
        val weight = weightKg?.takeIf { it > 0.0 } ?: return null
        val heightMeters = heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 } ?: return null
        val bodyFatRatio = bodyFatPercent
            ?.takeIf { it in 0.0..100.0 }
            ?.let { it / 100.0 }
            ?: return null
        val fatFreeMassKg = weight * (1.0 - bodyFatRatio)
        val ffmi = fatFreeMassKg / (heightMeters * heightMeters)
        return ffmi + (6.3 * (1.8 - heightMeters))
    }

    private suspend fun readDashboardSleep(
        date: LocalDate,
        sleepRangeMode: SleepRangeMode,
        calculateSleepScore: Boolean = true,
    ): DashboardSleepData {
        val zone = ZoneId.systemDefault()
        val sleepData = hc.readSleepData(
            startDate = date.minusDays(SleepScoreLookbackDays - 1),
            endDate = date,
            sleepRangeMode = sleepRangeMode,
        )
        val sessions = sleepData.sessions
        val aggregateDurationMs = sleepData.dailyAggregateDurations
            .firstOrNull { it.date == date }
            ?.durationMs
            ?: 0L
        val sleep = dailySleepSummary(
            sessions = sessions,
            selectedDate = date,
            sleepRangeMode = sleepRangeMode,
            zone = zone,
        )?.let { summary ->
            aggregateDurationMs
                .takeIf { it > 0L }
                ?.let { summary.copy(durationMs = it) }
                ?: summary
        }
        return DashboardSleepData(
            sleep = sleep,
            sleepScore = if (calculateSleepScore) {
                calculateSleepScoreForDate(
                    selectedDate = date,
                    sessions = sessions,
                    sleepRangeMode = sleepRangeMode,
                    zone = zone,
                )
            } else {
                SleepScoreEstimate.NoData
            },
        )
    }

    private suspend fun readDashboardWeeklyTrainingRawData(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        currentPeriodStart: LocalDate,
        currentPeriodEnd: LocalDate,
        heartRateSampleStart: LocalDate,
        heartRateSampleEnd: LocalDate,
        activityWeekMode: ActivityWeekMode,
        granted: Set<String>,
    ): DashboardWeeklyTrainingRawData {
        val zone = ZoneId.systemDefault()
        val rangeStartInstant = rangeStart.atStartOfDay(zone).toInstant()
        val rangeEndInstant = rangeEnd.plusDays(1).atStartOfDay(zone).toInstant()
        val heartRateSampleStartInstant = heartRateSampleStart.atStartOfDay(zone).toInstant()
        val heartRateSampleEndInstant = heartRateSampleEnd.plusDays(1).atStartOfDay(zone).toInstant()
        // Older history weeks use steps/workout fallback for cardio targets; HR samples are
        // limited to two weeks (current period plus one prior week) to balance accuracy and cost.
        return DashboardWeeklyTrainingRawData(
            dailySteps = readDashboardCardioLoadSteps(rangeStart, rangeEnd, granted),
            heartRateSamples = if (readHeartRatePermission in granted) {
                hc.readHeartRateSamples(heartRateSampleStartInstant, heartRateSampleEndInstant)
            } else {
                emptyList()
            },
            restingHeartRates = if (readRestingHRPermission in granted) {
                hc.readDailyRestingHR(rangeStart, rangeEnd)
            } else {
                emptyList()
            },
            workouts = if (readExercisePermission in granted) {
                hc.readExerciseSessions(rangeStartInstant, rangeEndInstant)
            } else {
                emptyList()
            },
        )
    }

    private suspend fun readDashboardWeeklyTrainingSignals(
        date: LocalDate,
        activityWeekMode: ActivityWeekMode,
        granted: Set<String>,
    ): DashboardWeeklyTrainingSignals {
        val currentPeriod = DashboardAggregator.cardioLoadPeriod(date, activityWeekMode)
        val rangeStart = currentPeriod.start.minusDays(DashboardCardioLoadHistoryPeriods * 7)
        val rangeEnd = currentPeriod.end
        val zone = ZoneId.systemDefault()

        val heartRateSampleStart = currentPeriod.start.minusDays(
            (DashboardWeeklyCardioHeartRateSampleWeeks - 1) * 7,
        )

        val rawData = readDashboardWeeklyTrainingRawData(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            currentPeriodStart = currentPeriod.start,
            currentPeriodEnd = currentPeriod.end,
            heartRateSampleStart = heartRateSampleStart,
            heartRateSampleEnd = currentPeriod.end,
            activityWeekMode = activityWeekMode,
            granted = granted,
        )
        val dailySteps = rawData.dailySteps
        val heartRateSamples = rawData.heartRateSamples
        val restingHeartRates = rawData.restingHeartRates
        val workouts = rawData.workouts

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
            val target = DashboardAggregator.weeklyCardioTarget(
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
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w(TAG, "Skipping dashboard metric $name after Health Connect failure", error)
            null
        }

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
                DashboardMetric.FAT,
                DashboardMetric.CAFFEINE,
                -> setOf(readNutritionPermission)
                DashboardMetric.WEIGHT -> setOf(readWeightPermission)
                DashboardMetric.HEIGHT -> setOf(readHeightPermission)
                DashboardMetric.BMI -> setOf(readWeightPermission, readHeightPermission)
                DashboardMetric.FFMI -> setOf(readWeightPermission, readHeightPermission, readBodyFatPermission)
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

private data class DashboardLoadInputs(
    val granted: Set<String>,
    val showOpenVitalsCalculatedCalories: Boolean,
)

private data class DashboardSleepData(
    val sleep: SleepData?,
    val sleepScore: SleepScoreEstimate,
)

private data class DashboardWeeklyTrainingSignals(
    val cardioLoad: DashboardWeeklyCardioLoad?,
    val intensityMinutes: DashboardWeeklyIntensityMinutes,
)

private data class DashboardWeeklyTrainingRawData(
    val dailySteps: List<DailySteps>,
    val heartRateSamples: List<HeartRateSample>,
    val restingHeartRates: List<DailyRestingHR>,
    val workouts: List<ExerciseData>,
)
