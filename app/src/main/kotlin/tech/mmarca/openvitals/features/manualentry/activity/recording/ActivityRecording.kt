package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.altitude.AltitudeConverter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarkerType
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.features.workoutplans.toPlanRunSteps
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator

enum class ActivityRecordingStatus {
    IDLE,
    RECORDING,
    RESTING,
    PAUSED,
}

enum class ActivityRecordingKind {
    GPS_ROUTE,
    REPETITION,
    TIMED,
}

enum class ActivityGpsStatus {
    WAITING_FOR_FIX,
    FIX,
    POOR_ACCURACY,
    LOST,
    DISABLED,
}

/**
 * Where a heart-rate-recovery test has got to. Not a recording status: the
 * recording stays RECORDING throughout. It must never borrow RESTING, whose
 * bookkeeping would subtract the recovery as a rest between sets.
 */
enum class ActivityRecordingHrrPhase {
    /** An ordinary recording. */
    NONE,

    /** Warming up, counting down to the effort. */
    WARMUP,

    /** Going hard, until the user says stop or the target heart rate is reached. */
    EFFORT,

    /** The measurement. The clock started the instant this began. */
    RECOVERY,

    /** Recovery time is up. The user still has to save. */
    COMPLETE,
}

/** How a heart-rate-recovery test was set up. */
@Immutable
data class HeartRateRecoveryTestConfig(
    /** 0 to skip the warmup and go straight to the effort. */
    val warmupSeconds: Int = 180,
    /** Ends the effort when the heart rate reaches this. The user can always end it by hand. */
    val targetHeartRateBpm: Int? = null,
    val recoverySeconds: Int = 300,
)

@Immutable
data class ActivityRecordingState(
    val status: ActivityRecordingStatus = ActivityRecordingStatus.IDLE,
    val recordingKind: ActivityRecordingKind = ActivityRecordingKind.GPS_ROUTE,
    val activityTypeId: String? = null,
    val exerciseType: Int? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val pausedStartedAt: Instant? = null,
    val totalPausedMillis: Long = 0L,
    val pauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val points: List<ExerciseRoutePoint> = emptyList(),
    val routeBreakIndexes: List<Int> = emptyList(),
    val manualLaps: List<ActivityRecordingLap> = emptyList(),
    val markers: List<ActivityRecordingMarker> = emptyList(),
    val latestUiPoint: ExerciseRoutePoint? = null,
    val distanceMeters: Double = 0.0,
    val elevationGainedMeters: Double = 0.0,
    val elevationLostMeters: Double = 0.0,
    val barometerElevationGainedMeters: Double = 0.0,
    val barometerElevationLostMeters: Double = 0.0,
    val hasBarometerElevation: Boolean = false,
    val lastBarometerAltitudeMeters: Double? = null,
    val currentSpeedMetersPerSecond: Double = 0.0,
    val maxSpeedMetersPerSecond: Double = 0.0,
    val gpsStatus: ActivityGpsStatus = ActivityGpsStatus.WAITING_FOR_FIX,
    val keepScreenOnDuringRecording: Boolean = ActivityRecordingPreferences.DefaultKeepScreenOnDuringRecording,
    val autoIdleEnabled: Boolean = ActivityRecordingPreferences.DefaultAutoIdleEnabled,
    val autoIdleTimeoutMillis: Long = ActivityRecordingPreferences.DefaultAutoIdleTimeoutSeconds * 1_000L,
    val lastMovementAt: Instant? = null,
    val totalIdleMillis: Long = 0L,
    val repetitionCount: Long = 0L,
    val currentSetRepetitionCount: Long = 0L,
    val repetitionSets: List<ActivityRecordedRepetitionSet> = emptyList(),
    val repetitionRestSeconds: Long = 0L,
    val currentSetStartedAt: Instant? = null,
    val restStartedAt: Instant? = null,
    val accumulatedRestMillis: Long = 0L,
    /** The plan being walked through, if any. */
    val planId: String? = null,
    val planTitle: String? = null,
    val planSteps: List<ActivityPlanRunStep> = emptyList(),
    /** Index of the step in progress; == planSteps.size once every step is done. */
    val planStepIndex: Int = 0,
    val lastAccuracyMeters: Double? = null,
    val lastLocationTime: Instant? = null,
    val droppedPointCount: Int = 0,
    val errorMessage: String? = null,
    val currentHeartRateBpm: Long? = null,
    val currentCyclingCadenceRpm: Long? = null,
    val currentPowerWatts: Double? = null,
    val currentSensorSpeedMetersPerSecond: Double? = null,
    val currentRunningCadenceRpm: Long? = null,
    val bleHeartRateNoSignal: Boolean = false,
    val bleDeviceStatuses: List<BleDeviceConnectionStatus> = emptyList(),
    val dashboardLayout: ActivityRecordingDashboardLayout = ActivityRecordingDashboardLayout(),
    val hrrPhase: ActivityRecordingHrrPhase = ActivityRecordingHrrPhase.NONE,
    val hrrConfig: HeartRateRecoveryTestConfig = HeartRateRecoveryTestConfig(),
    /**
     * The instant the effort stopped. Not persisted: the heart-rate samples do
     * not survive the process, and a rest segment claiming a recovery with no
     * heart rate behind it would be a fabricated measurement.
     */
    val hrrEffortEndedAt: Instant? = null,
) {
    val isActive: Boolean
        get() = status == ActivityRecordingStatus.RECORDING ||
            status == ActivityRecordingStatus.RESTING ||
            status == ActivityRecordingStatus.PAUSED

    val isHeartRateRecoveryTest: Boolean
        get() = hrrPhase != ActivityRecordingHrrPhase.NONE

    /** What the current phase is counting down to, or null when nothing is. */
    fun hrrPhaseRemaining(now: Instant = Instant.now()): Duration? = when (hrrPhase) {
        ActivityRecordingHrrPhase.WARMUP -> {
            val from = startTime
            if (from == null || hrrConfig.warmupSeconds <= 0) {
                null
            } else {
                remainingUntil(from.plusSeconds(hrrConfig.warmupSeconds.toLong()), now)
            }
        }
        ActivityRecordingHrrPhase.RECOVERY -> {
            hrrEffortEndedAt?.let { from ->
                remainingUntil(from.plusSeconds(hrrConfig.recoverySeconds.toLong()), now)
            }
        }
        // The effort has no deadline, and COMPLETE is the end.
        ActivityRecordingHrrPhase.NONE,
        ActivityRecordingHrrPhase.EFFORT,
        ActivityRecordingHrrPhase.COMPLETE,
        -> null
    }

    private fun remainingUntil(until: Instant, now: Instant): Duration {
        val left = Duration.between(now, until)
        return if (left.isNegative) Duration.ZERO else left
    }
}

