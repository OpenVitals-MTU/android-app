package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadEstimate
import tech.mmarca.openvitals.domain.insights.CardioLoadTimeWindow
import tech.mmarca.openvitals.domain.insights.calculateCardioLoad
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ActivityOverviewLookbackDays = 30L

@Immutable
data class ActivityOverviewDay(
    val date: LocalDate,
    val steps: Long = 0L,
    val distanceMeters: Double = 0.0,
    val activeCaloriesKcal: Double? = null,
    val energyBurnedKcal: Double = 0.0,
    val energyBurnedSource: CaloriesBurnedSource = CaloriesBurnedSource.NO_DATA,
    val workouts: List<ExerciseData> = emptyList(),
    val hrvRmssdMs: Double? = null,
    val cardioLoadScore: CardioLoadEstimate = CardioLoadEstimate.NoData,
) {
    val hasActivity: Boolean
        get() = steps > 0L ||
            distanceMeters > 0.0 ||
            activeCaloriesKcal.orZero() > 0.0 ||
            energyBurnedKcal > 0.0 ||
            workouts.isNotEmpty() ||
            cardioLoadConfidence != CardioLoadConfidence.NO_DATA

    val cardioLoad: Int
        get() = cardioLoadScore.score

    val cardioLoadConfidence: CardioLoadConfidence
        get() = cardioLoadScore.confidence
}

@Immutable
data class ActivityOverviewUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val days: List<ActivityOverviewDay> = emptyList(),
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val error: ScreenError? = null,
) {
    val today: ActivityOverviewDay
        get() = days.firstOrNull { it.date == selectedDate } ?: ActivityOverviewDay(selectedDate)

    val metricDays: List<ActivityOverviewDay>
        get() = days.daysIn(
            displayPeriodFor(
                range = TimeRange.WEEK,
                anchorDate = selectedDate,
                weekPeriodMode = activityWeekMode.toWeekPeriodMode(),
            )
        )
}

@HiltViewModel
class ActivityOverviewViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val heartRepository: HeartRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val preferencesRepository: PreferencesRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ActivityOverviewUiState(
            activityWeekMode = preferencesRepository?.activityWeekMode ?: ActivityWeekMode.MONDAY_TO_SUNDAY,
        )
    )
    val uiState: StateFlow<ActivityOverviewUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observePreferences()
        load()
    }

    private fun observePreferences() {
        val preferences = preferencesRepository ?: return
        viewModelScope.launch {
            preferences.activityWeekModeFlow.collect { mode ->
                _uiState.value = _uiState.value.copy(activityWeekMode = mode)
            }
        }
        viewModelScope.launch {
            var skipInitial = true
            preferences.showOpenVitalsCalculatedCaloriesFlow.collect {
                if (skipInitial) {
                    skipInitial = false
                } else {
                    load(_uiState.value.selectedDate)
                }
            }
        }
    }

    fun load(today: LocalDate = LocalDate.now()) {
        loadCoordinator.launch(viewModelScope) load@{
            val end = today
            val start = end.minusDays(ActivityOverviewLookbackDays - 1)
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedDate = today,
                error = null,
            )
            runCatching {
                loadActivityOverview(start, end)
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val days = withContext(dispatchers.default) {
                    result.toDays(start, end)
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = today,
                    days = days,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = today,
                    error = error.toScreenError(),
                )
            }
        }
    }

    private suspend fun loadActivityOverview(
        start: LocalDate,
        end: LocalDate,
    ): ActivityOverviewLoadResult = coroutineScope {
        val steps = async { activityRepository.loadDailySteps(start, end) }
        val nutrition = async { activityRepository.loadDailyNutrition(start, end) }
        val workouts = async { activityRepository.loadWorkouts(start, end) }
        val heartRateSamples = async { heartRepository.loadHeartRateSamples(start, end) }
        val restingHeartRate = async { heartRepository.loadDailyRestingHR(start, end) }
        val hrv = async { heartRepository.loadDailyHRV(start, end) }
        ActivityOverviewLoadResult(
            steps = steps.await(),
            nutrition = nutrition.await(),
            workouts = workouts.await(),
            heartRateSamples = heartRateSamples.await(),
            restingHeartRate = restingHeartRate.await(),
            hrv = hrv.await(),
        )
    }

    private data class ActivityOverviewLoadResult(
        val steps: List<DailySteps>,
        val nutrition: List<DailyNutrition>,
        val workouts: List<ExerciseData>,
        val heartRateSamples: List<HeartRateSample>,
        val restingHeartRate: List<DailyRestingHR>,
        val hrv: List<DailyHrv>,
    )

    private fun ActivityOverviewLoadResult.toDays(
        start: LocalDate,
        end: LocalDate,
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
            val dayHrv = hrvByDate[date]
            val dayWorkouts = workouts.overlapping(date, zone)
            val daySamples = heartRateSamplesByDate[date].orEmpty()
            ActivityOverviewDay(
                date = date,
                steps = daySteps?.steps ?: 0L,
                distanceMeters = daySteps?.distanceMeters ?: 0.0,
                activeCaloriesKcal = daySteps?.activeCaloriesKcal,
                energyBurnedKcal = dayNutrition?.caloriesBurnedKcal ?: 0.0,
                energyBurnedSource = dayNutrition?.caloriesBurnedSource ?: CaloriesBurnedSource.NO_DATA,
                workouts = dayWorkouts,
                hrvRmssdMs = dayHrv?.rmssdMs,
                cardioLoadScore = calculateCardioLoad(
                    steps = daySteps,
                    samples = daySamples,
                    restingHeartRate = restingHeartRateByDate[date]?.bpm,
                    baselineRestingHeartRate = baselineRestingHeartRate,
                    observedMaxHeartRate = observedMaxHeartRate,
                    activityWindows = dayWorkouts.toCardioLoadTimeWindows(date, zone),
                ),
            )
        }.toList()
    }
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

private fun Double?.orZero(): Double = this ?: 0.0

private fun List<ActivityOverviewDay>.daysIn(period: tech.mmarca.openvitals.core.period.DatePeriod): List<ActivityOverviewDay> {
    val daysByDate = associateBy { it.date }
    return generateSequence(period.start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { day ->
        daysByDate[day] ?: ActivityOverviewDay(date = day)
    }.toList()
}
