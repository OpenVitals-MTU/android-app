package tech.mmarca.openvitals.features.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BrowseCategory(val label: String) {
    WORKOUTS("Workouts"),
    SLEEP("Sleep"),
    WEIGHT("Weight"),
}

data class BrowseUiState(
    val isLoading: Boolean = false,
    val selectedCategory: BrowseCategory = BrowseCategory.WORKOUTS,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val workouts: List<ExerciseData> = emptyList(),
    val sleepSessions: List<SleepData> = emptyList(),
    val weightEntries: List<WeightEntry> = emptyList(),
    val error: String? = null,
)

class BrowseViewModel(
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val bodyRepository: BodyRepository,
    initialRange: TimeRange = TimeRange.MONTH,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState(selectedRange = initialRange))
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectCategory(category: BrowseCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
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
            val category = _uiState.value.selectedCategory
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                when (category) {
                    BrowseCategory.WORKOUTS -> {
                        val workouts = activityRepository.loadWorkouts(period.start, period.end)
                        _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, workouts = workouts)
                    }
                    BrowseCategory.SLEEP -> {
                        val sessions = sleepRepository.loadSleepSessions(period.start, period.end)
                        _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, sleepSessions = sessions)
                    }
                    BrowseCategory.WEIGHT -> {
                        val entries = bodyRepository.loadWeightEntries(period.start, period.end)
                        _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, weightEntries = entries)
                    }
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, error = it.message)
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
