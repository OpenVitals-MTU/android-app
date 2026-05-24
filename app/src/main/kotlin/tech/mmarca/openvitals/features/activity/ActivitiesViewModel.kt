package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoalMinutes: Double = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
    val workouts: List<ExerciseData> = emptyList(),
    val previousWorkouts: List<ExerciseData> = emptyList(),
    val baselineWorkouts: List<ExerciseData> = emptyList(),
    val crossDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val error: String? = null,
)

class ActivitiesViewModel(
    private val repository: ActivityRepository,
    private val heartRepository: HeartRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialDailyGoalMinutes: Double = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    private val goalKey = MetricDailyGoalKey.WORKOUT_MINUTES
    private val _uiState = MutableStateFlow(
        ActivitiesUiState(
            selectedRange = initialRange,
            dailyGoalMinutes = goalKey.normalize(initialDailyGoalMinutes),
        )
    )
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()

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
        setDailyGoalMinutes(_uiState.value.dailyGoalMinutes + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoalMinutes(_uiState.value.dailyGoalMinutes - goalKey.step)
    }

    fun setDailyGoalMinutes(minutes: Double) {
        val goal = goalKey.normalize(minutes)
        onDailyGoalChanged(goal)
        _uiState.value = _uiState.value.copy(dailyGoalMinutes = goal)
    }

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            val baselinePeriod = baselinePeriodBefore(period)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                ActivitiesLoadResult(
                    workouts = repository.loadWorkouts(period.start, period.end),
                    previousWorkouts = repository.loadWorkouts(previousPeriod.start, previousPeriod.end),
                    baselineWorkouts = repository.loadWorkouts(baselinePeriod.start, baselinePeriod.end),
                    crossDailyRestingHR = heartRepository?.loadDailyRestingHR(period.start, period.end).orEmpty(),
                )
            }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        workouts = result.workouts,
                        previousWorkouts = result.previousWorkouts,
                        baselineWorkouts = result.baselineWorkouts,
                        crossDailyRestingHR = result.crossDailyRestingHR,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = it.message,
                    )
                }
        }
    }

    private data class ActivitiesLoadResult(
        val workouts: List<ExerciseData>,
        val previousWorkouts: List<ExerciseData>,
        val baselineWorkouts: List<ExerciseData>,
        val crossDailyRestingHR: List<DailyRestingHR>,
    )

    private val periodSelection: PeriodSelection
        get() = PeriodSelection(_uiState.value.selectedRange, _uiState.value.selectedDate)

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}
