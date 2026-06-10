package tech.mmarca.openvitals.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.Record
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.BodyMeasurementEntry
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.CaloriesBurnedValue
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.IntermenstrualBleedingEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.OvulationTestEntry
import tech.mmarca.openvitals.domain.model.PermissionGrantMode
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SexualActivityEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.StepProgressPoint
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KClass
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade over Health Connect services.
 *
 * Public methods stay feature-oriented for repositories and ViewModels, while
 * permissions, availability, paging, reading, and mapping live in focused
 * Health Connect boundary classes.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val diagnostics = HealthConnectDiagnostics(context)
    private val availabilityService = HealthConnectAvailabilityService(context, diagnostics)
    private val permissionService = HealthConnectPermissionService(
        context = context,
        clientProvider = ::client,
        availabilityService = availabilityService,
        diagnostics = diagnostics,
    )
    private val readerSupport = HealthConnectReaderSupport(
        clientProvider = ::client,
        diagnostics = diagnostics,
        rateLimitMessage = { retryAfterMillis ->
            context.getString(
                R.string.message_health_connect_rate_limited,
                retryAfterMinutes(retryAfterMillis),
            )
        },
    )
    private val activityReader = ActivityHealthReader(readerSupport, context.packageName)
    private val hydrationReader = HydrationHealthReader(readerSupport, context.packageName)
    private val sleepReader = SleepHealthReader(readerSupport)
    private val heartReader = HeartHealthReader(readerSupport)
    private val bodyReader = BodyHealthReader(readerSupport, context.packageName)
    private val nutritionReader = NutritionHealthReader(readerSupport)
    private val mindfulnessReader = MindfulnessHealthReader(readerSupport, context.packageName)
    private val cycleReader = CycleHealthReader(readerSupport)
    private val vitalsReader = VitalsHealthReader(readerSupport, context.packageName)

    val corePermissions: Set<String> get() = permissionService.corePermissions
    val routePermissions: Set<String> get() = permissionService.routePermissions
    val activityWritePermissions: Set<String> get() = permissionService.activityWritePermissions
    val heartPermissions: Set<String> get() = permissionService.heartPermissions
    val bodyPermissions: Set<String> get() = permissionService.bodyPermissions
    val bodyWritePermissions: Set<String> get() = permissionService.bodyWritePermissions
    val activityExtrasPermissions: Set<String> get() = permissionService.activityExtrasPermissions
    val nutritionHydrationPermissions: Set<String> get() = permissionService.nutritionHydrationPermissions
    val hydrationWritePermissions: Set<String> get() = permissionService.hydrationWritePermissions
    val mindfulnessPermissions: Set<String> get() = permissionService.mindfulnessPermissions
    val mindfulnessWritePermissions: Set<String> get() = permissionService.mindfulnessWritePermissions
    val additionalDataAccessPermissions: Set<String> get() = permissionService.additionalDataAccessPermissions
    val vitalsPermissions: Set<String> get() = permissionService.vitalsPermissions
    val vitalsWritePermissions: Set<String> get() = permissionService.vitalsWritePermissions
    val dataImportWritePermissions: Set<String> get() = permissionService.dataImportWritePermissions
    val cyclePermissions: Set<String> get() = permissionService.cyclePermissions
    val phase1Permissions: Set<String> get() = permissionService.phase1Permissions
    val phase2Permissions: Set<String> get() = permissionService.phase2Permissions
    val phase3Permissions: Set<String> get() = permissionService.phase3Permissions
    val phase4Permissions: Set<String> get() = permissionService.phase4Permissions
    val manualOnlyPermissions: Set<String> get() = permissionService.manualOnlyPermissions
    val requestableAllPermissions: Set<String> get() = permissionService.requestableAllPermissions
    val requestableWritePermissions: Set<String> get() = permissionService.requestableWritePermissions
    val onboardingRequestablePermissions: Set<String> get() = permissionService.onboardingRequestablePermissions
    val requestableManagedPermissions: Set<String> get() = permissionService.requestableManagedPermissions
    val allPermissions: Set<String> get() = permissionService.allPermissions
    val managedPermissions: Set<String> get() = permissionService.managedPermissions

    fun grantModeFor(permission: String): PermissionGrantMode =
        permissionService.grantModeFor(permission)

    fun availability(): HealthConnectAvailability =
        availabilityService.availability()

    fun isMindfulnessSessionAvailable(): Boolean =
        permissionService.isMindfulnessSessionAvailable()

    fun isSkinTemperatureAvailable(): Boolean =
        permissionService.isSkinTemperatureAvailable()

    fun isPlannedExerciseAvailable(): Boolean =
        permissionService.isPlannedExerciseAvailable()

    suspend fun grantedPermissions(): Set<String> =
        permissionService.grantedPermissions()

    suspend fun hasPermission(permission: String): Boolean =
        permissionService.hasPermission(permission)

    fun permissionContract() =
        permissionService.permissionContract()

    suspend fun readSteps(date: LocalDate): Long =
        activityReader.readSteps(date)

    suspend fun readTodaySteps(): Long =
        activityReader.readTodaySteps()

    suspend fun readDailySteps(
        startDate: LocalDate,
        endDate: LocalDate,
        includeSteps: Boolean = true,
        includeDistance: Boolean = true,
        includeWheelchairPushes: Boolean = false,
        includeFloors: Boolean = false,
        includeActiveCalories: Boolean = false,
        includeElevation: Boolean = false,
    ): List<DailySteps> =
        activityReader.readDailySteps(
            startDate = startDate,
            endDate = endDate,
            includeSteps = includeSteps,
            includeDistance = includeDistance,
            includeWheelchairPushes = includeWheelchairPushes,
            includeFloors = includeFloors,
            includeActiveCalories = includeActiveCalories,
            includeElevation = includeElevation,
        )

    suspend fun readWheelchairPushes(date: LocalDate): Long =
        activityReader.readWheelchairPushes(date)

    suspend fun readFloorsClimbed(date: LocalDate): Int =
        activityReader.readFloorsClimbed(date)

    suspend fun readElevationGained(date: LocalDate): Double =
        activityReader.readElevationGained(date)

    suspend fun readStepProgress(date: LocalDate): List<StepProgressPoint> =
        activityReader.readStepProgress(date)

    suspend fun readActivityProgress(
        date: LocalDate,
        includeSteps: Boolean = true,
        includeDistance: Boolean,
        includeCalories: Boolean,
        includeActiveCalories: Boolean,
        includeCaloriesEstimate: Boolean = false,
        includeWheelchairPushes: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
    ): List<ActivityProgressPoint> =
        activityReader.readActivityProgress(
            date = date,
            includeSteps = includeSteps,
            includeDistance = includeDistance,
            includeCalories = includeCalories,
            includeActiveCalories = includeActiveCalories,
            includeCaloriesEstimate = includeCaloriesEstimate,
            includeWheelchairPushes = includeWheelchairPushes,
            includeFloors = includeFloors,
            includeElevation = includeElevation,
        )

    suspend fun readDistanceMeters(date: LocalDate): Double =
        activityReader.readDistanceMeters(date)

    suspend fun readTodayDistanceMeters(): Double =
        activityReader.readTodayDistanceMeters()

    suspend fun readCaloriesKcal(
        date: LocalDate,
        includeEstimatedCalories: Boolean = false,
    ): Double? =
        activityReader.readCaloriesKcal(date, includeEstimatedCalories)

    suspend fun readCaloriesBurned(
        date: LocalDate,
        includeEstimatedCalories: Boolean = false,
    ): CaloriesBurnedValue? =
        activityReader.readCaloriesBurned(date, includeEstimatedCalories)

    suspend fun readTodayCaloriesKcal(includeEstimatedCalories: Boolean = false): Double? =
        activityReader.readTodayCaloriesKcal(includeEstimatedCalories)

    suspend fun readCaloriesInKcal(date: LocalDate): Double? =
        nutritionReader.readCaloriesInKcal(date)

    suspend fun readHydrationLiters(date: LocalDate): Double? =
        hydrationReader.readHydrationLiters(date)

    suspend fun readTodayHydrationLiters(): Double? =
        hydrationReader.readTodayHydrationLiters()

    suspend fun readDailyHydration(startDate: LocalDate, endDate: LocalDate): List<DailyHydration> =
        hydrationReader.readDailyHydration(startDate, endDate)

    suspend fun readHydrationEntries(start: Instant, end: Instant): List<HydrationEntry> =
        hydrationReader.readHydrationEntries(start, end)

    suspend fun readHydrationEntry(id: String): HydrationEntry? =
        hydrationReader.readHydrationEntry(id)

    suspend fun writeHydrationEntry(request: HydrationWriteRequest): String =
        hydrationReader.writeHydrationEntry(request)

    suspend fun updateHydrationEntry(id: String, request: HydrationWriteRequest) =
        hydrationReader.updateHydrationEntry(id, request)

    suspend fun deleteHydrationEntry(id: String) =
        hydrationReader.deleteHydrationEntry(id)

    suspend fun readLatestWorkout(date: LocalDate): ExerciseData? =
        activityReader.readLatestWorkout(date)

    suspend fun readLatestWorkout(): ExerciseData? =
        activityReader.readLatestWorkout()

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseData> =
        activityReader.readExerciseSessions(start, end)

    suspend fun readExerciseSession(
        id: String,
        includeSteps: Boolean,
        includeDistance: Boolean,
        includeTotalCalories: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCaloriesEstimate: Boolean = false,
        includeWheelchairPushes: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
        includeSpeed: Boolean,
        includePower: Boolean,
        includeStepsCadence: Boolean,
        includeCyclingCadence: Boolean,
    ): ExerciseData? =
        activityReader.readExerciseSession(
            id = id,
            includeSteps = includeSteps,
            includeDistance = includeDistance,
            includeTotalCalories = includeTotalCalories,
            includeActiveCalories = includeActiveCalories,
            includeTotalCaloriesEstimate = includeTotalCaloriesEstimate,
            includeWheelchairPushes = includeWheelchairPushes,
            includeFloors = includeFloors,
            includeElevation = includeElevation,
            includeSpeed = includeSpeed,
            includePower = includePower,
            includeStepsCadence = includeStepsCadence,
            includeCyclingCadence = includeCyclingCadence,
        )

    suspend fun readPlannedExerciseSessions(start: Instant, end: Instant): List<PlannedExerciseData> =
        activityReader.readPlannedExerciseSessions(start, end)

    suspend fun writeActivityEntry(request: ActivityWriteRequest): String =
        activityReader.writeActivityEntry(request)

    suspend fun updateActivityEntry(id: String, request: ActivityWriteRequest) =
        activityReader.updateActivityEntry(id, request)

    suspend fun deleteActivityEntry(id: String) =
        activityReader.deleteActivityEntry(id)

    suspend fun readSleepSession(date: LocalDate): SleepData? =
        sleepReader.readSleepSession(date)

    suspend fun readLastSleepSession(): SleepData? =
        sleepReader.readLastSleepSession()

    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepData> =
        sleepReader.readSleepSessions(start, end)

    suspend fun readSleepSession(id: String): SleepData? =
        sleepReader.readSleepSession(id)

    suspend fun readAvgHeartRate(date: LocalDate): Long? =
        heartReader.readAvgHeartRate(date)

    suspend fun readAvgHeartRateToday(): Long? =
        heartReader.readAvgHeartRateToday()

    suspend fun readHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> =
        heartReader.readHeartRateSamples(start, end)

    suspend fun readDailyHeartRateSummaries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<HeartRateSummary> =
        heartReader.readDailyHeartRateSummaries(startDate, endDate)

    suspend fun readRestingHeartRate(date: LocalDate): Long? =
        heartReader.readRestingHeartRate(date)

    suspend fun readDailyRestingHR(startDate: LocalDate, endDate: LocalDate): List<DailyRestingHR> =
        heartReader.readDailyRestingHR(startDate, endDate)

    suspend fun readHrvRmssd(date: LocalDate): Double? =
        heartReader.readHrvRmssd(date)

    suspend fun readHrvSamples(start: Instant, end: Instant): List<HrvSample> =
        heartReader.readHrvSamples(start, end)

    suspend fun readDailyHRV(startDate: LocalDate, endDate: LocalDate): List<DailyHrv> =
        heartReader.readDailyHRV(startDate, endDate)

    suspend fun readLatestWeight(date: LocalDate): WeightEntry? =
        bodyReader.readLatestWeight(date)

    suspend fun readLatestWeight(): WeightEntry? =
        bodyReader.readLatestWeight()

    suspend fun readWeightEntries(start: Instant, end: Instant): List<WeightEntry> =
        bodyReader.readWeightEntries(start, end)

    suspend fun readLatestHeight(): Double? =
        bodyReader.readLatestHeight()

    suspend fun readLatestHeightEntry(): HeightEntry? =
        bodyReader.readLatestHeightEntry()

    suspend fun readHeightEntries(start: Instant, end: Instant): List<HeightEntry> =
        bodyReader.readHeightEntries(start, end)

    suspend fun readLatestBodyFat(): Double? =
        bodyReader.readLatestBodyFat()

    suspend fun readBodyFatEntries(start: Instant, end: Instant): List<BodyFatEntry> =
        bodyReader.readBodyFatEntries(start, end)

    suspend fun readLatestLeanBodyMass(): Double? =
        bodyReader.readLatestLeanBodyMass()

    suspend fun readLeanBodyMassEntries(start: Instant, end: Instant): List<LeanBodyMassEntry> =
        bodyReader.readLeanBodyMassEntries(start, end)

    suspend fun readLatestBMR(): Double? =
        bodyReader.readLatestBMR()

    suspend fun readBmrEntries(start: Instant, end: Instant): List<BmrEntry> =
        bodyReader.readBmrEntries(start, end)

    suspend fun readLatestBoneMass(): Double? =
        bodyReader.readLatestBoneMass()

    suspend fun readBoneMassEntries(start: Instant, end: Instant): List<BoneMassEntry> =
        bodyReader.readBoneMassEntries(start, end)

    suspend fun readLatestBodyWaterMass(): Double? =
        bodyReader.readLatestBodyWaterMass()

    suspend fun readBodyWaterMassEntries(start: Instant, end: Instant): List<BodyWaterMassEntry> =
        bodyReader.readBodyWaterMassEntries(start, end)

    suspend fun writeBodyMeasurementEntry(request: BodyMeasurementWriteRequest): String =
        bodyReader.writeBodyMeasurementEntry(request)

    suspend fun readBodyMeasurementEntry(type: BodyMeasurementType, id: String): BodyMeasurementEntry? =
        bodyReader.readBodyMeasurementEntry(type, id)

    suspend fun updateBodyMeasurementEntry(id: String, request: BodyMeasurementWriteRequest) =
        bodyReader.updateBodyMeasurementEntry(id, request)

    suspend fun deleteBodyMeasurementEntry(type: BodyMeasurementType, id: String) =
        bodyReader.deleteBodyMeasurementEntry(type, id)

    suspend fun readDailyNutrition(
        startDate: LocalDate,
        endDate: LocalDate,
        includeHydration: Boolean = true,
        includeCalories: Boolean = true,
        includeEstimatedCalories: Boolean = false,
    ): List<DailyNutrition> =
        nutritionReader.readDailyNutrition(
            startDate = startDate,
            endDate = endDate,
            includeHydration = includeHydration,
            includeCalories = includeCalories,
            includeEstimatedCalories = includeEstimatedCalories,
        )

    suspend fun readDailyMacros(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> =
        nutritionReader.readDailyMacros(startDate, endDate)

    suspend fun readNutritionEntries(start: Instant, end: Instant): List<NutritionEntry> =
        nutritionReader.readNutritionEntries(start, end)

    suspend fun readMindfulnessSessions(start: Instant, end: Instant): List<MindfulnessSession> =
        mindfulnessReader.readMindfulnessSessions(start, end)

    suspend fun readMindfulnessSession(id: String): MindfulnessSession? =
        mindfulnessReader.readMindfulnessSession(id)

    suspend fun readMindfulnessMinutes(date: LocalDate): Int =
        mindfulnessReader.readMindfulnessMinutes(date)

    suspend fun writeMindfulnessSessionEntry(request: MindfulnessSessionWriteRequest): String =
        mindfulnessReader.writeMindfulnessSessionEntry(request)

    suspend fun updateMindfulnessSessionEntry(id: String, request: MindfulnessSessionWriteRequest) =
        mindfulnessReader.updateMindfulnessSessionEntry(id, request)

    suspend fun deleteMindfulnessSessionEntry(id: String) =
        mindfulnessReader.deleteMindfulnessSessionEntry(id)

    suspend fun readMenstruationFlowEntries(start: Instant, end: Instant): List<MenstruationFlowEntry> =
        cycleReader.readMenstruationFlowEntries(start, end)

    suspend fun readMenstruationPeriods(start: Instant, end: Instant): List<MenstruationPeriodEntry> =
        cycleReader.readMenstruationPeriods(start, end)

    suspend fun readOvulationTests(start: Instant, end: Instant): List<OvulationTestEntry> =
        cycleReader.readOvulationTests(start, end)

    suspend fun readCervicalMucusEntries(start: Instant, end: Instant): List<CervicalMucusEntry> =
        cycleReader.readCervicalMucusEntries(start, end)

    suspend fun readBasalBodyTemperatureEntries(start: Instant, end: Instant): List<BasalBodyTemperatureEntry> =
        cycleReader.readBasalBodyTemperatureEntries(start, end)

    suspend fun readIntermenstrualBleedingEntries(start: Instant, end: Instant): List<IntermenstrualBleedingEntry> =
        cycleReader.readIntermenstrualBleedingEntries(start, end)

    suspend fun readSexualActivityEntries(start: Instant, end: Instant): List<SexualActivityEntry> =
        cycleReader.readSexualActivityEntries(start, end)

    suspend fun readBloodPressureEntries(start: Instant, end: Instant): List<BloodPressureEntry> =
        vitalsReader.readBloodPressureEntries(start, end)

    suspend fun readLatestBloodPressure(date: LocalDate): BloodPressureEntry? =
        vitalsReader.readLatestBloodPressure(date)

    suspend fun readSpO2Entries(start: Instant, end: Instant): List<SpO2Entry> =
        vitalsReader.readSpO2Entries(start, end)

    suspend fun readLatestSpO2(date: LocalDate): SpO2Entry? =
        vitalsReader.readLatestSpO2(date)

    suspend fun readRespiratoryRateEntries(start: Instant, end: Instant): List<RespiratoryRateEntry> =
        vitalsReader.readRespiratoryRateEntries(start, end)

    suspend fun readBodyTemperatureEntries(start: Instant, end: Instant): List<BodyTempEntry> =
        vitalsReader.readBodyTemperatureEntries(start, end)

    suspend fun readVo2MaxEntries(start: Instant, end: Instant): List<Vo2MaxEntry> =
        vitalsReader.readVo2MaxEntries(start, end)

    suspend fun readLatestVo2Max(date: LocalDate): Vo2MaxEntry? =
        vitalsReader.readLatestVo2Max(date)

    suspend fun readBloodGlucoseEntries(start: Instant, end: Instant): List<BloodGlucoseEntry> =
        vitalsReader.readBloodGlucoseEntries(start, end)

    suspend fun readSkinTemperatureEntries(start: Instant, end: Instant): List<SkinTemperatureEntry> =
        vitalsReader.readSkinTemperatureEntries(start, end)

    suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequest): String =
        vitalsReader.writeVitalsMeasurementEntry(request)

    suspend fun readVitalsMeasurementEntry(type: VitalsMeasurementType, id: String): VitalsMeasurementEntry? =
        vitalsReader.readVitalsMeasurementEntry(type, id)

    suspend fun updateVitalsMeasurementEntry(id: String, request: VitalsMeasurementWriteRequest) =
        vitalsReader.updateVitalsMeasurementEntry(id, request)

    suspend fun deleteVitalsMeasurementEntry(type: VitalsMeasurementType, id: String) =
        vitalsReader.deleteVitalsMeasurementEntry(type, id)

    suspend fun insertImportedRecords(records: List<Record>) {
        client().insertRecords(records)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun readImportedClientRecordIds(
        recordType: KClass<out Record>,
        start: Instant,
        end: Instant,
    ): Set<String> {
        val typedRecordType = recordType as KClass<Record>
        val clientRecordIds = mutableSetOf<String>()
        var pageToken: String? = null
        do {
            val response = client().readRecords(
                ReadRecordsRequest(
                    recordType = typedRecordType,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageSize = 1000,
                    pageToken = pageToken,
                ),
            )
            response.records.mapNotNullTo(clientRecordIds) { record ->
                record.metadata.clientRecordId?.takeIf { it.startsWith("apple_health_") }
            }
            pageToken = response.pageToken
        } while (!pageToken.isNullOrBlank())
        return clientRecordIds
    }

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)
}
