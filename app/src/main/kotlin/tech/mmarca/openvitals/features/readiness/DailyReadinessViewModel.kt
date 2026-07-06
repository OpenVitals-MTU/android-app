package tech.mmarca.openvitals.features.readiness

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.domain.insights.DailyReadinessGoalInputs
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.calculateDailyReadiness
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode

@Immutable
data class DailyReadinessUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val data: DashboardData? = null,
    val insight: DailyReadinessInsight? = null,
    val goals: DailyReadinessGoalInputs = DailyReadinessGoalInputs(),
    val isLoading: Boolean = true,
    val error: ScreenError? = null,
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
)

@HiltViewModel
class DailyReadinessViewModel @Inject constructor(
    private val loadDashboardDayUseCase: LoadDashboardDayUseCase,
    private val prefs: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DailyReadinessUiState(
            goals = prefs.dailyReadinessGoals(),
            sleepRangeMode = prefs.sleepRangeMode,
            activityWeekMode = prefs.activityWeekMode,
        )
    )
    val uiState: StateFlow<DailyReadinessUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()
    private var userPinnedPastDay = false

    init {
        load(_uiState.value.selectedDate)
    }

    fun refresh() {
        load(_uiState.value.selectedDate, RefreshMode.FORCE)
    }

    fun refreshPreferences() {
        val sleepRangeMode = prefs.sleepRangeMode
        val activityWeekMode = prefs.activityWeekMode
        val goals = prefs.dailyReadinessGoals()
        val current = _uiState.value
        if (
            current.sleepRangeMode != sleepRangeMode ||
            current.activityWeekMode != activityWeekMode ||
            current.goals != goals
        ) {
            _uiState.value = current.copy(
                sleepRangeMode = sleepRangeMode,
                activityWeekMode = activityWeekMode,
                goals = goals,
                insight = current.data?.let { data -> calculateDailyReadiness(data, goals) },
            )
            if (
                current.sleepRangeMode != sleepRangeMode ||
                current.activityWeekMode != activityWeekMode
            ) {
                load(current.selectedDate)
            }
        }
    }

    fun resumeCurrentDay() {
        refreshPreferences()
        // Reload whenever the user returns to "today" (e.g. from a detail screen), not only
        // on a day rollover: this ViewModel outlives detail screens on the back stack, so
        // ON_RESUME is the only signal that Health Connect data may have changed.
        if (!userPinnedPastDay) {
            load(LocalDate.now())
        }
    }

    fun previousDay() {
        val date = _uiState.value.selectedDate.minusDays(1)
        userPinnedPastDay = date.isBefore(LocalDate.now())
        load(date)
    }

    fun nextDay() {
        val today = LocalDate.now()
        val next = _uiState.value.selectedDate.plusDays(1)
        if (!next.isAfter(today)) {
            userPinnedPastDay = next.isBefore(today)
            load(next)
        }
    }

    fun selectDate(date: LocalDate) {
        val today = LocalDate.now()
        val clampedDate = date.coerceAtMost(today)
        userPinnedPastDay = clampedDate.isBefore(today)
        load(clampedDate)
    }

    fun load(date: LocalDate, refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        loadCoordinator.launch(viewModelScope) load@{
            val sleepRangeMode = prefs.sleepRangeMode
            val activityWeekMode = prefs.activityWeekMode
            val goals = prefs.dailyReadinessGoals()
            _uiState.value = _uiState.value.copy(
                selectedDate = clampedDate,
                goals = goals,
                sleepRangeMode = sleepRangeMode,
                activityWeekMode = activityWeekMode,
                isLoading = true,
                error = null,
            )
            runCatching {
                loadDashboardDayUseCase(
                    DashboardQuery(
                        date = clampedDate,
                        sleepRangeMode = sleepRangeMode,
                        activityWeekMode = activityWeekMode,
                        visibleMetrics = DailyReadinessMetrics,
                        refreshMode = refreshMode,
                    )
                )
            }
                .onSuccess { data ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        data = data,
                        insight = calculateDailyReadiness(data, goals),
                        isLoading = false,
                    )
                }
                .onFailure { error ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.toScreenError("Unknown error"),
                    )
                }
        }
    }
}

private val DailyReadinessMetrics = setOf(
    DashboardMetric.SLEEP,
    DashboardMetric.WORKOUT,
    DashboardMetric.AVG_HEART_RATE,
    DashboardMetric.RESTING_HEART_RATE,
    DashboardMetric.HRV,
    DashboardMetric.BODY_TEMPERATURE,
    DashboardMetric.SKIN_TEMPERATURE,
    DashboardMetric.WEEKLY_CARDIO_LOAD,
    DashboardMetric.INTENSITY_MINUTES,
    DashboardMetric.HYDRATION,
    DashboardMetric.CALORIES_IN,
    DashboardMetric.PROTEIN,
    DashboardMetric.CARBS,
    DashboardMetric.FAT,
    DashboardMetric.MINDFULNESS,
)

private fun PreferencesRepository.dailyReadinessGoals(): DailyReadinessGoalInputs =
    DailyReadinessGoalInputs(
        stepsGoal = dailyGoalFor(MetricDailyGoalKey.STEPS),
        hydrationLitersGoal = hydrationDailyGoalLiters,
        activeMinutesGoal = dailyGoalFor(MetricDailyGoalKey.ACTIVE_CALORIES_KCAL) / 10.0,
    )
