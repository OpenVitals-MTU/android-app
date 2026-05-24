package tech.mmarca.openvitals.features.mindfulness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MindfulnessUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoalMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
    val sessions: List<MindfulnessSession> = emptyList(),
    val error: String? = null,
) {
    val totalMinutes: Long get() = sessions.sumOf { it.durationMinutes }
}

class MindfulnessViewModel(
    private val repository: MindfulnessRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    initialDailyGoalMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    private val goalKey = MetricDailyGoalKey.MINDFULNESS_MINUTES
    private val _uiState = MutableStateFlow(
        MindfulnessUiState(
            selectedRange = initialRange,
            dailyGoalMinutes = goalKey.normalize(initialDailyGoalMinutes),
        )
    )
    val uiState: StateFlow<MindfulnessUiState> = _uiState.asStateFlow()

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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadMindfulnessSessions(period.start, period.end) }
                .onSuccess { sessions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sessions = sessions,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = error.message,
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
