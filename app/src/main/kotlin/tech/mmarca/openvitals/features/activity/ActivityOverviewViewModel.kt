package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.insights.CardioLoadConfidence
import tech.mmarca.openvitals.core.insights.CardioLoadEstimate
import tech.mmarca.openvitals.core.insights.CardioLoadTimeWindow
import tech.mmarca.openvitals.core.insights.calculateCardioLoad
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private const val ActivityOverviewLookbackDays = 30L
private const val RecentActivityInitialCount = 3
private const val RecentActivityPageSize = 5

data class ActivityOverviewDay(
    val date: LocalDate,
    val steps: Long = 0L,
    val distanceMeters: Double = 0.0,
    val activeCaloriesKcal: Double? = null,
    val energyBurnedKcal: Double = 0.0,
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

data class ActivityOverviewUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val days: List<ActivityOverviewDay> = emptyList(),
    val recentVisibleCount: Int = RecentActivityInitialCount,
    val error: String? = null,
) {
    val today: ActivityOverviewDay
        get() = days.firstOrNull { it.date == selectedDate } ?: ActivityOverviewDay(selectedDate)

    val recentActivities: List<ActivityOverviewDay>
        get() = days.asReversed().filter { it.hasActivity }

    val visibleRecentActivities: List<ActivityOverviewDay>
        get() = recentActivities.take(recentVisibleCount)

    val canLoadMoreRecentActivities: Boolean
        get() = visibleRecentActivities.size < recentActivities.size

    val metricDays: List<ActivityOverviewDay>
        get() = days.takeLast(7)
}

@HiltViewModel
class ActivityOverviewViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val heartRepository: HeartRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityOverviewUiState())
    val uiState: StateFlow<ActivityOverviewUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
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
                    recentVisibleCount = RecentActivityInitialCount,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = today,
                    error = error.message,
                )
            }
        }
    }

    fun loadMoreRecentActivities() {
        val current = _uiState.value
        _uiState.value = current.copy(
            recentVisibleCount = (current.recentVisibleCount + RecentActivityPageSize)
                .coerceAtMost(current.recentActivities.size),
        )
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
