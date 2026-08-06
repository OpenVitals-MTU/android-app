package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.mergeLoaded
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.HistorySyncScheduler
import java.time.LocalDate
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val data: DashboardData? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ScreenError? = null,
    val unacknowledgedWidgetPermissions: Set<String> = emptySet(),
    val sleepWindow: SleepWindow = SleepWindow.Default,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val dashboardWidgets: List<DashboardWidgetId> = DefaultDashboardWidgetIds,
    /**
     * The widgets the user actually placed — the persisted layout, empty until
     * they edit the dashboard for the first time. Only these keep an unsupported
     * metric in the edit grid; one that is merely part of the default layout
     * belongs in the add tray, where its "the device cannot serve this" status
     * is honest and it can still be placed deliberately.
     */
    val placedDashboardWidgets: Set<DashboardWidgetId> = emptySet(),
    val dailyGoals: DashboardDailyGoals = DashboardDailyGoals(),
    val isEditingDashboard: Boolean = false,
    val sortEmptyTilesLast: Boolean = true,
    val healthConnectSyncEnabled: Boolean = true,
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val minimumPermissionsGranted: Boolean = true,
    val display: DashboardDisplayState = DashboardDisplayState(),
    val loadingWidgets: Set<DashboardWidgetId> = emptySet(),
    val sensorStatus: DashboardSensorStatus = DashboardSensorStatus(),
)

@Immutable
data class DashboardSensorStatus(
    val devices: List<DashboardSensorDeviceStatus> = emptyList(),
) {
    val hasDevices: Boolean
        get() = devices.isNotEmpty()

    val enabledCount: Int
        get() = devices.count { it.enabled }

    val connectedCount: Int
        get() = devices.count { it.connectionStatus == BleConnectionStatus.CONNECTED }

    val lowestBatteryPercent: Int?
        get() = devices.mapNotNull { it.batteryPercent }.minOrNull()
}

@Immutable
data class DashboardSensorDeviceStatus(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val connectionStatus: BleConnectionStatus,
    val batteryPercent: Int?,
)

