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
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
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
import tech.mmarca.openvitals.domain.preferences.SleepWindow

@Immutable
data class DailyReadinessUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val data: DashboardData? = null,
    val insight: DailyReadinessInsight? = null,
    val goals: DailyReadinessGoalInputs = DailyReadinessGoalInputs(),
    val isLoading: Boolean = true,
    val error: ScreenError? = null,
    val sleepWindow: SleepWindow = SleepWindow.Default,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
)

@HiltViewModel
class DailyReadinessViewModel @Inject constructor(
    private val loadDashboardDayUseCase: LoadDashboardDayUseCase,
    private val prefs: PreferencesRepository,
    // Loaded beside the dashboard day: readiness needs the measured battery.
    private val bodyEnergyRepository: BodyEnergyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DailyReadinessUiState(
            goals = prefs.dailyReadinessGoals(),
            sleepWindow = prefs.sleepWindow,
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
        val sleepWindow = prefs.sleepWindow
        val activityWeekMode = prefs.activityWeekMode
        val goals = prefs.dailyReadinessGoals()
        val current = _uiState.value
        if (
            current.sleepWindow != sleepWindow ||
            current.activityWeekMode != activityWeekMode ||
            current.goals != goals
        ) {
            _uiState.value = current.copy(
                sleepWindow = sleepWindow,
                activityWeekMode = activityWeekMode,
                goals = goals,
                insight = current.data?.let { data -> calculateDailyReadiness(data, goals) },
            )
            if (
                current.sleepWindow != sleepWindow ||
                current.activityWeekMode != activityWeekMode
            ) {
                load(current.selectedDate)
            }
        }
    }

    fun resumeCurrentDay() {
        refreshPreferences()
        // Reload on return to today: ON_RESUME is the only signal data may have changed.
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
            val sleepWindow = prefs.sleepWindow
            val activityWeekMode = prefs.activityWeekMode
            val goals = prefs.dailyReadinessGoals()
            _uiState.value = _uiState.value.copy(
                selectedDate = clampedDate,
                goals = goals,
                sleepWindow = sleepWindow,
                activityWeekMode = activityWeekMode,
                isLoading = true,
                error = null,
            )
            runCatching {
                loadDashboardDayUseCase(
                    DashboardQuery(
                        date = clampedDate,
                        sleepWindow = sleepWindow,
                        activityWeekMode = activityWeekMode,
                        visibleMetrics = DailyReadinessMetrics,
                        refreshMode = refreshMode,
                    )
                )
            }
                .onSuccess { loaded ->
                    if (!isCurrent) return@load
                    val data = loaded.copy(
                        bodyEnergyTimeline = loadBodyEnergyTimeline(clampedDate, refreshMode),
                    )
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

    /** The measured battery for [date], or null. Best-effort, and skipped before setup completes. */
    private suspend fun loadBodyEnergyTimeline(
        date: LocalDate,
        refreshMode: RefreshMode,
    ): BodyEnergyTimeline? {
        if (!prefs.bodyEnergyCalibration().setupCompleted) return null
        return runCatching {
            bodyEnergyRepository.loadTimeline(
                BodyEnergyTimelineQuery(
                    period = DatePeriod(date, date),
                    range = TimeRange.DAY,
                    refreshMode = refreshMode,
                )
            ).latestDay
        }.getOrNull()
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
