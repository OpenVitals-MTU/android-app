package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.insights.CardioLoadTimeWindow
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.insights.calculateCardioLoad
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.core.preferences.ActivityWeekMode
import tech.mmarca.openvitals.core.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.data.model.CaloriesBurnedSource
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val dailyGoalMinutes: Double = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
    val workouts: List<ExerciseData> = emptyList(),
    val previousWorkouts: List<ExerciseData> = emptyList(),
    val baselineWorkouts: List<ExerciseData> = emptyList(),
    val overviewDays: List<ActivityOverviewDay> = emptyList(),
    val crossDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ActivitiesViewModel(
    private val repository: ActivityRepository,
    private val heartRepository: HeartRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialActivityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    initialDailyGoalMinutes: Double = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
    private val activityWeekModeChanges: Flow<ActivityWeekMode> = emptyFlow(),
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
        initialActivityWeekMode = preferencesRepository.activityWeekMode,
        initialDailyGoalMinutes = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.WORKOUT_MINUTES),
        activityWeekModeChanges = preferencesRepository.activityWeekModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.ACTIVITIES, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(MetricDailyGoalKey.WORKOUT_MINUTES, goal)
        },
    )

    private val goalKey = MetricDailyGoalKey.WORKOUT_MINUTES
    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialActivityWeekMode.toWeekPeriodMode(),
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        ActivitiesUiState(
            selectedRange = initialRange,
            activityWeekMode = initialActivityWeekMode,
            dailyGoalMinutes = goalKey.normalize(initialDailyGoalMinutes),
        )
    )
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observeActivityWeekMode()
        load()
    }

    private fun observeActivityWeekMode() {
        viewModelScope.launch {
            activityWeekModeChanges.drop(1).collect { mode ->
                periodDriver.weekPeriodMode = mode.toWeekPeriodMode()
                _uiState.value = _uiState.value.copy(activityWeekMode = mode)
                if (_uiState.value.selectedRange == TimeRange.WEEK) {
                    load()
                }
            }
        }
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
                weekPeriodMode = _uiState.value.activityWeekMode.toWeekPeriodMode(),
            )
            val windows = activityLoadWindows(
                query = query,
                activityWeekMode = _uiState.value.activityWeekMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                coroutineScope {
                    val currentDataEnd = windows.currentDataEnd
                    val workouts = async { repository.loadWorkouts(windows.current.start, currentDataEnd) }
                    val previousWorkouts = async { repository.loadWorkouts(windows.previous.start, windows.previous.end) }
                    val baselineWorkouts = async { repository.loadWorkouts(windows.baseline.start, windows.baseline.end) }
                    val dailySteps = async { repository.loadDailySteps(windows.current.start, currentDataEnd) }
                    val nutrition = async { repository.loadDailyNutrition(windows.current.start, currentDataEnd) }
                    val restingHeartRate = async {
                        heartRepository?.loadDailyRestingHR(windows.current.start, currentDataEnd).orEmpty()
                    }
                    val hrv = async {
                        heartRepository?.loadDailyHRV(windows.current.start, currentDataEnd).orEmpty()
                    }
                    val heartRateSamples = async {
                        if (query.range == TimeRange.YEAR) {
                            emptyList()
                        } else {
                            heartRepository?.loadHeartRateSamples(windows.current.start, currentDataEnd).orEmpty()
                        }
                    }
                    val loadedRestingHeartRate = restingHeartRate.await()
                    val loadedWorkouts = workouts.await()
                    ActivitiesLoadResult(
                        workouts = loadedWorkouts,
                        previousWorkouts = previousWorkouts.await(),
                        baselineWorkouts = baselineWorkouts.await(),
                        overviewDays = activityOverviewDays(
                            start = windows.current.start,
                            end = windows.current.end,
                            steps = dailySteps.await(),
                            nutrition = nutrition.await(),
                            workouts = loadedWorkouts,
                            heartRateSamples = heartRateSamples.await(),
                            restingHeartRate = loadedRestingHeartRate,
                            hrv = hrv.await(),
                        ),
                        crossDailyRestingHR = loadedRestingHeartRate,
                    )
                }
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        workouts = result.workouts,
                        previousWorkouts = result.previousWorkouts,
                        baselineWorkouts = result.baselineWorkouts,
                        overviewDays = result.overviewDays,
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
        val overviewDays: List<ActivityOverviewDay>,
        val crossDailyRestingHR: List<DailyRestingHR>,
    )

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}

