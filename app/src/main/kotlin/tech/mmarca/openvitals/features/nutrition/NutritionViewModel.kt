package tech.mmarca.openvitals.features.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.NutritionRepository
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyMacros: List<DailyMacros> = emptyList(),
    val entries: List<NutritionEntry> = emptyList(),
    val error: String? = null,
) {
    val totalEnergyKcal: Double get() = dailyMacros.sumOf { it.energyKcal }
    val totalProteinGrams: Double get() = dailyMacros.sumOf { it.proteinGrams }
    val totalCarbsGrams: Double get() = dailyMacros.sumOf { it.carbsGrams }
    val totalFatGrams: Double get() = dailyMacros.sumOf { it.fatGrams }
}

class NutritionViewModel(
    private val repository: NutritionRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    private val selectedMetric: NutritionMetric = NutritionMetric.CALORIES_IN,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState(selectedRange = initialRange))
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

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

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                NutritionLoadResult(
                    dailyMacros = repository.loadDailyMacros(period.start, period.end),
                    entries = if (selectedMetric.loadsMealEntries(range)) {
                        repository.loadNutritionEntries(period.start, period.end)
                    } else {
                        emptyList()
                    },
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailyMacros = result.dailyMacros,
                    entries = result.entries,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = error.message,
                )
            }
        }
    }

    private data class NutritionLoadResult(
        val dailyMacros: List<DailyMacros>,
        val entries: List<NutritionEntry>,
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

private fun NutritionMetric.loadsMealEntries(range: TimeRange): Boolean =
    this == NutritionMetric.CALORIES_IN && range != TimeRange.YEAR