@Immutable
data class ActivityRecordedRepetitionSet(
    val repetitions: Long,
    val restSeconds: Long,
    val activeMillis: Long,
    /** The exercise, when it is not the session type's own. */
    val segmentType: Int? = null,
    val label: String? = null,
    /** A timed hold: [activeMillis] is the goal. */
    val isDuration: Boolean = false,
    /** Which plan step produced this set, for Back. */
    val planStepIndex: Int? = null,
)

@Immutable
data class ActivityRecordingSnapshot(
    val exerciseType: Int,
    val recordingKind: ActivityRecordingKind = ActivityRecordingKind.GPS_ROUTE,
    val activityTypeId: String? = null,
    val startTime: Instant,
    val endTime: Instant,
    val points: List<ExerciseRoutePoint>,
    val pauseIntervals: List<ActivityPauseInterval>,
    val routeBreakIndexes: List<Int> = emptyList(),
    val manualLaps: List<ActivityRecordingLap> = emptyList(),
    val markers: List<ActivityRecordingMarker> = emptyList(),
    val distanceMeters: Double,
    val elevationGainedMeters: Double,
    val repetitionCount: Long = 0L,
    val repetitionSets: List<ActivityRecordedRepetitionSet> = emptyList(),
    val planId: String? = null,
    val planTitle: String? = null,
    val bleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    /** When the effort stopped in a recovery test. Written as a trailing rest segment. */
    val hrrEffortEndedAt: Instant? = null,
    /** CoMaps guidance banked during the recording. App-local only; empty unless enabled. */
    val coMapsNavigationSamples: List<CoMapsNavigationSnapshot> = emptyList(),
)