private fun activityOverviewDays(
    start: LocalDate,
    end: LocalDate,
    steps: List<DailySteps>,
    nutrition: List<DailyNutrition>,
    workouts: List<ExerciseData>,
    heartRateSamples: List<HeartRateSample>,
    restingHeartRate: List<DailyRestingHR>,
    hrv: List<DailyHrv>,
): List<ActivityOverviewDay> {
    val stepsByDate = steps.associateBy { it.date }
    val nutritionByDate = nutrition.associateBy { it.date }
    val hrvByDate = hrv.associateBy { it.date }
    val zone = ZoneId.systemDefault()
    val heartRateSamplesByDate = heartRateSamples
        .sortedBy { it.time }
        .groupBy { it.time.atZone(zone).toLocalDate() }
    val restingHeartRateByDate = restingHeartRate.associateBy { it.date }
    val baselineRestingHeartRate = restingHeartRate
        .map { it.bpm }
        .medianOrNull()
    val observedMaxHeartRate = heartRateSamples.maxOfOrNull { it.beatsPerMinute }

    return generateSequence(start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(end) }
    }.map { date ->
        val daySteps = stepsByDate[date]
        val dayNutrition = nutritionByDate[date]
        val dayWorkouts = workouts.overlapping(date, zone)
        ActivityOverviewDay(
            date = date,
            steps = daySteps?.steps ?: 0L,
            distanceMeters = daySteps?.distanceMeters ?: 0.0,
            activeCaloriesKcal = daySteps?.activeCaloriesKcal,
            energyBurnedKcal = dayNutrition?.caloriesBurnedKcal ?: 0.0,
            energyBurnedSource = dayNutrition?.caloriesBurnedSource ?: CaloriesBurnedSource.NO_DATA,
            workouts = dayWorkouts,
            hrvRmssdMs = hrvByDate[date]?.rmssdMs,
            cardioLoadScore = calculateCardioLoad(
                steps = daySteps,
                samples = heartRateSamplesByDate[date].orEmpty(),
                restingHeartRate = restingHeartRateByDate[date]?.bpm,
                baselineRestingHeartRate = baselineRestingHeartRate,
                observedMaxHeartRate = observedMaxHeartRate,
                activityWindows = dayWorkouts.toCardioLoadTimeWindows(date, zone),
            ),
        )
    }.toList()
}

private data class ActivityLoadWindows(
    val current: DatePeriod,
    val currentDataEnd: LocalDate,
    val previous: DatePeriod,
    val baseline: DatePeriod,
)

private fun activityLoadWindows(
    query: PeriodLoadQuery,
    activityWeekMode: ActivityWeekMode,
): ActivityLoadWindows {
    val current = activityDisplayPeriod(
        selectedRange = query.range,
        selectedDate = query.selectedDate,
        activityWeekMode = activityWeekMode,
        today = query.today,
    )
    if (query.range != TimeRange.WEEK) {
        return ActivityLoadWindows(
            current = current,
            currentDataEnd = current.end,
            previous = query.windows.previous,
            baseline = query.windows.baseline,
        )
    }

    return ActivityLoadWindows(
        current = current,
        currentDataEnd = current.end.coerceAtMost(query.today),
        previous = query.windows.previous,
        baseline = query.windows.baseline,
    )
}

internal fun activityDisplayPeriod(
    selectedRange: TimeRange,
    selectedDate: LocalDate,
    activityWeekMode: ActivityWeekMode,
    today: LocalDate = LocalDate.now(),
): DatePeriod {
    val date = selectedDate.coerceAtMost(today)
    return displayPeriodFor(
        range = selectedRange,
        anchorDate = date,
        today = today,
        weekPeriodMode = activityWeekMode.toWeekPeriodMode(),
    )
}

private fun List<Long>.medianOrNull(): Long? {
    if (isEmpty()) return null
    val sorted = sorted()
    return sorted[sorted.lastIndex / 2]
}

private fun List<ExerciseData>.overlapping(date: LocalDate, zone: ZoneId): List<ExerciseData> {
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    return filter { workout ->
        workout.endTime.isAfter(dayStart) && workout.startTime.isBefore(dayEnd)
    }.sortedByDescending { it.startTime }
}

private fun List<ExerciseData>.toCardioLoadTimeWindows(date: LocalDate, zone: ZoneId): List<CardioLoadTimeWindow> {
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    return mapNotNull { workout ->
        CardioLoadTimeWindow(
            start = maxOf(workout.startTime, dayStart),
            end = minOf(workout.endTime, dayEnd),
        ).takeIf { it.durationMinutes > 0.0 }
    }
}