@Immutable
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
    private val loadDashboardDayUseCase: LoadDashboardDayUseCase,
    private val repository: HealthRepository,
    private val prefs: PreferencesRepository,
    private val unitFormatter: UnitFormatter,
    private val dateTimeFormatterProvider: DateTimeFormatterProvider,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val activityRepository: ActivityRepository? = null,
    private val bodyEnergyRepository: BodyEnergyRepository? = null,
    private val bleDeviceRepository: BleDeviceRepository? = null,
    private val bleSensorCoordinator: BleSensorCoordinator? = null,
    private val historySyncScheduler: HistorySyncScheduler? = null,
) : ViewModel() {

    val minimumOnboardingPermissions get() = repository.minimumOnboardingPermissions

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            dashboardWidgets = dashboardWidgetIdsFromStored(prefs.dashboardWidgetOrder()),
            placedDashboardWidgets = prefs.dashboardWidgetOrder()
                ?.let { stored -> dashboardWidgetIdsFromStored(stored).toSet() }
                .orEmpty(),
            dailyGoals = prefs.dashboardDailyGoals(),
            sleepWindow = prefs.sleepWindow,
            activityWeekMode = prefs.activityWeekMode,
            showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()
    private var userPinnedPastDay = false
    private var permissionPromptDismissedForLoad = false
    private var backgroundMetricsJob: Job? = null
    private var bodyEnergyJob: Job? = null
    private var loadGeneration = 0L

    /**
     * The day the current coordinator job is loading, or null once it settles.
     * Opening the dashboard fires both the init load and the first ON_RESUME
     * within one frame; restarting the in-flight load would throw away Health
     * Connect reads already issued and pay for them a second time. A NORMAL
     * request for the day already being loaded is therefore absorbed here —
     * FORCE (pull-to-refresh) and date changes still restart as before.
     */
    private var inFlightLoadDate: LocalDate? = null

    init {
        observeSensorStatus()
        load(_uiState.value.selectedDate)
    }

    fun refresh() {
        load(_uiState.value.selectedDate, RefreshMode.FORCE)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeSensorStatus() {
        val deviceRepository = bleDeviceRepository ?: return
        val metricsFlow = bleSensorCoordinator?.metrics ?: flowOf(BleRecordingMetrics())
        viewModelScope.launch {
            combine(
                deviceRepository.devicesFlow,
                metricsFlow,
            ) { devices, metrics ->
                devices.toDashboardSensorStatus(metrics.deviceStatuses)
            }.collect { sensorStatus ->
                _uiState.update { it.copy(sensorStatus = sensorStatus) }
            }
        }
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
                    error = error.toScreenError("Unable to delete activity."),
                )
            }
        }
    }

    fun refreshPreferences() {
        val sleepWindow = prefs.sleepWindow
        val activityWeekMode = prefs.activityWeekMode
        val showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories
        val dailyGoals = prefs.dashboardDailyGoals()
        val current = _uiState.value
        val sleepRangeChanged = current.sleepWindow != sleepWindow
        val activityWeekModeChanged = current.activityWeekMode != activityWeekMode
        val calorieModeChanged = current.showOpenVitalsCalculatedCalories != showOpenVitalsCalculatedCalories
        if (
            sleepRangeChanged ||
            activityWeekModeChanged ||
            calorieModeChanged ||
            current.dailyGoals != dailyGoals
        ) {
            viewModelScope.launch {
                val display = current.data?.let { data ->
                    buildDisplay(data, dailyGoals, current.loadingWidgets)
                } ?: current.display
                _uiState.value = current.copy(
                    sleepWindow = sleepWindow,
                    activityWeekMode = activityWeekMode,
                    showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                    dailyGoals = dailyGoals,
                    display = display,
                )
            }
        }
        if (sleepRangeChanged || activityWeekModeChanged || calorieModeChanged) {
            // Bypasses the in-flight dedupe on purpose: a load already running
            // read the old preferences, so it has to be restarted, not absorbed.
            load(current.selectedDate, RefreshMode.NORMAL, retryOnCancellation = true)
        }
    }

    fun resumeCurrentDay() {
        refreshPreferences()
        // Reload whenever the user returns to "today" (e.g. from a metric detail screen),
        // not only on a day rollover: this ViewModel outlives detail screens on the back
        // stack, so ON_RESUME is the only signal that Health Connect data may have changed.
        if (!userPinnedPastDay) {
            load(LocalDate.now())
        }
    }

    fun load(date: LocalDate, refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        if (refreshMode == RefreshMode.NORMAL && clampedDate == inFlightLoadDate) return
        load(date = clampedDate, refreshMode = refreshMode, retryOnCancellation = true)
    }

    private fun load(
        date: LocalDate,
        refreshMode: RefreshMode,
        retryOnCancellation: Boolean,
    ) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        _uiState.value = _uiState.value.copy(
            sortEmptyTilesLast = prefs.dashboardSortEmptyTilesLast,
        )
        val generation = ++loadGeneration
        backgroundMetricsJob?.cancel()
        bodyEnergyJob?.cancel()
        inFlightLoadDate = clampedDate
        loadCoordinator.launch(viewModelScope) load@{
            try {
                runLoadPass(
                    clampedDate = clampedDate,
                    refreshMode = refreshMode,
                    retryOnCancellation = retryOnCancellation,
                    generation = generation,
                )
            } finally {
                // A superseded job must not clear the marker the newer load owns.
                if (isCurrent) inFlightLoadDate = null
            }
        }
    }

    private suspend fun LoadCoordinator.LoadScope.runLoadPass(
        clampedDate: LocalDate,
        refreshMode: RefreshMode,
        retryOnCancellation: Boolean,
        generation: Long,
    ) {
        val sleepWindow = prefs.sleepWindow
        val activityWeekMode = prefs.activityWeekMode
        val showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories
        val dailyGoals = prefs.dashboardDailyGoals()
        permissionPromptDismissedForLoad = false
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
            isRefreshing = true,
            error = null,
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
            dailyGoals = dailyGoals,
            healthConnectSyncEnabled = prefs.healthConnectSyncEnabled,
            healthConnectAvailability = availability,
            minimumPermissionsGranted = repository.minimumOnboardingPermissions.all { it in granted },
            loadingWidgets = emptySet(),
        )
        val quickWidgetIds = firstVisibleDashboardWidgetIds(_uiState.value.dashboardWidgets)
        val quickMetrics = quickWidgetIds.toDashboardMetrics()
        val dashboardQuery = DashboardQuery(
            date = clampedDate,
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            visibleMetrics = quickMetrics,
            refreshMode = refreshMode,
            includeHistoricalBaselines = false,
            includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in quickMetrics,
        )
        val data = try {
            loadDashboardDayUseCase(dashboardQuery)
        } catch (error: CancellationException) {
            if (!isCurrent) return
            if (retryOnCancellation) {
                load(
                    date = clampedDate,
                    refreshMode = refreshMode,
                    retryOnCancellation = false,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            }
            return
        } catch (error: Throwable) {
            if (!isCurrent) return
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = error.toScreenError("Unknown error"),
            )
            return
        }

        if (!isCurrent) return
        val currentData = _uiState.value.data
        val mergedData = if (currentData?.date == clampedDate) {
            currentData.mergeLoaded(data)
        } else {
            data
        }
        val backgroundWidgetIds = (_uiState.value.dashboardWidgets - quickWidgetIds.toSet())
        val loadingWidgets = buildSet {
            addAll(backgroundWidgetIds)
            if (bodyEnergyRepository != null &&
                DashboardWidgetId.BODY_ENERGY in _uiState.value.dashboardWidgets &&
                mergedData.bodyEnergyTimeline == null
            ) {
                add(DashboardWidgetId.BODY_ENERGY)
            }
        }
        publishDashboardData(
            data = mergedData,
            loadingWidgets = loadingWidgets,
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            goals = prefs.dashboardDailyGoals(),
        )
        if (data.loadedMetrics.isNotEmpty() || quickMetrics.isEmpty()) {
            launchBackgroundMetricLoad(
                date = clampedDate,
                refreshMode = refreshMode,
                quickMetrics = quickMetrics,
                generation = generation,
            )
        }
        launchBodyEnergyLoad(
            date = clampedDate,
            refreshMode = refreshMode,
            generation = generation,
        )
        // Once per app open, after the dashboard's own load has settled:
        // drain the daily-aggregate caches' changes tokens. Incremental
        // only — a cache that never full-synced stays untouched until its
        // screen pays for the first rebuild.
        historySyncScheduler?.let { scheduler ->
            viewModelScope.launch {
                runCatching { scheduler.drainIncrementalOnce() }
            }
        }
    }

    private fun launchBackgroundMetricLoad(
        date: LocalDate,
        refreshMode: RefreshMode,
        quickMetrics: Set<DashboardMetric>,
        generation: Long,
    ) {
        val allWidgetMetrics = _uiState.value.dashboardWidgets.toDashboardMetrics()
        val backgroundMetrics = allWidgetMetrics - quickMetrics
        if (backgroundMetrics.isEmpty()) return

        backgroundMetricsJob = viewModelScope.launch {
            val sleepWindow = prefs.sleepWindow
            val activityWeekMode = prefs.activityWeekMode
            val data = runCatching {
                loadDashboardDayUseCase(
                    DashboardQuery(
                        date = date,
                        sleepWindow = sleepWindow,
                        activityWeekMode = activityWeekMode,
                        visibleMetrics = backgroundMetrics,
                        refreshMode = refreshMode,
                        includeHistoricalBaselines = true,
                        includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in backgroundMetrics,
                    )
                )
            }.getOrNull() ?: return@launch
            if (generation != loadGeneration || _uiState.value.selectedDate != date) return@launch
            val currentData = _uiState.value.data ?: return@launch
            if (currentData.date != date) return@launch
            val mergedData = currentData.mergeLoaded(data)
            val loadedWidgetIds = _uiState.value.dashboardWidgets.filter { widgetId ->
                widgetId.toDashboardMetricOrNull()?.let { it in backgroundMetrics } == true
            }.toSet()
            publishDashboardData(
                data = mergedData,
                loadingWidgets = _uiState.value.loadingWidgets - loadedWidgetIds,
                sleepWindow = prefs.sleepWindow,
                activityWeekMode = prefs.activityWeekMode,
                goals = prefs.dashboardDailyGoals(),
            )
        }
    }

    private fun launchBodyEnergyLoad(
        date: LocalDate,
        refreshMode: RefreshMode,
        generation: Long,
    ) {
        val repository = bodyEnergyRepository ?: return
        if (DashboardWidgetId.BODY_ENERGY !in _uiState.value.dashboardWidgets) return

        bodyEnergyJob = viewModelScope.launch {
            val timeline = runCatching {
                repository.loadTimeline(
                    BodyEnergyTimelineQuery(
                        period = DatePeriod(date, date),
                        range = TimeRange.DAY,
                        refreshMode = refreshMode,
                    )
                ).latestDay
            }.getOrNull() ?: return@launch
            if (generation != loadGeneration || _uiState.value.selectedDate != date) return@launch
            val currentData = _uiState.value.data ?: return@launch
            if (currentData.date != date) return@launch
            publishDashboardData(
                data = currentData.copy(bodyEnergyTimeline = timeline),
                loadingWidgets = _uiState.value.loadingWidgets - DashboardWidgetId.BODY_ENERGY,
                sleepWindow = prefs.sleepWindow,
                activityWeekMode = prefs.activityWeekMode,
                goals = prefs.dashboardDailyGoals(),
            )
        }
    }

    private suspend fun publishDashboardData(
        data: DashboardData,
        loadingWidgets: Set<DashboardWidgetId>,
        sleepWindow: SleepWindow,
        activityWeekMode: ActivityWeekMode,
        goals: DashboardDailyGoals,
    ) {
        // A reload that found nothing new keeps the OLD instances. Every
        // widget animation is keyed on the data it shows; handing the tiles a
        // fresh-but-equal object replayed every ring sweep and chart reveal
        // on every sync that changed nothing.
        val current = _uiState.value
        val stableData = if (data == current.data) current.data ?: data else data
        val builtDisplay = buildDisplay(stableData, goals, loadingWidgets)
        val display = if (builtDisplay == current.display) current.display else builtDisplay
        _uiState.value = _uiState.value.copy(
            data = stableData,
            isLoading = false,
            isRefreshing = false,
            unacknowledgedWidgetPermissions = unacknowledgedWidgetPermissions(data.missingPermissions),
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
            dailyGoals = goals,
            display = display,
            loadingWidgets = loadingWidgets,
        )
    }

    private fun firstVisibleDashboardWidgetIds(widgetIds: List<DashboardWidgetId>): List<DashboardWidgetId> {
        val fixedIds = dashboardWidgetIdsThatFitRows(
            widgetIds = widgetIds,
            rows = DashboardFixedWidgetRows,
        )
        val fixedIdSet = fixedIds.toSet()
        val firstCarouselPage = dashboardWidgetIdsInGridPages(
            widgetIds = widgetIds.filterNot { it in fixedIdSet },
            rows = DashboardCarouselWidgetRows,
        ).firstOrNull().orEmpty()
        return fixedIds + firstCarouselPage
    }

    private fun Collection<DashboardWidgetId>.toDashboardMetrics(): Set<DashboardMetric> =
        mapNotNull { it.toDashboardMetricOrNull() }.toSet()

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
        permissionPromptDismissedForLoad = true
        _uiState.value = _uiState.value.copy(unacknowledgedWidgetPermissions = emptySet())
    }

    private fun unacknowledgedWidgetPermissions(missingPermissions: Set<String>): Set<String> =
        if (permissionPromptDismissedForLoad) emptySet()
        else missingPermissions - prefs.acknowledgedPermissionsFor(HealthConnectFeature.DASHBOARD)

    fun toggleDashboardEdit() {
        val editing = !_uiState.value.isEditingDashboard
        _uiState.value = _uiState.value.copy(isEditingDashboard = editing)
        // Edit mode materialises the metrics the device cannot serve, so the
        // display has to be rebuilt — no reload, the same day's data.
        rebuildDisplay()
    }

    private fun rebuildDisplay() {
        val current = _uiState.value
        val data = current.data ?: return
        viewModelScope.launch {
            val display = buildDisplay(data, current.dailyGoals, current.loadingWidgets)
            _uiState.update { it.copy(display = display) }
        }
    }

    fun removeDashboardWidget(widgetId: DashboardWidgetId) {
        updateDashboardWidgets(_uiState.value.dashboardWidgets - widgetId)
    }

    fun addDashboardWidget(widgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        // Adding one that is already in the (default) layout is not a no-op: it
        // records the placement, which is what keeps an unsupported metric in
        // the grid instead of bouncing it back to the tray.
        updateDashboardWidgets(if (widgetId in current) current else current + widgetId)
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
        _uiState.value = _uiState.value.copy(
            dashboardWidgets = customizableWidgets,
            placedDashboardWidgets = customizableWidgets.toSet(),
        )
    }

    private suspend fun buildDisplay(
        data: DashboardData,
        dailyGoals: DashboardDailyGoals,
        loadingWidgets: Set<DashboardWidgetId> = emptySet(),
    ): DashboardDisplayState = withContext(dispatchers.default) {
        DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            loadingWidgets = loadingWidgets,
            bodyEnergySetupCompleted = prefs.bodyEnergyCalibration().setupCompleted,
            // Edit mode is the only place an unsupported metric materialises —
            // in the add tray, so it can be placed rather than lost.
            includeUnsupported = _uiState.value.isEditingDashboard,
        )
    }
}

