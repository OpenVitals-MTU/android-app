package tech.mmarca.openvitals.features.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.repository.NutritionRepository
import tech.mmarca.openvitals.ui.components.periodFor
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

class NutritionViewModel(private val repository: NutritionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(
            selectedRange = range,
            selectedDate = _uiState.value.selectedDate.coerceAtMost(LocalDate.now()),
        )
        load()
    }

    fun previousPeriod() {
        _uiState.value = _uiState.value.copy(
            selectedDate = when (_uiState.value.selectedRange) {
                TimeRange.DAY -> _uiState.value.selectedDate.minusDays(1)
                TimeRange.WEEK -> _uiState.value.selectedDate.minusWeeks(1)
                TimeRange.MONTH -> _uiState.value.selectedDate.minusMonths(1)
                TimeRange.YEAR -> _uiState.value.selectedDate.minusYears(1)
            },
        )
        load()
    }

    fun nextPeriod() {
        val nextDate = when (_uiState.value.selectedRange) {
            TimeRange.DAY -> _uiState.value.selectedDate.plusDays(1)
            TimeRange.WEEK -> _uiState.value.selectedDate.plusWeeks(1)
            TimeRange.MONTH -> _uiState.value.selectedDate.plusMonths(1)
            TimeRange.YEAR -> _uiState.value.selectedDate.plusYears(1)
        }
        val today = LocalDate.now()
        if (!periodFor(_uiState.value.selectedRange, nextDate).end.isAfter(today)) {
            _uiState.value = _uiState.value.copy(selectedDate = nextDate)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date.coerceAtMost(LocalDate.now()))
        load()
    }

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                coroutineScope {
                    val macrosDeferred = async { repository.loadDailyMacros(period.start, period.end) }
                    val entriesDeferred = async { repository.loadNutritionEntries(period.start, period.end) }
                    NutritionLoadResult(
                        dailyMacros = macrosDeferred.await(),
                        entries = entriesDeferred.await(),
                    )
                }
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
}
