package tech.mmarca.openvitals.features.nutrition

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val dailyGoal: Double = NutritionMetric.CALORIES_IN.dailyGoalKey.defaultValue,
    val dailyMacros: List<DailyMacros> = emptyList(),
    val previousDailyMacros: List<DailyMacros> = emptyList(),
    val baselineDailyMacros: List<DailyMacros> = emptyList(),
    val entries: List<NutritionEntry> = emptyList(),
    val display: NutritionDisplayState = NutritionDisplayState(),
    val error: ScreenError? = null,
)

@HiltViewModel
class NutritionViewModel(
    private val repository: NutritionRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    initialRange: TimeRange = TimeRange.WEEK,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val selectedMetric: NutritionMetric = NutritionMetric.CALORIES_IN,
    initialDailyGoal: Double = selectedMetric.dailyGoalKey.defaultValue,
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: NutritionRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: SavedStateHandle,
        dispatchers: DispatcherProvider,
    ) : this(
        repository = repository,
        dispatchers = dispatchers,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.NUTRITION),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        selectedMetric = nutritionMetricFromRoute(savedStateHandle[METRIC_ID_ARG]),
        initialDailyGoal = preferencesRepository.dailyGoalFor(
            nutritionMetricFromRoute(savedStateHandle[METRIC_ID_ARG]).dailyGoalKey
        ),
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.NUTRITION, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(
                nutritionMetricFromRoute(savedStateHandle[METRIC_ID_ARG]).dailyGoalKey,
                goal,
            )
        },
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
    )

    private val goalKey = selectedMetric.dailyGoalKey
    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        NutritionUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            dailyGoal = goalKey.normalize(initialDailyGoal),
        )
    )
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observeWeekPeriodMode()
        load()
    }

    private fun observeWeekPeriodMode() {
        viewModelScope.launch {
            weekPeriodModeChanges.drop(1).collect { mode ->
                periodDriver.weekPeriodMode = mode
                _uiState.value = _uiState.value.copy(weekPeriodMode = mode)
                if (_uiState.value.selectedRange == TimeRange.WEEK) {
                    load()
                }
            }
        }
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
        setDailyGoal(_uiState.value.dailyGoal + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal - goalKey.step)
    }

    fun setDailyGoal(goal: Double) {
        val normalized = goalKey.normalize(goal)
        onDailyGoalChanged(normalized)
        _uiState.value = _uiState.value.copy(dailyGoal = normalized).withDisplay()
    }

    fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                if (refreshMode == RefreshMode.NORMAL) {
                    repository.loadNutritionPeriod(query)
                } else {
                    repository.loadNutritionPeriod(query, refreshMode)
                }
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val display = withContext(dispatchers.default) {
                    NutritionPresentationMapper.build(
                        query = query,
                        metric = selectedMetric,
                        dailyGoal = _uiState.value.dailyGoal,
                        dailyMacros = result.dailyMacros,
                        previousDailyMacros = result.previousDailyMacros,
                        baselineDailyMacros = result.baselineDailyMacros,
                        entries = result.entries,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailyMacros = result.dailyMacros,
                    previousDailyMacros = result.previousDailyMacros,
                    baselineDailyMacros = result.baselineDailyMacros,
                    entries = result.entries,
                    display = display,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = error.toScreenError(),
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

    private fun NutritionUiState.withDisplay(): NutritionUiState {
        val query = PeriodLoadQuery(
            range = selectedRange,
            anchorDate = selectedDate,
            weekPeriodMode = weekPeriodMode,
        )
        return copy(
            display = NutritionPresentationMapper.build(
                query = query,
                metric = selectedMetric,
                dailyGoal = dailyGoal,
                dailyMacros = dailyMacros,
                previousDailyMacros = previousDailyMacros,
                baselineDailyMacros = baselineDailyMacros,
                entries = entries,
            ),
        )
    }
}

private fun nutritionMetricFromRoute(metricId: String?): NutritionMetric =
    runCatching { metricId?.let(NutritionMetric::valueOf) }.getOrNull() ?: NutritionMetric.CALORIES_IN
