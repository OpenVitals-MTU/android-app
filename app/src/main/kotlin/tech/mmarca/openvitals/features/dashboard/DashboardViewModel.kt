package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.devices.garmin.GarminRealtimeState
import tech.mmarca.openvitals.devices.garmin.GarminRealtimeStore
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
import tech.mmarca.openvitals.domain.model.dashboardMetricLoadGroups
import tech.mmarca.openvitals.domain.model.mergeLoaded
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.HistorySyncScheduler
import java.time.LocalDate
import tech.mmarca.openvitals.features.watches.DeviceSyncController
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.ceil

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
     * The widgets the user placed. Only these keep an unsupported metric in
     * the edit grid; a default-layout one belongs in the add tray.
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
    /** Minutes until Health Connect serves this app again after a rate limit. */
    val rateLimitedRetryAfterMinutes: Long? = null,
    val watch: WatchWidgetDisplay? = null,
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
    private val deviceSyncController: DeviceSyncController? = null,
    private val garminRealtimeStore: GarminRealtimeStore? = null,
) : ViewModel() {

    val minimumOnboardingPermissions get() = repository.minimumOnboardingPermissions

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            // Appends widgets added by an update to a layout saved before them.
            dashboardWidgets = dashboardWidgetIdsWithNewOnesAppended(
                storedIds = prefs.dashboardWidgetOrder(),
                knownIds = prefs.dashboardKnownWidgetIds(),
                persist = { order, known ->
                    order?.let(prefs::setDashboardWidgetOrder)
                    prefs.setDashboardKnownWidgetIds(known)
                },
            ),
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
    private var loadGeneration = 0L

    /** Serialises publishing: each merge is a read-modify-write of the shared state. */
    private val publishMutex = Mutex()

    /**
     * The day being loaded, or null once settled. A NORMAL request for the
     * same day is absorbed; FORCE and date changes restart.
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
        // The watch tile is device state, not day data; nothing else rebuilds it.
        val syncStates = deviceSyncController?.state?.map { it.syncingDeviceId } ?: flowOf(null)
        val liveReadings = garminRealtimeStore?.readings ?: flowOf(GarminRealtimeState())
        viewModelScope.launch {
            combine(
                deviceRepository.devicesFlow,
                syncStates,
                liveReadings,
            ) { devices, syncingDeviceId, live ->
                devices.toWatchWidgetDisplay(syncingDeviceId = syncingDeviceId, live = live)
            }
                .distinctUntilChanged()
                .collect { watch ->
                    if (watch != _uiState.value.watch) {
                        _uiState.update { it.copy(watch = watch) }
                        rebuildDisplay()
                    }
                }
        }
    }

    /** Syncs the watch the tile shows. A no-op while any sync runs. */
    fun syncWatchNow() {
        val deviceId = _uiState.value.watch?.deviceId ?: return
        deviceSyncController?.syncDevice(
            deviceId,
            listenAfter = DeviceSyncController.MANUAL_SYNC_LINGER,
        )
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
            // Bypasses the in-flight dedupe: the running load read old preferences.
            forceLoad(current.selectedDate, RefreshMode.NORMAL)
        }
    }

    fun resumeCurrentDay() {
        refreshPreferences()
        // Reload whenever the user returns to today: ON_RESUME is the only signal
        // that Health Connect data may have changed.
        if (!userPinnedPastDay) {
            load(LocalDate.now())
        }
    }

    fun load(date: LocalDate, refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        if (refreshMode == RefreshMode.NORMAL && clampedDate == inFlightLoadDate) return
        forceLoad(date = clampedDate, refreshMode = refreshMode)
    }

    private fun forceLoad(
        date: LocalDate,
        refreshMode: RefreshMode,
    ) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        // The date flips before any read, so the navigator answers instantly.
        _uiState.value = _uiState.value.copy(
            selectedDate = clampedDate,
            sortEmptyTilesLast = prefs.dashboardSortEmptyTilesLast,
        )
        val generation = ++loadGeneration
        inFlightLoadDate = clampedDate
        loadCoordinator.launch(viewModelScope) load@{
            try {
                runLoadPass(
                    clampedDate = clampedDate,
                    refreshMode = refreshMode,
                    generation = generation,
                )
            } finally {
                // A superseded job must not clear the newer load's marker.
                if (isCurrent) inFlightLoadDate = null
            }
        }
    }

    private suspend fun LoadCoordinator.LoadScope.runLoadPass(
        clampedDate: LocalDate,
        refreshMode: RefreshMode,
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
        _uiState.value = current.copy(
            selectedDate = clampedDate,
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
        val widgets = _uiState.value.dashboardWidgets
        // Widget order is load order: first-screen tiles read first.
        val orderedMetrics = widgets.mapNotNull { it.toDashboardMetricOrNull() }.distinct()
        val groups = dashboardMetricLoadGroups(orderedMetrics)
        val metricWidgetIds = widgets.filter { it.toDashboardMetricOrNull() != null }.toSet()
        val wantsBodyEnergy = bodyEnergyRepository != null &&
            DashboardWidgetId.BODY_ENERGY in widgets

        // Nothing here may await a metric: the screen shows "loading" tiles at once.
        val existingData = current.data?.takeIf { it.date == clampedDate }
        val hadExistingData = existingData != null
        val seed = (existingData ?: DashboardData(date = clampedDate))
            // A reload re-establishes these; the merge can only add to the set.
            .copy(missingPermissions = emptySet())
        publishDashboardData(
            data = seed,
            loadingWidgets = metricWidgetIds + listOfNotNull(
                DashboardWidgetId.BODY_ENERGY.takeIf { wantsBodyEnergy },
            ),
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            goals = dailyGoals,
        )

        // Children of the coordinator's job, so the next load cancels them.
        val failures = coroutineScope {
            val metricPasses = groups.map { group ->
                async {
                    loadMetricGroup(
                        metrics = group,
                        date = clampedDate,
                        refreshMode = refreshMode,
                        sleepWindow = sleepWindow,
                        activityWeekMode = activityWeekMode,
                        generation = generation,
                    )
                }
            }
            if (wantsBodyEnergy) {
                launch {
                    loadBodyEnergy(
                        date = clampedDate,
                        refreshMode = refreshMode,
                        generation = generation,
                    )
                }
            }
            metricPasses.awaitAll()
        }

        if (!isCurrent) return
        // One metric failing is that tile's problem. Every metric failing is the screen's.
        val firstFailure = failures.filterNotNull().firstOrNull()
        if (firstFailure != null && failures.size == groups.size && failures.none { it == null }) {
            _uiState.value = _uiState.value.copy(
                error = firstFailure.toScreenError("Unknown error"),
                // Nothing shown before: the error is the whole screen. Otherwise a toast.
                data = if (hadExistingData) _uiState.value.data else null,
                isLoading = false,
                isRefreshing = false,
            )
        }
        // Once per app open, after the load settles: drain the caches' change tokens.
        historySyncScheduler?.let { scheduler ->
            viewModelScope.launch {
                runCatching { scheduler.drainIncrementalOnce() }
            }
        }
    }

    /**
     * Loads one group of metrics and merges it in. A failed group is not an
     * error state; [rateLimitedRetryAfterMinutes] separates "no data" from
     * "Health Connect will not answer".
     */
    private suspend fun loadMetricGroup(
        metrics: Set<DashboardMetric>,
        date: LocalDate,
        refreshMode: RefreshMode,
        sleepWindow: SleepWindow,
        activityWeekMode: ActivityWeekMode,
        generation: Long,
    ): Throwable? {
        val widgetIds = _uiState.value.dashboardWidgets
            .filter { it.toDashboardMetricOrNull() in metrics }
            .toSet()
        val query = DashboardQuery(
            date = date,
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            visibleMetrics = metrics,
            refreshMode = refreshMode,
            // Nothing is gated on a pass any more, so the tile can afford these reads.
            includeHistoricalBaselines = true,
            includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in metrics,
        )
        var failure: Throwable? = null
        var data: DashboardData? = null
        // Two attempts: identical in-flight loads are coalesced, so a live caller
        // can be handed the cancellation of the pass it shared.
        repeat(2) {
            if (data != null) return@repeat
            try {
                data = loadDashboardDayUseCase(query)
                failure = null
            } catch (error: CancellationException) {
                if (!currentCoroutineContext().isActive) throw error
                failure = error
            } catch (error: Throwable) {
                failure = error
            }
        }
        val loaded = data
        publishMerged(generation = generation, date = date, clearing = widgetIds) { current ->
            if (loaded == null) current else current.mergeLoaded(loaded)
        }
        return failure
    }

    private suspend fun loadBodyEnergy(
        date: LocalDate,
        refreshMode: RefreshMode,
        generation: Long,
    ) {
        val repository = bodyEnergyRepository ?: return
        val timeline = try {
            repository.loadTimeline(
                BodyEnergyTimelineQuery(
                    period = DatePeriod(date, date),
                    range = TimeRange.DAY,
                    refreshMode = refreshMode,
                )
            ).latestDay
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            null
        }
        publishMerged(
            generation = generation,
            date = date,
            clearing = setOf(DashboardWidgetId.BODY_ENERGY),
        ) { current ->
            if (timeline == null) current else current.copy(bodyEnergyTimeline = timeline)
        }
    }

    /** Merges one finished pass into the state under [publishMutex]. */
    private suspend fun publishMerged(
        generation: Long,
        date: LocalDate,
        clearing: Set<DashboardWidgetId>,
        merge: (DashboardData) -> DashboardData,
    ) {
        publishMutex.withLock {
            if (generation != loadGeneration) return
            val state = _uiState.value
            if (state.selectedDate != date) return
            val currentData = state.data?.takeIf { it.date == date } ?: return
            publishDashboardData(
                data = merge(currentData),
                loadingWidgets = state.loadingWidgets - clearing,
                sleepWindow = state.sleepWindow,
                activityWeekMode = state.activityWeekMode,
                goals = state.dailyGoals,
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
        // A reload that found nothing new keeps the old instances, or every
        // animation replays on every sync.
        val current = _uiState.value
        val stableData = if (data == current.data) current.data ?: data else data
        val builtDisplay = buildDisplay(stableData, goals, loadingWidgets)
        val display = if (builtDisplay == current.display) current.display else builtDisplay
        _uiState.value = _uiState.value.copy(
            data = stableData,
            isLoading = false,
            // Done when the last tile stops loading.
            isRefreshing = loadingWidgets.isNotEmpty(),
            unacknowledgedWidgetPermissions = unacknowledgedWidgetPermissions(data.missingPermissions),
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
            dailyGoals = goals,
            display = display,
            loadingWidgets = loadingWidgets,
            rateLimitedRetryAfterMinutes = rateLimitedRetryAfterMinutes(),
        )
    }

    /** Minutes Health Connect is refusing reads for, or null. */
    private fun rateLimitedRetryAfterMinutes(): Long? =
        repository.rateLimitRetryAfterMillis()
            .takeIf { it > 0L }
            ?.let { millis -> ceil(millis / 60_000.0).toLong().coerceAtLeast(1L) }

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
        // Edit mode materialises unsupported metrics; rebuild the display, same data.
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
        // Recording the placement keeps an unsupported metric in the grid.
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
            // From the registry, not ui state: a display built before the device
            // flow's first emission would drop the tile.
            watch = bleDeviceRepository?.devices?.toWatchWidgetDisplay(
                syncingDeviceId = deviceSyncController?.state?.value?.syncingDeviceId,
                live = garminRealtimeStore?.readings?.value ?: GarminRealtimeState(),
            ),
            // Only edit mode shows unsupported metrics, in the add tray.
            includeUnsupported = _uiState.value.isEditingDashboard,
        )
    }
}

