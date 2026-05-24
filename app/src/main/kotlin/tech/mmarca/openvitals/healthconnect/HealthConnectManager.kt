package tech.mmarca.openvitals.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.BoneMassEntry
import tech.mmarca.openvitals.data.model.CervicalMucusEntry
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.data.model.HydrationEntry
import tech.mmarca.openvitals.data.model.LeanBodyMassEntry
import tech.mmarca.openvitals.data.model.MenstruationFlowEntry
import tech.mmarca.openvitals.data.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.data.model.OvulationTestEntry
import tech.mmarca.openvitals.data.model.PermissionGrantMode
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.StepProgressPoint
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import java.time.Instant
import java.time.LocalDate

/**
 * Facade over Health Connect services.
 *
 * Public methods stay feature-oriented for repositories and ViewModels, while
 * permissions, availability, paging, reading, and mapping live in focused
 * Health Connect boundary classes.
 */
class HealthConnectManager(private val context: Context) {
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
    private val activityReader = ActivityHealthReader(readerSupport)
    private val hydrationReader = HydrationHealthReader(readerSupport)
    private val sleepReader = SleepHealthReader(readerSupport)
    private val heartReader = HeartHealthReader(readerSupport)
    private val bodyReader = BodyHealthReader(readerSupport)
    private val nutritionReader = NutritionHealthReader(readerSupport)
    private val mindfulnessReader = MindfulnessHealthReader(readerSupport)
    private val cycleReader = CycleHealthReader(readerSupport)
    private val vitalsReader = VitalsHealthReader(readerSupport)

    val corePermissions: Set<String> get() = permissionService.corePermissions
    val routePermissions: Set<String> get() = permissionService.routePermissions
    val heartPermissions: Set<String> get() = permissionService.heartPermissions
    val bodyPermissions: Set<String> get() = permissionService.bodyPermissions
    val activityExtrasPermissions: Set<String> get() = permissionService.activityExtrasPermissions
    val nutritionHydrationPermissions: Set<String> get() = permissionService.nutritionHydrationPermissions
    val mindfulnessPermissions: Set<String> get() = permissionService.mindfulnessPermissions
    val additionalDataAccessPermissions: Set<String> get() = permissionService.additionalDataAccessPermissions
    val vitalsPermissions: Set<String> get() = permissionService.vitalsPermissions
    val cyclePermissions: Set<String> get() = permissionService.cyclePermissions
    val phase1Permissions: Set<String> get() = permissionService.phase1Permissions
    val phase2Permissions: Set<String> get() = permissionService.phase2Permissions
    val phase3Permissions: Set<String> get() = permissionService.phase3Permissions
    val phase4Permissions: Set<String> get() = permissionService.phase4Permissions
    val manualOnlyPermissions: Set<String> get() = permissionService.manualOnlyPermissions
    val requestableAllPermissions: Set<String> get() = permissionService.requestableAllPermissions
    val requestableManagedPermissions: Set<String> get() = permissionService.requestableManagedPermissions
    val allPermissions: Set<String> get() = permissionService.allPermissions
    val managedPermissions: Set<String> get() = permissionService.managedPermissions

    fun grantModeFor(permission: String): PermissionGrantMode =
        permissionService.grantModeFor(permission)

    fun availability(): HealthConnectAvailability =
        availabilityService.availability()

    fun isMindfulnessSessionAvailable(): Boolean =
        permissionService.isMindfulnessSessionAvailable()

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
        includeFloors: Boolean = false,
        includeActiveCalories: Boolean = false,
        includeElevation: Boolean = false,
    ): List<DailySteps> =
        activityReader.readDailySteps(startDate, endDate, includeFloors, includeActiveCalories, includeElevation)

    suspend fun readFloorsClimbed(date: LocalDate): Int =
        activityReader.readFloorsClimbed(date)

    suspend fun readElevationGained(date: LocalDate): Double =
        activityReader.readElevationGained(date)

    suspend fun readStepProgress(date: LocalDate): List<StepProgressPoint> =
        activityReader.readStepProgress(date)

    suspend fun readActivityProgress(
        date: LocalDate,
        includeDistance: Boolean,
        includeCalories: Boolean,
        includeActiveCalories: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
    ): List<ActivityProgressPoint> =
        activityReader.readActivityProgress(
            date = date,
            includeDistance = includeDistance,
            includeCalories = includeCalories,
            includeActiveCalories = includeActiveCalories,
            includeFloors = includeFloors,
            includeElevation = includeElevation,
        )

    suspend fun readDistanceMeters(date: LocalDate): Double =
        activityReader.readDistanceMeters(date)

    suspend fun readTodayDistanceMeters(): Double =
        activityReader.readTodayDistanceMeters()

    suspend fun readCaloriesKcal(date: LocalDate): Double? =
        activityReader.readCaloriesKcal(date)

    suspend fun readTodayCaloriesKcal(): Double? =
        activityReader.readTodayCaloriesKcal()

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
        includeFloors: Boolean,
        includeElevation: Boolean,
    ): ExerciseData? =
        activityReader.readExerciseSession(
            id = id,
            includeSteps = includeSteps,
            includeDistance = includeDistance,
            includeTotalCalories = includeTotalCalories,
            includeActiveCalories = includeActiveCalories,
            includeFloors = includeFloors,
            includeElevation = includeElevation,
        )

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

    suspend fun readDailyNutrition(
        startDate: LocalDate,
        endDate: LocalDate,
        includeHydration: Boolean = true,
        includeCalories: Boolean = true,
    ): List<DailyNutrition> =
        nutritionReader.readDailyNutrition(startDate, endDate, includeHydration, includeCalories)

    suspend fun readDailyMacros(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> =
        nutritionReader.readDailyMacros(startDate, endDate)

    suspend fun readNutritionEntries(start: Instant, end: Instant): List<NutritionEntry> =
        nutritionReader.readNutritionEntries(start, end)

    suspend fun readMindfulnessSessions(start: Instant, end: Instant): List<MindfulnessSession> =
        mindfulnessReader.readMindfulnessSessions(start, end)

    suspend fun readMindfulnessMinutes(date: LocalDate): Int =
        mindfulnessReader.readMindfulnessMinutes(date)

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

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)
}
