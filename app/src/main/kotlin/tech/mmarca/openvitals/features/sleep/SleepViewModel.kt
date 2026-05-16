package tech.mmarca.openvitals.features.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.repository.SleepRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SleepUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val sessions: List<SleepData> = emptyList(),
    val error: String? = null,
)

class SleepViewModel(
    private val repository: SleepRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    initialSleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    sleepRangeModeFlow: Flow<SleepRangeMode>? = null,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SleepUiState(
            selectedRange = initialRange,
            sleepRangeMode = initialSleepRangeMode,
        )
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        sleepRangeModeFlow
            ?.distinctUntilChanged()
            ?.onEach { mode ->
                if (_uiState.value.sleepRangeMode != mode) {
                    _uiState.value = _uiState.value.copy(sleepRangeMode = mode)
                    load()
                }
            }
            ?.launchIn(viewModelScope)
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
            val sleepRangeMode = _uiState.value.sleepRangeMode
            val period = periodFor(range, date)
            val queryStart = when (sleepRangeMode) {
                SleepRangeMode.ROLLING_24H -> period.start
                SleepRangeMode.NOON,
                SleepRangeMode.EVENING_18H -> period.start.minusDays(1)
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadSleepSessions(queryStart, period.end) }
                .onSuccess { sessions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepRangeMode = sleepRangeMode,
                        sessions = sessions,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sleepRangeMode = sleepRangeMode,
                        error = it.message,
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
