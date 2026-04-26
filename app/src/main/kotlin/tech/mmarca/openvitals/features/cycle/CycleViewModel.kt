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
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.repository.CycleRepository
import tech.mmarca.openvitals.ui.components.periodFor
import java.time.LocalDate

data class CycleUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val data: CycleData = CycleData(),
    val missingPermissions: Set<String> = emptySet(),
    val error: String? = null,
)

class CycleViewModel(private val repository: CycleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleUiState())
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    val cyclePermissions: Set<String> get() = repository.phase4Permissions

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
        if (!periodFor(_uiState.value.selectedRange, nextDate).end.isAfter(LocalDate.now())) {
            _uiState.value = _uiState.value.copy(selectedDate = nextDate)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date.coerceAtMost(LocalDate.now()))
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
}

private data class CycleLoadResult(
    val data: CycleData,
    val missingPermissions: Set<String>,
)
