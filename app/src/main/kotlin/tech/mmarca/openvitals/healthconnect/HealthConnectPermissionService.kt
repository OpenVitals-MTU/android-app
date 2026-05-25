package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
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
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.PermissionGrantMode

@OptIn(ExperimentalMindfulnessSessionApi::class)
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

    val hydrationWritePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
    )

    val mindfulnessPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
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
    val phase3Permissions: Set<String> = vitalsPermissions

    /** Phase 4 - sensitive cycle tracking, requested only after explicit opt-in from Settings */
    val phase4Permissions: Set<String> = cyclePermissions

    val manualOnlyPermissions: Set<String> get() = routePermissions

    val requestableAllPermissions: Set<String>
        get() = phase1Permissions + phase2Permissions + phase3Permissions + additionalDataAccessPermissions

    val requestableManagedPermissions: Set<String> get() = requestableAllPermissions + phase4Permissions

    val allPermissions: Set<String> get() =
        requestableAllPermissions + manualOnlyPermissions + hydrationWritePermissions

    val managedPermissions: Set<String> get() =
        requestableManagedPermissions + manualOnlyPermissions + hydrationWritePermissions

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

    suspend fun grantedPermissions(): Set<String> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            managedPermissions.filterTo(mutableSetOf()) { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }.also { granted ->
                Log.d(TAG, "grantedPermissions(runtime) count=${granted.size} granted=${granted.sorted()} ${diagnostics.summary()}")
            }
        } else {
            withLogging("permissionController.getGrantedPermissions", emptySet()) {
                clientProvider().permissionController.getGrantedPermissions()
            }.also { granted ->
                Log.d(TAG, "grantedPermissions(client) count=${granted.size} granted=${granted.sorted()}")
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
    } catch (t: Throwable) {
        Log.e(TAG, "Failed $operation ${diagnostics.summary()}", t)
        fallback
    }

    private companion object {
        private const val TAG = "HealthConnectPermissions"
        private const val READ_EXERCISE_ROUTES_PERMISSION = "android.permission.health.READ_EXERCISE_ROUTES"
    }
}
