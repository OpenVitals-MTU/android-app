package tech.mmarca.openvitals.features.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

@HiltViewModel
class BrowseViewModel(
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val bodyRepository: BodyRepository,
    initialRange: TimeRange = TimeRange.MONTH,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        activityRepository: ActivityRepository,
        sleepRepository: SleepRepository,
        bodyRepository: BodyRepository,
        preferencesRepository: PreferencesRepository,
    ) : this(
        activityRepository = activityRepository,
        sleepRepository = sleepRepository,
        bodyRepository = bodyRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.BROWSE),
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.BROWSE, range)
        },
    )

    private val periodDriver = PeriodSelectionDriver(initialRange, onRangeSelected = onRangeSelected)
    private val _uiState = MutableStateFlow(BrowseUiState(selectedRange = initialRange))
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun selectCategory(category: BrowseCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
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

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val category = _uiState.value.selectedCategory
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
            )
            val date = query.selectedDate
            val period = query.windows.current
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                when (category) {
                    BrowseCategory.WORKOUTS -> {
                        val workouts = activityRepository.loadWorkouts(period.start, period.end)
                        if (!isCurrent) return@load
                        _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, workouts = workouts)
                    }
                    BrowseCategory.SLEEP -> {
                        val sessions = sleepRepository.loadSleepSessions(period.start, period.end)
                        if (!isCurrent) return@load
                        _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, sleepSessions = sessions)
                    }
                    BrowseCategory.WEIGHT -> {
                        val entries = bodyRepository.loadWeightEntries(period.start, period.end)
                        if (!isCurrent) return@load
                        _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, weightEntries = entries)
                    }
                }
            }.onFailure {
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(isLoading = false, selectedDate = date, error = it.message)
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
