package tech.mmarca.openvitals.features.devicesync

import java.time.Instant
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.features.devicesync.bluetooth.DiscoveredSyncDevice
import tech.mmarca.openvitals.features.devicesync.protocol.SyncProgress
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRole

/** How far back the user chose to sync. */
enum class SyncRange { DAYS_30, MONTHS_6, YEAR_1, ALL }

/** The inclusive `[start, end]` window for a range, anchored at now. */
fun SyncRange.window(now: Instant = Instant.now()): Pair<Instant, Instant> {
    val start = when (this) {
        SyncRange.DAYS_30 -> now.minus(30, ChronoUnit.DAYS)
        SyncRange.MONTHS_6 -> now.minus(182, ChronoUnit.DAYS)
        SyncRange.YEAR_1 -> now.minus(365, ChronoUnit.DAYS)
        SyncRange.ALL -> Instant.parse("2000-01-01T00:00:00Z")
    }
    return start to now
}

/** The steps of the pairing + sync wizard. */
enum class DeviceSyncStep {
    ROLE,
    HOST_WAITING,
    GUEST_SCANNING,
    GUEST_CODE,
    RANGE,
    TYPES,
    SYNCING,
    REPORT,
}

/** A failure surfaced to the wizard, mapped to a localized message in the UI. */
enum class DeviceSyncError {
    PERMISSION_DENIED,
    DISCOVERABLE_DECLINED,
    CONNECT_FAILED,
    CONNECT_TIMEOUT,
    SYNC_FAILED,
    RECORDING_ACTIVE,
}

/**
 * Every record type the sync can move, mapped to its Health Connect permission
 * suffix (`READ_<suffix>` / `WRITE_<suffix>`). The generic read/write path
 * (`readRecordsForSync` / `insertImportedRecords`) covers all of these; the
 * per-device permission gate then hides any a given provider/manifest doesn't
 * grant.
 */
val syncableTypePermissionSuffix: Map<String, String> = linkedMapOf(
    // Activity
    "StepsRecord" to "STEPS",
    "DistanceRecord" to "DISTANCE",
    "ActiveCaloriesBurnedRecord" to "ACTIVE_CALORIES_BURNED",
    "TotalCaloriesBurnedRecord" to "TOTAL_CALORIES_BURNED",
    "FloorsClimbedRecord" to "FLOORS_CLIMBED",
    "ElevationGainedRecord" to "ELEVATION_GAINED",
    "WheelchairPushesRecord" to "WHEELCHAIR_PUSHES",
    "SpeedRecord" to "SPEED",
    "StepsCadenceRecord" to "STEPS_CADENCE",
    "CyclingPedalingCadenceRecord" to "CYCLING_PEDALING_CADENCE",
    "PowerRecord" to "POWER",
    // Heart
    "HeartRateRecord" to "HEART_RATE",
    "RestingHeartRateRecord" to "RESTING_HEART_RATE",
    "HeartRateVariabilityRmssdRecord" to "HEART_RATE_VARIABILITY",
    // Body
    "WeightRecord" to "WEIGHT",
    "HeightRecord" to "HEIGHT",
    "BodyFatRecord" to "BODY_FAT",
    "LeanBodyMassRecord" to "LEAN_BODY_MASS",
    "BasalMetabolicRateRecord" to "BASAL_METABOLIC_RATE",
    "BoneMassRecord" to "BONE_MASS",
    "BodyWaterMassRecord" to "BODY_WATER_MASS",
    // Hydration / Nutrition
    "HydrationRecord" to "HYDRATION",
    "NutritionRecord" to "NUTRITION",
    // Vitals
    "BloodPressureRecord" to "BLOOD_PRESSURE",
    "OxygenSaturationRecord" to "OXYGEN_SATURATION",
    "RespiratoryRateRecord" to "RESPIRATORY_RATE",
    "BodyTemperatureRecord" to "BODY_TEMPERATURE",
    "Vo2MaxRecord" to "VO2_MAX",
    "BloodGlucoseRecord" to "BLOOD_GLUCOSE",
    "BasalBodyTemperatureRecord" to "BASAL_BODY_TEMPERATURE",
    "SkinTemperatureRecord" to "SKIN_TEMPERATURE",
    // Sleep / Workouts / Mindfulness
    "SleepSessionRecord" to "SLEEP",
    "ExerciseSessionRecord" to "EXERCISE",
    "PlannedExerciseSessionRecord" to "PLANNED_EXERCISE",
    "MindfulnessSessionRecord" to "MINDFULNESS",
    // Cycle
    "MenstruationFlowRecord" to "MENSTRUATION",
    "MenstruationPeriodRecord" to "MENSTRUATION",
    "OvulationTestRecord" to "OVULATION_TEST",
    "CervicalMucusRecord" to "CERVICAL_MUCUS",
    "IntermenstrualBleedingRecord" to "INTERMENSTRUAL_BLEEDING",
    "SexualActivityRecord" to "SEXUAL_ACTIVITY",
)

