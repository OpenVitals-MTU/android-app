package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.PermissionGrantMode

internal class HealthConnectPermissionService(
    private val context: Context,
    private val clientProvider: () -> HealthConnectClient,
    private val availabilityService: HealthConnectAvailabilityService,
    private val diagnostics: HealthConnectDiagnostics,
) {
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
            if (isPlannedExerciseAvailable()) {
                add(HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class))
            }
        }

    val nutritionHydrationPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    val hydrationWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
    )

    val bodyWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(HeightRecord::class),
        HealthPermission.getWritePermission(BodyFatRecord::class),
    )

    val mindfulnessPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    )

    val mindfulnessWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(MindfulnessSessionRecord::class),
    )

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
    )

    val dataImportWritePermissions: Set<String> get() = dataImportWritePermissions(trackCycle = true)

    fun dataImportWritePermissions(trackCycle: Boolean): Set<String> = buildSet {
        add(HealthPermission.getWritePermission(StepsRecord::class))
        add(HealthPermission.getWritePermission(DistanceRecord::class))
        add(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
        add(HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class))
        add(HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class))
        add(HealthPermission.getWritePermission(FloorsClimbedRecord::class))
        add(HealthPermission.getWritePermission(ElevationGainedRecord::class))
        add(HealthPermission.getWritePermission(WheelchairPushesRecord::class))
        add(HealthPermission.getWritePermission(HeartRateRecord::class))
        add(HealthPermission.getWritePermission(RestingHeartRateRecord::class))
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
        if (isMindfulnessSessionAvailable()) {
            add(HealthPermission.getWritePermission(MindfulnessSessionRecord::class))
        }
        if (trackCycle) {
            add(HealthPermission.getWritePermission(MenstruationFlowRecord::class))
            add(HealthPermission.getWritePermission(OvulationTestRecord::class))
            add(HealthPermission.getWritePermission(CervicalMucusRecord::class))
            add(HealthPermission.getWritePermission(BasalBodyTemperatureRecord::class))
            add(HealthPermission.getWritePermission(IntermenstrualBleedingRecord::class))
            add(HealthPermission.getWritePermission(SexualActivityRecord::class))
        }
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

    /** Phase 1 - core metrics requested on first launch */
    val phase1Permissions: Set<String> = corePermissions

    /** Phase 2 - extended metrics requested by category during onboarding */
    val phase2Permissions: Set<String>
        get() = heartPermissions +
            bodyPermissions +
            activityExtrasPermissions +
            nutritionHydrationPermissions +
            (if (isMindfulnessSessionAvailable()) mindfulnessPermissions else emptySet())

    /** Phase 3 - vitals, requested by category during onboarding or when opening Heart & Vitals */
    val phase3Permissions: Set<String> get() = vitalsPermissions

    /** Phase 4 - sensitive cycle tracking, requested only after explicit opt-in from Settings */
    val phase4Permissions: Set<String> = cyclePermissions

    val manualOnlyPermissions: Set<String> get() = routePermissions

    val requestableAllPermissions: Set<String>
        get() = phase1Permissions + phase2Permissions

    val requestableWritePermissions: Set<String>
        get() = activityWritePermissions +
            hydrationWritePermissions +
            bodyWritePermissions +
            vitalsWritePermissions +
            (if (isMindfulnessSessionAvailable()) mindfulnessWritePermissions else emptySet())

    val onboardingRequestablePermissions: Set<String>
        get() = requestableAllPermissions +
            phase3Permissions +
            additionalDataAccessPermissions +
            requestableWritePermissions

    val requestableManagedPermissions: Set<String>
        get() = onboardingRequestablePermissions + phase4Permissions

    val allPermissions: Set<String> get() =
        requestableAllPermissions +
            phase3Permissions +
            additionalDataAccessPermissions +
            manualOnlyPermissions +
            activityWritePermissions +
            hydrationWritePermissions +
            bodyWritePermissions +
            vitalsWritePermissions +
            mindfulnessWritePermissions +
            dataImportWritePermissions

    val managedPermissions: Set<String> get() =
        requestableManagedPermissions +
            manualOnlyPermissions +
            activityWritePermissions +
            hydrationWritePermissions +
            bodyWritePermissions +
            vitalsWritePermissions +
            mindfulnessWritePermissions +
            dataImportWritePermissions

    fun grantModeFor(permission: String): PermissionGrantMode =
        if (permission in manualOnlyPermissions) {
            PermissionGrantMode.MANUAL
        } else {
            PermissionGrantMode.REQUESTABLE
        }

    fun isMindfulnessSessionAvailable(): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = withLogging(
            "features.getFeatureStatus[mindfulness]",
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        ) {
            clientProvider().features.getFeatureStatus(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION)
        }
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "mindfulnessFeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    fun isHealthDataHistoryAvailable(): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = withLogging(
            "features.getFeatureStatus[history]",
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        ) {
            clientProvider().features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY)
        }
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "historyFeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    fun isBackgroundHealthDataReadAvailable(): Boolean {
        if (availabilityService.availability() != HealthConnectAvailability.AVAILABLE) return false

        val status = withLogging(
            "features.getFeatureStatus[background]",
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        ) {
            clientProvider().features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND)
        }
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

        val status = withLogging(
            "features.getFeatureStatus[$logName]",
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        ) {
            clientProvider().features.getFeatureStatus(feature)
        }
        val available = status == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        Log.d(TAG, "${logName}FeatureStatus=$status available=$available ${diagnostics.summary()}")
        return available
    }

    suspend fun grantedPermissions(): Set<String> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            managedPermissions.filterTo(mutableSetOf()) { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }.also { granted ->
                Log.d(TAG, "grantedPermissions(runtime) count=${granted.size} ${diagnostics.summary()}")
            }
        } else {
            withLogging("permissionController.getGrantedPermissions", emptySet()) {
                clientProvider().permissionController.getGrantedPermissions()
            }.also { granted ->
                Log.d(TAG, "grantedPermissions(client) count=${granted.size}")
            }
        }
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

    private companion object {
        private const val TAG = "HealthConnectPermissions"
        private const val READ_EXERCISE_ROUTES_PERMISSION = "android.permission.health.READ_EXERCISE_ROUTES"
    }
}
