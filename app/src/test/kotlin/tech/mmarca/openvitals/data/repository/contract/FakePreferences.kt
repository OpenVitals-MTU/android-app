package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.SleepWindow
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
) : PeriodPreferences,
    DailyGoalPreferences,
    BodyProfilePreferences,
    CalorieDisplayPreferences,
    SleepWindowPreferences {

    private val ranges = mutableMapOf<PeriodRangePreferenceKey, TimeRange>()
    private val goals = mutableMapOf<MetricDailyGoalKey, Double>()
    private val defaultRange = initialRange
    private val defaultGoal = initialGoal
    private val weekMode = MutableStateFlow(initialWeekMode)
    private val profile = MutableStateFlow(initialProfile)
    private val calculatedCalories = MutableStateFlow(initialShowCalculatedCalories)
    private val sleep = MutableStateFlow(initialSleepWindow)

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
}
