package tech.mmarca.openvitals.features.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.model.CycleData
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.CycleRepository
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate

data class CycleUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val data: CycleData = CycleData(),
    val missingPermissions: Set<String> = emptySet(),
    val error: String? = null,
)

class CycleViewModel(
    private val repository: CycleRepository,
    initialRange: TimeRange = TimeRange.MONTH,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleUiState(selectedRange = initialRange))
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    val cyclePermissions: Set<String> get() = repository.phase4Permissions

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

    fun onCyclePermissionsResult(granted: Set<String>) {
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
                    val missingPermissions = async { repository.missingPermissions() }
                    val data = async { repository.loadCycleData(period.start, period.end) }
                    CycleLoadResult(
                        data = data.await(),
                        missingPermissions = missingPermissions.await(),
                    )
                }
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    data = result.data,
                    missingPermissions = result.missingPermissions,
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

private data class CycleLoadResult(
    val data: CycleData,
    val missingPermissions: Set<String>,
)
