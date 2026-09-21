package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.MindfulnessBellSound
import tech.mmarca.openvitals.domain.model.MindfulnessTimerConfig
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode

/**
 * In-memory stand-in for the preference contracts.
 *
 * The real repository needs a Context, so view model tests used to mock its hundred members.
 * Set what the test needs, read back what the view model wrote.
 */
class FakePreferences(
    /** Used for every key. Null leaves each key on its own default. */
    initialRange: TimeRange? = null,
    initialGoal: Double? = null,
    initialWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    initialProfile: BodyProfile = BodyProfile(),
    initialShowCalculatedCalories: Boolean = true,
    initialSleepWindow: SleepWindow = SleepWindow.Default,
    initialNutritionAverageBasis: NutritionAverageBasis = NutritionAverageBasis.LOGGED_DAYS,
    initialGuideline: BloodPressureGuideline = BloodPressureGuideline.ACC_AHA_2017,
    initialCalibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    initialCaffeine: CaffeinePreferences = CaffeinePreferences(),
    initialTimer: MindfulnessTimerConfig = MindfulnessTimerConfig(
        durationMinutes = 10,
        intervalMinutes = null,
        bellSound = MindfulnessBellSound.STRUCK,
    ),
    override var highHeartRateThresholdBpm: Int = 120,
    override var lowHeartRateThresholdBpm: Int = 50,
    override var hydrationDailyGoalLiters: Double = 2.0,
    initialSplitDistanceMeters: Double = ActivitySplitDistance.defaultMeters,
    initialFavoriteExerciseType: Int? = null,
    initialLastExerciseType: Int? = null,
    override var unitSystem: UnitSystem = UnitSystem.METRIC,
    override var onboardingDone: Boolean = false,
    override var appLanguage: AppLanguage = AppLanguage.SYSTEM,
    override var mindfulnessOptIn: Boolean = false,
    override var healthConnectMindfulnessEnabled: Boolean = false,
    override var healthConnectSyncEnabled: Boolean = true,
    override var appLockEnabled: Boolean = false,
) : PeriodPreferences,
    DailyGoalPreferences,
    BodyProfilePreferences,
    CalorieDisplayPreferences,
    SleepWindowPreferences,
    NutritionDisplayPreferences,
    HeartThresholdPreferences,
    HydrationGoalPreferences,
    BodyEnergyCalibrationPreferences,
    CaffeineModelPreferences,
    MindfulnessTimerPreferences,
    ActivitySplitPreferences,
    WidgetOrderPreferences,
    RecordingPreferences,
    UnitPreferences,
    OnboardingPreferences,
    HealthConnectPreferences {

    private val ranges = mutableMapOf<PeriodRangePreferenceKey, TimeRange>()
    private val goals = mutableMapOf<MetricDailyGoalKey, Double>()
    private val defaultRange = initialRange
    private val defaultGoal = initialGoal
    private val weekMode = MutableStateFlow(initialWeekMode)
    private val profile = MutableStateFlow(initialProfile)
    private val calculatedCalories = MutableStateFlow(initialShowCalculatedCalories)
    private val sleep = MutableStateFlow(initialSleepWindow)
    private val nutritionBasis = MutableStateFlow(initialNutritionAverageBasis)
    private val guideline = MutableStateFlow(initialGuideline)
    private val calibration = MutableStateFlow(initialCalibration)
    private val caffeine = MutableStateFlow(initialCaffeine)
    private var timer = initialTimer
    private val splitDistance = MutableStateFlow(initialSplitDistanceMeters)

    /** Every range the view model stored, in order. */
    val storedRanges = mutableListOf<TimeRange>()

    /** Every goal the view model stored, in order. */
    val storedGoals = mutableListOf<Double>()

    override var activityWeekMode: ActivityWeekMode
        get() = weekMode.value
        set(value) { weekMode.value = value }

    override val activityWeekModeFlow: Flow<ActivityWeekMode> = weekMode

    override val weekPeriodMode: WeekPeriodMode
        get() = weekMode.value.toWeekPeriodMode()

    override val weekPeriodModeFlow: Flow<WeekPeriodMode> = weekMode.map { it.toWeekPeriodMode() }

    override fun timeRangeFor(key: PeriodRangePreferenceKey): TimeRange =
        ranges[key] ?: defaultRange ?: key.defaultRange

    override fun setTimeRangeFor(key: PeriodRangePreferenceKey, range: TimeRange) {
        ranges[key] = range
        storedRanges += range
    }

    override fun dailyGoalFor(key: MetricDailyGoalKey): Double =
        goals[key] ?: defaultGoal ?: key.defaultValue

    override fun setDailyGoalFor(key: MetricDailyGoalKey, value: Double) {
        goals[key] = value
        storedGoals += value
    }

    override fun bodyProfile(): BodyProfile = profile.value

    override val bodyProfileFlow: Flow<BodyProfile> = profile

    override fun setBodyProfile(profile: BodyProfile) {
        this.profile.value = profile
    }

    override var showOpenVitalsCalculatedCalories: Boolean
        get() = calculatedCalories.value
        set(value) { calculatedCalories.value = value }

    override val showOpenVitalsCalculatedCaloriesFlow: Flow<Boolean> = calculatedCalories

    override val sleepWindow: SleepWindow
        get() = sleep.value

    override val sleepWindowFlow: Flow<SleepWindow> = sleep

    /** Moves the window, the way Settings does, so the screen's observer fires. */
    fun setSleepWindow(window: SleepWindow) {
        sleep.value = window
    }

    override var nutritionAverageBasis: NutritionAverageBasis
        get() = nutritionBasis.value
        set(value) { nutritionBasis.value = value }

    override val nutritionAverageBasisFlow: Flow<NutritionAverageBasis> = nutritionBasis

    override var bloodPressureGuideline: BloodPressureGuideline
        get() = guideline.value
        set(value) { guideline.value = value }

    override val bloodPressureGuidelineFlow: Flow<BloodPressureGuideline> = guideline

    override fun bodyEnergyCalibration(): BodyEnergyCalibration = calibration.value

    override val bodyEnergyCalibrationFlow: Flow<BodyEnergyCalibration> = calibration

    override fun setBodyEnergyCalibration(calibration: BodyEnergyCalibration) {
        this.calibration.value = calibration
    }

    override fun caffeinePreferences(): CaffeinePreferences = caffeine.value

    override val caffeinePreferencesFlow: Flow<CaffeinePreferences> = caffeine

    /** Normalizes, as the repository does, so a test sees the stored value. */
    override fun setCaffeinePreferences(preferences: CaffeinePreferences) {
        caffeine.value = preferences.normalized()
    }

    override fun mindfulnessTimerConfig(): MindfulnessTimerConfig = timer

    override fun setMindfulnessTimerConfig(config: MindfulnessTimerConfig) {
        timer = config
    }

    override var activitySplitDistanceMeters: Double
        get() = splitDistance.value
        set(value) { splitDistance.value = value }

    override val activitySplitDistanceMetersFlow: Flow<Double> = splitDistance

    private var manualEntryOrder: List<String>? = null
    private var metricDetailOrder: List<String>? = null

    override fun manualEntryWidgetOrder(): List<String>? = manualEntryOrder

    override fun setManualEntryWidgetOrder(widgetIds: List<String>) {
        manualEntryOrder = widgetIds
    }

    override fun metricDetailSectionOrder(): List<String>? = metricDetailOrder

    override fun setMetricDetailSectionOrder(sectionIds: List<String>) {
        metricDetailOrder = sectionIds
    }

    private var recording = ActivityRecordingPreferences()

    override fun activityRecordingPreferences(): ActivityRecordingPreferences = recording

    override fun setActivityRecordingPreferences(preferences: ActivityRecordingPreferences) {
        recording = preferences.normalized()
    }

    override var lastActivityExerciseType: Int? = initialLastExerciseType

    override var favoriteActivityExerciseType: Int? = initialFavoriteExerciseType

    private val unitOverrides = mutableMapOf<UnitQuantity, UnitSystem>()

    override fun unitOverride(quantity: UnitQuantity): UnitSystem? = unitOverrides[quantity]

    fun setUnitOverride(quantity: UnitQuantity, override: UnitSystem?) {
        if (override == null) unitOverrides.remove(quantity) else unitOverrides[quantity] = override
    }

    /** How often the current policy was accepted. */
    var privacyPolicyAccepted = 0
        private set

    override fun acceptCurrentPrivacyPolicy() {
        privacyPolicyAccepted++
    }
}
