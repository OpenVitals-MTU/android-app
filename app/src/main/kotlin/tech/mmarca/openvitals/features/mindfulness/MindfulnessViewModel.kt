package tech.mmarca.openvitals.features.mindfulness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.repository.SleepRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MindfulnessUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoalMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val sessions: List<MindfulnessSession> = emptyList(),
    val previousSessions: List<MindfulnessSession> = emptyList(),
    val baselineSessions: List<MindfulnessSession> = emptyList(),
    val crossSleepSessions: List<SleepData> = emptyList(),
    val error: String? = null,
) {
    val totalMinutes: Long get() = sessions.sumOf { it.durationMinutes }
}

class MindfulnessViewModel(
    private val repository: MindfulnessRepository,
    private val sleepRepository: SleepRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialSleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    initialDailyGoalMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    private val goalKey = MetricDailyGoalKey.MINDFULNESS_MINUTES
    private val _uiState = MutableStateFlow(
        MindfulnessUiState(
            selectedRange = initialRange,
            sleepRangeMode = initialSleepRangeMode,
            dailyGoalMinutes = goalKey.normalize(initialDailyGoalMinutes),
        )
    )
    val uiState: StateFlow<MindfulnessUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

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
        loadCoordinator.launch(viewModelScope) load@{
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            val baselinePeriod = baselinePeriodBefore(period)
            val sleepRangeMode = _uiState.value.sleepRangeMode
            val sleepQueryStart = when (sleepRangeMode) {
                SleepRangeMode.ROLLING_24H -> period.start
                SleepRangeMode.NOON,
                SleepRangeMode.EVENING_18H -> period.start.minusDays(1)
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                MindfulnessLoadResult(
                    sessions = repository.loadMindfulnessSessions(period.start, period.end),
                    previousSessions = repository.loadMindfulnessSessions(previousPeriod.start, previousPeriod.end),
                    baselineSessions = repository.loadMindfulnessSessions(baselinePeriod.start, baselinePeriod.end),
                    crossSleepSessions = sleepRepository?.loadSleepSessions(sleepQueryStart, period.end).orEmpty(),
                )
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sessions = result.sessions,
                        previousSessions = result.previousSessions,
                        baselineSessions = result.baselineSessions,
                        crossSleepSessions = result.crossSleepSessions,
                    )
                }
                .onFailure { error ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = error.message,
                    )
                }
        }
    }

    private data class MindfulnessLoadResult(
        val sessions: List<MindfulnessSession>,
        val previousSessions: List<MindfulnessSession>,
        val baselineSessions: List<MindfulnessSession>,
        val crossSleepSessions: List<SleepData>,
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
