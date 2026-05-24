package tech.mmarca.openvitals.features.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.repository.SleepRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SleepUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val dailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    val sessions: List<SleepData> = emptyList(),
    val previousSessions: List<SleepData> = emptyList(),
    val baselineSessions: List<SleepData> = emptyList(),
    val error: String? = null,
)

class SleepViewModel(
    private val repository: SleepRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    initialSleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    initialDailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    sleepRangeModeFlow: Flow<SleepRangeMode>? = null,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    private val goalKey = MetricDailyGoalKey.SLEEP_HOURS
    private val _uiState = MutableStateFlow(
        SleepUiState(
            selectedRange = initialRange,
            sleepRangeMode = initialSleepRangeMode,
            dailyGoalHours = goalKey.normalize(initialDailyGoalHours),
        )
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        sleepRangeModeFlow
            ?.distinctUntilChanged()
            ?.onEach { mode ->
                if (_uiState.value.sleepRangeMode != mode) {
                    _uiState.value = _uiState.value.copy(sleepRangeMode = mode)
                    load()
                }
            }
            ?.launchIn(viewModelScope)
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
        setDailyGoalHours(_uiState.value.dailyGoalHours + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoalHours(_uiState.value.dailyGoalHours - goalKey.step)
    }

    fun setDailyGoalHours(hours: Double) {
        val goal = goalKey.normalize(hours)
        onDailyGoalChanged(goal)
        _uiState.value = _uiState.value.copy(dailyGoalHours = goal)
    }

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val sleepRangeMode = _uiState.value.sleepRangeMode
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            val baselinePeriod = baselinePeriodBefore(period)
            val queryStart = when (sleepRangeMode) {
                SleepRangeMode.ROLLING_24H -> period.start
                SleepRangeMode.NOON,
                SleepRangeMode.EVENING_18H -> period.start.minusDays(1)
            }
            val previousQueryStart = when (sleepRangeMode) {
                SleepRangeMode.ROLLING_24H -> previousPeriod.start
                SleepRangeMode.NOON,
                SleepRangeMode.EVENING_18H -> previousPeriod.start.minusDays(1)
            }
            val baselineQueryStart = when (sleepRangeMode) {
                SleepRangeMode.ROLLING_24H -> baselinePeriod.start
                SleepRangeMode.NOON,
                SleepRangeMode.EVENING_18H -> baselinePeriod.start.minusDays(1)
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                SleepLoadResult(
                    sessions = repository.loadSleepSessions(queryStart, period.end),
                    previousSessions = repository.loadSleepSessions(previousQueryStart, previousPeriod.end),
                    baselineSessions = repository.loadSleepSessions(baselineQueryStart, baselinePeriod.end),
                )
            }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepRangeMode = sleepRangeMode,
                        sessions = result.sessions,
                        previousSessions = result.previousSessions,
                        baselineSessions = result.baselineSessions,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepRangeMode = sleepRangeMode,
                        error = it.message,
                    )
                }
        }
    }

    private data class SleepLoadResult(
        val sessions: List<SleepData>,
        val previousSessions: List<SleepData>,
        val baselineSessions: List<SleepData>,
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
