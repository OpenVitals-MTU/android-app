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
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.usecase.LoadSleepPeriodUseCase
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
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
class SleepViewModel(
    private val loadSleepPeriodUseCase: LoadSleepPeriodUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    initialRange: TimeRange = TimeRange.WEEK,
    initialDate: java.time.LocalDate? = null,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    initialSleepWindow: SleepWindow = SleepWindow.Default,
    initialDailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    weekPeriodModeFlow: Flow<WeekPeriodMode>? = null,
    sleepWindowFlow: Flow<SleepWindow>? = null,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
    private val ageYearsForDate: (LocalDate) -> Int? = { null },
) : ViewModel() {

    @Inject
    constructor(
        repository: SleepRepository,
        heartRepository: HeartRepository,
        loadSleepPeriodUseCase: LoadSleepPeriodUseCase,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: androidx.lifecycle.SavedStateHandle,
    ) : this(
        loadSleepPeriodUseCase = loadSleepPeriodUseCase,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.SLEEP),
        initialDate = savedStateHandle.selectedDayOrNull(),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        initialSleepWindow = preferencesRepository.sleepWindow,
        initialDailyGoalHours = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS),
        weekPeriodModeFlow = preferencesRepository.weekPeriodModeFlow,
        sleepWindowFlow = preferencesRepository.sleepWindowFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.SLEEP, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS, goal)
        },
        ageYearsForDate = { date -> preferencesRepository.bodyProfile().ageYears(date) },
    )

    constructor(
        repository: SleepRepository,
        heartRepository: HeartRepository? = null,
        dispatchers: DispatcherProvider = DefaultDispatcherProvider,
        initialRange: TimeRange = TimeRange.WEEK,
        initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        initialSleepWindow: SleepWindow = SleepWindow.Default,
        initialDailyGoalHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
        weekPeriodModeFlow: Flow<WeekPeriodMode>? = null,
        sleepWindowFlow: Flow<SleepWindow>? = null,
        onRangeSelected: (TimeRange) -> Unit = {},
        onDailyGoalChanged: (Double) -> Unit = {},
        ageYearsForDate: (LocalDate) -> Int? = { null },
    ) : this(
        loadSleepPeriodUseCase = LoadSleepPeriodUseCase(repository, heartRepository),
        dispatchers = dispatchers,
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        initialSleepWindow = initialSleepWindow,
        initialDailyGoalHours = initialDailyGoalHours,
        weekPeriodModeFlow = weekPeriodModeFlow,
        sleepWindowFlow = sleepWindowFlow,
        onRangeSelected = onRangeSelected,
        onDailyGoalChanged = onDailyGoalChanged,
        ageYearsForDate = ageYearsForDate,
    )

    private val goalKey = MetricDailyGoalKey.SLEEP_HOURS
    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = initialDate ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        SleepUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            sleepWindow = initialSleepWindow,
            dailyGoalHours = goalKey.normalize(initialDailyGoalHours),
        )
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        weekPeriodModeFlow
            ?.distinctUntilChanged()
            ?.onEach { mode ->
                if (_uiState.value.weekPeriodMode != mode) {
                    periodDriver.weekPeriodMode = mode
                    _uiState.value = _uiState.value.copy(weekPeriodMode = mode)
                    if (_uiState.value.selectedRange == TimeRange.WEEK) {
                        load()
                    }
                }
            }
            ?.launchIn(viewModelScope)
        sleepWindowFlow
            ?.distinctUntilChanged()
            ?.onEach { mode ->
                if (_uiState.value.sleepWindow != mode) {
                    _uiState.value = _uiState.value.copy(sleepWindow = mode)
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

    fun selectDay(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDay(date))
        load()
    }

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load(RefreshMode.FORCE)
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
        onDailyGoalChanged(goal)
        _uiState.value = _uiState.value.copy(dailyGoalHours = goal)
    }

    fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
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
                loadSleepPeriodUseCase(query, sleepWindow, refreshMode)
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
                            ageYears = ageYearsForDate(date),
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
