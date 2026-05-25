package tech.mmarca.openvitals.features.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.data.repository.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoal: Double = NutritionMetric.CALORIES_IN.dailyGoalKey.defaultValue,
    val dailyMacros: List<DailyMacros> = emptyList(),
    val previousDailyMacros: List<DailyMacros> = emptyList(),
    val baselineDailyMacros: List<DailyMacros> = emptyList(),
    val entries: List<NutritionEntry> = emptyList(),
    val totalEnergyKcal: Double = 0.0,
    val totalProteinGrams: Double = 0.0,
    val totalCarbsGrams: Double = 0.0,
    val totalFatGrams: Double = 0.0,
    val error: String? = null,
)

@HiltViewModel
class NutritionViewModel(
    private val repository: NutritionRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    private val selectedMetric: NutritionMetric = NutritionMetric.CALORIES_IN,
    initialDailyGoal: Double = selectedMetric.dailyGoalKey.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: NutritionRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.NUTRITION),
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
    )

    private val goalKey = selectedMetric.dailyGoalKey
    private val periodDriver = PeriodSelectionDriver(initialRange, onRangeSelected = onRangeSelected)
    private val _uiState = MutableStateFlow(
        NutritionUiState(
            selectedRange = initialRange,
            dailyGoal = goalKey.normalize(initialDailyGoal),
        )
    )
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
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
        setDailyGoal(_uiState.value.dailyGoal + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoal(_uiState.value.dailyGoal - goalKey.step)
    }

    fun setDailyGoal(goal: Double) {
        val normalized = goalKey.normalize(goal)
        onDailyGoalChanged(normalized)
        _uiState.value = _uiState.value.copy(dailyGoal = normalized)
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadNutritionPeriod(query)
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val totals = result.dailyMacros.totals()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailyMacros = result.dailyMacros,
                    previousDailyMacros = result.previousDailyMacros,
                    baselineDailyMacros = result.baselineDailyMacros,
                    entries = result.entries,
                    totalEnergyKcal = totals.energyKcal,
                    totalProteinGrams = totals.proteinGrams,
                    totalCarbsGrams = totals.carbsGrams,
                    totalFatGrams = totals.fatGrams,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = error.message,
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

private fun nutritionMetricFromRoute(metricId: String?): NutritionMetric =
    runCatching { metricId?.let(NutritionMetric::valueOf) }.getOrNull() ?: NutritionMetric.CALORIES_IN

private data class NutritionTotals(
    val energyKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)

private fun List<DailyMacros>.totals(): NutritionTotals =
    NutritionTotals(
        energyKcal = sumOf { it.energyKcal },
        proteinGrams = sumOf { it.proteinGrams },
        carbsGrams = sumOf { it.carbsGrams },
        fatGrams = sumOf { it.fatGrams },
    )