/** Every syncable record type (the keys of [syncableTypePermissionSuffix]). */
val syncableRecordTypes: List<String> = syncableTypePermissionSuffix.keys.toList()

fun healthReadPermission(suffix: String): String = "android.permission.health.READ_$suffix"
fun healthWritePermission(suffix: String): String = "android.permission.health.WRITE_$suffix"

/** Groups the syncable record types by category for the type picker. */
enum class DeviceSyncCategory(val types: List<String>) {
    ACTIVITY(
        listOf(
            "StepsRecord",
            "DistanceRecord",
            "ActiveCaloriesBurnedRecord",
            "TotalCaloriesBurnedRecord",
            "FloorsClimbedRecord",
            "ElevationGainedRecord",
            "WheelchairPushesRecord",
            "SpeedRecord",
            "StepsCadenceRecord",
            "CyclingPedalingCadenceRecord",
            "PowerRecord",
        ),
    ),
    WORKOUTS(listOf("ExerciseSessionRecord", "PlannedExerciseSessionRecord")),
    HEART(listOf("HeartRateRecord", "RestingHeartRateRecord", "HeartRateVariabilityRmssdRecord")),
    SLEEP(listOf("SleepSessionRecord")),
    BODY(
        listOf(
            "WeightRecord",
            "HeightRecord",
            "BodyFatRecord",
            "LeanBodyMassRecord",
            "BasalMetabolicRateRecord",
            "BoneMassRecord",
            "BodyWaterMassRecord",
        ),
    ),
    VITALS(
        listOf(
            "BloodPressureRecord",
            "OxygenSaturationRecord",
            "RespiratoryRateRecord",
            "BodyTemperatureRecord",
            "Vo2MaxRecord",
            "BloodGlucoseRecord",
            "BasalBodyTemperatureRecord",
            "SkinTemperatureRecord",
        ),
    ),
    NUTRITION(listOf("NutritionRecord")),
    HYDRATION(listOf("HydrationRecord")),
    MINDFULNESS(listOf("MindfulnessSessionRecord")),
    CYCLE(
        listOf(
            "MenstruationFlowRecord",
            "MenstruationPeriodRecord",
            "OvulationTestRecord",
            "CervicalMucusRecord",
            "IntermenstrualBleedingRecord",
            "SexualActivityRecord",
        ),
    ),
}

/** The wizard's whole state, rendered step by step by the screen. */
data class DeviceSyncState(
    val step: DeviceSyncStep = DeviceSyncStep.ROLE,
    val role: SyncRole? = null,
    /** The pairing code: generated on the host, typed on the guest. */
    val code: String = "",
    val devices: List<DiscoveredSyncDevice> = emptyList(),
    val selectedDevice: DiscoveredSyncDevice? = null,
    val codeEntry: String = "",
    val codeError: Boolean = false,
    val range: SyncRange = SyncRange.YEAR_1,
    /**
     * The syncable types this device can actually read AND write (granted
     * Health Connect permissions on a provider that defines them). The picker
     * only offers these.
     */
    val availableTypes: Set<String> = emptySet(),
    val selectedTypes: Set<String> = emptySet(),
    val progress: SyncProgress? = null,
    val report: SyncReport? = null,
    /** The shareable report text (for Copy/Share), set when a report is produced. */
    val reportText: String = "",
    /**
     * The persisted report of the LAST sync (this run or an earlier one), read
     * back from [tech.mmarca.openvitals.features.devicesync.store.DeviceSyncReportStore]
     * so the role step can offer it — the user who hit a failure yesterday can
     * still send us the evidence today.
     */
    val lastReportText: String = "",
    val error: DeviceSyncError? = null,
    val bluetoothUnavailable: Boolean = false,
    /** True while a discovery scan window is open (guest scanning step). */
    val scanning: Boolean = false,
)
