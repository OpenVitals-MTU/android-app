package tech.mmarca.openvitals.features.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.data.repository.ActivityRepository

private val LegacyActivityStartDate: LocalDate = LocalDate.of(2009, 1, 1)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val badges: List<AchievementProgress> = emptyList(),
    val stats: AchievementStats = AchievementStats(),
    val error: String? = null,
) {
    val unlockedCount: Int get() = badges.count { it.isUnlocked }
    val totalCount: Int get() = badges.size
    val completionRatio: Float get() = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount
    val hasActivityHistory: Boolean get() = stats.trackedDays > 0
    val hasFloorHistory: Boolean get() = stats.hasFloorData
}

data class AchievementStats(
    val startDate: LocalDate = LegacyActivityStartDate,
    val endDate: LocalDate = LocalDate.now(),
    val trackedDays: Int = 0,
    val maxDailySteps: Long = 0L,
    val totalDistanceMeters: Double = 0.0,
    val maxDailyFloors: Int = 0,
    val totalFloors: Int = 0,
    val hasFloorData: Boolean = false,
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val currentValue: Double,
    val progressRatio: Float,
    val isUnlocked: Boolean,
    val timesEarned: Int,
    val achievedOn: LocalDate? = null,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(AchievementsUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<AchievementsUiState> = _uiState

    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    fun load(today: LocalDate = LocalDate.now()) {
        val end = today
        val start = LegacyActivityStartDate
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
            )
            runCatching {
                val dailyActivity = activityRepository.loadDailySteps(start, end)
                withContext(dispatchers.default) {
                    dailyActivity.toAchievementState(start, end)
                }
            }.onSuccess { state ->
                if (!isCurrent) return@load
                _uiState.value = state
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message,
                )
            }
        }
    }
}

private fun List<DailySteps>.toAchievementState(
    start: LocalDate,
    end: LocalDate,
): AchievementsUiState {
    val sorted = sortedBy { it.date }
    val maxDailySteps = sorted.maxOfOrNull { it.steps } ?: 0L
    val totalDistanceMeters = sorted.sumOf { it.distanceMeters }
    val floorDays = sorted.filter { it.floorsClimbed != null }
    val maxDailyFloors = floorDays.maxOfOrNull { it.floorsClimbed ?: 0 } ?: 0
    val totalFloors = floorDays.sumOf { it.floorsClimbed ?: 0 }
    val stats = AchievementStats(
        startDate = start,
        endDate = end,
        trackedDays = sorted.count { it.steps > 0L || it.distanceMeters > 0.0 || it.floorsClimbed != null },
        maxDailySteps = maxDailySteps,
        totalDistanceMeters = totalDistanceMeters,
        maxDailyFloors = maxDailyFloors,
        totalFloors = totalFloors,
        hasFloorData = floorDays.isNotEmpty(),
    )
    val badges = AchievementDefinitions.map { definition ->
        definition.progressFor(sorted, stats)
    }
    return AchievementsUiState(
        isLoading = false,
        badges = badges,
        stats = stats,
    )
}

private fun AchievementDefinition.progressFor(
    days: List<DailySteps>,
    stats: AchievementStats,
): AchievementProgress {
    val currentValue = when (metric) {
        AchievementMetric.DAILY_STEPS -> stats.maxDailySteps.toDouble()
        AchievementMetric.LIFETIME_DISTANCE_METERS -> stats.totalDistanceMeters
        AchievementMetric.DAILY_FLOORS -> stats.maxDailyFloors.toDouble()
        AchievementMetric.LIFETIME_FLOORS -> stats.totalFloors.toDouble()
    }
    val timesEarned = when (metric) {
        AchievementMetric.DAILY_STEPS -> days.count { it.steps >= target.roundToLong() }
        AchievementMetric.DAILY_FLOORS -> days.count { (it.floorsClimbed ?: 0) >= target.roundToLong() }
        AchievementMetric.LIFETIME_DISTANCE_METERS,
        AchievementMetric.LIFETIME_FLOORS -> if (currentValue >= target) 1 else 0
    }
    val achievedOn = when (metric) {
        AchievementMetric.DAILY_STEPS -> days.firstOrNull { it.steps >= target.roundToLong() }?.date
        AchievementMetric.DAILY_FLOORS -> days.firstOrNull { (it.floorsClimbed ?: 0) >= target.roundToLong() }?.date
        AchievementMetric.LIFETIME_DISTANCE_METERS -> days.firstCumulativeDate(target) { it.distanceMeters }
        AchievementMetric.LIFETIME_FLOORS -> days.firstCumulativeDate(target) { (it.floorsClimbed ?: 0).toDouble() }
    }
    return AchievementProgress(
        definition = this,
        currentValue = currentValue,
        progressRatio = if (target <= 0.0) 0f else (currentValue / target).coerceIn(0.0, 1.0).toFloat(),
        isUnlocked = currentValue >= target,
        timesEarned = timesEarned,
        achievedOn = achievedOn,
    )
}

private fun List<DailySteps>.firstCumulativeDate(
    target: Double,
    value: (DailySteps) -> Double,
): LocalDate? {
    var total = 0.0
    for (day in this) {
        total += value(day)
        if (total >= target) return day.date
    }
    return null
}
