package tech.mmarca.openvitals.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.mergeLoaded
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val data: DashboardData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val unacknowledgedWidgetPermissions: Set<String> = emptySet(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val dashboardWidgets: List<DashboardWidgetId> = DefaultDashboardWidgetIds,
    val dailyGoals: DashboardDailyGoals = DashboardDailyGoals(),
    val isEditingDashboard: Boolean = false,
    val pendingWidgets: Set<DashboardWidgetId> = emptySet(),
    val healthConnectSyncEnabled: Boolean = true,
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val minimumPermissionsGranted: Boolean = true,
)

data class DashboardDailyGoals(
    val steps: Double = MetricDailyGoalKey.STEPS.defaultValue,
    val distanceMeters: Double = MetricDailyGoalKey.DISTANCE_METERS.defaultValue,
    val caloriesOutKcal: Double = MetricDailyGoalKey.CALORIES_OUT_KCAL.defaultValue,
    val activeCaloriesKcal: Double = MetricDailyGoalKey.ACTIVE_CALORIES_KCAL.defaultValue,
    val floors: Double = MetricDailyGoalKey.FLOORS.defaultValue,
    val elevationMeters: Double = MetricDailyGoalKey.ELEVATION_METERS.defaultValue,
    val wheelchairPushes: Double = MetricDailyGoalKey.WHEELCHAIR_PUSHES.defaultValue,
    val sleepHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    val hydrationLiters: Double = 2.0,
    val caloriesInKcal: Double = MetricDailyGoalKey.CALORIES_IN_KCAL.defaultValue,
    val proteinGrams: Double = MetricDailyGoalKey.PROTEIN_GRAMS.defaultValue,
    val carbsGrams: Double = MetricDailyGoalKey.CARBS_GRAMS.defaultValue,
    val fatGrams: Double = MetricDailyGoalKey.FAT_GRAMS.defaultValue,
    val mindfulnessMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val prefs: PreferencesRepository,
    private val activityRepository: ActivityRepository? = null,
) : ViewModel() {

    val minimumOnboardingPermissions get() = repository.minimumOnboardingPermissions

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            dashboardWidgets = dashboardWidgetIdsFromStored(prefs.dashboardWidgetOrder()),
            dailyGoals = prefs.dashboardDailyGoals(),
            sleepRangeMode = prefs.sleepRangeMode,
            activityWeekMode = prefs.activityWeekMode,
            showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()
    private var userPinnedPastDay = false

    init {
        load(_uiState.value.selectedDate)
    }

    fun refresh() {
        load(_uiState.value.selectedDate, RefreshMode.FORCE)
    }

    fun deleteActivityEntry(entryId: String) {
        if (entryId.isBlank()) return
        val activityRepository = activityRepository ?: return
        val entry = _uiState.value.data?.workouts.orEmpty()
            .plus(_uiState.value.data?.workout)
            .filterNotNull()
            .firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return

        viewModelScope.launch {
            runCatching {
                activityRepository.deleteActivityEntry(entryId)
            }.onSuccess {
                refresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message ?: "Unable to delete activity.",
                )
            }
        }
    }

    fun refreshPreferences() {
        val sleepRangeMode = prefs.sleepRangeMode
        val activityWeekMode = prefs.activityWeekMode
        val showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories
        val dailyGoals = prefs.dashboardDailyGoals()
        val current = _uiState.value
        val sleepRangeChanged = current.sleepRangeMode != sleepRangeMode
        val activityWeekModeChanged = current.activityWeekMode != activityWeekMode
        val calorieModeChanged = current.showOpenVitalsCalculatedCalories != showOpenVitalsCalculatedCalories
        if (
            sleepRangeChanged ||
            activityWeekModeChanged ||
            calorieModeChanged ||
            current.dailyGoals != dailyGoals
        ) {
            _uiState.value = current.copy(
                sleepRangeMode = sleepRangeMode,
                activityWeekMode = activityWeekMode,
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                dailyGoals = dailyGoals,
            )
        }
        if (sleepRangeChanged || activityWeekModeChanged || calorieModeChanged) {
            load(current.selectedDate)
        }
    }

    fun resumeCurrentDay() {
        refreshPreferences()
        val today = LocalDate.now()
        if (!userPinnedPastDay && _uiState.value.selectedDate.isBefore(today)) {
            load(today)
        }
    }

    fun load(date: LocalDate, refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        loadCoordinator.launch(viewModelScope) load@{
            val sleepRangeMode = prefs.sleepRangeMode
            val activityWeekMode = prefs.activityWeekMode
            val showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories
            val dailyGoals = prefs.dashboardDailyGoals()
            val dashboardWidgets = _uiState.value.dashboardWidgets
            val primaryMetrics = DashboardFastMetrics
            val deferredWidgets = dashboardWidgets
                .filter { widgetId ->
                    widgetId.toDashboardMetricOrNull()
                        ?.let { metric -> metric !in primaryMetrics }
                        ?: false
                }
                .toSet()
            val current = _uiState.value
            val availability = repository.availability()
            val granted = if (availability == HealthConnectAvailability.AVAILABLE) {
                repository.grantedPermissions()
            } else {
                emptySet()
            }
            val keepCurrentDataVisible = refreshMode == RefreshMode.FORCE && current.data != null
            _uiState.value = current.copy(
                selectedDate = clampedDate,
                isLoading = !keepCurrentDataVisible,
                errorMessage = null,
                sleepRangeMode = sleepRangeMode,
                activityWeekMode = activityWeekMode,
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                dailyGoals = dailyGoals,
                pendingWidgets = deferredWidgets,
                healthConnectSyncEnabled = prefs.healthConnectSyncEnabled,
                healthConnectAvailability = availability,
                minimumPermissionsGranted = repository.minimumOnboardingPermissions.all { it in granted },
            )
            runCatching {
                repository.loadDashboard(
                    DashboardQuery(
                        date = clampedDate,
                        sleepRangeMode = sleepRangeMode,
                        activityWeekMode = activityWeekMode,
                        visibleMetrics = primaryMetrics,
                        refreshMode = refreshMode,
                    )
                )
            }
                .onSuccess { data ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        data = data,
                        isLoading = false,
                        unacknowledgedWidgetPermissions = unacknowledgedWidgetPermissions(data.missingPermissions),
                        sleepRangeMode = sleepRangeMode,
                        activityWeekMode = activityWeekMode,
                        showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
                        dailyGoals = prefs.dashboardDailyGoals(),
                    )
                    loadDeferredDashboardMetrics(
                        date = clampedDate,
                        sleepRangeMode = sleepRangeMode,
                        activityWeekMode = activityWeekMode,
                        widgets = deferredWidgets.toList(),
                        refreshMode = refreshMode,
                        isCurrentLoad = { isCurrent },
                    )
                }
                .onFailure { error ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                    )
                }
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

    fun acknowledgeWidgetMissingPermissions() {
        val missing = _uiState.value.unacknowledgedWidgetPermissions
        if (missing.isEmpty()) return
        prefs.acknowledgePermissionsFor(HealthConnectFeature.DASHBOARD, missing)
        _uiState.value = _uiState.value.copy(unacknowledgedWidgetPermissions = emptySet())
    }

    private fun unacknowledgedWidgetPermissions(missingPermissions: Set<String>): Set<String> =
        missingPermissions - prefs.acknowledgedPermissionsFor(HealthConnectFeature.DASHBOARD)

    fun toggleDashboardEdit() {
        _uiState.value = _uiState.value.copy(isEditingDashboard = !_uiState.value.isEditingDashboard)
    }

    fun removeDashboardWidget(widgetId: DashboardWidgetId) {
        updateDashboardWidgets(_uiState.value.dashboardWidgets - widgetId)
    }

    fun addDashboardWidget(widgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        if (widgetId !in current) {
            updateDashboardWidgets(current + widgetId)
        }
    }

    fun moveDashboardWidget(widgetId: DashboardWidgetId, offset: Int) {
        val current = _uiState.value.dashboardWidgets
        val fromIndex = current.indexOf(widgetId)
        if (fromIndex == -1) return

        val toIndex = (fromIndex + offset).coerceIn(current.indices)
        if (fromIndex == toIndex) return

        updateDashboardWidgets(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(toIndex, widgetId)
            }
        )
    }

    fun moveDashboardWidgetToTarget(widgetId: DashboardWidgetId, targetWidgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        val fromIndex = current.indexOf(widgetId)
        val targetIndex = current.indexOf(targetWidgetId)
        if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return

        val fixedWidgetIds = dashboardWidgetIdsThatFitRows(
            widgetIds = current.filterNot { it == DashboardWidgetId.WORKOUT },
            rows = DashboardFixedWidgetRows,
        )
        val fromFixedSection = widgetId in fixedWidgetIds
        val targetFixedSection = targetWidgetId in fixedWidgetIds
        val updated = current.toMutableList().apply {
            if (fromFixedSection == targetFixedSection) {
                removeAt(fromIndex)
                add(targetIndex, widgetId)
            } else {
                this[fromIndex] = targetWidgetId
                this[targetIndex] = widgetId
            }
        }

        updateDashboardWidgets(updated)
    }

    private fun updateDashboardWidgets(widgets: List<DashboardWidgetId>) {
        val customizableWidgets = customizableDashboardWidgetIds(widgets)
        prefs.setDashboardWidgetOrder(customizableWidgets.map { it.name })
        _uiState.value = _uiState.value.copy(dashboardWidgets = customizableWidgets)
    }

    private suspend fun loadDeferredDashboardMetrics(
        date: LocalDate,
        sleepRangeMode: SleepRangeMode,
        activityWeekMode: ActivityWeekMode,
        widgets: List<DashboardWidgetId>,
        refreshMode: RefreshMode,
        isCurrentLoad: () -> Boolean,
    ) {
        val currentData = _uiState.value.data ?: return
        if (widgets.isEmpty()) return
        if (currentData.loadedMetrics.isEmpty()) {
            _uiState.value = _uiState.value.copy(pendingWidgets = emptySet())
            return
        }

        val metricsByWidget = widgets
            .mapNotNull { widgetId -> widgetId.toDashboardMetricOrNull()?.let { widgetId to it } }
        val orderedMetrics = metricsByWidget
            .map { (_, metric) -> metric }
            .distinct()

        orderedMetrics.forEach { metric ->
            if (!isCurrentLoad()) return
            runCatching {
                repository.loadDashboard(
                    DashboardQuery(
                        date = date,
                        sleepRangeMode = sleepRangeMode,
                        activityWeekMode = activityWeekMode,
                        visibleMetrics = setOf(metric),
                        refreshMode = refreshMode,
                    )
                )
            }
                .onSuccess { remainingData ->
                    if (!isCurrentLoad()) return
                    val loadedWidgets = metricsByWidget
                        .filter { (_, widgetMetric) -> widgetMetric == metric }
                        .map { (widgetId, _) -> widgetId }
                        .toSet()
                    val mergedData = (_uiState.value.data ?: currentData).mergeLoaded(remainingData)
                    _uiState.value = _uiState.value.copy(
                        data = mergedData,
                        unacknowledgedWidgetPermissions = unacknowledgedWidgetPermissions(mergedData.missingPermissions),
                        pendingWidgets = _uiState.value.pendingWidgets - loadedWidgets,
                    )
                }
                .onFailure { error ->
                    if (!isCurrentLoad()) return
                    val failedWidgets = metricsByWidget
                        .filter { (_, widgetMetric) -> widgetMetric == metric }
                        .map { (widgetId, _) -> widgetId }
                        .toSet()
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Unknown error",
                        pendingWidgets = _uiState.value.pendingWidgets - failedWidgets,
                    )
                }
        }
    }
}