/**
 * The dashboard's roll-up of the paired BLE devices the Sensors & devices screen
 * actually lists — [BleSensorDevice.isLiveSensorCapable] ones. The top-bar
 * battery action opens that screen, so its visibility has to mean "there is
 * something behind this tap": a stored watch-era registry entry alone must not
 * put the icon up over an empty list, nor skew the "lowest battery" figure. A
 * bike computer still counts — it broadcasts standard GATT like any sensor.
 */
internal fun List<BleSensorDevice>.toDashboardSensorStatus(
    connectionStatuses: List<BleDeviceConnectionStatus>,
): DashboardSensorStatus {
    val statusesById = connectionStatuses.associateBy { it.deviceId }
    val statusesByAddress = connectionStatuses.associateBy { it.address }
    return DashboardSensorStatus(
        devices = filter { it.isLiveSensorCapable }.map { device ->
            val liveStatus = statusesById[device.id] ?: statusesByAddress[device.address]
            DashboardSensorDeviceStatus(
                id = device.id,
                displayName = device.displayName,
                enabled = device.enabled,
                connectionStatus = liveStatus?.status ?: BleConnectionStatus.DISCONNECTED,
                batteryPercent = liveStatus?.batteryPercent ?: device.batteryPercent,
            )
        },
    )
}

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
