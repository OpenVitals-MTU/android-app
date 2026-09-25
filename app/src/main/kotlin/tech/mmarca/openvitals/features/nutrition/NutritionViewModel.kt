package tech.mmarca.openvitals.features.nutrition

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.navigation.selectedDayOrNull
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
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.contract.DailyGoalPreferences
import tech.mmarca.openvitals.data.repository.contract.NutritionDisplayPreferences
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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
class NutritionViewModel @Inject constructor(
    private val repository: NutritionRepository,
    private val periodPreferences: PeriodPreferences,
    private val dailyGoalPreferences: DailyGoalPreferences,
    private val nutritionDisplayPreferences: NutritionDisplayPreferences,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The metric the route named; calories in when it named none. */
    private val selectedMetric = nutritionMetricFromRoute(savedStateHandle[METRIC_ID_ARG])
    private val initialRange = periodPreferences.timeRangeFor(PeriodRangePreferenceKey.NUTRITION)
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode
    private val goalKey = selectedMetric.dailyGoalKey
    private var averageBasis = nutritionDisplayPreferences.nutritionAverageBasis
    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = savedStateHandle.selectedDayOrNull() ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = { range ->
            periodPreferences.setTimeRangeFor(PeriodRangePreferenceKey.NUTRITION, range)
        },
    )
    private val _uiState = MutableStateFlow(
        NutritionUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            dailyGoal = goalKey.normalize(dailyGoalPreferences.dailyGoalFor(goalKey)),
        )
    )
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observeWeekPeriodMode()
        observeAverageBasis()
        load()
    }

    /** The basis only changes the divisor, so a change re-maps without re-reading. */
    private fun observeAverageBasis() {
        viewModelScope.launch {
            nutritionDisplayPreferences.nutritionAverageBasisFlow.drop(1).collect { basis ->
                averageBasis = basis
                _uiState.value = _uiState.value.withDisplay()
            }
        }
    }

    private fun observeWeekPeriodMode() {
        viewModelScope.launch {
            periodPreferences.weekPeriodModeFlow.drop(1).collect { mode ->
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
        setDailyGoal(_uiState.value.dailyGoal + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal - goalKey.step)
    }

    fun setDailyGoal(goal: Double) {
        val normalized = goalKey.normalize(goal)
        dailyGoalPreferences.setDailyGoalFor(goalKey, normalized)
        _uiState.value = _uiState.value.copy(dailyGoal = normalized).withDisplay()
    }

    /** Removes a meal optimistically, deletes it, then force-reloads; restores on failure. */
    fun deleteNutritionEntry(entryId: String) {
        if (entryId.isBlank()) return
        val entry = _uiState.value.entries.firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return
        viewModelScope.launch {
            val previous = _uiState.value
            // Rebuilt synchronously: the swipe row needs the item gone before the next frame.
            _uiState.value = previous.withDeletedEntry(entryId)
            runCatching {
                repository.deleteNutritionEntry(entryId)
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadNutritionPeriod(query)
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
                        averageBasis = averageBasis,
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

    private fun NutritionUiState.withDeletedEntry(entryId: String): NutritionUiState =
        copy(
            entries = entries.filterNot { it.id == entryId },
            error = null,
        ).withDisplay()

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
                averageBasis = averageBasis,
            ),
        )
    }
}

/** The route argument that names [this]; the inverse of [nutritionMetricFromRoute]. */
internal fun NutritionMetric.routeId(): String = name

/** The metric a route argument names. The ids are [tech.mmarca.openvitals.domain.dashboard.DashboardWidgetId] names. */
internal fun nutritionMetricFromRoute(metricId: String?): NutritionMetric =
    runCatching { metricId?.let(NutritionMetric::valueOf) }.getOrNull() ?: NutritionMetric.CALORIES_IN