private val DashboardFastMetrics = setOf(
    DashboardMetric.STEPS,
    DashboardMetric.DISTANCE,
    DashboardMetric.CALORIES_OUT,
    DashboardMetric.WHEELCHAIR_PUSHES,
    DashboardMetric.WORKOUT,
)

private fun PreferencesRepository.dashboardDailyGoals(): DashboardDailyGoals =
    DashboardDailyGoals(
        steps = dailyGoalFor(MetricDailyGoalKey.STEPS),
        distanceMeters = dailyGoalFor(MetricDailyGoalKey.DISTANCE_METERS),
        caloriesOutKcal = dailyGoalFor(MetricDailyGoalKey.CALORIES_OUT_KCAL),
        activeCaloriesKcal = dailyGoalFor(MetricDailyGoalKey.ACTIVE_CALORIES_KCAL),
        floors = dailyGoalFor(MetricDailyGoalKey.FLOORS),
        elevationMeters = dailyGoalFor(MetricDailyGoalKey.ELEVATION_METERS),
        wheelchairPushes = dailyGoalFor(MetricDailyGoalKey.WHEELCHAIR_PUSHES),
        sleepHours = dailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS),
        hydrationLiters = hydrationDailyGoalLiters,
        caloriesInKcal = dailyGoalFor(MetricDailyGoalKey.CALORIES_IN_KCAL),
        proteinGrams = dailyGoalFor(MetricDailyGoalKey.PROTEIN_GRAMS),
        carbsGrams = dailyGoalFor(MetricDailyGoalKey.CARBS_GRAMS),
        fatGrams = dailyGoalFor(MetricDailyGoalKey.FAT_GRAMS),
        mindfulnessMinutes = dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES),
    )