/** The watch tile: the most recently synced watch, others behind a count. Bike computers excluded. */
internal fun List<BleSensorDevice>.toWatchWidgetDisplay(
    syncingDeviceId: String? = null,
    live: GarminRealtimeState = GarminRealtimeState(),
    now: Instant = Instant.now(),
): WatchWidgetDisplay? {
    val watches = filter { it.isWatch }
    // The watch being synced owns the tile; failing that, the most recently synced.
    val primary = watches.firstOrNull { it.id == syncingDeviceId }
        ?: watches.maxByOrNull { it.lastSyncedAt?.toEpochMilli() ?: Long.MIN_VALUE }
        ?: return null
    return WatchWidgetDisplay(
        deviceId = primary.id,
        name = primary.displayName,
        batteryPercent = primary.batteryPercent,
        lastSyncedAt = primary.lastSyncedAt,
        additionalCount = watches.size - 1,
        isSyncing = primary.id == syncingDeviceId,
        // Only one watch can hold a link, so the live values belong to it or nothing.
        liveHeartRateBpm = live.freshHeartRate(now),
        liveSteps = live.freshSteps(now),
    )
}

/**
 * The paired BLE devices the Sensors & devices screen lists. Watches are
 * excluded: they live under Settings and skewed the lowest-battery figure.
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