@Singleton
class ActivityRecordingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val bleSensorCoordinator: BleSensorCoordinator,
    private val coMapsNavigationRepository: CoMapsNavigationRepository,
    private val coMapsGuidanceFeed: CoMapsGuidanceFeed,
    private val recordingStore: ActivityRecordingStore = ActivityRecordingStore(context),
) {
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bleMetricsScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationProcessingDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val locationProcessingScope = CoroutineScope(SupervisorJob() + locationProcessingDispatcher)
    private val altitudeConverter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        AltitudeConverter()
    } else {
        null
    }
    private val _state = MutableStateFlow(recordingStore.restore())
    private var recordingGeneration = 0L
    private var restCompletionJob: Job? = null
    private var planStepJob: Job? = null
    val state: StateFlow<ActivityRecordingState> = _state.asStateFlow()

    private val coMapsPrestartWatchRequested = MutableStateFlow(false)

    private val coMapsWatch = CoMapsRecordingWatch(
        repository = coMapsNavigationRepository,
        feed = coMapsGuidanceFeed,
        scope = bleMetricsScope,
        isEnabled = {
            preferencesRepository.activityRecordingPreferences().coMapsNavigationContextEnabled
        },
        isSavingEnabled = {
            preferencesRepository.activityRecordingPreferences().saveCoMapsNavigationContext
        },
        isPrestartWatchRequested = { coMapsPrestartWatchRequested.value },
    )

    /** Live CoMaps guidance, Disabled whenever the watch is not running. */
    val coMapsNavigation: StateFlow<CoMapsNavigationState> = coMapsWatch.navigation

    /** The route CoMaps is guiding along, never persisted. */
    val coMapsRoute: StateFlow<CoMapsRoutePolyline?> = coMapsWatch.route

    init {
        scheduleRestCompletion(_state.value)
        schedulePlanStepCompletion(_state.value)
        bleSensorCoordinator.metrics
            .onEach { metrics -> acceptBleMetrics(metrics) }
            .launchIn(bleMetricsScope)
        state
            .onEach { recording -> coMapsWatch.sync(recording) }
            .launchIn(bleMetricsScope)
        coMapsPrestartWatchRequested
            .onEach { coMapsWatch.sync(_state.value) }
            .launchIn(bleMetricsScope)
    }

    /** Re-reads guidance after a permission grant. */
    fun refreshCoMapsGuidance() {
        coMapsWatch.refresh()
    }

    /** Hands the map to CoMaps so the user can plan a route on it. */
    fun planInCoMaps() {
        val point = _state.value.points.lastOrNull()
        coMapsNavigationRepository.launchForPlanning(point?.latitude, point?.longitude)
    }

    /** The flavour-specific CoMaps permission to request, null without a CoMaps installed. */
    fun coMapsPermissionName(): String? = coMapsNavigationRepository.permissionName()

    /** Armed by the pre-start screen only. Lets a route set in CoMaps auto-start the recording. */
    fun setCoMapsPrestartWatch(active: Boolean) {
        coMapsPrestartWatchRequested.value = active
    }

    fun startRecording(activityType: ActivityEntryType, initialFix: Location?): Boolean =
        startRecording(activityType, initialFix, repetitionRestSeconds = 0L)

    fun startRecording(
        activityType: ActivityEntryType,
        initialFix: Location?,
        repetitionRestSeconds: Long,
        // Record a GPS-capable activity without GPS: duration and heart rate only.
        withoutGps: Boolean = false,
    ): Boolean =
        if (activityType.supportsGpsRoute && !withoutGps) {
            startGpsRecording(activityType, initialFix)
        } else if (activityType.supportsGpsRoute && withoutGps) {
            startTimedRecording(activityType)
        } else if (activityType.isRepetitionLike) {
            startRepetitionRecording(activityType, repetitionRestSeconds)
        } else if (activityType.recordingSensor == ActivityRecordingSensor.BLE) {
            startTimedRecording(activityType)
        } else {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_unsupported_type),
                )
            )
            false
        }

    fun startRecording(exerciseType: Int, initialFix: Location?): Boolean {
        val activityType = DefaultActivityEntryTypes.firstOrNull { it.exerciseType == exerciseType && it.supportsGpsRoute }
            ?: return false
        return startRecording(activityType, initialFix)
    }

    fun prepareRecordingDashboard(activityType: ActivityEntryType) {
        if (_state.value.isActive) return
        val recordingKind = activityType.recordingKind()
        persistenceScope.coroutineContext.cancelChildren()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.IDLE,
                recordingKind = recordingKind,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id),
            ),
            replaceRoutePoints = true,
        )
        previewBleConnections()
    }

    fun updateDashboardLayout(layout: ActivityRecordingDashboardLayout) {
        val current = _state.value
        val activityTypeId = current.activityTypeId ?: return
        if (current.status == ActivityRecordingStatus.RECORDING) return
        val normalized = layout.normalized()
        preferencesRepository.setActivityRecordingDashboardLayout(activityTypeId, normalized)
        updateAndPersist(current.copy(dashboardLayout = normalized, errorMessage = null))
    }

    fun clearPreparedRecording() {
        if (_state.value.isActive) return
        clearRecording()
    }

    private fun startGpsRecording(activityType: ActivityEntryType, initialFix: Location?): Boolean {
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id)
        if (!hasPreciseLocationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_precise_location_permission),
                )
            )
            return false
        }
        if (!hasNotificationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_notification_permission),
                )
            )
            return false
        }
        val now = Instant.now()
        val lockedFix = initialFix
        val initialFixQuality = lockedFix?.activityGpsFixQuality(
            now = now,
            requiredAccuracyMeters = recordingPreferences.requiredGpsAccuracyMeters.toDouble(),
        )
        if (lockedFix == null || initialFixQuality?.isPrecise != true) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_waiting_for_gps),
                )
            )
            return false
        }

        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.GPS_ROUTE,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                startTime = now,
                gpsStatus = ActivityGpsStatus.FIX,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                autoIdleEnabled = recordingPreferences.autoIdleEnabled,
                autoIdleTimeoutMillis = recordingPreferences.autoIdleTimeoutSeconds * 1_000L,
                lastMovementAt = now,
                lastAccuracyMeters = initialFixQuality.accuracyMeters,
                lastLocationTime = now,
                dashboardLayout = dashboardLayout,
            ),
            replaceRoutePoints = true,
        )
        acceptLocation(Location(lockedFix).apply { time = now.toEpochMilli() })
        bleSensorCoordinator.startRecording()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
    }

    /** Walks a plan inside a repetition recording. The cursor rides in the state and survives a kill. */
    fun startPlanRecording(plan: PlannedExerciseData, activityType: ActivityEntryType): Boolean {
        val steps = plan.toPlanRunSteps(localizedTitle = { context.getString(it.labelRes) })
        if (steps.isEmpty()) {
            updateAndPersist(
                _state.value.copy(errorMessage = context.getString(R.string.activity_recording_error_plan_empty)),
            )
            return false
        }
        val started = startRepetitionRecording(
            activityType = activityType,
            repetitionRestSeconds = steps.first().restSeconds,
            planSeed = PlanSeed(plan.id, plan.title, steps),
        )
        if (started) cuePlanStep(steps.first(), withBell = true)
        return started
    }

    private data class PlanSeed(val id: String, val title: String?, val steps: List<ActivityPlanRunStep>)

    private fun startRepetitionRecording(
        activityType: ActivityEntryType,
        repetitionRestSeconds: Long,
        planSeed: PlanSeed? = null,
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_notification_permission),
                )
            )
            return false
        }

        val now = Instant.now()
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id)
        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.REPETITION,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                startTime = now,
                // A plan run is read from the floor; a dark screen leaves only the bell.
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording || planSeed != null,
                currentSetStartedAt = now,
                repetitionRestSeconds = repetitionRestSeconds.coerceAtLeast(0L),
                planId = planSeed?.id,
                planTitle = planSeed?.title,
                planSteps = planSeed?.steps.orEmpty(),
                planStepIndex = 0,
                dashboardLayout = dashboardLayout,
            ),
            replaceRoutePoints = true,
        )
        bleSensorCoordinator.startRecording()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
    }

    private fun startTimedRecording(activityType: ActivityEntryType): Boolean {
        if (!hasNotificationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_notification_permission),
                )
            )
            return false
        }

        val now = Instant.now()
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id)
        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.TIMED,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                startTime = now,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                dashboardLayout = dashboardLayout,
            ),
            replaceRoutePoints = true,
        )
        bleSensorCoordinator.startRecording()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
    }

    // Heart-rate recovery test: a timed recording with a protocol over it.
    // Warm up, go hard, then stop dead. The recorded abrupt stop is the point.

    private var hrrPhaseJob: Job? = null

    /** Consecutive samples at or above the target. One spurious reading must not end the effort. */
    private var hrrTargetHits = 0

    /** Starts a guided recovery test: warm up, go hard, stop dead. */
    fun startHeartRateRecoveryTest(
        activityType: ActivityEntryType,
        config: HeartRateRecoveryTestConfig,
    ): Boolean {
        if (!startTimedRecording(activityType)) return false
        hrrTargetHits = 0
        updateAndPersist(
            _state.value.copy(
                hrrPhase = if (config.warmupSeconds > 0) {
                    ActivityRecordingHrrPhase.WARMUP
                } else {
                    ActivityRecordingHrrPhase.EFFORT
                },
                hrrConfig = config,
            )
        )
        scheduleHrrPhase(_state.value)
        return true
    }

    /** Ends the effort and starts the recovery now. Always available during the effort. */
    fun endHeartRateRecoveryEffort() {
        val current = _state.value
        if (current.hrrPhase != ActivityRecordingHrrPhase.WARMUP &&
            current.hrrPhase != ActivityRecordingHrrPhase.EFFORT
        ) {
            return
        }
        enterHrrRecovery(current)
    }

    /** Stamps the stop before the cue fires, so a slow speech engine cannot delay the clock. */
    private fun enterHrrRecovery(current: ActivityRecordingState) {
        hrrTargetHits = 0
        updateAndPersist(
            current.copy(
                hrrPhase = ActivityRecordingHrrPhase.RECOVERY,
                hrrEffortEndedAt = Instant.now(),
            )
        )
        cueHrr(context.getString(R.string.activity_recording_hrr_cue_slow_down))
        scheduleHrrPhase(_state.value)
    }

    private fun scheduleHrrPhase(state: ActivityRecordingState) {
        hrrPhaseJob?.cancel()
        hrrPhaseJob = null

        val deadline = when (state.hrrPhase) {
            ActivityRecordingHrrPhase.WARMUP ->
                state.startTime?.plusSeconds(state.hrrConfig.warmupSeconds.toLong())
            ActivityRecordingHrrPhase.RECOVERY ->
                state.hrrEffortEndedAt?.plusSeconds(state.hrrConfig.recoverySeconds.toLong())
            // The effort has no deadline, and COMPLETE is the end.
            else -> null
        } ?: return

        val delayMillis = Duration.between(Instant.now(), deadline).toMillis().coerceAtLeast(0L)
        val scheduledPhase = state.hrrPhase
        val scheduledEffortEnd = state.hrrEffortEndedAt

        hrrPhaseJob = persistenceScope.launch {
            delay(delayMillis)
            val current = _state.value
            // A timer for a test that has already moved on must do nothing.
            if (current.hrrPhase != scheduledPhase || current.hrrEffortEndedAt != scheduledEffortEnd) {
                return@launch
            }
            hrrPhaseJob = null

            if (scheduledPhase == ActivityRecordingHrrPhase.WARMUP) {
                updateAndPersist(current.copy(hrrPhase = ActivityRecordingHrrPhase.EFFORT))
                cueHrr(context.getString(R.string.activity_recording_hrr_cue_effort))
            } else if (scheduledPhase == ActivityRecordingHrrPhase.RECOVERY) {
                // Not auto-finished: the rest segment runs to the session end, so a
                // slow save cannot break the measurement.
                updateAndPersist(current.copy(hrrPhase = ActivityRecordingHrrPhase.COMPLETE))
                cueHrr(context.getString(R.string.activity_recording_hrr_cue_complete))
            }
        }
    }

    /** Ends the effort when the heart rate reaches the target, if one was set. */
    private fun maybeEndHrrEffortOnTarget(state: ActivityRecordingState) {
        if (state.hrrPhase != ActivityRecordingHrrPhase.EFFORT) {
            hrrTargetHits = 0
            return
        }
        val target = state.hrrConfig.targetHeartRateBpm ?: return
        val bpm = state.currentHeartRateBpm ?: return
        if (bpm < target) {
            hrrTargetHits = 0
            return
        }
        hrrTargetHits += 1
        if (hrrTargetHits >= HrrTargetHitsToEndEffort) {
            enterHrrRecovery(state)
        }
    }

    /**
     * Bell, voice and a hard buzz, not behind a preference. The cue is the
     * protocol: a "stop now" that went unheard measured nothing.
     */
    private fun cueHrr(text: String) {
        playBell()
        speakCue(text)
        vibrate(HrrCueVibrationMillis)
    }

    /** A plan step change: the rest bell, a short buzz, and the next step spoken if voice is on. */
    private fun cuePlanStep(step: ActivityPlanRunStep, withBell: Boolean) {
        if (withBell) playRestTimerBellIfEnabled()
        vibrate(PlanStepCueVibrationMillis)
        if (preferencesRepository.activityRecordingPreferences().voiceAnnouncementsEnabled) {
            speakCue(
                listOfNotNull(
                    context.getString(R.string.activity_recording_plan_cue_next, step.spokenGoal(context)),
                    // The pace announcer never speaks for repetition sessions.
                    _state.value.currentHeartRateBpm?.let { context.getString(R.string.activity_recording_plan_cue_heart_rate, it) },
                ).joinToString(". "),
            )
        }
    }

    private var restCountdownJob: Job? = null

    /** One beep per second for the last [RestCountdownSeconds] of a rest. Same gate as the bell. */
    private fun scheduleRestCountdown(state: ActivityRecordingState) {
        restCountdownJob?.cancel()
        restCountdownJob = null
        if (!state.isPlanRun) return
        val restEnd = state.restEndTime() ?: return
        val scheduledStart = state.restStartedAt
        restCountdownJob = bleMetricsScope.launch {
            for (secondsLeft in RestCountdownSeconds downTo 1L) {
                val delayMillis = Duration.between(Instant.now(), restEnd.minusSeconds(secondsLeft)).toMillis()
                if (delayMillis < 0L) continue
                delay(delayMillis)
                val current = _state.value
                if (current.status != ActivityRecordingStatus.RESTING || current.restStartedAt != scheduledStart) return@launch
                if (preferencesRepository.activityRecordingPreferences().restTimerBellEnabled) playCountdownBeep()
                vibrate(CountdownVibrationMillis)
            }
        }
    }

    private fun playCountdownBeep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, CountdownBeepVolume)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, CountdownBeepMillis)
            bleMetricsScope.launch {
                delay(CountdownBeepMillis.toLong() + 50L)
                runCatching { tone.release() }
            }
        }
    }

    private fun cuePlanRest(restSeconds: Long, next: ActivityPlanRunStep?, withBell: Boolean) {
        if (withBell) playRestTimerBellIfEnabled()
        vibrate(PlanStepCueVibrationMillis)
        if (preferencesRepository.activityRecordingPreferences().voiceAnnouncementsEnabled) {
            val text = if (next != null) {
                context.getString(R.string.activity_recording_plan_cue_rest, restSeconds, next.spokenGoal(context))
            } else {
                context.getString(R.string.activity_recording_plan_cue_rest_last, restSeconds)
            }
            speakCue(text)
        }
    }

    private fun cuePlanComplete() {
        playRestTimerBellIfEnabled()
        vibrate(PlanStepCueVibrationMillis)
        if (preferencesRepository.activityRecordingPreferences().voiceAnnouncementsEnabled) {
            speakCue(context.getString(R.string.activity_recording_plan_cue_complete))
        }
    }

    private var hrrCueTts: TextToSpeech? = null

    private fun speakCue(text: String) {
        runCatching {
            hrrCueTts?.shutdown()
            var tts: TextToSpeech? = null
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    tts?.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "openvitals_hrr_${System.nanoTime()}",
                    )
                }
            }
            hrrCueTts = tts
        }
    }

    private fun vibrate(millis: Long) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(
                VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

    fun previewBleConnections() {
        if (_state.value.isActive) return
        bleSensorCoordinator.refreshConnections()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
    }

    fun stopBlePreview() {
        if (_state.value.isActive) return
        bleSensorCoordinator.disconnectAll()
    }

    fun acceptBleMetrics(metrics: BleRecordingMetrics) {
        val current = _state.value
        acceptBleMetricsInternal(current, metrics)
        // Against the heart rate just taken.
        maybeEndHrrEffortOnTarget(_state.value)
    }

    private fun acceptBleMetricsInternal(current: ActivityRecordingState, metrics: BleRecordingMetrics) {
        val next = current.copy(
            currentHeartRateBpm = metrics.heartRateBpm,
            currentCyclingCadenceRpm = metrics.cyclingCadenceRpm,
            currentPowerWatts = metrics.powerWatts,
            currentSensorSpeedMetersPerSecond = metrics.cyclingSpeedMetersPerSecond
                ?: metrics.runningSpeedMetersPerSecond,
            currentRunningCadenceRpm = metrics.runningCadenceRpm,
            bleHeartRateNoSignal = metrics.heartRateNoSignal && metrics.heartRateBpm == null,
            bleDeviceStatuses = metrics.deviceStatuses.ifEmpty { current.bleDeviceStatuses },
        )
        // BLE sensors re-emit unchanged values; do not re-serialize for them.
        if (next == current) return
        updateAndPersist(next, throttlePersist = true)
    }

    fun pauseRecording() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) return
        updateAndPersist(
            current.copy(
                status = ActivityRecordingStatus.PAUSED,
                pausedStartedAt = Instant.now(),
                errorMessage = null,
            )
        )
    }

    fun resumeRecording() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.PAUSED) return
        val now = Instant.now()
        val pausedMillis = current.pausedStartedAt
            ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
            ?: 0L
        val closedPause = current.pausedStartedAt?.toPauseInterval(now)
        updateAndPersist(
            current.copy(
                status = ActivityRecordingStatus.RECORDING,
                pausedStartedAt = null,
                totalPausedMillis = current.totalPausedMillis + pausedMillis,
                pauseIntervals = current.pauseIntervals + listOfNotNull(closedPause),
                // A paused plank stops counting down.
                currentSetStartedAt = if (current.isPlanRun) {
                    current.currentSetStartedAt?.plusMillis(pausedMillis)
                } else {
                    current.currentSetStartedAt
                },
                errorMessage = null,
            )
        )
    }

    fun addManualLap() {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        val start = current.manualLaps.maxByOrNull { it.endTime }?.endTime
            ?: current.startTime
            ?: return
        val end = Instant.now()
        if (!start.isBefore(end)) return
        val distanceMeters = activityRecordingRouteDistanceMeters(
            points = current.points,
            routeBreakIndexes = current.routeBreakIndexes,
            startTime = start,
            endTime = end,
        ).takeIf { it > 0.0 }
        updateAndPersist(
            current.copy(
                manualLaps = current.manualLaps + ActivityRecordingLap(
                    startTime = start,
                    endTime = end,
                    distanceMeters = distanceMeters,
                ),
                errorMessage = null,
            )
        )
    }

    fun addMarker() {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        val point = current.latestUiPoint ?: current.points.lastOrNull() ?: return
        val markerNumber = current.markers.size + 1
        updateAndPersist(
            current.copy(
                markers = current.markers + ActivityRecordingMarker(
                    id = UUID.randomUUID().toString(),
                    time = point.time,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitudeMeters = point.altitudeMeters,
                    name = context.getString(R.string.activity_entry_recording_marker_default_name, markerNumber),
                    type = ActivityRecordingMarkerType.Generic.value,
                ),
                errorMessage = null,
            )
        )
    }

    fun updateMarker(marker: ActivityRecordingMarker) {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(
            current.copy(
                markers = current.markers.map { existing ->
                    if (existing.id == marker.id) marker else existing
                },
                errorMessage = null,
            )
        )
    }

    fun deleteMarker(markerId: String) {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(
            current.copy(
                markers = current.markers.filterNot { it.id == markerId },
                errorMessage = null,
            )
        )
    }

    fun discardRecording() {
        bleSensorCoordinator.stopRecording()
        clearRecording()
        stopRecordingService()
    }

    fun finishRecording(): ActivityRecordingSnapshot? {
        val current = _state.value
        val start = current.startTime ?: return null
        val exerciseType = current.exerciseType ?: return null
        if (!current.isActive) return null

        val end = Instant.now().takeIf { it.isAfter(start) } ?: start.plusSeconds(1)
        val pauseIntervals = current.pauseIntervals +
            listOfNotNull(current.pausedStartedAt?.toPauseInterval(end))
        val manualLaps = current.closedManualLaps(end)
        val repetitionSets = current.recordedRepetitionSets(end)
        val bleSamples = bleSensorCoordinator.stopRecording()
        // Taken before clearing: going inactive resets the recorder.
        val coMapsSamples = coMapsWatch.samples()
        val snapshot = ActivityRecordingSnapshot(
            exerciseType = exerciseType,
            recordingKind = current.recordingKind,
            activityTypeId = current.activityTypeId,
            startTime = start,
            endTime = end,
            points = current.points,
            pauseIntervals = pauseIntervals,
            routeBreakIndexes = current.routeBreakIndexes,
            manualLaps = manualLaps,
            markers = current.markers,
            distanceMeters = current.distanceMeters,
            // The filtered figure the dashboard showed, not the raw running sum.
            elevationGainedMeters = current.displayElevationGainedMeters(),
            repetitionCount = current.repetitionCount,
            repetitionSets = repetitionSets,
            planId = current.planId,
            planTitle = current.planTitle,
            bleSamples = bleSamples,
            // Only with heart rate behind it, or the rest segment claims a measurement never taken.
            hrrEffortEndedAt = if (bleSamples.heartRateSamples.isEmpty()) {
                null
            } else {
                current.hrrEffortEndedAt
            },
            coMapsNavigationSamples = coMapsSamples,
        )
        clearRecording()
        stopRecordingService()
        return snapshot
    }

    fun acceptLocation(location: Location) {
        val generation = recordingGeneration
        updateGpsStatus(location)
        locationProcessingScope.launch {
            acceptConvertedLocation(
                location = location.withMslAltitude(),
                generation = generation,
                recordingPreferences = preferencesRepository.activityRecordingPreferences(),
            )
        }
    }

    private fun acceptConvertedLocation(
        location: Location,
        generation: Long,
        recordingPreferences: ActivityRecordingPreferences,
    ) {
        if (generation != recordingGeneration) return
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) return
        if (current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return

        val fixQuality = location.activityGpsFixQuality(
            startTime = current.startTime,
            requiredAccuracyMeters = recordingPreferences.requiredGpsAccuracyMeters.toDouble(),
        )
        val point = location.toRoutePoint(fixQuality.locationTime)
        if (!fixQuality.isPrecise) {
            updateAndPersist(
                current.withDroppedLocation(
                    fixQuality.accuracyMeters,
                    fixQuality.locationTime,
                    gpsStatus = if (fixQuality.accuracyMeters == null) {
                        ActivityGpsStatus.WAITING_FOR_FIX
                    } else {
                        ActivityGpsStatus.POOR_ACCURACY
                    },
                ).copy(latestUiPoint = point)
            )
            return
        }
        val accuracy = fixQuality.accuracyMeters ?: return

        val next = current.withAcceptedLocation(
            point = point,
            accuracyMeters = accuracy,
            recordingPreferences = recordingPreferences,
        )
        if (next.points.size <= current.points.size) {
            // Dropped, or shown live but not banked: nothing new to append.
            updateAndPersist(next)
            return
        }
        updateAndPersist(
            next,
            routePointToAppend = point,
            // The route grows every fix, so the full metadata re-serialize is throttled.
            throttlePersist = true,
        )
    }

    fun acceptRecognizedRepetition() {
        adjustRepetitionCount(1)
    }

    fun adjustRepetitionCount(delta: Long) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) {
            return
        }
        if (current.recordingKind == ActivityRecordingKind.GPS_ROUTE) {
            if (activityEntryTypeById(current.activityTypeId)?.supportsStepCounting != true) return
            val nextCount = (current.repetitionCount + delta).coerceAtLeast(0L)
            updateAndPersist(
                current.copy(
                    currentSetRepetitionCount = nextCount,
                    repetitionCount = nextCount,
                    errorMessage = null,
                )
            )
            return
        }
        if (current.recordingKind != ActivityRecordingKind.REPETITION) return

        val nextCurrentSetCount = (current.currentSetRepetitionCount + delta).coerceAtLeast(0L)
        val completedCount = current.repetitionSets.sumOf { it.repetitions }
        val next = current.copy(
            currentSetRepetitionCount = nextCurrentSetCount,
            repetitionCount = completedCount + nextCurrentSetCount,
            errorMessage = null,
        )
        // Reaching the target is "done"; counting past it via "+" is done too.
        val step = next.currentPlanStep
        if (delta > 0L && step != null && step.goalKind == ActivityPlanGoalKind.REPS && nextCurrentSetCount >= step.goalValue) {
            completePlanStep(next, Instant.now())
            return
        }
        updateAndPersist(next)
    }

    /** "Done" for the step in progress. A rep step with nothing counted is skipped. */
    fun completeCurrentPlanStep() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || !current.isPlanRun || current.isPlanComplete) return
        completePlanStep(current, Instant.now())
    }

    /** Back one step: the step just finished is reopened with its count restored. */
    fun undoPlanStep() {
        val current = _state.value
        if (!current.isPlanRun || current.status == ActivityRecordingStatus.PAUSED || !current.isActive) return
        val now = Instant.now()
        val previousIndex = when {
            current.isPlanComplete -> current.planSteps.lastIndex
            current.status == ActivityRecordingStatus.RESTING -> current.planStepIndex - 1
            else -> current.planStepIndex - 1
        }
        if (previousIndex < 0) return
        val previousStep = current.planSteps[previousIndex]
        val lastSet = current.repetitionSets.lastOrNull()?.takeIf { it.planStepIndex == previousIndex }
        val sets = if (lastSet != null) current.repetitionSets.dropLast(1) else current.repetitionSets
        val restoredCount = lastSet?.takeIf { !it.isDuration }?.repetitions ?: 0L
        updateAndPersist(
            current.copy(
                status = ActivityRecordingStatus.RECORDING,
                repetitionSets = sets,
                repetitionCount = sets.sumOf { it.repetitions } + restoredCount,
                currentSetRepetitionCount = restoredCount,
                accumulatedRestMillis = current.accumulatedRestMillis + current.openRestMillis(now),
                restStartedAt = null,
                currentSetStartedAt = now,
                repetitionRestSeconds = previousStep.restSeconds,
                planStepIndex = previousIndex,
                errorMessage = null,
            ),
        )
        cuePlanStep(previousStep, withBell = false)
    }

    fun skipPlanStep() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || !current.isPlanRun || current.isPlanComplete) return
        advancePlanStep(current, completedSet = null, now = Instant.now(), restAfter = 0L)
    }

    private fun completePlanStep(state: ActivityRecordingState, now: Instant) {
        val step = state.currentPlanStep ?: return
        val isTimed = step.goalKind == ActivityPlanGoalKind.SECONDS
        if (!isTimed && state.currentSetRepetitionCount <= 0L) {
            advancePlanStep(state, completedSet = null, now = now, restAfter = 0L)
            return
        }
        val activeMillis = Duration.between(state.currentSetStartedAt ?: state.startTime ?: now, now)
            .toMillis()
            .coerceAtLeast(1L)
        val completedSet = ActivityRecordedRepetitionSet(
            repetitions = if (isTimed) 0L else state.currentSetRepetitionCount,
            restSeconds = step.restSeconds,
            activeMillis = activeMillis,
            segmentType = step.segmentType,
            label = step.label,
            isDuration = isTimed,
            planStepIndex = state.planStepIndex,
        )
        advancePlanStep(state, completedSet, now, restAfter = step.restSeconds)
    }

    private fun advancePlanStep(
        state: ActivityRecordingState,
        completedSet: ActivityRecordedRepetitionSet?,
        now: Instant,
        restAfter: Long,
    ) {
        val nextIndex = state.planStepIndex + 1
        val sets = state.repetitionSets + listOfNotNull(completedSet)
        val completedCount = sets.sumOf { it.repetitions }
        val next = state.planSteps.getOrNull(nextIndex)
        when {
            next == null -> {
                // The last step has nothing to rest for.
                updateAndPersist(
                    state.copy(
                        status = ActivityRecordingStatus.RECORDING,
                        repetitionSets = sets.withLastRestSeconds(0L),
                        repetitionCount = completedCount,
                        currentSetRepetitionCount = 0L,
                        currentSetStartedAt = null,
                        restStartedAt = null,
                        planStepIndex = state.planSteps.size,
                        errorMessage = null,
                    ),
                )
                cuePlanComplete()
            }
            restAfter > 0L -> {
                updateAndPersist(
                    state.copy(
                        status = ActivityRecordingStatus.RESTING,
                        repetitionSets = sets,
                        repetitionCount = completedCount,
                        currentSetRepetitionCount = 0L,
                        restStartedAt = now,
                        // This step's rest, not the plan's first.
                        repetitionRestSeconds = restAfter,
                        currentSetStartedAt = null,
                        planStepIndex = nextIndex,
                        errorMessage = null,
                    ),
                )
                cuePlanRest(restAfter, next, withBell = true)
            }
            else -> {
                updateAndPersist(
                    state.copy(
                        status = ActivityRecordingStatus.RECORDING,
                        repetitionSets = sets,
                        repetitionCount = completedCount,
                        currentSetRepetitionCount = 0L,
                        restStartedAt = null,
                        repetitionRestSeconds = next.restSeconds,
                        currentSetStartedAt = now,
                        planStepIndex = nextIndex,
                        errorMessage = null,
                    ),
                )
                cuePlanStep(next, withBell = true)
            }
        }
    }

    fun endRepetitionSet() {
        val current = _state.value
        if (current.isPlanRun) {
            completeCurrentPlanStep()
            return
        }
        if (current.status != ActivityRecordingStatus.RECORDING ||
            current.recordingKind != ActivityRecordingKind.REPETITION ||
            current.currentSetRepetitionCount <= 0L
        ) {
            return
        }
        val now = Instant.now()
        val activeMillis = Duration.between(current.currentSetStartedAt ?: current.startTime ?: now, now)
            .toMillis()
            .coerceAtLeast(1L)
        val completedSet = ActivityRecordedRepetitionSet(
            repetitions = current.currentSetRepetitionCount,
            restSeconds = current.repetitionRestSeconds,
            activeMillis = activeMillis,
        )
        val nextState = if (current.repetitionRestSeconds > 0L) {
            current.copy(
                status = ActivityRecordingStatus.RESTING,
                repetitionSets = current.repetitionSets + completedSet,
                currentSetRepetitionCount = 0L,
                restStartedAt = now,
                currentSetStartedAt = null,
                errorMessage = null,
            )
        } else {
            current.copy(
                repetitionSets = current.repetitionSets + completedSet,
                currentSetRepetitionCount = 0L,
                currentSetStartedAt = now,
                errorMessage = null,
            )
        }
        updateAndPersist(nextState)
    }

    fun startNextRepetitionSet() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RESTING ||
            current.recordingKind != ActivityRecordingKind.REPETITION
        ) {
            return
        }
        startNextRepetitionSet(current, Instant.now())
    }

    fun reportRecordingError(message: String) {
        updateAndPersist(_state.value.copy(errorMessage = message))
    }

    fun reportGpsDisabled() {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(current.copy(gpsStatus = ActivityGpsStatus.DISABLED))
    }

    fun reportGpsLost() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(current.copy(gpsStatus = ActivityGpsStatus.LOST))
    }

    fun acceptBarometerPressure(pressureHpa: Float) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        if (!preferencesRepository.activityRecordingPreferences().barometerClimbEnabled) return

        val altitudeMeters = android.hardware.SensorManager.getAltitude(
            android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
            pressureHpa,
        ).toDouble()
        val smoothedAltitude = current.lastBarometerAltitudeMeters?.let { previous ->
            previous + ((altitudeMeters - previous) * BarometerSmoothingAlpha)
        } ?: altitudeMeters
        val previousAltitude = current.lastBarometerAltitudeMeters
        if (previousAltitude == null) {
            updateAndPersist(
                current.copy(
                    hasBarometerElevation = true,
                    lastBarometerAltitudeMeters = smoothedAltitude,
                )
            )
            return
        }

        val delta = smoothedAltitude - previousAltitude
        val gainedMeters = if (delta >= MinBarometerElevationStepMeters) delta else 0.0
        val lostMeters = if (delta <= -MinBarometerElevationStepMeters) -delta else 0.0
        updateAndPersist(
            current.copy(
                hasBarometerElevation = true,
                barometerElevationGainedMeters = current.barometerElevationGainedMeters + gainedMeters,
                barometerElevationLostMeters = current.barometerElevationLostMeters + lostMeters,
                lastBarometerAltitudeMeters = if (gainedMeters > 0.0 || lostMeters > 0.0) {
                    smoothedAltitude
                } else {
                    previousAltitude
                },
            )
        )
    }

    private fun updateGpsStatus(location: Location) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val fixQuality = location.activityGpsFixQuality(
            startTime = current.startTime,
            requiredAccuracyMeters = recordingPreferences.requiredGpsAccuracyMeters.toDouble(),
        )
        val gpsStatus = when {
            fixQuality.isPrecise -> ActivityGpsStatus.FIX
            fixQuality.accuracyMeters == null -> ActivityGpsStatus.WAITING_FOR_FIX
            else -> ActivityGpsStatus.POOR_ACCURACY
        }
        updateAndPersist(
            current.copy(
                gpsStatus = gpsStatus,
                latestUiPoint = fixQuality.locationTime?.let { location.toRoutePoint(it) } ?: current.latestUiPoint,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                autoIdleEnabled = recordingPreferences.autoIdleEnabled,
                autoIdleTimeoutMillis = recordingPreferences.autoIdleTimeoutSeconds * 1_000L,
                lastAccuracyMeters = fixQuality.accuracyMeters ?: current.lastAccuracyMeters,
                lastLocationTime = fixQuality.locationTime ?: current.lastLocationTime,
            )
        )
    }

    private fun clearRecording() {
        persistenceScope.coroutineContext.cancelChildren()
        restCompletionJob?.cancel()
        planStepJob?.cancel()
        planStepJob = null
        restCountdownJob?.cancel()
        restCountdownJob = null
        hrrPhaseJob?.cancel()
        hrrPhaseJob = null
        hrrTargetHits = 0
        recordingGeneration += 1
        _state.value = ActivityRecordingState()
        recordingStore.clear()
    }

    private var lastMetadataPersistMillis = 0L

    private fun updateAndPersist(
        state: ActivityRecordingState,
        routePointToAppend: ExerciseRoutePoint? = null,
        replaceRoutePoints: Boolean = false,
        throttlePersist: Boolean = false,
    ) {
        _state.value = state
        persistMetadata(state, throttle = throttlePersist)
        scheduleRestCompletion(state)
        schedulePlanStepCompletion(state)
        scheduleRestCountdown(state)
        if (replaceRoutePoints) {
            persistenceScope.launch {
                recordingStore.replaceRoutePoints(state.points)
            }
        }
        routePointToAppend?.let { point ->
            persistenceScope.launch {
                recordingStore.appendRoutePoint(point)
            }
        }
    }

    /**
     * Metadata is re-serialized on every persist, route included, so sensor
     * updates throttle it to once per [MetadataPersistThrottleMillis]. A crash
     * loses at most that much from the restored state.
     */
    private fun persistMetadata(state: ActivityRecordingState, throttle: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (throttle && now - lastMetadataPersistMillis < MetadataPersistThrottleMillis) {
            return
        }
        lastMetadataPersistMillis = now
        recordingStore.storeMetadata(state)
    }

    private fun scheduleRestCompletion(state: ActivityRecordingState) {
        restCompletionJob?.cancel()
        restCompletionJob = null
        val restEnd = state.restEndTime() ?: return
        val now = Instant.now()
        val delayMillis = Duration.between(now, restEnd).toMillis().coerceAtLeast(0L)
        restCompletionJob = persistenceScope.launch {
            delay(delayMillis)
            val current = _state.value
            if (current.status == ActivityRecordingStatus.RESTING &&
                current.restStartedAt == state.restStartedAt
            ) {
                restCompletionJob = null
                playRestTimerBellIfEnabled()
                startNextRepetitionSet(current, Instant.now(), cueBell = false)
            }
        }
    }

    private fun playRestTimerBellIfEnabled() {
        if (!preferencesRepository.activityRecordingPreferences().restTimerBellEnabled) return
        playBell()
    }

    /**
     * The bell, with no preference in front of it. Ducks other audio rather
     * than taking focus, so music dips and comes back.
     */
    private fun playBell() {
        val player = runCatching { MediaPlayer.create(context, R.raw.bowl_struck) }.getOrNull()
            ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val focusRequest = audioManager?.let {
            runCatching {
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .build()
                    .also { request -> it.requestAudioFocus(request) }
            }.getOrNull()
        }
        fun releaseFocus() {
            focusRequest?.let { request ->
                runCatching { audioManager?.abandonAudioFocusRequest(request) }
            }
        }
        player.setOnCompletionListener { completedPlayer ->
            completedPlayer.release()
            releaseFocus()
        }
        player.setOnErrorListener { errorPlayer, _, _ ->
            errorPlayer.release()
            releaseFocus()
            true
        }
        runCatching {
            player.setVolume(RestTimerBellVolume, RestTimerBellVolume)
            player.start()
        }.onFailure {
            player.release()
            releaseFocus()
        }
    }

    private fun startNextRepetitionSet(state: ActivityRecordingState, now: Instant, cueBell: Boolean = true) {
        val actualRestMillis = state.openRestMillis(now)
        val updatedSets = state.repetitionSets.withLastRestSeconds((actualRestMillis / 1_000L).coerceAtLeast(0L))
        val nextStep = state.currentPlanStep
        val nextState = state.copy(
            status = ActivityRecordingStatus.RECORDING,
            repetitionSets = updatedSets,
            accumulatedRestMillis = state.accumulatedRestMillis + actualRestMillis,
            restStartedAt = null,
            currentSetStartedAt = now,
            // The next step's own rest.
            repetitionRestSeconds = nextStep?.restSeconds ?: state.repetitionRestSeconds,
            errorMessage = null,
        )
        updateAndPersist(nextState)
        if (nextStep != null) cuePlanStep(nextStep, withBell = cueBell)
    }

    /** A timed step ends by itself. The deadline is checked against the state it was scheduled for. */
    private fun schedulePlanStepCompletion(state: ActivityRecordingState) {
        planStepJob?.cancel()
        planStepJob = null
        val deadline = state.planStepEndTime() ?: return
        val scheduledIndex = state.planStepIndex
        val scheduledStart = state.currentSetStartedAt
        val delayMillis = Duration.between(Instant.now(), deadline).toMillis().coerceAtLeast(0L)
        planStepJob = bleMetricsScope.launch {
            delay(delayMillis)
            val current = _state.value
            if (current.status == ActivityRecordingStatus.RECORDING &&
                current.planStepIndex == scheduledIndex &&
                current.currentSetStartedAt == scheduledStart
            ) {
                planStepJob = null
                completeCurrentPlanStep()
            }
        }
    }

    private fun stopRecordingService() {
        context.stopService(Intent(context, ActivityRecordingService::class.java))
    }

    @SuppressLint("NewApi")
    private fun Location.withMslAltitude(): Location {
        val converted = Location(this)
        val converter = altitudeConverter ?: return converted
        if (!converted.hasAltitude()) return converted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                converter.addMslAltitudeToLocation(context, converted)
            }
        }
        return converted
    }

    companion object {
        fun hasPreciseLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        fun hasLocationPermission(context: Context): Boolean =
            hasPreciseLocationPermission(context) ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

        fun hasActivityRecognitionPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                ) == PackageManager.PERMISSION_GRANTED
    }
}

