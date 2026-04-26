package tech.mmarca.openvitals.features.mindfulness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.ui.components.periodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MindfulnessUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val sessions: List<MindfulnessSession> = emptyList(),
    val error: String? = null,
) {
    val totalMinutes: Long get() = sessions.sumOf { it.durationMinutes }
}

class MindfulnessViewModel(private val repository: MindfulnessRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MindfulnessUiState())
    val uiState: StateFlow<MindfulnessUiState> = _uiState.asStateFlow()

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
            runCatching { repository.loadMindfulnessSessions(period.start, period.end) }
                .onSuccess { sessions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sessions = sessions,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = error.message,
                    )
                }
        }
    }
}
