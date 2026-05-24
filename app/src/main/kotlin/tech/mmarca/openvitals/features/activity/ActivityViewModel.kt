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
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivityUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailySteps: List<DailySteps> = emptyList(),
    val nutrition: List<DailyNutrition> = emptyList(),
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    val error: String? = null,
)

class ActivityViewModel(
    private val repository: ActivityRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    private val selectedMetric: ActivityMetric = ActivityMetric.STEPS,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState(selectedRange = initialRange))
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

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val dailySteps = if (selectedMetric.usesDailySteps) {
                    repository.loadDailySteps(period.start, period.end)
                } else {
                    emptyList()
                }
                val nutrition = if (selectedMetric.usesDailyNutrition) {
                    repository.loadDailyNutrition(period.start, period.end)
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
                    nutrition = nutrition,
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
