package tech.mmarca.openvitals.features.sleep

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.navigation.selectedDayOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
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
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.DailyGoalPreferences
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.data.repository.contract.SleepWindowPreferences
import tech.mmarca.openvitals.domain.usecase.LoadSleepPeriodUseCase
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

@Immutable
data class SleepUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val sleepWindow: SleepWindow = SleepWindow.Default,
    val dailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    val sessions: List<SleepData> = emptyList(),
    val previousSessions: List<SleepData> = emptyList(),
    val baselineSessions: List<SleepData> = emptyList(),
    val crossDailyHrv: List<DailyHrv> = emptyList(),
    val display: SleepDisplayState = SleepDisplayState(),
    val error: ScreenError? = null,
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val loadSleepPeriodUseCase: LoadSleepPeriodUseCase,
    private val periodPreferences: PeriodPreferences,
    private val dailyGoalPreferences: DailyGoalPreferences,
    private val sleepWindowPreferences: SleepWindowPreferences,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {

    private val goalKey = MetricDailyGoalKey.SLEEP_HOURS
    private val initialRange = periodPreferences.timeRangeFor(PeriodRangePreferenceKey.SLEEP)
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode
    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = savedStateHandle.selectedDayOrNull() ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = { range ->
            periodPreferences.setTimeRangeFor(PeriodRangePreferenceKey.SLEEP, range)
        },
    )
    private val _uiState = MutableStateFlow(
        SleepUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            sleepWindow = sleepWindowPreferences.sleepWindow,
            dailyGoalHours = goalKey.normalize(dailyGoalPreferences.dailyGoalFor(goalKey)),
        )
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        periodPreferences.weekPeriodModeFlow
            .distinctUntilChanged()
            .onEach { mode ->
                if (_uiState.value.weekPeriodMode != mode) {
                    periodDriver.weekPeriodMode = mode
                    _uiState.value = _uiState.value.copy(weekPeriodMode = mode)
                    if (_uiState.value.selectedRange == TimeRange.WEEK) {
                        load()
                    }
                }
            }
            .launchIn(viewModelScope)
        sleepWindowPreferences.sleepWindowFlow
            .distinctUntilChanged()
            .onEach { window ->
                if (_uiState.value.sleepWindow != window) {
                    _uiState.value = _uiState.value.copy(sleepWindow = window)
                    load()
                }
            }
            .launchIn(viewModelScope)
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
        setDailyGoalHours(_uiState.value.dailyGoalHours + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoalHours(_uiState.value.dailyGoalHours - goalKey.step)
    }

    fun setDailyGoalHours(hours: Double) {
        val goal = goalKey.normalize(hours)
        dailyGoalPreferences.setDailyGoalFor(goalKey, goal)
        _uiState.value = _uiState.value.copy(dailyGoalHours = goal)
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            val sleepWindow = _uiState.value.sleepWindow
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                loadSleepPeriodUseCase(query, sleepWindow)
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    val display = withContext(dispatchers.default) {
                        SleepPresentationMapper.build(
                            query = query,
                            sleepWindow = sleepWindow,
                            sessions = result.sessions,
                            previousSessions = result.previousSessions,
                            baselineSessions = result.baselineSessions,
                            dailyDurations = result.dailyDurations,
                            previousDailyDurations = result.previousDailyDurations,
                            baselineDailyDurations = result.baselineDailyDurations,
                            crossDailyHrv = result.crossDailyHrv,
                            ageYears = bodyProfilePreferences.bodyProfile().ageYears(date),
                        )
                    }
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepWindow = sleepWindow,
                        sessions = result.sessions,
                        previousSessions = result.previousSessions,
                        baselineSessions = result.baselineSessions,
                        crossDailyHrv = result.crossDailyHrv,
                        display = display,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepWindow = sleepWindow,
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
}
