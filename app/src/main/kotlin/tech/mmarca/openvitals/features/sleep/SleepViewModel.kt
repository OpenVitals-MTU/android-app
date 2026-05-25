package tech.mmarca.openvitals.features.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SleepUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val dailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    val sessions: List<SleepData> = emptyList(),
    val previousSessions: List<SleepData> = emptyList(),
    val baselineSessions: List<SleepData> = emptyList(),
    val crossDailyHrv: List<DailyHrv> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SleepViewModel(
    private val repository: SleepRepository,
    private val heartRepository: HeartRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialSleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    initialDailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    sleepRangeModeFlow: Flow<SleepRangeMode>? = null,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: SleepRepository,
        heartRepository: HeartRepository,
        preferencesRepository: PreferencesRepository,
    ) : this(
        repository = repository,
        heartRepository = heartRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.SLEEP),
        initialSleepRangeMode = preferencesRepository.sleepRangeMode,
        initialDailyGoalHours = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS),
        sleepRangeModeFlow = preferencesRepository.sleepRangeModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.SLEEP, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS, goal)
        },
    )

    private val goalKey = MetricDailyGoalKey.SLEEP_HOURS
    private val periodDriver = PeriodSelectionDriver(initialRange, onRangeSelected = onRangeSelected)
    private val _uiState = MutableStateFlow(
        SleepUiState(
            selectedRange = initialRange,
            sleepRangeMode = initialSleepRangeMode,
            dailyGoalHours = goalKey.normalize(initialDailyGoalHours),
        )
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

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
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
            )
            val windows = query.windows
            val date = query.selectedDate
            val sleepRangeMode = _uiState.value.sleepRangeMode
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val periodData = repository.loadSleepPeriod(query, sleepRangeMode)
                SleepLoadResult(
                    sessions = periodData.sessions,
                    previousSessions = periodData.previousSessions,
                    baselineSessions = periodData.baselineSessions,
                    crossDailyHrv = heartRepository?.loadDailyHRV(windows.current.start, windows.current.end).orEmpty(),
                )
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepRangeMode = sleepRangeMode,
                        sessions = result.sessions,
                        previousSessions = result.previousSessions,
                        baselineSessions = result.baselineSessions,
                        crossDailyHrv = result.crossDailyHrv,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
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
        val crossDailyHrv: List<DailyHrv>,
    )

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}
