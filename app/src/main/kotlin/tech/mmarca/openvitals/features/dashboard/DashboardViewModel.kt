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
    /**
     * Minutes until Health Connect will serve this app again, when the last
     * load ran into its rate limit. Reads no longer sit out that backoff, so
     * without this the tiles they gave up on would be indistinguishable from
     * tiles that genuinely have no data.
     */
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
            // Appends widgets added by an app update to a layout saved before
            // they existed; without it a new tile is invisible to anyone who
            // has ever edited their dashboard.
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

    /**
     * Serialises publishing. Every metric group finishes on its own and merges
     * itself into the state, and a merge is a read-modify-write of a value that
     * all of them are writing — unguarded, the tiles that landed together drop
     * each other's results.
     */
    private val publishMutex = Mutex()

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
        // The watch tile is device state, not day data, so nothing else would
        // ever rebuild it: without this a watch paired while the dashboard is
        // open stays invisible until the next reload, and the sync it is
        // running never shows on the tile that started it.
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

    /**
     * Syncs the watch the tile is showing. A no-op while any sync is running —
     * the radio is one resource, and the controller refuses a second anyway.
     */
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
            // Bypasses the in-flight dedupe on purpose: a load already running
            // read the old preferences, so it has to be restarted, not absorbed.
            forceLoad(current.selectedDate, RefreshMode.NORMAL)
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
        forceLoad(date = clampedDate, refreshMode = refreshMode)
    }

    private fun forceLoad(
        date: LocalDate,
        refreshMode: RefreshMode,
    ) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        // The date flips before any Health Connect call: the day navigator
        // answers the tap instantly, and the tiles read "loading" (not the old
        // day's numbers) for as long as the shown data lags the selection.
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
                // A superseded job must not clear the marker the newer load owns.
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
        // Widget order IS load order: the tiles on the first screen ask for
        // their reads first, and Health Connect serves a couple at a time.
        val orderedMetrics = widgets.mapNotNull { it.toDashboardMetricOrNull() }.distinct()
        val groups = dashboardMetricLoadGroups(orderedMetrics)
        val metricWidgetIds = widgets.filter { it.toDashboardMetricOrNull() != null }.toSet()
        val wantsBodyEnergy = bodyEnergyRepository != null &&
            DashboardWidgetId.BODY_ENERGY in widgets

        // The dashboard is on screen before a single read has been issued,
        // every tile on it reading "loading". Nothing here is allowed to await
        // a metric: the full-screen spinner this replaced was gated on the
        // slowest read in the batch, which on a data-dense phone with a
        // throttled Health Connect meant minutes of a blank screen.
        val existingData = current.data?.takeIf { it.date == clampedDate }
        val hadExistingData = existingData != null
        val seed = (existingData ?: DashboardData(date = clampedDate))
            // A reload re-establishes these from scratch. Carried over, a
            // permission the user granted since the last load would keep being
            // reported as missing, because the merge can only add to the set.
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

        // Children of the coordinator's job, so the next load cancels them the
        // same way it cancels this one — the reason the two hand-held Job
        // fields this replaced existed.
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
        // One metric failing is that tile's problem and it says so by coming up
        // empty. Every metric failing is the SCREEN's problem — Health Connect
        // is gone, or paused, or refusing — and staying quiet about it would
        // leave a full grid of tiles claiming the day holds no data at all.
        val firstFailure = failures.filterNotNull().firstOrNull()
        if (firstFailure != null && failures.size == groups.size && failures.none { it == null }) {
            _uiState.value = _uiState.value.copy(
                error = firstFailure.toScreenError("Unknown error"),
                // With nothing to show and nothing shown before, the error is
                // the whole screen. A reload that still has yesterday's answers
                // on it keeps them and speaks through a toast instead.
                data = if (hadExistingData) _uiState.value.data else null,
                isLoading = false,
                isRefreshing = false,
            )
        }
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

    /**
     * Loads one group of metrics and merges it into whatever is already on
     * screen, clearing only the tiles it covers.
     *
     * A group that fails is not an error state. The rest of the dashboard is
     * already showing, so its tiles simply stop loading and say what they know;
     * [rateLimitedRetryAfterMinutes] is what separates "no data" from "Health
     * Connect will not answer for another minute".
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
            // Both were skipped on the pass that used to gate the screen, to
            // keep it short. Nothing is gated on a pass any more, so the tile
            // can afford the reads that make it right the first time it fills
            // in instead of the second.
            includeHistoricalBaselines = true,
            includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in metrics,
        )
        var failure: Throwable? = null
        var data: DashboardData? = null
        // Two attempts, for one specific case: identical in-flight loads are
        // coalesced onto one of them, so a caller that is perfectly alive can
        // be handed the CANCELLATION of the pass it was sharing. That is not
        // this pass being cancelled, and the check below is what tells them
        // apart — a real cancellation rethrows and takes the load down with it.
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

    /**
     * Merges one finished pass into the state and takes its tiles off the
     * loading list, under [publishMutex] so concurrent passes cannot clobber
     * one another.
     */
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
            // The load is done when the last tile stops loading, not when any
            // one pass returns: the pull-to-refresh indicator tracks the whole
            // dashboard, and the tiles track themselves.
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

    /**
     * Whether Health Connect is currently refusing this app's reads, in whole
     * minutes, or null when it is not.
     */
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
            // Read straight from the registry rather than from ui state: the
            // two are filled by different coroutines, and a display built
            // before the device flow's first emission would drop the tile and
            // never get it back (the rebuild that emission triggers no-ops
            // while the day's data is still loading).
            watch = bleDeviceRepository?.devices?.toWatchWidgetDisplay(
                syncingDeviceId = deviceSyncController?.state?.value?.syncingDeviceId,
                live = garminRealtimeStore?.readings?.value ?: GarminRealtimeState(),
            ),
            // Edit mode is the only place an unsupported metric materialises —
            // in the add tray, so it can be placed rather than lost.
            includeUnsupported = _uiState.value.isEditingDashboard,
        )
    }
}

/**
 * The watch tile's content: the most recently synced watch, with the others
 * behind a count. A bike computer is deliberately not a watch here — it has its
 * own place among the sensors.
 */
internal fun List<BleSensorDevice>.toWatchWidgetDisplay(
    syncingDeviceId: String? = null,
    live: GarminRealtimeState = GarminRealtimeState(),
    now: Instant = Instant.now(),
): WatchWidgetDisplay? {
    val watches = filter { it.isWatch }
    // The watch being synced right now owns the tile, whichever it is —
    // otherwise the spinner would sit on a watch that is not the one working.
    // Failing that, the most recently synced, so the tile follows the watch
    // actually in use; a never-synced one sorts last rather than claiming it.
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
        // Only one watch can hold a link at a time, and the tile shows the one
        // in use — so the live values belong to it or to nothing.
        liveHeartRateBpm = live.freshHeartRate(now),
        liveSteps = live.freshSteps(now),
    )
}

/**
 * The dashboard's roll-up of the paired BLE devices the Sensors & devices screen
 * actually lists — [BleSensorDevice.isLiveSensorCapable] ones. The top-bar
 * battery action opens that screen, so its visibility has to mean "there is
 * something behind this tap": a paired watch alone used to put the icon up over
 * an empty list (watches live under Settings > Watches, with their own battery
 * surface), and its battery skewed the "lowest battery" figure. A bike computer
 * still counts — it broadcasts standard GATT like any sensor.
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
