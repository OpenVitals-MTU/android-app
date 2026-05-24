package tech.mmarca.openvitals.features.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HydrationUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyHydration: List<DailyHydration> = emptyList(),
    val error: String? = null,
) {
    val totalLiters: Double get() = dailyHydration.sumOf { it.liters }
    val trackedDays: Int get() = dailyHydration.count { it.liters > 0.0 }
    val averageLiters: Double get() = trackedDays.takeIf { it > 0 }?.let { totalLiters / it } ?: 0.0
    val bestDayLiters: Double get() = dailyHydration.maxOfOrNull { it.liters } ?: 0.0
    val currentTrackedStreakDays: Int
        get() = dailyHydration
            .sortedBy { it.date }
            .asReversed()
            .takeWhile { it.liters > 0.0 }
            .count()
}

class HydrationViewModel(
    private val repository: HydrationRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(HydrationUiState(selectedRange = initialRange))
    val uiState: StateFlow<HydrationUiState> = _uiState.asStateFlow()

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
                repository.loadDailyHydration(period.start, period.end)
            }.onSuccess { dailyHydration ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailyHydration = dailyHydration,
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

    private val periodSelection: PeriodSelection
        get() = PeriodSelection(_uiState.value.selectedRange, _uiState.value.selectedDate)

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}