@Singleton
class ActivityRecordingStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(RecordingPreferencesName, Context.MODE_PRIVATE)
    private val routePointsFile = File(context.filesDir, RecordingRoutePointsFileName)

    fun restore(): ActivityRecordingState {
        val restored = preferences.restoreRecordingState()
        val filePoints = routePointsFile
            .takeIf { it.exists() }
            ?.readText()
            .orEmpty()
            .decodeRoutePoints()
        return if (filePoints.isNotEmpty()) {
            restored.copy(points = filePoints)
        } else {
            restored
        }
    }

    fun storeMetadata(state: ActivityRecordingState) {
        if (state.status == ActivityRecordingStatus.IDLE) {
            clear()
        } else {
            preferences.storeRecordingMetadata(state)
        }
    }

    fun replaceRoutePoints(points: List<ExerciseRoutePoint>) {
        if (points.isEmpty()) {
            routePointsFile.delete()
        } else {
            routePointsFile.writeText(points.encodeRoutePoints())
        }
        preferences.edit {
            putString(KeyPoints, points.encodeRoutePoints())
        }
    }

    fun appendRoutePoint(point: ExerciseRoutePoint) {
        routePointsFile.parentFile?.mkdirs()
        routePointsFile.appendText(point.encodeRoutePoint() + "\n")
    }

    fun clear() {
        preferences.edit {
            clear()
        }
        routePointsFile.delete()
    }
}

