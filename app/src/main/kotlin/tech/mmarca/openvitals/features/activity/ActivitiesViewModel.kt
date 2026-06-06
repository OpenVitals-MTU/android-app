package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoalMinutes: Double = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
    val workouts: List<ExerciseData> = emptyList(),
    val previousWorkouts: List<ExerciseData> = emptyList(),
    val baselineWorkouts: List<ExerciseData> = emptyList(),
    val crossDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ActivitiesViewModel(
    private val repository: ActivityRepository,
    private val heartRepository: HeartRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialDailyGoalMinutes: Double = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        heartRepository: HeartRepository,
        preferencesRepository: PreferencesRepository,
    ) : this(
        repository = repository,
        heartRepository = heartRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.ACTIVITIES),
        initialDailyGoalMinutes = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.WORKOUT_MINUTES),
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.ACTIVITIES, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(MetricDailyGoalKey.WORKOUT_MINUTES, goal)
        },
    )

    private val goalKey = MetricDailyGoalKey.WORKOUT_MINUTES
    private val periodDriver = PeriodSelectionDriver(initialRange, onRangeSelected = onRangeSelected)
    private val _uiState = MutableStateFlow(
        ActivitiesUiState(
            selectedRange = initialRange,
            dailyGoalMinutes = goalKey.normalize(initialDailyGoalMinutes),
        )
    )
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()
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
        setDailyGoalMinutes(_uiState.value.dailyGoalMinutes + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoalMinutes(_uiState.value.dailyGoalMinutes - goalKey.step)
    }

    fun setDailyGoalMinutes(minutes: Double) {
        val goal = goalKey.normalize(minutes)
        onDailyGoalChanged(goal)
        _uiState.value = _uiState.value.copy(dailyGoalMinutes = goal)
    }

    fun deleteActivityEntry(entryId: String) {
        if (entryId.isBlank()) return
        val entry = _uiState.value.workouts.firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return
        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.copy(
                workouts = previous.workouts.filterNot { it.id == entryId },
                error = null,
            )
            runCatching {
                repository.deleteActivityEntry(entryId)
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.message)
            }
        }
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
            )
            val windows = query.windows
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val periodData = repository.loadActivitiesPeriod(query)
                ActivitiesLoadResult(
                    workouts = periodData.workouts,
                    previousWorkouts = periodData.previousWorkouts,
                    baselineWorkouts = periodData.baselineWorkouts,
                    crossDailyRestingHR = heartRepository
                        ?.loadDailyRestingHR(windows.current.start, windows.current.end)
                        .orEmpty(),
                )
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        workouts = result.workouts,
                        previousWorkouts = result.previousWorkouts,
                        baselineWorkouts = result.baselineWorkouts,
                        crossDailyRestingHR = result.crossDailyRestingHR,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = it.message,
                    )
                }
        }
    }

    private data class ActivitiesLoadResult(
        val workouts: List<ExerciseData>,
        val previousWorkouts: List<ExerciseData>,
        val baselineWorkouts: List<ExerciseData>,
        val crossDailyRestingHR: List<DailyRestingHR>,
    )

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}
