package tech.mmarca.openvitals.healthconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.model.OnboardingCategoryId
import tech.mmarca.openvitals.domain.model.OnboardingPermissionCatalog
import tech.mmarca.openvitals.domain.model.OnboardingPermissionCategory
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PermissionGrantMode

internal class HealthConnectPermissionService(
    private val context: Context,
    private val clientProvider: () -> HealthConnectClient,
    private val availabilityService: HealthConnectAvailabilityService,
    private val diagnostics: HealthConnectDiagnostics,
    private val mindfulnessIntegrationEnabled: () -> Boolean = { true },
) {
    private val featureStatusCache = mutableMapOf<Int, Int>()
    private val featureStatusCacheLock = Any()

    @Volatile
    private var grantedPermissionsCache: GrantedPermissionsCache? = null

    /**
     * Serialises the cache MISS, so callers that arrive together pay for one
     * sweep between them.
     *
     * The dashboard loads a pass per metric and they all start in the same
     * instant, so on a cold cache every one of them used to walk the whole
     * managed-permission list against the package manager — the same answer,
     * recomputed a couple of dozen times, before a single record was read.
     */
    private val grantedPermissionsMutex = Mutex()

    val corePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    val routePermissions: Set<String> = setOf(
        READ_EXERCISE_ROUTES_PERMISSION,
    )

    val activityWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(ElevationGainedRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE,
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(PowerRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class),
        HealthPermission.getWritePermission(StepsCadenceRecord::class),
    )

    val plannedExercisePermissions: Set<String>
        get() = if (isPlannedExerciseAvailable()) {
            setOf(
                HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class),
                HealthPermission.getWritePermission(PlannedExerciseSessionRecord::class),
            )
        } else {
            emptySet()
        }

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
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
    )

    val activityExtrasPermissions: Set<String>
        get() = buildSet {
            add(HealthPermission.getReadPermission(FloorsClimbedRecord::class))
            add(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
            add(HealthPermission.getReadPermission(ElevationGainedRecord::class))
            add(HealthPermission.getReadPermission(WheelchairPushesRecord::class))
            add(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
            add(HealthPermission.getReadPermission(SpeedRecord::class))
            add(HealthPermission.getReadPermission(PowerRecord::class))
            add(HealthPermission.getReadPermission(StepsCadenceRecord::class))
            add(HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class))
            addAll(plannedExercisePermissions)
        }

    val nutritionHydrationPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    val hydrationWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
    )

    val nutritionWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
    )

    val bodyWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(HeightRecord::class),
        HealthPermission.getWritePermission(BodyFatRecord::class),
    )

    private val rawMindfulnessPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    )

    val mindfulnessPermissions: Set<String>
        get() = if (isMindfulnessSessionAvailable()) rawMindfulnessPermissions else emptySet()

    private val rawMindfulnessWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(MindfulnessSessionRecord::class),
    )

    val mindfulnessWritePermissions: Set<String>
        get() = if (isMindfulnessSessionAvailable()) rawMindfulnessWritePermissions else emptySet()

    val additionalDataAccessPermissions: Set<String>
        get() = buildSet {
            if (isHealthDataHistoryAvailable()) {
                add(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
            }
            if (isBackgroundHealthDataReadAvailable()) {
                add(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
            }
        }

    val vitalsPermissions: Set<String>
        get() = buildSet {
            add(HealthPermission.getReadPermission(BloodPressureRecord::class))
            add(HealthPermission.getReadPermission(OxygenSaturationRecord::class))
            add(HealthPermission.getReadPermission(RespiratoryRateRecord::class))
            add(HealthPermission.getReadPermission(BodyTemperatureRecord::class))
            add(HealthPermission.getReadPermission(Vo2MaxRecord::class))
            add(HealthPermission.getReadPermission(BloodGlucoseRecord::class))
            if (isSkinTemperatureAvailable()) {
                add(HealthPermission.getReadPermission(SkinTemperatureRecord::class))
            }
        }

    val vitalsWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        HealthPermission.getWritePermission(RespiratoryRateRecord::class),
        HealthPermission.getWritePermission(BodyTemperatureRecord::class),
        // The HRV tile writes HeartRateVariabilityRmssdRecord; keeping it here
        // keeps the Settings "Manual entry write access" card in step with
        // what the log can actually write.
        HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class),
    )

    val dataImportWritePermissions: Set<String> get() = buildSet {
        add(HealthPermission.getWritePermission(StepsRecord::class))
        add(HealthPermission.getWritePermission(DistanceRecord::class))
        add(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
        add(HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE)
        add(HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class))
        add(HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class))
        add(HealthPermission.getWritePermission(FloorsClimbedRecord::class))
        add(HealthPermission.getWritePermission(ElevationGainedRecord::class))
        add(HealthPermission.getWritePermission(WheelchairPushesRecord::class))
        add(HealthPermission.getWritePermission(SpeedRecord::class))
        add(HealthPermission.getWritePermission(HeartRateRecord::class))
        add(HealthPermission.getWritePermission(RestingHeartRateRecord::class))
        add(HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class))
        add(HealthPermission.getWritePermission(WeightRecord::class))
        add(HealthPermission.getWritePermission(HeightRecord::class))
        add(HealthPermission.getWritePermission(BodyFatRecord::class))
        add(HealthPermission.getWritePermission(LeanBodyMassRecord::class))
        add(HealthPermission.getWritePermission(BasalMetabolicRateRecord::class))
        add(HealthPermission.getWritePermission(BoneMassRecord::class))
        add(HealthPermission.getWritePermission(BodyWaterMassRecord::class))
        add(HealthPermission.getWritePermission(HydrationRecord::class))
        add(HealthPermission.getWritePermission(NutritionRecord::class))
        add(HealthPermission.getWritePermission(SleepSessionRecord::class))
        add(HealthPermission.getWritePermission(BloodPressureRecord::class))
        add(HealthPermission.getWritePermission(OxygenSaturationRecord::class))
        add(HealthPermission.getWritePermission(RespiratoryRateRecord::class))
        add(HealthPermission.getWritePermission(BodyTemperatureRecord::class))
        add(HealthPermission.getWritePermission(BloodGlucoseRecord::class))
        add(HealthPermission.getWritePermission(Vo2MaxRecord::class))
        addAll(mindfulnessWritePermissions)
        addAll(cycleWritePermissions)
    }

    val cyclePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MenstruationFlowRecord::class),
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(OvulationTestRecord::class),
        HealthPermission.getReadPermission(CervicalMucusRecord::class),
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
        HealthPermission.getReadPermission(IntermenstrualBleedingRecord::class),
        HealthPermission.getReadPermission(SexualActivityRecord::class),
    )

    // MenstruationPeriodRecord shares WRITE_MENSTRUATION with the flow record,
    // so the set carries six distinct permission strings for seven record types.
    val cycleWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(MenstruationFlowRecord::class),
        HealthPermission.getWritePermission(MenstruationPeriodRecord::class),
        HealthPermission.getWritePermission(OvulationTestRecord::class),
        HealthPermission.getWritePermission(CervicalMucusRecord::class),
        HealthPermission.getWritePermission(BasalBodyTemperatureRecord::class),
        HealthPermission.getWritePermission(IntermenstrualBleedingRecord::class),
        HealthPermission.getWritePermission(SexualActivityRecord::class),
    )

    // ── Onboarding categories ────────────────────────────────────────────────
    // Grouped the way Health Connect itself groups permissions, so a row named
    // "Activity" produces a system dialog headed "Activity". Each category
    // carries read AND write together — entries created in the app go back to
    // Health Connect. (VO2 max lives under Activity: that is where Health
    // Connect files it, not under Vitals.)

    private fun readWrite(record: kotlin.reflect.KClass<out androidx.health.connect.client.records.Record>): Set<String> =
        setOf(HealthPermission.getReadPermission(record), HealthPermission.getWritePermission(record))

    val onboardingActivityCategoryPermissions: Set<String>
        get() = buildSet {
            add(HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE)
            addAll(readWrite(StepsRecord::class))
            addAll(readWrite(DistanceRecord::class))
            addAll(readWrite(ExerciseSessionRecord::class))
            addAll(readWrite(FloorsClimbedRecord::class))
            addAll(readWrite(ElevationGainedRecord::class))
            addAll(readWrite(WheelchairPushesRecord::class))
            addAll(readWrite(ActiveCaloriesBurnedRecord::class))
            addAll(readWrite(TotalCaloriesBurnedRecord::class))
            addAll(readWrite(SpeedRecord::class))
            addAll(readWrite(PowerRecord::class))
            addAll(readWrite(StepsCadenceRecord::class))
            addAll(readWrite(CyclingPedalingCadenceRecord::class))
            addAll(readWrite(Vo2MaxRecord::class))
            addAll(plannedExercisePermissions)
        }

    val onboardingBodyCategoryPermissions: Set<String> =
        readWrite(WeightRecord::class) +
            readWrite(HeightRecord::class) +
            readWrite(BodyFatRecord::class) +
            readWrite(LeanBodyMassRecord::class) +
            readWrite(BasalMetabolicRateRecord::class) +
            readWrite(BoneMassRecord::class) +
            readWrite(BodyWaterMassRecord::class)

    val onboardingNutritionCategoryPermissions: Set<String> =
        readWrite(HydrationRecord::class) + readWrite(NutritionRecord::class)

    val onboardingSleepCategoryPermissions: Set<String> =
        readWrite(SleepSessionRecord::class)

    val onboardingVitalsCategoryPermissions: Set<String>
        get() = buildSet {
            addAll(readWrite(HeartRateRecord::class))
            addAll(readWrite(RestingHeartRateRecord::class))
            addAll(readWrite(HeartRateVariabilityRmssdRecord::class))
            addAll(readWrite(BloodPressureRecord::class))
            addAll(readWrite(OxygenSaturationRecord::class))
            addAll(readWrite(RespiratoryRateRecord::class))
            addAll(readWrite(BodyTemperatureRecord::class))
            addAll(readWrite(BasalBodyTemperatureRecord::class))
            addAll(readWrite(BloodGlucoseRecord::class))
            if (isSkinTemperatureAvailable()) {
                // Read-only: the app never writes skin temperature.
                add(HealthPermission.getReadPermission(SkinTemperatureRecord::class))
            }
        }

    val onboardingCycleCategoryPermissions: Set<String> = buildSet {
        addAll(readWrite(MenstruationFlowRecord::class))
        addAll(readWrite(OvulationTestRecord::class))
        addAll(readWrite(CervicalMucusRecord::class))
        addAll(readWrite(IntermenstrualBleedingRecord::class))
        addAll(readWrite(SexualActivityRecord::class))
        // The period write shares WRITE_MENSTRUATION with the flow record; the
        // app maintains derived MenstruationPeriodRecords from logged flows.
        addAll(readWrite(MenstruationPeriodRecord::class))
    }

    val onboardingMindfulnessCategoryPermissions: Set<String>
        get() = mindfulnessPermissions + mindfulnessWritePermissions

    fun onboardingPermissionCatalog(): OnboardingPermissionCatalog {
        val required = onboardingActivityCategoryPermissions + onboardingSleepCategoryPermissions
        val categories = listOf(
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.ACTIVITY,
                permissions = onboardingActivityCategoryPermissions,
                required = true,
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.BODY,
                permissions = onboardingBodyCategoryPermissions,
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.NUTRITION,
                permissions = onboardingNutritionCategoryPermissions,
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.SLEEP,
                permissions = onboardingSleepCategoryPermissions,
                required = true,
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.VITALS,
                permissions = onboardingVitalsCategoryPermissions,
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.CYCLE_TRACKING,
                permissions = onboardingCycleCategoryPermissions,
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.MINDFULNESS,
                permissions = onboardingMindfulnessCategoryPermissions,
                available = isMindfulnessSessionAvailable(),
            ),
            OnboardingPermissionCategory(
                id = OnboardingCategoryId.ADDITIONAL_ACCESS,
                permissions = additionalDataAccessPermissions,
            ),
        ).filter { it.permissions.isNotEmpty() }
        return OnboardingPermissionCatalog(
            categories = categories,
            requiredPermissions = required - manualOnlyPermissions,
            routeReadPermission = READ_EXERCISE_ROUTES_PERMISSION,
            // The device's answer alone. This decides whether onboarding OFFERS
            // the opt-in, so gating it on the opt-in would hide the only place
            // the opt-in can be made.
            mindfulnessSupportedByDevice = isMindfulnessSessionSupportedByDevice(),
        )
    }

    /** Minimum permissions to complete first-run onboarding */
    val minimumOnboardingPermissions: Set<String> = corePermissions + heartPermissions + vitalsPermissions

    /** Phase 1 - core metrics requested on first launch */
    val phase1Permissions: Set<String> = corePermissions

    /** Phase 2 - extended metrics requested by category during onboarding */
    val phase2Permissions: Set<String>
        get() = heartPermissions +
            bodyPermissions +
            activityExtrasPermissions +
            nutritionHydrationPermissions +
            mindfulnessPermissions

    /** Phase 3 - vitals, requested by category during onboarding or when opening Heart & Vitals */
    val phase3Permissions: Set<String> get() = vitalsPermissions

    /** Phase 4 - cycle records, surfaced as a regular optional permission category. */
    val phase4Permissions: Set<String> = cyclePermissions

    val manualOnlyPermissions: Set<String> get() = routePermissions

    val requestableAllPermissions: Set<String>
        get() = phase1Permissions + phase2Permissions

    val requestableWritePermissions: Set<String>
        get() = activityWritePermissions +
            plannedExercisePermissions +
            hydrationWritePermissions +
            nutritionWritePermissions +
            bodyWritePermissions +
            vitalsWritePermissions +
            mindfulnessWritePermissions +
            cycleWritePermissions

    val onboardingRequestablePermissions: Set<String>
        get() = requestableAllPermissions +
            phase3Permissions +
            phase4Permissions +
            additionalDataAccessPermissions +
            requestableWritePermissions +
            dataImportWritePermissions

    val requestableManagedPermissions: Set<String>
        get() = onboardingRequestablePermissions + phase4Permissions

    val allPermissions: Set<String> get() =
        requestableAllPermissions +
            phase3Permissions +
            phase4Permissions +
            additionalDataAccessPermissions +
            manualOnlyPermissions +
            activityWritePermissions +
            plannedExercisePermissions +
            hydrationWritePermissions +
            nutritionWritePermissions +
            bodyWritePermissions +
            vitalsWritePermissions +
            mindfulnessWritePermissions +
            cycleWritePermissions +
            dataImportWritePermissions

    val managedPermissions: Set<String> get() =
        requestableManagedPermissions +
            manualOnlyPermissions +
            activityWritePermissions +
            plannedExercisePermissions +
            hydrationWritePermissions +
            nutritionWritePermissions +
            bodyWritePermissions +
            vitalsWritePermissions +
            mindfulnessWritePermissions +
            cycleWritePermissions +
            dataImportWritePermissions

    fun grantModeFor(permission: String): PermissionGrantMode =
        if (permission in manualOnlyPermissions) {
            PermissionGrantMode.MANUAL
        } else {
            PermissionGrantMode.REQUESTABLE
        }

    /**
     * Whether the app may ask for mindfulness: the device reports the feature
     * AND the user has opted in. Everything that derives a permission set reads
     * this one, never [isMindfulnessSessionSupportedByDevice].
     */
    fun isMindfulnessSessionAvailable(): Boolean =
        mindfulnessIntegrationEnabled() && isMindfulnessSessionSupportedByDevice()

    /**
     * The device's own answer alone, before the user's opt-in is folded in.
     *
     * The only legitimate use is deciding whether to OFFER the opt-in: a phone
     * whose Health Connect has no mindfulness feature should not be shown a
     * toggle it cannot honour. Deriving permissions from it would defeat the
     * opt-in.
     *
     * Onboarding needs exactly this distinction, and using the opt-in-gated
     * answer for it deadlocked: the mindfulness STEP was shown only when
     * mindfulness was already enabled, but the toggle that enables it lives on
     * that step — so a fresh install skipped the step forever and was never
     * offered the permission at all.
     */
    fun isMindfulnessSessionSupportedByDevice(): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = featureStatusCached(
            feature = HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION,
            logName = "mindfulness",
        )
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "mindfulnessFeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    fun isHealthDataHistoryAvailable(): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = featureStatusCached(
            feature = HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY,
            logName = "history",
        )
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "historyFeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    fun isBackgroundHealthDataReadAvailable(): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = featureStatusCached(
            feature = HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
            logName = "background",
        )
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "backgroundFeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    fun isSkinTemperatureAvailable(): Boolean =
        isFeatureAvailable(
            feature = HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
            logName = "skinTemperature",
        )

    fun isPlannedExerciseAvailable(): Boolean =
        isFeatureAvailable(
            feature = HealthConnectFeatures.FEATURE_PLANNED_EXERCISE,
            logName = "plannedExercise",
        )

    private fun isFeatureAvailable(feature: Int, logName: String): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = featureStatusCached(feature, logName)
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "${logName}FeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    suspend fun grantedPermissions(): Set<String> {
        freshGrantedPermissions()?.let { cached ->
            Log.d(TAG, "grantedPermissions(cache) count=${cached.size} ${diagnostics.summary()}")
            return cached
        }

        return grantedPermissionsMutex.withLock {
            // Whoever held the lock may have just filled the cache; the sweep
            // this call queued for is that same sweep.
            freshGrantedPermissions()?.let { cached ->
                Log.d(TAG, "grantedPermissions(cache) count=${cached.size} ${diagnostics.summary()}")
                return@withLock cached
            }
            loadGrantedPermissions()
        }
    }

    private fun freshGrantedPermissions(): Set<String>? {
        val nowMs = SystemClock.elapsedRealtime()
        return grantedPermissionsCache
            ?.takeIf { nowMs - it.loadedAtMs <= GrantedPermissionsCacheMillis }
            ?.permissions
    }

    private suspend fun loadGrantedPermissions(): Set<String> {
        val granted = withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                managedPermissions.filterTo(mutableSetOf()) { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }.also { permissions ->
                    Log.d(TAG, "grantedPermissions(runtime) count=${permissions.size} ${diagnostics.summary()}")
                }
            } else {
                withLogging("permissionController.getGrantedPermissions", emptySet()) {
                    clientProvider().permissionController.getGrantedPermissions()
                }.also { permissions ->
                    Log.d(TAG, "grantedPermissions(client) count=${permissions.size}")
                }
            }
        }
        grantedPermissionsCache = GrantedPermissionsCache(
            permissions = granted,
            loadedAtMs = SystemClock.elapsedRealtime(),
        )
        return granted
    }


    private fun featureStatusCached(feature: Int, logName: String): Int {
        synchronized(featureStatusCacheLock) {
            featureStatusCache[feature]?.let { cached ->
                Log.d(TAG, "features.getFeatureStatus[$logName](cache) status=$cached ${diagnostics.summary()}")
                return cached
            }
        }
        val status = withLogging(
            "features.getFeatureStatus[$logName]",
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        ) {
            featureStatus(feature)
        }
        synchronized(featureStatusCacheLock) {
            featureStatusCache[feature] = status
        }
        return status
    }

    suspend fun hasPermission(permission: String): Boolean =
        grantedPermissions().contains(permission)

    fun permissionContract() =
        PermissionController.createRequestPermissionResultContract()

    private inline fun <T> withLogging(
        operation: String,
        fallback: T,
        block: () -> T,
    ): T = try {
        Log.d(TAG, "Starting $operation ${diagnostics.summary()}")
        block().also {
            Log.d(TAG, "Finished $operation successfully")
        }
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        Log.e(TAG, "Failed $operation ${diagnostics.summary()}", t)
        fallback
    }

    @SuppressLint("WrongConstant")
    private fun featureStatus(feature: Int): Int =
        clientProvider().features.getFeatureStatus(feature)

    private data class GrantedPermissionsCache(
        val permissions: Set<String>,
        val loadedAtMs: Long,
    )

    companion object {
        /** Bump when requestable/managed permissions change so existing users see the new-permissions prompt. */
        const val PERMISSION_SET_VERSION = 4

        private const val GrantedPermissionsCacheMillis = 500L
        private const val TAG = "HealthConnectPermissions"
        private const val READ_EXERCISE_ROUTES_PERMISSION = "android.permission.health.READ_EXERCISE_ROUTES"
    }
}
