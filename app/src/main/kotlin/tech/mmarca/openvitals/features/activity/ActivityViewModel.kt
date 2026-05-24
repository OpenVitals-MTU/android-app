package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivityUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoal: Double = ActivityMetric.STEPS.dailyGoalKey.defaultValue,
    val dailySteps: List<DailySteps> = emptyList(),
    val previousDailySteps: List<DailySteps> = emptyList(),
    val nutrition: List<DailyNutrition> = emptyList(),
    val previousNutrition: List<DailyNutrition> = emptyList(),
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    val error: String? = null,
)

class ActivityViewModel(
    private val repository: ActivityRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    private val selectedMetric: ActivityMetric = ActivityMetric.STEPS,
    initialDailyGoal: Double = selectedMetric.dailyGoalKey.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    private val goalKey = selectedMetric.dailyGoalKey
    private val _uiState = MutableStateFlow(
        ActivityUiState(
            selectedRange = initialRange,
            dailyGoal = goalKey.normalize(initialDailyGoal),
        )
    )
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectRange(range: TimeRange) {
        onRangeSelected(range)
        applyPeriodSelection(periodSelection.selectRange(range))
        load()
    }

    fun previousPeriod() {
        applyPeriodSelection(periodSelection.previousPeriod())
        load()
    }

    fun nextPeriod() {
        val current = periodSelection
        val next = current.nextPeriod()
        if (next != current) {
            applyPeriodSelection(next)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodSelection.selectDate(date))
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
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val dailySteps = if (selectedMetric.usesDailySteps) {
                    repository.loadDailySteps(period.start, period.end)
                } else {
                    emptyList()
                }
                val previousDailySteps = if (selectedMetric.usesDailySteps) {
                    repository.loadDailySteps(previousPeriod.start, previousPeriod.end)
                } else {
                    emptyList()
                }
                val nutrition = if (selectedMetric.usesDailyNutrition) {
                    repository.loadDailyNutrition(period.start, period.end)
                } else {
                    emptyList()
                }
                val previousNutrition = if (selectedMetric.usesDailyNutrition) {
                    repository.loadDailyNutrition(previousPeriod.start, previousPeriod.end)
                } else {
                    emptyList()
                }
                val activityProgress = if (range == TimeRange.DAY) {
                    repository.loadActivityProgress(period.start)
                } else {
                    emptyList()
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailySteps = dailySteps,
                    previousDailySteps = previousDailySteps,
                    nutrition = nutrition,
                    previousNutrition = previousNutrition,
                    activityProgress = activityProgress,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = it.message,
                )
            }
        }
    }

    private val periodSelection: PeriodSelection
        get() = PeriodSelection(_uiState.value.selectedRange, _uiState.value.selectedDate)

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
