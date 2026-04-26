package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.CervicalMucusEntry
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DataSource
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.MenstruationFlowEntry
import tech.mmarca.openvitals.data.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.data.model.OvulationTestEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.StepProgressPoint
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Wraps the Health Connect AndroidX client.
 *
 * All public methods are suspend functions; call them from a coroutine scope.
 * Methods degrade gracefully when called without the required permission —
 * they return empty collections rather than throwing.
 */
@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthConnectManager(private val context: Context) {
    companion object {
        private const val TAG = "HealthConnectManager"
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    val corePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    val heartPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
    )

    val bodyPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(BoneMassRecord::class),
    )

    val activityExtrasPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    val nutritionHydrationPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    val mindfulnessPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    )

    val vitalsPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
    )

    val cyclePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MenstruationFlowRecord::class),
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(OvulationTestRecord::class),
        HealthPermission.getReadPermission(CervicalMucusRecord::class),
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
    )

    /** Phase 1 – core metrics requested on first launch */
    val phase1Permissions: Set<String> = corePermissions

    /** Phase 2 – extended metrics requested by category during onboarding */
    val phase2Permissions: Set<String>
        get() = heartPermissions +
            bodyPermissions +
            activityExtrasPermissions +
            nutritionHydrationPermissions +
            (if (isMindfulnessSessionAvailable()) mindfulnessPermissions else emptySet())

    /** Phase 3 – vitals, requested by category during onboarding or when opening Heart & Vitals */
    val phase3Permissions: Set<String> = vitalsPermissions

    /** Phase 4 – sensitive cycle tracking, requested only after explicit opt-in from Settings */
    val phase4Permissions: Set<String> = cyclePermissions

    val allPermissions: Set<String> get() = phase1Permissions + phase2Permissions + phase3Permissions

    val managedPermissions: Set<String> get() = allPermissions + phase4Permissions

    // ─── Availability ─────────────────────────────────────────────────────────

    private fun diagnosticsSummary(): String =
        "pkg=${context.packageName}, uid=${Process.myUid()}, sdk=${Build.VERSION.SDK_INT}, profile=${isRunningInUnsupportedProfile()}"

    private fun isRunningInUnsupportedProfile(): Boolean =
        context.getSystemService(UserManager::class.java)?.isProfile == true

    fun availability(): HealthConnectAvailability {
        if (isRunningInUnsupportedProfile()) {
            Log.w(TAG, "Health Connect unavailable in current profile: ${diagnosticsSummary()}")
            return HealthConnectAvailability.NOT_SUPPORTED
        }

        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        val availability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NEEDS_PROVIDER_UPDATE
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
        Log.d(TAG, "availability=$availability sdkStatus=$sdkStatus ${diagnosticsSummary()}")
        return availability
    }

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    fun isMindfulnessSessionAvailable(): Boolean {
        if (availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = withLogging(
            "features.getFeatureStatus[mindfulness]",
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        ) {
            client().features.getFeatureStatus(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION)
        }
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "mindfulnessFeatureStatus=$status available=$available ${diagnosticsSummary()}")
        return available
    }

    // ─── Permission queries ───────────────────────────────────────────────────

    private inline fun <T> withLogging(
        operation: String,
        fallback: T,
        block: () -> T,
    ): T = try {
        Log.d(TAG, "Starting $operation ${diagnosticsSummary()}")
        block().also {
            Log.d(TAG, "Finished $operation successfully")
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed $operation ${diagnosticsSummary()}", t)
        fallback
    }

    private inline fun <T> withNullableLogging(
        operation: String,
        block: () -> T?,
    ): T? = try {
        Log.d(TAG, "Starting $operation ${diagnosticsSummary()}")
        block().also {
            Log.d(TAG, "Finished $operation successfully")
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed $operation ${diagnosticsSummary()}", t)
        null
    }

    suspend fun grantedPermissions(): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            managedPermissions.filterTo(mutableSetOf()) { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }.also { granted ->
                Log.d(TAG, "grantedPermissions(runtime) count=${granted.size} granted=${granted.sorted()} ${diagnosticsSummary()}")
            }
        } else {
            withLogging("permissionController.getGrantedPermissions", emptySet()) {
                client().permissionController.getGrantedPermissions()
            }.also { granted ->
                Log.d(TAG, "grantedPermissions(client) count=${granted.size} granted=${granted.sorted()}")
            }
        }

    suspend fun hasPermission(permission: String): Boolean =
        grantedPermissions().contains(permission)

    fun permissionContract() =
        PermissionController.createRequestPermissionResultContract()

    // ─── Steps ───────────────────────────────────────────────────────────────

    suspend fun readSteps(date: LocalDate): Long {
        val (start, end) = dayRange(date)
        return withLogging("readSteps[$date][$start..$end]", 0L) {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    }

    suspend fun readTodaySteps(): Long = readSteps(LocalDate.now())

    suspend fun readDailySteps(
        startDate: LocalDate,
        endDate: LocalDate,
        includeFloors: Boolean = false,
        includeActiveCalories: Boolean = false,
        includeElevation: Boolean = false,
    ): List<DailySteps> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailySteps[$start..$end]", emptyList()) {
            val metrics = buildSet {
                add(StepsRecord.COUNT_TOTAL)
                add(DistanceRecord.DISTANCE_TOTAL)
                if (includeFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
                if (includeActiveCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                if (includeElevation) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
            }
            client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                val date = bucket.startTime.atZone(zone).toLocalDate()
                DailySteps(
                    date = date,
                    steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L,
                    distanceMeters = bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
                    floorsClimbed = if (includeFloors) bucket.result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0 else null,
                    activeCaloriesKcal = if (includeActiveCalories) bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0 else null,
                    elevationGainedMeters = if (includeElevation) bucket.result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0 else null,
                )
            }
        }
    }

    suspend fun readFloorsClimbed(date: LocalDate): Int {
        val (start, end) = dayRange(date)
        return withLogging("readFloorsClimbed[$date][$start..$end]", 0) {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0
        }
    }

    suspend fun readElevationGained(date: LocalDate): Double {
        val (start, end) = dayRange(date)
        return withLogging("readElevationGained[$date][$start..$end]", 0.0) {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(ElevationGainedRecord.ELEVATION_GAINED_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0
        }
    }

    suspend fun readStepProgress(date: LocalDate): List<StepProgressPoint> {
        val (start, end) = dayRange(date)
        return withLogging("readStepProgress[$date][$start..$end]", emptyList()) {
            var runningTotal = 0L
            client().readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 1000,
                )
            ).records.map { record ->
                runningTotal += record.count
                StepProgressPoint(
                    time = record.endTime,
                    totalSteps = runningTotal,
                )
            }
        }
    }

    suspend fun readActivityProgress(
        date: LocalDate,
        includeDistance: Boolean,
        includeCalories: Boolean,
    ): List<ActivityProgressPoint> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now()) Instant.now() else date.plusDays(1).atStartOfDay(zone).toInstant()
        val metrics = buildSet {
            add(StepsRecord.COUNT_TOTAL)
            if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
            if (includeCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
        }
        return withLogging("readActivityProgress[$date][$start..$end]", emptyList()) {
            var cumulativeSteps = 0L
            var cumulativeDistance = 0.0
            var cumulativeCalories = 0.0

            client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofHours(1),
                )
            ).map { bucket ->
                cumulativeSteps += bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                if (includeDistance) {
                    cumulativeDistance += bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                }
                if (includeCalories) {
                    cumulativeCalories += bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                }
                ActivityProgressPoint(
                    time = bucket.startTime.plus(Duration.ofHours(1)),
                    totalSteps = cumulativeSteps,
                    totalDistanceMeters = if (includeDistance) cumulativeDistance else null,
                    totalCaloriesBurnedKcal = if (includeCalories) cumulativeCalories else null,
                )
            }
        }
    }

    // ─── Distance ────────────────────────────────────────────────────────────

    suspend fun readDistanceMeters(date: LocalDate): Double {
        val (start, end) = dayRange(date)
        return withLogging("readDistanceMeters[$date][$start..$end]", 0.0) {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
        }
    }

    suspend fun readTodayDistanceMeters(): Double = readDistanceMeters(LocalDate.now())

    // ─── Calories ────────────────────────────────────────────────────────────

    suspend fun readCaloriesKcal(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return withNullableLogging("readCaloriesKcal[$date][$start..$end]") {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
        }
    }

    suspend fun readTodayCaloriesKcal(): Double? = readCaloriesKcal(LocalDate.now())

    suspend fun readCaloriesInKcal(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return withNullableLogging("readCaloriesInKcal[$date][$start..$end]") {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[NutritionRecord.ENERGY_TOTAL]?.inKilocalories
        }
    }

    // ─── Hydration ───────────────────────────────────────────────────────────

    suspend fun readHydrationLiters(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return withNullableLogging("readHydrationLiters[$date][$start..$end]") {
            val aggregateLiters = client().aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[HydrationRecord.VOLUME_TOTAL]?.inLiters
            aggregateLiters?.takeIf { it > 0.0 }
                ?: readHydrationRecordsByDate(start, end, zone).values.sum().takeIf { it > 0.0 }
                ?: aggregateLiters
        }
    }

    suspend fun readTodayHydrationLiters(): Double? = readHydrationLiters(LocalDate.now())

    suspend fun readDailyHydration(startDate: LocalDate, endDate: LocalDate): List<DailyHydration> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailyHydration[$start..$end]", emptyList()) {
            val aggregateBuckets = client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyHydration(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    liters = bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0,
                )
            }
            val hydrationByDate = if (aggregateBuckets.any { it.liters > 0.0 }) {
                aggregateBuckets.associate { it.date to it.liters }
            } else {
                readHydrationRecordsByDate(start, end, zone)
            }
            dailyHydrationSeries(startDate, endDate, hydrationByDate)
        }
    }

    // ─── Exercise sessions ───────────────────────────────────────────────────

    suspend fun readLatestWorkout(date: LocalDate): ExerciseData? {
        val (start, end) = dayRange(date)
        return withNullableLogging("readLatestWorkout[$date][$start..$end]") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.toExerciseData()
        }
    }

    suspend fun readLatestWorkout(): ExerciseData? =
        withNullableLogging("readLatestWorkout") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.toExerciseData()
        }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseData> =
        withLogging("readExerciseSessions[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 50,
                )
            ).records.map { it.toExerciseData() }
        }

    // ─── Sleep sessions ──────────────────────────────────────────────────────

    suspend fun readSleepSession(date: LocalDate): SleepData? {
        val (start, end) = dayRange(date)
        val queryStart = start.minus(Duration.ofDays(1))
        return withNullableLogging("readSleepSession[$date][$queryStart..$end]") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(queryStart, end),
                    ascendingOrder = false,
                    pageSize = 10,
                )
            ).records.firstOrNull { record ->
                !record.endTime.isBefore(start) && record.endTime.isBefore(end)
            }?.toSleepData()
        }
    }

    suspend fun readLastSleepSession(): SleepData? =
        withNullableLogging("readLastSleepSession") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.toSleepData()
        }

    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepData> =
        withLogging("readSleepSessions[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 50,
                )
            ).records.map { it.toSleepData() }
        }

    // ─── Heart rate ──────────────────────────────────────────────────────────

    suspend fun readAvgHeartRate(date: LocalDate): Long? {
        val (start, end) = dayRange(date)
        return withNullableLogging("readAvgHeartRate[$date][$start..$end]") {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[HeartRateRecord.BPM_AVG]
        }
    }

    suspend fun readAvgHeartRateToday(): Long? = readAvgHeartRate(LocalDate.now())

    suspend fun readHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> =
        withLogging("readHeartRateSamples[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 500,
                )
            ).records.flatMap { record ->
                val source = record.metadata.dataOrigin.packageName
                record.samples.map { sample ->
                    HeartRateSample(
                        time = sample.time,
                        beatsPerMinute = sample.beatsPerMinute,
                        source = source,
                    )
                }
            }
        }

    suspend fun readDailyHeartRateSummaries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<HeartRateSummary> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailyHeartRateSummaries[$start..$end]", emptyList()) {
            client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(
                        HeartRateRecord.BPM_AVG,
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).mapNotNull { bucket ->
                val avg = bucket.result[HeartRateRecord.BPM_AVG] ?: return@mapNotNull null
                HeartRateSummary(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    avgBpm = avg,
                    minBpm = bucket.result[HeartRateRecord.BPM_MIN] ?: avg,
                    maxBpm = bucket.result[HeartRateRecord.BPM_MAX] ?: avg,
                )
            }
        }
    }

    // ─── Resting heart rate ──────────────────────────────────────────────────

    suspend fun readRestingHeartRate(date: LocalDate): Long? {
        val (start, end) = dayRange(date)
        return withNullableLogging("readRestingHeartRate[$date][$start..$end]") {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[RestingHeartRateRecord.BPM_AVG]
        }
    }

    suspend fun readDailyRestingHR(startDate: LocalDate, endDate: LocalDate): List<DailyRestingHR> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailyRestingHR[$start..$end]", emptyList()) {
            client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).mapNotNull { bucket ->
                val bpm = bucket.result[RestingHeartRateRecord.BPM_AVG] ?: return@mapNotNull null
                DailyRestingHR(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    bpm = bpm,
                )
            }
        }
    }

    // ─── Heart rate variability ───────────────────────────────────────────────

    suspend fun readHrvRmssd(date: LocalDate): Double? {
        val (start, end) = dayRange(date)
        return withNullableLogging("readHrvRmssd[$date][$start..$end]") {
            val records = client().readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 100,
                )
            ).records
            if (records.isEmpty()) null
            else records.map { it.heartRateVariabilityMillis }.average()
        }
    }

    suspend fun readDailyHRV(startDate: LocalDate, endDate: LocalDate): List<DailyHrv> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailyHRV[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 1000,
                )
            ).records
                .groupBy { it.time.atZone(zone).toLocalDate() }
                .map { (date, records) ->
                    DailyHrv(
                        date = date,
                        rmssdMs = records.map { it.heartRateVariabilityMillis }.average(),
                    )
                }
        }
    }

    // ─── Weight ──────────────────────────────────────────────────────────────

    suspend fun readLatestWeight(date: LocalDate): WeightEntry? {
        val (start, end) = dayRange(date)
        return withNullableLogging("readLatestWeight[$date][$start..$end]") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.let { record ->
                WeightEntry(
                    time = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }
    }

    suspend fun readLatestWeight(): WeightEntry? =
        withNullableLogging("readLatestWeight") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.let { record ->
                WeightEntry(
                    time = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readWeightEntries(start: Instant, end: Instant): List<WeightEntry> =
        withLogging("readWeightEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                )
            ).records.map { record ->
                WeightEntry(
                    time = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    // ─── Body composition helpers ─────────────────────────────────────────────

    suspend fun readLatestHeight(): Double? =
        withNullableLogging("readLatestHeight") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.height?.inMeters?.times(100.0)
        }

    suspend fun readLatestBodyFat(): Double? =
        withNullableLogging("readLatestBodyFat") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.percentage?.value
        }

    suspend fun readBodyFatEntries(start: Instant, end: Instant): List<BodyFatEntry> =
        withLogging("readBodyFatEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                )
            ).records.map { record ->
                BodyFatEntry(
                    time = record.time,
                    percent = record.percentage.value,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestLeanBodyMass(): Double? =
        withNullableLogging("readLatestLeanBodyMass") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = LeanBodyMassRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.mass?.inKilograms
        }

    suspend fun readLatestBMR(): Double? =
        withNullableLogging("readLatestBMR") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BasalMetabolicRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.basalMetabolicRate?.inKilocaloriesPerDay
        }

    suspend fun readLatestBoneMass(): Double? =
        withNullableLogging("readLatestBoneMass") {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BoneMassRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.firstOrNull()?.mass?.inKilograms
        }

    // ─── Nutrition helpers ────────────────────────────────────────────────────

    suspend fun readDailyNutrition(startDate: LocalDate, endDate: LocalDate): List<DailyNutrition> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailyNutrition[$start..$end]", emptyList()) {
            val aggregateRows = client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(
                        HydrationRecord.VOLUME_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyNutrition(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    hydrationLiters = bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0,
                    caloriesBurnedKcal = bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0,
                )
            }
            if (aggregateRows.any { it.hydrationLiters > 0.0 }) {
                aggregateRows
            } else {
                val hydrationByDate = readHydrationRecordsByDate(start, end, zone)
                if (aggregateRows.isEmpty() && hydrationByDate.isNotEmpty()) {
                    dailyNutritionSeries(startDate, endDate, hydrationByDate)
                } else {
                    aggregateRows.map { row ->
                        row.copy(hydrationLiters = hydrationByDate[row.date] ?: row.hydrationLiters)
                    }
                }
            }
        }
    }

    suspend fun readDailyMacros(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return withLogging("readDailyMacros[$start..$end]", emptyList()) {
            client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.PROTEIN_TOTAL,
                        NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                        NutritionRecord.TOTAL_FAT_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyMacros(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    energyKcal = bucket.result[NutritionRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0,
                    proteinGrams = bucket.result[NutritionRecord.PROTEIN_TOTAL]?.inGrams ?: 0.0,
                    carbsGrams = bucket.result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams ?: 0.0,
                    fatGrams = bucket.result[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams ?: 0.0,
                )
            }
        }
    }

    suspend fun readNutritionEntries(start: Instant, end: Instant): List<NutritionEntry> =
        withLogging("readNutritionEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { record ->
                NutritionEntry(
                    time = record.startTime,
                    mealType = record.mealType,
                    name = record.name,
                    energyKcal = record.energy?.inKilocalories,
                    proteinGrams = record.protein?.inGrams,
                    carbsGrams = record.totalCarbohydrate?.inGrams,
                    fatGrams = record.totalFat?.inGrams,
                    fiberGrams = record.dietaryFiber?.inGrams,
                    sugarGrams = record.sugar?.inGrams,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    // ─── Mindfulness helpers ─────────────────────────────────────────────────

    suspend fun readMindfulnessSessions(start: Instant, end: Instant): List<MindfulnessSession> =
        withLogging("readMindfulnessSessions[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = MindfulnessSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { it.toMindfulnessSession() }
        }

    suspend fun readMindfulnessMinutes(date: LocalDate): Int {
        val (start, end) = dayRange(date)
        return readMindfulnessSessions(start, end).sumOf { it.durationMinutes }.toInt()
    }

    // ─── Cycle helpers ───────────────────────────────────────────────────────

    suspend fun readMenstruationFlowEntries(start: Instant, end: Instant): List<MenstruationFlowEntry> =
        withLogging("readMenstruationFlowEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = MenstruationFlowRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 200,
                )
            ).records.map { record ->
                MenstruationFlowEntry(
                    time = record.time,
                    flow = record.flow,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readMenstruationPeriods(start: Instant, end: Instant): List<MenstruationPeriodEntry> =
        withLogging("readMenstruationPeriods[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = MenstruationPeriodRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 100,
                )
            ).records.map { record ->
                MenstruationPeriodEntry(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readOvulationTests(start: Instant, end: Instant): List<OvulationTestEntry> =
        withLogging("readOvulationTests[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = OvulationTestRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 200,
                )
            ).records.map { record ->
                OvulationTestEntry(
                    time = record.time,
                    result = record.result,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readCervicalMucusEntries(start: Instant, end: Instant): List<CervicalMucusEntry> =
        withLogging("readCervicalMucusEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = CervicalMucusRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 200,
                )
            ).records.map { record ->
                CervicalMucusEntry(
                    time = record.time,
                    appearance = record.appearance,
                    sensation = record.sensation,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readBasalBodyTemperatureEntries(start: Instant, end: Instant): List<BasalBodyTemperatureEntry> =
        withLogging("readBasalBodyTemperatureEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BasalBodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageSize = 200,
                )
            ).records.map { record ->
                BasalBodyTemperatureEntry(
                    time = record.time,
                    temperatureCelsius = record.temperature.inCelsius,
                    measurementLocation = record.measurementLocation,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    // ─── Vitals helpers ──────────────────────────────────────────────────────

    suspend fun readBloodPressureEntries(start: Instant, end: Instant): List<BloodPressureEntry> =
        withLogging("readBloodPressureEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { record ->
                BloodPressureEntry(
                    time = record.time,
                    systolicMmHg = record.systolic.inMillimetersOfMercury.toInt(),
                    diastolicMmHg = record.diastolic.inMillimetersOfMercury.toInt(),
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestBloodPressure(date: LocalDate): BloodPressureEntry? {
        val (start, end) = dayRange(date)
        return readBloodPressureEntries(start, end).maxByOrNull { it.time }
    }

    suspend fun readSpO2Entries(start: Instant, end: Instant): List<SpO2Entry> =
        withLogging("readSpO2Entries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { record ->
                SpO2Entry(
                    time = record.time,
                    percent = record.percentage.value,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestSpO2(date: LocalDate): SpO2Entry? {
        val (start, end) = dayRange(date)
        return readSpO2Entries(start, end).maxByOrNull { it.time }
    }

    suspend fun readRespiratoryRateEntries(start: Instant, end: Instant): List<RespiratoryRateEntry> =
        withLogging("readRespiratoryRateEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = RespiratoryRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { record ->
                RespiratoryRateEntry(
                    time = record.time,
                    breathsPerMinute = record.rate,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readBodyTemperatureEntries(start: Instant, end: Instant): List<BodyTempEntry> =
        withLogging("readBodyTemperatureEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { record ->
                BodyTempEntry(
                    time = record.time,
                    temperatureCelsius = record.temperature.inCelsius,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readVo2MaxEntries(start: Instant, end: Instant): List<Vo2MaxEntry> =
        withLogging("readVo2MaxEntries[$start..$end]", emptyList()) {
            client().readRecords(
                ReadRecordsRequest(
                    recordType = Vo2MaxRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 200,
                )
            ).records.map { record ->
                Vo2MaxEntry(
                    time = record.time,
                    vo2MaxMlPerKgPerMin = record.vo2MillilitersPerMinuteKilogram,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestVo2Max(date: LocalDate): Vo2MaxEntry? {
        val (start, end) = dayRange(date)
        return readVo2MaxEntries(start, end).maxByOrNull { it.time }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun dayRange(date: LocalDate): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now(zone)) {
            Instant.now()
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant()
        }
        return start to end
    }

    private suspend fun readHydrationRecordsByDate(
        start: Instant,
        end: Instant,
        zone: ZoneId,
    ): Map<LocalDate, Double> =
        client().readRecords(
            ReadRecordsRequest(
                recordType = HydrationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 1000,
            )
        ).records
            .groupBy { record -> record.startTime.atZone(zone).toLocalDate() }
            .mapValues { (_, records) -> records.sumOf { it.volume.inLiters } }

    private fun dailyHydrationSeries(
        startDate: LocalDate,
        endDate: LocalDate,
        hydrationByDate: Map<LocalDate, Double>,
    ): List<DailyHydration> =
        generateSequence(startDate) { date ->
            date.plusDays(1).takeUnless { it.isAfter(endDate) }
        }.map { date ->
            DailyHydration(
                date = date,
                liters = hydrationByDate[date] ?: 0.0,
            )
        }.toList()

    private fun dailyNutritionSeries(
        startDate: LocalDate,
        endDate: LocalDate,
        hydrationByDate: Map<LocalDate, Double>,
    ): List<DailyNutrition> =
        generateSequence(startDate) { date ->
            date.plusDays(1).takeUnless { it.isAfter(endDate) }
        }.map { date ->
            DailyNutrition(
                date = date,
                hydrationLiters = hydrationByDate[date] ?: 0.0,
                caloriesBurnedKcal = 0.0,
            )
        }.toList()

    private fun ExerciseSessionRecord.toExerciseData() = ExerciseData(
        id = metadata.id,
        title = title,
        exerciseType = exerciseType,
        startTime = startTime,
        endTime = endTime,
        durationMs = endTime.toEpochMilli() - startTime.toEpochMilli(),
        source = metadata.dataOrigin.packageName,
    )

    private fun SleepSessionRecord.toSleepData() = SleepData(
        id = metadata.id,
        startTime = startTime,
        endTime = endTime,
        durationMs = endTime.toEpochMilli() - startTime.toEpochMilli(),
        source = metadata.dataOrigin.packageName,
        stages = stages.map { stage ->
            SleepStage(
                startTime = stage.startTime,
                endTime = stage.endTime,
                stageType = stage.stage,
            )
        },
    )

    private fun MindfulnessSessionRecord.toMindfulnessSession() = MindfulnessSession(
        id = metadata.id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        durationMs = endTime.toEpochMilli() - startTime.toEpochMilli(),
        source = metadata.dataOrigin.packageName,
    )
}
