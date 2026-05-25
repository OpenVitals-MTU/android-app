package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActivityUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoal: Double = ActivityMetric.STEPS.dailyGoalKey.defaultValue,
    val dailySteps: List<DailySteps> = emptyList(),
    val previousDailySteps: List<DailySteps> = emptyList(),
    val baselineDailySteps: List<DailySteps> = emptyList(),
    val nutrition: List<DailyNutrition> = emptyList(),
    val previousNutrition: List<DailyNutrition> = emptyList(),
    val baselineNutrition: List<DailyNutrition> = emptyList(),
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ActivityViewModel(
    private val repository: ActivityRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    private val selectedMetric: ActivityMetric = ActivityMetric.STEPS,
    initialDailyGoal: Double = selectedMetric.dailyGoalKey.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.STEPS),
        selectedMetric = activityMetricFromRoute(savedStateHandle[METRIC_ID_ARG]),
        initialDailyGoal = preferencesRepository.dailyGoalFor(
            activityMetricFromRoute(savedStateHandle[METRIC_ID_ARG]).dailyGoalKey
        ),
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.STEPS, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(
                activityMetricFromRoute(savedStateHandle[METRIC_ID_ARG]).dailyGoalKey,
                goal,
            )
        },
    )

    private val goalKey = selectedMetric.dailyGoalKey
    private val periodDriver = PeriodSelectionDriver(initialRange, onRangeSelected = onRangeSelected)
    private val _uiState = MutableStateFlow(
        ActivityUiState(
            selectedRange = initialRange,
            dailyGoal = goalKey.normalize(initialDailyGoal),
        )
    )
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
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

    fun increaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal - goalKey.step)
    }

    fun setDailyGoal(goal: Double) {
        val normalized = goalKey.normalize(goal)
        onDailyGoalChanged(normalized)
        _uiState.value = _uiState.value.copy(dailyGoal = normalized)
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadActivityPeriod(
                    query = query,
                    includeSteps = selectedMetric.usesDailySteps,
                    includeNutrition = selectedMetric.usesDailyNutrition,
                )
            }.onSuccess { result ->
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
                )
            }.onFailure {
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = it.message,
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
}

private val ActivityMetric.usesDailySteps: Boolean
    get() = this != ActivityMetric.CALORIES_BURNED

private val ActivityMetric.usesDailyNutrition: Boolean
    get() = this == ActivityMetric.CALORIES_BURNED

private fun activityMetricFromRoute(metricId: String?): ActivityMetric =
    when (metricId) {
        "DISTANCE" -> ActivityMetric.DISTANCE
        "CALORIES_OUT" -> ActivityMetric.CALORIES_BURNED
        "ACTIVE_CALORIES" -> ActivityMetric.ACTIVE_CALORIES
        "FLOORS" -> ActivityMetric.FLOORS
        "ELEVATION" -> ActivityMetric.ELEVATION
        else -> ActivityMetric.STEPS
    }