internal fun ActivityEntryType.recordingKind(): ActivityRecordingKind =
    when {
        supportsGpsRoute -> ActivityRecordingKind.GPS_ROUTE
        isRepetitionLike -> ActivityRecordingKind.REPETITION
        else -> ActivityRecordingKind.TIMED
    }

internal const val RecordingPreferencesName = "activity_recording"
internal const val RecordingRoutePointsFileName = "activity_recording_points.csv"
internal const val MaxLocationAgeMillis = 10_000L
internal const val MaxLocationFutureSkewSeconds = 5L
internal const val MaxPlausibleSpeedMetersPerSecond = 55.0
internal const val MinSampleIntervalMillis = 500L
internal const val MinElevationGainIncrementMeters = 1.0
internal const val BarometerSmoothingAlpha = 0.3
internal const val MinBarometerElevationStepMeters = 3.0
internal const val RestTimerBellVolume = 0.42f
internal const val HrrTargetHitsToEndEffort = 2
internal const val PlanStepCueVibrationMillis = 150L
internal const val CountdownVibrationMillis = 60L
// Five, not three: three gave barely enough time to get back into position (#285).
internal const val RestCountdownSeconds = 5L
internal const val CountdownBeepMillis = 120
internal const val CountdownBeepVolume = 70
internal const val HrrCueVibrationMillis = 400L
internal const val MetadataPersistThrottleMillis = 2_000L
internal const val MissingInt = Int.MIN_VALUE
internal const val MissingLong = Long.MIN_VALUE
internal const val MissingFloat = -1f
internal const val KeyStatus = "status"
internal const val KeyRecordingKind = "recording_kind"
internal const val KeyActivityTypeId = "activity_type_id"
internal const val KeyExerciseType = "exercise_type"
internal const val KeyStartTime = "start_time"
internal const val KeyEndTime = "end_time"
internal const val KeyPausedStartedAt = "paused_started_at"
internal const val KeyTotalPausedMillis = "total_paused_millis"
internal const val KeyPauseIntervals = "pause_intervals"
internal const val KeyPoints = "points"
internal const val KeyRouteBreakIndexes = "route_break_indexes"
internal const val KeyManualLaps = "manual_laps"
internal const val KeyMarkers = "markers"
internal const val KeyDistanceMeters = "distance_meters"
internal const val KeyElevationMeters = "elevation_meters"
internal const val KeyElevationLostMeters = "elevation_lost_meters"
internal const val KeyBarometerElevationGainedMeters = "barometer_elevation_gained_meters"
internal const val KeyBarometerElevationLostMeters = "barometer_elevation_lost_meters"
internal const val KeyHasBarometerElevation = "has_barometer_elevation"
internal const val KeyLastBarometerAltitudeMeters = "last_barometer_altitude_meters"
internal const val KeyCurrentSpeedMetersPerSecond = "current_speed_meters_per_second"
internal const val KeyMaxSpeedMetersPerSecond = "max_speed_meters_per_second"
internal const val KeyGpsStatus = "gps_status"
internal const val KeyKeepScreenOnDuringRecording = "keep_screen_on_during_recording"
internal const val KeyAutoIdleEnabled = "auto_idle_enabled"
internal const val KeyAutoIdleTimeoutMillis = "auto_idle_timeout_millis"
internal const val KeyLastMovementAt = "last_movement_at"
internal const val KeyTotalIdleMillis = "total_idle_millis"
internal const val KeyRepetitionCount = "repetition_count"
internal const val KeyCurrentSetRepetitionCount = "current_set_repetition_count"
internal const val KeyRepetitionSets = "repetition_sets"
internal const val KeyRepetitionRestSeconds = "repetition_rest_seconds"
internal const val KeyCurrentSetStartedAt = "current_set_started_at"
internal const val KeyRestStartedAt = "rest_started_at"
internal const val KeyAccumulatedRestMillis = "accumulated_rest_millis"
internal const val KeyPlanId = "plan_id"
internal const val KeyPlanTitle = "plan_title"
internal const val KeyPlanSteps = "plan_steps"
internal const val KeyPlanStepIndex = "plan_step_index"
internal const val KeyLastAccuracyMeters = "last_accuracy_meters"
internal const val KeyLastLocationTime = "last_location_time"
internal const val KeyDroppedPointCount = "dropped_point_count"
internal const val KeyErrorMessage = "error_message"
internal const val KeyDashboardTemplate = "dashboard_template"
internal const val KeyDashboardFields = "dashboard_fields"
