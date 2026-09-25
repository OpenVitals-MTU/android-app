package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.navigation.selectedDayOrNull
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.CalorieDisplayPreferences
import tech.mmarca.openvitals.data.repository.contract.DailyGoalPreferences
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class ActivityUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val dailyGoal: Double = ActivityMetric.STEPS.dailyGoalKey.defaultValue,
    val dailySteps: List<DailySteps> = emptyList(),
    val previousDailySteps: List<DailySteps> = emptyList(),
    val baselineDailySteps: List<DailySteps> = emptyList(),
    val nutrition: List<DailyNutrition> = emptyList(),
    val previousNutrition: List<DailyNutrition> = emptyList(),
    val baselineNutrition: List<DailyNutrition> = emptyList(),
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    val display: ActivityDisplayState = ActivityDisplayState(),
    val error: ScreenError? = null,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val periodPreferences: PeriodPreferences,
    private val dailyGoalPreferences: DailyGoalPreferences,
    private val calorieDisplayPreferences: CalorieDisplayPreferences,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The metric the route named; steps when it named none. */
    private val selectedMetric = activityMetricFromRoute(savedStateHandle[METRIC_ID_ARG])
    private val initialRange = periodPreferences.timeRangeFor(PeriodRangePreferenceKey.STEPS)
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode
    private val goalKey = selectedMetric.dailyGoalKey
    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = savedStateHandle.selectedDayOrNull() ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = { range ->
            periodPreferences.setTimeRangeFor(PeriodRangePreferenceKey.STEPS, range)
        },
    )
    private val _uiState = MutableStateFlow(
        ActivityUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            dailyGoal = goalKey.normalize(dailyGoalPreferences.dailyGoalFor(goalKey)),
        )
    )
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observeWeekPeriodMode()
        observeCalorieDataMode()
        load()
    }

    private fun observeWeekPeriodMode() {
        viewModelScope.launch {
            periodPreferences.weekPeriodModeFlow.drop(1).collect { mode ->
                periodDriver.weekPeriodMode = mode
                _uiState.value = _uiState.value.copy(weekPeriodMode = mode)
                if (_uiState.value.selectedRange == TimeRange.WEEK) {
                    load()
                }
            }
        }
    }

    private fun observeCalorieDataMode() {
        if (selectedMetric != ActivityMetric.CALORIES_BURNED) return
        viewModelScope.launch {
            var skipInitial = true
            calorieDisplayPreferences.showOpenVitalsCalculatedCaloriesFlow.collect {
                if (skipInitial) {
                    skipInitial = false
                } else {
                    load()
                }
            }
        }
    }

    fun selectRange(range: TimeRange) {
        applyPeriodSelection(periodDriver.selectRange(range))
        load()
    }

    fun previousPeriod() {
        applyPeriodSelection(periodDriver.previousPeriod())
        load()
    }

    fun nextPeriod() {
        periodDriver.nextPeriod()?.let { next ->
            applyPeriodSelection(next)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDate(date))
        load()
    }

    fun selectDay(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDay(date))
        load()
    }

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load()
            return
        }
        applyPeriodSelection(selection)
        load()
    }

    fun increaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal - goalKey.step)
    }

    fun setDailyGoal(goal: Double) {
        val normalized = goalKey.normalize(goal)
        dailyGoalPreferences.setDailyGoalFor(goalKey, normalized)
        _uiState.value = _uiState.value.copy(dailyGoal = normalized).withDisplay(selectedMetric)
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadActivityPeriod(
                    query = query,
                    includeSteps = selectedMetric.usesDailySteps,
                    includeNutrition = selectedMetric.usesDailyNutrition,
                    includeWheelchairPushes = selectedMetric.usesWheelchairPushes,
                )
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val display = withContext(dispatchers.default) {
                    ActivityPresentationMapper.build(
                        query = query,
                        metric = selectedMetric,
                        dailyGoal = _uiState.value.dailyGoal,
                        dailySteps = result.dailySteps,
                        previousDailySteps = result.previousDailySteps,
                        baselineDailySteps = result.baselineDailySteps,
                        nutrition = result.nutrition,
                        previousNutrition = result.previousNutrition,
                        baselineNutrition = result.baselineNutrition,
                        activityProgress = result.activityProgress,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailySteps = result.dailySteps,
                    previousDailySteps = result.previousDailySteps,
                    baselineDailySteps = result.baselineDailySteps,
                    nutrition = result.nutrition,
                    previousNutrition = result.previousNutrition,
                    baselineNutrition = result.baselineNutrition,
                    activityProgress = result.activityProgress,
                    display = display,
                )
            }.onFailure {
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = it.toScreenError(),
                )
            }
        }
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    private fun ActivityUiState.withDisplay(metric: ActivityMetric): ActivityUiState {
        val query = PeriodLoadQuery(
            range = selectedRange,
            anchorDate = selectedDate,
            weekPeriodMode = weekPeriodMode,
        )
        return copy(
            dailyGoal = dailyGoal,
            display = ActivityPresentationMapper.build(
                query = query,
                metric = metric,
                dailyGoal = dailyGoal,
                dailySteps = dailySteps,
                previousDailySteps = previousDailySteps,
                baselineDailySteps = baselineDailySteps,
                nutrition = nutrition,
                previousNutrition = previousNutrition,
                baselineNutrition = baselineNutrition,
                activityProgress = activityProgress,
            ),
        )
    }
}

private val ActivityMetric.usesDailySteps: Boolean
    get() = this != ActivityMetric.CALORIES_BURNED && this != ActivityMetric.WHEELCHAIR_PUSHES

private val ActivityMetric.usesDailyNutrition: Boolean
    get() = this == ActivityMetric.CALORIES_BURNED

private val ActivityMetric.usesWheelchairPushes: Boolean
    get() = this == ActivityMetric.WHEELCHAIR_PUSHES

/** The route argument that names [this]; the inverse of [activityMetricFromRoute]. */
internal fun ActivityMetric.routeId(): String = when (this) {
    ActivityMetric.CALORIES_BURNED -> "CALORIES_OUT"
    else -> name
}

/** The metric a route argument names. The ids are [tech.mmarca.openvitals.domain.dashboard.DashboardWidgetId] names. */
internal fun activityMetricFromRoute(metricId: String?): ActivityMetric =
    when (metricId) {
        "DISTANCE" -> ActivityMetric.DISTANCE
        "CALORIES_OUT" -> ActivityMetric.CALORIES_BURNED
        "ACTIVE_CALORIES" -> ActivityMetric.ACTIVE_CALORIES
        "FLOORS" -> ActivityMetric.FLOORS
        "ELEVATION" -> ActivityMetric.ELEVATION
        "WHEELCHAIR_PUSHES" -> ActivityMetric.WHEELCHAIR_PUSHES
        else -> ActivityMetric.STEPS
    }
