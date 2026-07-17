import 'dart:async';
import 'dart:math' as math;
import 'dart:ui' show Locale, PlatformDispatcher;

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:geolocator/geolocator.dart';
import 'package:sensors_plus/sensors_plus.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../data/prefs/preferences_repository.dart';
import '../../../../domain/model/activity_models.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import '../../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../../domain/preferences/activity_recording_preferences.dart';
import '../../../imports/applehealth/apple_health_import_foreground_controller.dart'
    show appleHealthImportOwnsForegroundService;
import '../../../../l10n/app_localizations.dart';
import '../../../../navigation/app_routes.dart';
import '../../../../data/repository/contract/ble_sensor_repository.dart';
import '../../../../domain/model/activity_entry_types.dart';
import '../repetition_recognizers.dart';
import 'activity_recording.dart';
import 'activity_recording_announcements.dart';
import 'activity_recording_device_support.dart';
import 'activity_recording_native_sensors.dart';
import 'activity_recording_serialization.dart';
import 'activity_recording_splits.dart';
import 'activity_recording_task_handler.dart';

/// Device-bound port of the Kotlin `ActivityRecordingController` +
/// `ActivityRecordingService`, wiring GPS ([Geolocator]), motion/barometer
/// ([sensors_plus]), the foreground service ([flutter_foreground_task], which
/// posts its own ongoing notification) and voice announcements ([flutter_tts])
/// onto the pure [ActivityRecordingState] model.
///
/// This is the PLATFORM side of the recording feature: everything the OS talks
/// to lives here — the foreground-task lifecycle, the notification-button
/// callbacks, the GPS/BLE/motion streams and the draft-store writes — and it
/// stays imperative (a [ValueListenable] of [ActivityRecordingState]) on
/// purpose. `ActivityRecordingViewModel` is the SCREEN side, and sits on top of
/// this; nothing in this file may be reshaped for the screen's convenience.
///
/// The pure state transitions (distance/pace/elevation accumulation, pause /
/// rest / repetition bookkeeping, snapshot building) are shared with the tested
/// model helpers. The device I/O is best-effort and intentionally deferred from
/// unit testing — only compile-cleanliness is required here.
class ActivityRecordingService implements ActivityRecordingController {
  ActivityRecordingService({
    required this.preferencesRepository,
    required this.bleSensorCoordinator,
    required this.recordingStore,
    required this.unitFormatter,
    required this.deviceSupport,
    this.nativeSensors = const ActivityRecordingNativeSensors(),
  }) {
    _state = ValueNotifier<ActivityRecordingState>(recordingStore.restore());
    _bleSub =
        bleSensorCoordinator.metricsStream.listen(acceptBleMetrics);
    _scheduleRestCompletion(_state.value);
    FlutterForegroundTask.addTaskDataCallback(_onNotificationAction);
    _resumeRestoredRecording();
  }

  final PreferencesRepository preferencesRepository;
  final BleSensorRepository bleSensorCoordinator;
  final ActivityRecordingStore recordingStore;
  final UnitFormatter unitFormatter;
  final ActivityRecordingDeviceSupport deviceSupport;
  final ActivityRecordingNativeSensors nativeSensors;

  late final ValueNotifier<ActivityRecordingState> _state;
  @override
  ValueListenable<ActivityRecordingState> get state => _state;

  final FlutterTts _tts = FlutterTts();
  bool _ttsLanguageConfigured = false;
  final ActivityRecordingAnnouncementTracker _announcementTracker =
      ActivityRecordingAnnouncementTracker();

  StreamSubscription<Position>? _positionSub;
  StreamSubscription<BarometerEvent>? _barometerSub;
  StreamSubscription<AccelerometerEvent>? _accelSub;
  StreamSubscription<double>? _proximitySub;
  StreamSubscription<int>? _stepSub;
  StreamSubscription<BleRecordingMetrics>? _bleSub;
  Timer? _restCompletionTimer;
  Timer? _gpsLostTimer;
  int _recordingGeneration = 0;
  JumpRepetitionRecognizer? _jumpRecognizer;
  PullUpRepetitionRecognizer? _pullRecognizer;
  PushUpProximityRecognizer? _pushUpRecognizer;
  StepDetectorRepetitionRecognizer? _stepRecognizer;
  String? _lastNotificationSignature;

  void dispose() {
    FlutterForegroundTask.removeTaskDataCallback(_onNotificationAction);
    _positionSub?.cancel();
    _barometerSub?.cancel();
    _accelSub?.cancel();
    _proximitySub?.cancel();
    _stepSub?.cancel();
    _bleSub?.cancel();
    _restCompletionTimer?.cancel();
    _hrrPhaseTimer?.cancel();
    _gpsLostTimer?.cancel();
    _state.dispose();
  }

  /// The device locale's localizations, for texts spoken/posted while no
  /// BuildContext exists (TTS, the service notification). Kotlin reads the
  /// same strings through the service's Context.
  AppLocalizations _l10n() {
    try {
      return lookupAppLocalizations(PlatformDispatcher.instance.locale);
    } on FlutterError {
      return lookupAppLocalizations(const Locale('en'));
    }
  }

  /// Kotlin `ActivityRecordingService.observeRecordingState` restart path: a
  /// recording restored from disk after process death re-attaches its device
  /// streams and re-enters the foreground, instead of silently going numb.
  void _resumeRestoredRecording() {
    final current = _state.value;
    if (!current.isActive) return;
    if (current.status == ActivityRecordingStatus.recording) {
      _startDeviceStreamsFor(current);
    }
    bleSensorCoordinator.startRecording();
    _startForegroundService(current);
  }

  /// Port of `ActivityRecordingService.onStartCommand`'s pause/resume/discard
  /// intent handling, receiving the notification-button presses relayed from
  /// the service isolate.
  void _onNotificationAction(Object data) {
    switch (data) {
      case kActivityRecordingActionPause:
        pauseRecording();
      case kActivityRecordingActionResume:
        resumeRecording();
      case kActivityRecordingActionDiscard:
        discardRecording();
    }
  }

  // ── Preparation ──────────────────────────────────────────────────────────

  @override
  void prepareRecordingDashboard(ActivityEntryType activityType) {
    if (_state.value.isActive) return;
    _recordingGeneration += 1;
    _updateAndPersist(
      ActivityRecordingState(
        status: ActivityRecordingStatus.idle,
        recordingKind: _recordingKind(activityType),
        activityTypeId: activityType.id,
        exerciseType: activityType.exerciseType,
        dashboardLayout: preferencesRepository
            .activityRecordingDashboardLayout(activityType.id),
      ),
    );
    previewBleConnections();
  }

  @override
  void updateDashboardLayout(ActivityRecordingDashboardLayout layout) {
    final current = _state.value;
    final activityTypeId = current.activityTypeId;
    if (activityTypeId == null) return;
    if (current.status == ActivityRecordingStatus.recording) return;
    final normalized = layout.normalized();
    preferencesRepository.setActivityRecordingDashboardLayout(
        activityTypeId, normalized);
    _updateAndPersist(
        current.copyWith(dashboardLayout: normalized, errorMessage: null));
  }

  @override
  void clearPreparedRecording() {
    if (_state.value.isActive) return;
    _clearRecording();
  }

  @override
  void previewBleConnections() {
    if (_state.value.isActive) return;
    bleSensorCoordinator.refreshConnections();
    acceptBleMetrics(bleSensorCoordinator.metrics);
  }

  @override
  void stopBlePreview() {
    if (_state.value.isActive) return;
    bleSensorCoordinator.disconnectAll();
  }

  // ── Start ────────────────────────────────────────────────────────────────

  @override
  Future<bool> startRecording(
    ActivityEntryType activityType,
    ActivityRecordingInitialFix? initialFix, {
    int repetitionRestSeconds = 0,
    bool withoutGps = false,
  }) async {
    // A run is a run whether or not the phone was listening to satellites.
    //
    // GPS was a property of the TYPE, so running and cycling were GPS-or-nothing: to
    // record a run without it you had to lie and call it a treadmill. Now it is a
    // property of the RECORDING, and a run with no route is recorded as a timed session
    // -- duration and heart rate, and the distance typed in on the form afterwards.
    //
    // No location permission is asked for, no fix is waited on, and the saved session
    // simply has no route. There is nothing here that could give anyone the impression
    // that GPS was working: they turned it off themselves.
    if (activityType.supportsGpsRoute && !withoutGps) {
      return _startGpsRecording(activityType, initialFix);
    }
    if (activityType.supportsGpsRoute && withoutGps) {
      return _startTimedRecording(activityType);
    }
    if (activityType.isRepetitionLike) {
      return _startRepetitionRecording(activityType, repetitionRestSeconds);
    }
    if (activityType.recordingSensor == ActivityRecordingSensor.ble) {
      return _startTimedRecording(activityType);
    }
    _updateAndPersist(_state.value
        .copyWith(errorMessage: _l10n().activityRecordingErrorUnsupportedType));
    return false;
  }

  /// Kotlin `startGpsRecording` re-validates permissions and the locked fix at
  /// the very start; a stale or imprecise fix must not seed a route.
  Future<bool> _startGpsRecording(
      ActivityEntryType activityType, ActivityRecordingInitialFix? initialFix) async {
    final preferences = preferencesRepository.activityRecordingPreferences();
    final dashboardLayout =
        preferencesRepository.activityRecordingDashboardLayout(activityType.id);
    if (!await deviceSupport.hasPreciseLocationPermission()) {
      _updateAndPersist(_state.value.copyWith(
          errorMessage:
              _l10n().activityRecordingErrorPreciseLocationPermission));
      return false;
    }
    if (!await deviceSupport.hasNotificationPermission()) {
      _updateAndPersist(_state.value.copyWith(
          errorMessage: _l10n().activityRecordingErrorNotificationPermission));
      return false;
    }
    final now = DateTime.now().toUtc();
    if (initialFix == null || !_isPreciseInitialFix(initialFix, preferences, now)) {
      _updateAndPersist(_state.value
          .copyWith(errorMessage: _l10n().activityRecordingErrorWaitingForGps));
      return false;
    }
    await recordingStore.clear();
    _recordingGeneration += 1;
    _announcementTracker.reset();
    // Kotlin seeds the route by feeding the locked fix through acceptLocation
    // with its time bumped to the session start — which MSL-converts its
    // altitude like every other fix.
    final seedAltitude = initialFix.altitudeMeters;
    final seedMslAltitude = seedAltitude == null || seedAltitude == 0.0
        ? null
        : await _mslAltitudeMeters(
            latitude: initialFix.latitude,
            longitude: initialFix.longitude,
            altitudeMeters: seedAltitude,
          );
    final seedPoint = ExerciseRoutePoint(
      time: now,
      latitude: initialFix.latitude,
      longitude: initialFix.longitude,
      altitudeMeters: seedMslAltitude ?? seedAltitude,
      horizontalAccuracyMeters: initialFix.accuracyMeters,
      verticalAccuracyMeters: null,
    );
    _updateAndPersist(
      ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.gpsRoute,
        activityTypeId: activityType.id,
        exerciseType: activityType.exerciseType,
        startTime: now,
        points: [seedPoint],
        latestUiPoint: seedPoint,
        gpsStatus: ActivityGpsStatus.fix,
        keepScreenOnDuringRecording: preferences.keepScreenOnDuringRecording,
        autoIdleEnabled: preferences.autoIdleEnabled,
        autoIdleTimeoutMillis: preferences.autoIdleTimeoutSeconds * 1000,
        lastMovementAt: now,
        lastAccuracyMeters: initialFix.accuracyMeters,
        lastLocationTime: now,
        dashboardLayout: dashboardLayout,
      ),
    );
    _startLocationUpdates(preferences);
    _startBarometerUpdates(preferences);
    // Kotlin's service registers the step detector alongside GPS for
    // step-counting route types (walking), so steps count while the route
    // records.
    if (activityType.supportsStepCounting) _startMotionRecognizer(activityType);
    bleSensorCoordinator.startRecording();
    acceptBleMetrics(bleSensorCoordinator.metrics);
    _startForegroundService(_state.value);
    return true;
  }

  /// The freshness half of Kotlin `Location.activityGpsFixQuality` applied to
  /// the plugin-free initial fix (there is no start time yet at this point).
  bool _isPreciseInitialFix(
    ActivityRecordingInitialFix fix,
    ActivityRecordingPreferences preferences,
    DateTime now,
  ) {
    final accuracy = fix.accuracyMeters;
    if (accuracy == null ||
        accuracy > preferences.requiredGpsAccuracyMeters.toDouble()) {
      return false;
    }
    final timeMillis = fix.timeMillis;
    if (timeMillis == null) return false;
    final fixTime = DateTime.fromMillisecondsSinceEpoch(timeMillis, isUtc: true);
    return now.difference(fixTime).inMilliseconds <= kMaxLocationAgeMillis &&
        !fixTime.isAfter(
            now.add(const Duration(seconds: kMaxLocationFutureSkewSeconds)));
  }

  Future<bool> _startRepetitionRecording(
      ActivityEntryType activityType, int repetitionRestSeconds) async {
    if (!await deviceSupport.hasNotificationPermission()) {
      _updateAndPersist(_state.value.copyWith(
          errorMessage: _l10n().activityRecordingErrorNotificationPermission));
      return false;
    }
    final preferences = preferencesRepository.activityRecordingPreferences();
    final dashboardLayout =
        preferencesRepository.activityRecordingDashboardLayout(activityType.id);
    final now = DateTime.now().toUtc();
    await recordingStore.clear();
    _recordingGeneration += 1;
    _announcementTracker.reset();
    _updateAndPersist(
      ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.repetition,
        activityTypeId: activityType.id,
        exerciseType: activityType.exerciseType,
        startTime: now,
        keepScreenOnDuringRecording: preferences.keepScreenOnDuringRecording,
        currentSetStartedAt: now,
        repetitionRestSeconds:
            repetitionRestSeconds < 0 ? 0 : repetitionRestSeconds,
        dashboardLayout: dashboardLayout,
      ),
    );
    _startMotionRecognizer(activityType);
    bleSensorCoordinator.startRecording();
    acceptBleMetrics(bleSensorCoordinator.metrics);
    _startForegroundService(_state.value);
    return true;
  }

  Future<bool> _startTimedRecording(ActivityEntryType activityType) async {
    if (!await deviceSupport.hasNotificationPermission()) {
      _updateAndPersist(_state.value.copyWith(
          errorMessage: _l10n().activityRecordingErrorNotificationPermission));
      return false;
    }
    final preferences = preferencesRepository.activityRecordingPreferences();
    final dashboardLayout =
        preferencesRepository.activityRecordingDashboardLayout(activityType.id);
    final now = DateTime.now().toUtc();
    await recordingStore.clear();
    _recordingGeneration += 1;
    _announcementTracker.reset();
    _updateAndPersist(
      ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.timed,
        activityTypeId: activityType.id,
        exerciseType: activityType.exerciseType,
        startTime: now,
        keepScreenOnDuringRecording: preferences.keepScreenOnDuringRecording,
        dashboardLayout: dashboardLayout,
      ),
    );
    bleSensorCoordinator.startRecording();
    acceptBleMetrics(bleSensorCoordinator.metrics);
    _startForegroundService(_state.value);
    return true;
  }

  // ── Heart-rate recovery test ─────────────────────────────────────────────
  //
  // A timed recording underneath -- same foreground service, same sensors, same write
  // path -- with a protocol laid over it: warm up, go hard, then stop DEAD. The abrupt,
  // recorded stop is the whole point. It is what a workout read back from a watch can
  // never give us, because nothing in the data says where the effort ended.

  Timer? _hrrPhaseTimer;

  /// Consecutive samples at or above the target. One spurious reading must not end an
  /// effort the rider is still in the middle of.
  int _hrrTargetHits = 0;

  static const int _hrrTargetHitsToEndEffort = 2;

  @override
  Future<bool> startHeartRateRecoveryTest(
    ActivityEntryType activityType,
    HeartRateRecoveryTestConfig config,
  ) async {
    if (!await _startTimedRecording(activityType)) return false;
    _hrrTargetHits = 0;
    _updateAndPersist(_state.value.copyWith(
      hrrPhase: config.warmupSeconds > 0
          ? ActivityRecordingHrrPhase.warmup
          : ActivityRecordingHrrPhase.effort,
      hrrConfig: config,
    ));
    _scheduleHrrPhase(_state.value);
    return true;
  }

  @override
  void endHeartRateRecoveryEffort() {
    final current = _state.value;
    if (current.hrrPhase != ActivityRecordingHrrPhase.warmup &&
        current.hrrPhase != ActivityRecordingHrrPhase.effort) {
      return;
    }
    _enterHrrRecovery(current);
  }

  /// The instant that matters. Everything the measurement says is relative to it, so it
  /// is stamped BEFORE the cue is fired rather than after: a text-to-speech engine taking
  /// half a second to warm up must not push the recovery clock half a second late.
  void _enterHrrRecovery(ActivityRecordingState current) {
    _hrrTargetHits = 0;
    _updateAndPersist(current.copyWith(
      hrrPhase: ActivityRecordingHrrPhase.recovery,
      hrrEffortEndedAt: DateTime.now().toUtc(),
    ));
    _cueHrr(_l10n().activityRecordingHrrCueSlowDown);
    _scheduleHrrPhase(_state.value);
  }

  void _scheduleHrrPhase(ActivityRecordingState state) {
    _hrrPhaseTimer?.cancel();
    _hrrPhaseTimer = null;

    DateTime? deadline;
    if (state.hrrPhase == ActivityRecordingHrrPhase.warmup) {
      deadline = state.startTime
          ?.add(Duration(seconds: state.hrrConfig.warmupSeconds));
    } else if (state.hrrPhase == ActivityRecordingHrrPhase.recovery) {
      deadline = state.hrrEffortEndedAt
          ?.add(Duration(seconds: state.hrrConfig.recoverySeconds));
    }
    // The effort has no deadline: it ends when the rider does, or when their heart rate
    // says so. And `complete` is the end of the road.
    if (deadline == null) return;

    final now = DateTime.now().toUtc();
    final delayMillis =
        (deadline.millisecondsSinceEpoch - now.millisecondsSinceEpoch)
            .clamp(0, 1 << 62);
    final scheduledPhase = state.hrrPhase;
    final scheduledEffortEnd = state.hrrEffortEndedAt;

    _hrrPhaseTimer = Timer(Duration(milliseconds: delayMillis), () {
      final current = _state.value;
      // The state that scheduled this timer must still be the state we are in, or the
      // timer belongs to a test that has already moved on (or been discarded).
      if (current.hrrPhase != scheduledPhase ||
          current.hrrEffortEndedAt != scheduledEffortEnd) {
        return;
      }
      _hrrPhaseTimer = null;

      if (scheduledPhase == ActivityRecordingHrrPhase.warmup) {
        _updateAndPersist(
            current.copyWith(hrrPhase: ActivityRecordingHrrPhase.effort));
        _cueHrr(_l10n().activityRecordingHrrCueEffort);
      } else if (scheduledPhase == ActivityRecordingHrrPhase.recovery) {
        // NOT auto-finished. The recording keeps running and the rider saves it -- and
        // because the rest segment is written to the session's end rather than to a fixed
        // five minutes, taking a while to press save cannot break the measurement.
        _updateAndPersist(
            current.copyWith(hrrPhase: ActivityRecordingHrrPhase.complete));
        _cueHrr(_l10n().activityRecordingHrrCueComplete);
      }
    });
  }

  /// Ends the effort when the heart rate reaches the target, if one was set.
  void _maybeEndHrrEffortOnTarget(ActivityRecordingState state) {
    if (state.hrrPhase != ActivityRecordingHrrPhase.effort) {
      _hrrTargetHits = 0;
      return;
    }
    final target = state.hrrConfig.targetHeartRateBpm;
    final bpm = state.currentHeartRateBpm;
    if (target == null || bpm == null) return;
    if (bpm < target) {
      _hrrTargetHits = 0;
      return;
    }
    _hrrTargetHits += 1;
    if (_hrrTargetHits >= _hrrTargetHitsToEndEffort) {
      _enterHrrRecovery(state);
    }
  }

  /// Bell, voice and a hard buzz, all four channels at once.
  ///
  /// Not behind a preference. The phone is on a bike mount or in a pocket, the rider is
  /// at their limit and is not looking at the screen — and the cue IS the protocol. A
  /// heart-rate recovery test whose "stop now" went unheard has measured nothing. The
  /// pace announcer is optional because a ride works without it; this does not.
  void _cueHrr(String text) {
    _playBell();
    unawaited(_speak(text));
    unawaited(HapticFeedback.heavyImpact());
  }

  // ── BLE ──────────────────────────────────────────────────────────────────

  void acceptBleMetrics(BleRecordingMetrics metrics) {
    final current = _state.value;
    _updateAndPersist(
      current.copyWith(
        currentHeartRateBpm: metrics.heartRateBpm,
        currentCyclingCadenceRpm: metrics.cyclingCadenceRpm,
        currentPowerWatts: metrics.powerWatts,
        currentSensorSpeedMetersPerSecond: metrics.cyclingSpeedMetersPerSecond ??
            metrics.runningSpeedMetersPerSecond,
        currentRunningCadenceRpm: metrics.runningCadenceRpm,
        bleHeartRateNoSignal:
            metrics.heartRateNoSignal && metrics.heartRateBpm == null,
        bleDeviceStatuses: metrics.deviceStatuses.isEmpty
            ? current.bleDeviceStatuses
            : metrics.deviceStatuses,
      ),
    );
    // Against the heart rate we just took, not the one before it.
    _maybeEndHrrEffortOnTarget(_state.value);
  }

  // ── Pause / resume ─────────────────────────────────────────────────────────

  @override
  void pauseRecording() {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.recording) return;
    _updateAndPersist(current.copyWith(
      status: ActivityRecordingStatus.paused,
      pausedStartedAt: DateTime.now().toUtc(),
      errorMessage: null,
    ));
    // Kotlin's service stops location/sensor/pressure updates whenever the
    // status leaves RECORDING; mirror that so a paused session stops draining
    // GPS and cannot trip the GPS-lost watchdog.
    _stopDeviceStreams();
  }

  void _stopDeviceStreams() {
    _positionSub?.cancel();
    _positionSub = null;
    _barometerSub?.cancel();
    _barometerSub = null;
    _stopMotionSensors();
    _gpsLostTimer?.cancel();
    _gpsLostTimer = null;
  }

  /// Kotlin `ActivityRecordingService.stopSensorUpdates`: unregister and drop
  /// the recognizers so a later session starts from a clean state machine.
  void _stopMotionSensors() {
    _accelSub?.cancel();
    _accelSub = null;
    _proximitySub?.cancel();
    _proximitySub = null;
    _stepSub?.cancel();
    _stepSub = null;
    _jumpRecognizer = null;
    _pullRecognizer = null;
    _pushUpRecognizer = null;
    _stepRecognizer = null;
  }

  void _startDeviceStreamsFor(ActivityRecordingState state) {
    final preferences = preferencesRepository.activityRecordingPreferences();
    final activityType = activityEntryTypeById(state.activityTypeId);
    switch (state.recordingKind) {
      case ActivityRecordingKind.gpsRoute:
        _startLocationUpdates(preferences);
        _startBarometerUpdates(preferences);
        // Kotlin's GPS_ROUTE branch of observeRecordingState also registers
        // the step detector for step-counting route types (walking), so live
        // step counting runs alongside the location updates.
        if (activityType != null && activityType.supportsStepCounting) {
          _startMotionRecognizer(activityType);
        }
      case ActivityRecordingKind.repetition:
        if (activityType != null) _startMotionRecognizer(activityType);
      case ActivityRecordingKind.timed:
        // A run recorded WITHOUT GPS still has a barometer and a step detector.
        //
        // Neither of them needs a position. Elevation gain comes from air pressure and
        // steps from the accelerometer, and they were only being lost because switching
        // GPS off routed the recording down the timed branch, which started no sensors at
        // all. Turning down the satellites is no reason to stop counting the climb.
        //
        // Gated on `supportsGpsRoute`, so this reaches a run or a walk recorded without
        // GPS and leaves the stationary bike and the strength session -- the timed
        // recordings that never had these sensors -- exactly as they were.
        if (activityType != null && activityType.supportsGpsRoute) {
          _startBarometerUpdates(preferences);
          if (activityType.supportsStepCounting) {
            _startMotionRecognizer(activityType);
          }
        }
    }
  }

  @override
  void resumeRecording() {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.paused) return;
    final now = DateTime.now().toUtc();
    final pausedStart = current.pausedStartedAt;
    final pausedMillis = pausedStart == null
        ? 0
        : (now.millisecondsSinceEpoch - pausedStart.millisecondsSinceEpoch)
            .clamp(0, 1 << 62);
    final closedPause =
        pausedStart == null ? null : toPauseInterval(pausedStart, now);
    _updateAndPersist(current.copyWith(
      status: ActivityRecordingStatus.recording,
      pausedStartedAt: null,
      totalPausedMillis: current.totalPausedMillis + pausedMillis,
      pauseIntervals: [...current.pauseIntervals, ?closedPause],
      errorMessage: null,
    ));
    _startDeviceStreamsFor(_state.value);
  }

  // ── Laps / markers ─────────────────────────────────────────────────────────

  @override
  void addManualLap() {
    final current = _state.value;
    if (!current.isActive ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    final start = current.manualLaps.isNotEmpty
        ? current.manualLaps
            .reduce((a, b) => a.endTime.isAfter(b.endTime) ? a : b)
            .endTime
        : current.startTime;
    if (start == null) return;
    final end = DateTime.now().toUtc();
    if (!start.isBefore(end)) return;
    final distance = activityRecordingRouteDistanceMeters(
      points: current.points,
      routeBreakIndexes: current.routeBreakIndexes,
      startTime: start,
      endTime: end,
    );
    _updateAndPersist(current.copyWith(
      manualLaps: [
        ...current.manualLaps,
        ActivityRecordingLap(
          startTime: start,
          endTime: end,
          distanceMeters: distance > 0.0 ? distance : null,
        ),
      ],
      errorMessage: null,
    ));
  }

  @override
  void addMarker() {
    final current = _state.value;
    if (!current.isActive ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    final point = current.latestUiPoint ??
        (current.points.isNotEmpty ? current.points.last : null);
    if (point == null) return;
    final markerNumber = current.markers.length + 1;
    _updateAndPersist(current.copyWith(
      markers: [
        ...current.markers,
        ActivityRecordingMarker(
          id: DateTime.now().microsecondsSinceEpoch.toString(),
          time: point.time,
          latitude: point.latitude,
          longitude: point.longitude,
          altitudeMeters: point.altitudeMeters,
          name: 'Marker $markerNumber',
          type: ActivityRecordingMarkerType.generic.value,
        ),
      ],
      errorMessage: null,
    ));
  }

  @override
  void updateMarker(ActivityRecordingMarker marker) {
    final current = _state.value;
    if (!current.isActive ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    _updateAndPersist(current.copyWith(
      markers: [
        for (final existing in current.markers)
          existing.id == marker.id ? marker : existing,
      ],
      errorMessage: null,
    ));
  }

  @override
  void deleteMarker(String markerId) {
    final current = _state.value;
    if (!current.isActive ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    _updateAndPersist(current.copyWith(
      markers: current.markers.where((m) => m.id != markerId).toList(),
      errorMessage: null,
    ));
  }

  // ── Discard / finish ───────────────────────────────────────────────────────

  @override
  void discardRecording() {
    bleSensorCoordinator.stopRecording();
    _clearRecording();
    _stopForegroundService();
  }

  @override
  ActivityRecordingSnapshot? finishRecording() {
    final current = _state.value;
    final start = current.startTime;
    final exerciseType = current.exerciseType;
    if (start == null || exerciseType == null || !current.isActive) return null;

    final nowCandidate = DateTime.now().toUtc();
    final end = nowCandidate.isAfter(start)
        ? nowCandidate
        : start.add(const Duration(seconds: 1));
    final pauseIntervals = [
      ...current.pauseIntervals,
      ?(current.pausedStartedAt == null
          ? null
          : toPauseInterval(current.pausedStartedAt!, end)),
    ];
    final manualLaps = current.closedManualLaps(end);
    final repetitionSets = current.recordedRepetitionSets(end);
    final bleSamples = bleSensorCoordinator.stopRecording();
    final snapshot = ActivityRecordingSnapshot(
      exerciseType: exerciseType,
      recordingKind: current.recordingKind,
      activityTypeId: current.activityTypeId,
      startTime: start,
      endTime: end,
      points: current.points,
      pauseIntervals: pauseIntervals,
      routeBreakIndexes: current.routeBreakIndexes,
      manualLaps: manualLaps,
      markers: current.markers,
      distanceMeters: current.distanceMeters,
      elevationGainedMeters: current.hasBarometerElevation
          ? current.barometerElevationGainedMeters
          : current.elevationGainedMeters,
      repetitionCount: current.repetitionCount,
      repetitionSets: repetitionSets,
      bleSamples: bleSamples,
      // Only when there is heart rate to go with it. A rest segment saying "the recovery
      // began here" with no heart-rate series behind it is a claim to a measurement that
      // was never taken -- and anything reading the session back later, this app
      // included, would believe it.
      hrrEffortEndedAt: bleSamples.heartRateSamples.isEmpty
          ? null
          : current.hrrEffortEndedAt,
    );
    _clearRecording();
    _stopForegroundService();
    return snapshot;
  }

  // ── Repetition counting ──────────────────────────────────────────────────

  @override
  void adjustRepetitionCount(int delta) {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.recording) return;
    if (current.recordingKind == ActivityRecordingKind.gpsRoute) {
      if (activityEntryTypeById(current.activityTypeId)?.supportsStepCounting !=
          true) {
        return;
      }
      final nextCount = (current.repetitionCount + delta).clamp(0, 1 << 62);
      _updateAndPersist(current.copyWith(
        currentSetRepetitionCount: nextCount,
        repetitionCount: nextCount,
        errorMessage: null,
      ));
      return;
    }
    if (current.recordingKind != ActivityRecordingKind.repetition) return;
    final nextCurrentSetCount =
        (current.currentSetRepetitionCount + delta).clamp(0, 1 << 62);
    final completedCount =
        current.repetitionSets.fold<int>(0, (sum, s) => sum + s.repetitions);
    _updateAndPersist(current.copyWith(
      currentSetRepetitionCount: nextCurrentSetCount,
      repetitionCount: completedCount + nextCurrentSetCount,
      errorMessage: null,
    ));
  }

  @override
  void endRepetitionSet() {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.recording ||
        current.recordingKind != ActivityRecordingKind.repetition ||
        current.currentSetRepetitionCount <= 0) {
      return;
    }
    final now = DateTime.now().toUtc();
    final activeMillis = (now.millisecondsSinceEpoch -
            (current.currentSetStartedAt ?? current.startTime ?? now)
                .millisecondsSinceEpoch)
        .clamp(1, 1 << 62);
    final completedSet = ActivityRecordedRepetitionSet(
      repetitions: current.currentSetRepetitionCount,
      restSeconds: current.repetitionRestSeconds,
      activeMillis: activeMillis,
    );
    final ActivityRecordingState next;
    if (current.repetitionRestSeconds > 0) {
      next = current.copyWith(
        status: ActivityRecordingStatus.resting,
        repetitionSets: [...current.repetitionSets, completedSet],
        currentSetRepetitionCount: 0,
        restStartedAt: now,
        currentSetStartedAt: null,
        errorMessage: null,
      );
    } else {
      next = current.copyWith(
        repetitionSets: [...current.repetitionSets, completedSet],
        currentSetRepetitionCount: 0,
        currentSetStartedAt: now,
        errorMessage: null,
      );
    }
    _updateAndPersist(next);
  }

  @override
  void startNextRepetitionSet() {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.resting ||
        current.recordingKind != ActivityRecordingKind.repetition) {
      return;
    }
    _startNextRepetitionSet(current, DateTime.now().toUtc());
  }

  void _startNextRepetitionSet(ActivityRecordingState current, DateTime now) {
    final actualRestMillis = current.openRestMillis(now);
    final updatedSets = withLastRestSeconds(
        current.repetitionSets, (actualRestMillis ~/ 1000).clamp(0, 1 << 62));
    _updateAndPersist(current.copyWith(
      status: ActivityRecordingStatus.recording,
      repetitionSets: updatedSets,
      accumulatedRestMillis: current.accumulatedRestMillis + actualRestMillis,
      restStartedAt: null,
      currentSetStartedAt: now,
      errorMessage: null,
    ));
  }

  // ── Location processing ────────────────────────────────────────────────────

  /// Kotlin samples GPS_PROVIDER at a fixed 1 s / 0 m and filters in the
  /// controller (`minimumSampleDistanceMeters`), so the OS-level distance
  /// filter stays at zero here too.
  void _startLocationUpdates(ActivityRecordingPreferences preferences) {
    _positionSub?.cancel();
    // Stale callbacks from a cancelled subscription must not leak into a newer
    // session: the generation is captured when the stream starts, mirroring
    // Kotlin's capture in acceptLocation before the async hop.
    final generation = _recordingGeneration;
    _positionSub = Geolocator.getPositionStream(
      locationSettings: activityRecordingLocationSettings(),
    ).listen(
      (position) => _acceptPosition(position, generation),
      onError: (Object error) {
        // Kotlin's LocationListener.onProviderDisabled path.
        if (error is LocationServiceDisabledException) {
          _reportGpsDisabled();
          _reportRecordingError(_l10n().activityRecordingErrorProvider);
        }
      },
    );
    _scheduleGpsLostTimeout();
  }

  /// Kotlin `ActivityRecordingService.scheduleGpsLostTimeout`: 30 s without a
  /// fix flips the GPS status to LOST. Re-armed on every incoming position.
  void _scheduleGpsLostTimeout() {
    _gpsLostTimer?.cancel();
    _gpsLostTimer = Timer(const Duration(milliseconds: kGpsLostTimeoutMillis),
        _reportGpsLost);
  }

  /// Kotlin `ActivityRecordingController.reportGpsLost`.
  void _reportGpsLost() {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.recording ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    _updateAndPersist(current.copyWith(gpsStatus: ActivityGpsStatus.lost));
  }

  /// Kotlin `ActivityRecordingController.reportGpsDisabled`.
  void _reportGpsDisabled() {
    final current = _state.value;
    if (!current.isActive ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    _updateAndPersist(current.copyWith(gpsStatus: ActivityGpsStatus.disabled));
  }

  /// Kotlin `ActivityRecordingController.reportRecordingError`.
  void _reportRecordingError(String message) {
    _updateAndPersist(_state.value.copyWith(errorMessage: message));
  }

  Future<void> _acceptPosition(Position position, int generation) async {
    _scheduleGpsLostTimeout();
    if (generation != _recordingGeneration) return;
    // Kotlin `acceptLocation` hops to a background coroutine, converts the
    // fix's WGS84 ellipsoid altitude to mean sea level (`withMslAltitude`) and
    // only then processes the converted location under the generation captured
    // before the hop; this await is that hop.
    final mslAltitudeMeters = position.altitude == 0.0
        ? null
        : await _mslAltitudeMeters(
            latitude: position.latitude,
            longitude: position.longitude,
            altitudeMeters: position.altitude,
          );
    if (generation != _recordingGeneration) return;
    final preferences = preferencesRepository.activityRecordingPreferences();
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.recording) return;
    if (current.recordingKind != ActivityRecordingKind.gpsRoute) return;

    // Kotlin gates every live fix through activityGpsFixQuality: accuracy,
    // staleness (10 s), pre-start timestamps and future clock skew all make a
    // fix imprecise, not just poor reported accuracy.
    final fixQuality = activityGpsFixQuality(
      position,
      startTime: current.startTime,
      now: DateTime.now().toUtc(),
      requiredAccuracyMeters: preferences.requiredGpsAccuracyMeters.toDouble(),
    );
    final locationTime = fixQuality.locationTime ?? position.timestamp.toUtc();
    final point = _toRoutePoint(position, locationTime, mslAltitudeMeters);

    if (!fixQuality.isPrecise) {
      _updateAndPersist(
        withDroppedLocation(
          current,
          fixQuality.accuracyMeters,
          locationTime: locationTime,
          gpsStatus: fixQuality.accuracyMeters == null
              ? ActivityGpsStatus.waitingForFix
              : ActivityGpsStatus.poorAccuracy,
        ).copyWith(latestUiPoint: point),
      );
      return;
    }
    final accuracy = fixQuality.accuracyMeters;
    if (accuracy == null) return;

    // Everything a good fix implies — distance, elevation, speed, route breaks,
    // auto-idle — is a pure fold of the fix into the state, and lives in
    // `withAcceptedLocation`, where it is testable without a GPS receiver.
    _updateAndPersist(
      withAcceptedLocation(
        current,
        point: point,
        accuracyMeters: accuracy,
        preferences: preferences,
      ),
    );
  }

  /// Kotlin `Location.withMslAltitude` (via the native `AltitudeConverter`,
  /// API 34+, off the platform thread), applied per fix like Kotlin — no
  /// caching. Best-effort: null (raw ellipsoid altitude stays in use) below
  /// API 34, on conversion failure, or when the conversion's first geoid-grid
  /// disk read takes longer than the route can wait.
  Future<double?> _mslAltitudeMeters({
    required double latitude,
    required double longitude,
    required double altitudeMeters,
  }) async {
    try {
      return await nativeSensors
          .convertToMsl(
            latitude: latitude,
            longitude: longitude,
            altitudeMeters: altitudeMeters,
          )
          .timeout(const Duration(seconds: 1));
    } on TimeoutException {
      return null;
    }
  }

  /// Kotlin `Location.toRoutePoint`: prefer the converted MSL altitude,
  /// falling back to the raw (ellipsoid) altitude; geolocator reports 0.0
  /// where Kotlin has `hasAltitude() == false`. Deviation: Kotlin's preferred
  /// `mslAltitudeAccuracyMeters` never exists here, because the conversion is
  /// fed a bare lat/lon/altitude Location and the platform only computes MSL
  /// accuracy when the input carries a vertical accuracy.
  ExerciseRoutePoint _toRoutePoint(
          Position position, DateTime time, double? mslAltitudeMeters) =>
      ExerciseRoutePoint(
        time: time,
        latitude: position.latitude,
        longitude: position.longitude,
        altitudeMeters: mslAltitudeMeters ??
            (position.altitude == 0.0 ? null : position.altitude),
        horizontalAccuracyMeters: position.accuracy,
        verticalAccuracyMeters:
            position.altitudeAccuracy == 0.0 ? null : position.altitudeAccuracy,
      );

  // ── Barometer ──────────────────────────────────────────────────────────────

  void _startBarometerUpdates(ActivityRecordingPreferences preferences) {
    if (!preferences.barometerClimbEnabled) return;
    _barometerSub?.cancel();
    _barometerSub = barometerEventStream()
        .listen((event) => acceptBarometerPressure(event.pressure),
            onError: (Object _) {});
  }

  void acceptBarometerPressure(double pressureHpa) {
    final current = _state.value;
    if (current.status != ActivityRecordingStatus.recording ||
        current.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    if (!preferencesRepository
        .activityRecordingPreferences()
        .barometerClimbEnabled) {
      return;
    }
    // Barometric altitude via the international barometric formula.
    const seaLevelHpa = 1013.25;
    final altitudeMeters = 44330.0 *
        (1.0 - math.pow(pressureHpa / seaLevelHpa, 0.1902949).toDouble());
    final previous = current.lastBarometerAltitudeMeters;
    final smoothed = previous == null
        ? altitudeMeters
        : previous + (altitudeMeters - previous) * barometerSmoothingAlpha;
    if (previous == null) {
      _updateAndPersist(current.copyWith(
        hasBarometerElevation: true,
        lastBarometerAltitudeMeters: smoothed,
      ));
      return;
    }
    final delta = smoothed - previous;
    final gained = delta >= minBarometerElevationStepMeters ? delta : 0.0;
    final lost = delta <= -minBarometerElevationStepMeters ? -delta : 0.0;
    _updateAndPersist(current.copyWith(
      hasBarometerElevation: true,
      barometerElevationGainedMeters:
          current.barometerElevationGainedMeters + gained,
      barometerElevationLostMeters: current.barometerElevationLostMeters + lost,
      lastBarometerAltitudeMeters: (gained > 0.0 || lost > 0.0) ? smoothed : previous,
    ));
  }

  // ── Motion recognizers (repetition sensors) ──────────────────────────────

  /// Kotlin `ActivityRecordingService.startSensorUpdates`: resolve which
  /// sensor this activity counts with — step-counting types always use the
  /// step detector, even GPS ones like walking — and wire the matching
  /// recognizer onto its event stream.
  void _startMotionRecognizer(ActivityEntryType activityType) {
    _stopMotionSensors();
    final sensorKind = activityType.supportsStepCounting
        ? ActivityRecordingSensor.stepDetector
        : activityType.recordingSensor;
    switch (sensorKind) {
      case ActivityRecordingSensor.accelerometer:
        if (activityType.segmentType == ExerciseSegmentType.pullUp) {
          _pullRecognizer = PullUpRepetitionRecognizer();
        } else {
          // Kotlin gives trampoline jumps a wider window than rope skips.
          _jumpRecognizer = JumpRepetitionRecognizer(
              maxJumpDurationMillis:
                  activityType.id == 'trampoline_jumping' ? 2500 : 1250);
        }
        _accelSub = accelerometerEventStream().listen(_acceptAcceleration,
            onError: (Object _) {});
      case ActivityRecordingSensor.proximity:
      case ActivityRecordingSensor.stepDetector:
        unawaited(_startNativeMotionSensor(sensorKind));
      case ActivityRecordingSensor.gps:
      case ActivityRecordingSensor.ble:
      case ActivityRecordingSensor.none:
        return;
    }
  }

  /// The async half of Kotlin `startSensorUpdates` for the two sensors that
  /// need platform queries first: the step detector is gated on the
  /// ACTIVITY_RECOGNITION runtime permission, and a missing sensor reports its
  /// per-sensor error instead of silently never counting. The generation is
  /// re-checked after every await so a stop/discard during the checks wins.
  Future<void> _startNativeMotionSensor(
      ActivityRecordingSensor sensorKind) async {
    final generation = _recordingGeneration;
    if (sensorKind == ActivityRecordingSensor.stepDetector &&
        !await deviceSupport.hasActivityRecognitionPermission()) {
      if (generation != _recordingGeneration) return;
      _reportRecordingError(
          _l10n().activityRecordingErrorActivityRecognitionPermission);
      return;
    }
    if (!await nativeSensors.hasSensor(sensorKind)) {
      if (generation != _recordingGeneration) return;
      _reportRecordingError(sensorKind == ActivityRecordingSensor.proximity
          ? _l10n().activityRecordingErrorProximitySensor
          : _l10n().activityRecordingErrorStepDetector);
      return;
    }
    // A pause during the checks already cancelled these streams; do not
    // resubscribe behind its back (resume restarts them).
    if (generation != _recordingGeneration ||
        _state.value.status != ActivityRecordingStatus.recording) {
      return;
    }
    if (sensorKind == ActivityRecordingSensor.proximity) {
      _pushUpRecognizer = PushUpProximityRecognizer();
      _proximitySub?.cancel();
      _proximitySub = nativeSensors
          .proximityEvents()
          .listen(_acceptProximity, onError: (Object _) {});
    } else {
      _stepRecognizer = StepDetectorRepetitionRecognizer();
      _stepSub?.cancel();
      _stepSub = nativeSensors
          .stepDetectorEvents()
          .listen(_acceptStep, onError: (Object _) {});
    }
  }

  /// Kotlin `sensorListener.onSensorChanged` for `TYPE_PROXIMITY`.
  void _acceptProximity(double valueCentimeters) {
    if (_state.value.status != ActivityRecordingStatus.recording) return;
    final recognized = _pushUpRecognizer?.onProximity(
        valueCentimeters, DateTime.now().millisecondsSinceEpoch);
    if (recognized != null) adjustRepetitionCount(1);
  }

  /// Kotlin `sensorListener.onSensorChanged` for `TYPE_STEP_DETECTOR`: one
  /// event per detected step, stamped with the native receipt time.
  void _acceptStep(int timestampMillis) {
    if (_state.value.status != ActivityRecordingStatus.recording) return;
    final recognized = _stepRecognizer?.onStep(timestampMillis);
    if (recognized != null) adjustRepetitionCount(1);
  }

  void _acceptAcceleration(AccelerometerEvent event) {
    if (_state.value.status != ActivityRecordingStatus.recording) return;
    final nowMillis = DateTime.now().millisecondsSinceEpoch;
    final recognized = _pullRecognizer?.onAcceleration(
          event.x,
          event.y,
          event.z,
          nowMillis,
        ) ??
        _jumpRecognizer?.onAcceleration(event.x, event.y, event.z, nowMillis);
    if (recognized != null) adjustRepetitionCount(1);
  }

  // ── Voice announcements (best effort) ───────────────────────────────────────

  /// Kotlin `ActivityRecordingVoiceAnnouncer.onRecordingState`, evaluated on
  /// every state change like the service's state observer does.
  void _maybeAnnounce(ActivityRecordingState state) {
    final preferences = preferencesRepository.activityRecordingPreferences();
    if (!preferences.voiceAnnouncementsEnabled ||
        state.recordingKind != ActivityRecordingKind.gpsRoute) {
      return;
    }
    final text = _announcementTracker.announcementFor(
      state,
      preferences,
      now: DateTime.now().toUtc(),
      l10n: _l10n(),
      unitFormatter: unitFormatter,
    );
    if (text != null) unawaited(_speak(text));
  }

  Future<void> _speak(String text) async {
    try {
      if (!_ttsLanguageConfigured) {
        _ttsLanguageConfigured = true;
        // Kotlin sets `textToSpeech.language = Locale.getDefault()`.
        await _tts
            .setLanguage(PlatformDispatcher.instance.locale.toLanguageTag());
      }
      await _tts.speak(text);
    } catch (_) {
      // Voice output is best-effort.
    }
  }

  // ── Foreground service + notification ────────────────────────────────────

  /// Kotlin `ActivityRecordingState.foregroundServiceType`: location only for
  /// GPS routes, health for step-counting/repetition/timed kinds, and
  /// connected-device only while BLE sensors are attached. Must stay a subset
  /// of the `android:foregroundServiceType` list in the manifest.
  static List<ForegroundServiceTypes> _serviceTypesFor(
      ActivityRecordingState state) {
    final hasBleDevices = state.bleDeviceStatuses.isNotEmpty;
    if (state.recordingKind == ActivityRecordingKind.gpsRoute) {
      return [
        ForegroundServiceTypes.location,
        if (activityEntryTypeById(state.activityTypeId)?.supportsStepCounting ==
            true)
          ForegroundServiceTypes.health,
        if (hasBleDevices) ForegroundServiceTypes.connectedDevice,
      ];
    }
    return [
      ForegroundServiceTypes.health,
      if (hasBleDevices) ForegroundServiceTypes.connectedDevice,
    ];
  }

  /// Kotlin's notification action buttons: pause/resume by status, always
  /// discard. Presses come back through [_onNotificationAction].
  static List<NotificationButton> _notificationButtonsFor(
      ActivityRecordingState state, AppLocalizations l10n) {
    return [
      if (state.status == ActivityRecordingStatus.recording)
        NotificationButton(
            id: kActivityRecordingActionPause, text: l10n.actionPause)
      else if (state.status == ActivityRecordingStatus.paused)
        NotificationButton(
            id: kActivityRecordingActionResume, text: l10n.actionResume),
      NotificationButton(
          id: kActivityRecordingActionDiscard, text: l10n.actionDiscard),
    ];
  }

  /// Keeps GPS alive with the screen off. Without this the OS suspends the
  /// process a few seconds after backgrounding and the route simply stops.
  ///
  /// `startService` throws `ServiceNotInitializedException` unless `init` ran
  /// first, so the two must stay together — an earlier version called only
  /// `startService` inside a swallow-all `catch`, which silently meant no
  /// foreground service at all.
  void _startForegroundService(ActivityRecordingState state) {
    unawaited(() async {
      try {
        final l10n = _l10n();
        FlutterForegroundTask.init(
          androidNotificationOptions: AndroidNotificationOptions(
            channelId: 'activity_recording',
            channelName: l10n.activityRecordingNotificationChannel,
            channelImportance: NotificationChannelImportance.LOW,
            priority: NotificationPriority.LOW,
            onlyAlertOnce: true,
          ),
          iosNotificationOptions: const IOSNotificationOptions(
            showNotification: true,
            playSound: false,
          ),
          foregroundTaskOptions: ForegroundTaskOptions(
            // The recorder drives itself from the geolocator stream in the
            // main isolate; the service isolate only relays notification
            // button presses, so it needs no repeat events.
            eventAction: ForegroundTaskEventAction.nothing(),
            allowWakeLock: true,
            autoRunOnBoot: false,
          ),
        );
        final text = activityRecordingNotificationText(
          state,
          now: DateTime.now().toUtc(),
          l10n: l10n,
          unitFormatter: unitFormatter,
        );
        if (await FlutterForegroundTask.isRunningService) {
          // The app declares exactly ONE ForegroundService, so a running one is
          // either this recorder's (refresh it) or an Apple Health import's — in
          // which case it must be left alone: updating it would replace the
          // import's progress notification with the recording's, and the import
          // would take the service down with it when it finishes. The recording
          // keeps working in the foreground, exactly as when the service fails.
          if (await appleHealthImportOwnsForegroundService()) {
            debugPrint(
              'Activity recording foreground service skipped: an Apple Health '
              'import owns it',
            );
            return;
          }
          // Process-death restart with the service still alive (START_STICKY
          // equivalent): refresh instead of double-starting.
          await FlutterForegroundTask.updateService(
            notificationTitle: l10n.activityRecordingNotificationTitle,
            notificationText: text,
            notificationButtons: _notificationButtonsFor(state, l10n),
          );
          return;
        }
        await FlutterForegroundTask.startService(
          serviceTypes: _serviceTypesFor(state),
          notificationTitle: l10n.activityRecordingNotificationTitle,
          notificationText: text,
          notificationButtons: _notificationButtonsFor(state, l10n),
          // Tapping the notification opens the activity entry screen, the
          // Kotlin content intent's deep link.
          notificationInitialRoute: AppRoutes.activityEntry,
          callback: activityRecordingTaskCallback,
        );
      } catch (error) {
        // A recording without the service still works in the foreground, so
        // surface the failure rather than tearing the session down.
        debugPrint('Activity recording foreground service failed: $error');
      }
    }());
  }

  /// Kotlin `ActivityRecordingService.updateNotification`, driven off every
  /// state change so the ongoing notification shows live time/distance/GPS
  /// status and the correct pause/resume action.
  void _updateForegroundNotification(ActivityRecordingState state) {
    if (!state.isActive) return;
    final l10n = _l10n();
    final text = activityRecordingNotificationText(
      state,
      now: DateTime.now().toUtc(),
      l10n: l10n,
      unitFormatter: unitFormatter,
    );
    final buttons = _notificationButtonsFor(state, l10n);
    final signature = '$text|${buttons.map((b) => b.id).join(',')}';
    if (signature == _lastNotificationSignature) return;
    _lastNotificationSignature = signature;
    unawaited(() async {
      try {
        if (!await FlutterForegroundTask.isRunningService) return;
        await FlutterForegroundTask.updateService(
          notificationTitle: l10n.activityRecordingNotificationTitle,
          notificationText: text,
          notificationButtons: buttons,
        );
      } catch (error) {
        debugPrint('Activity recording notification update failed: $error');
      }
    }());
  }

  void _stopForegroundService() {
    unawaited(() async {
      try {
        await FlutterForegroundTask.stopService();
      } catch (error) {
        debugPrint('Activity recording foreground service stop failed: $error');
      }
    }());
  }

  // ── Rest-completion timer ────────────────────────────────────────────────

  void _scheduleRestCompletion(ActivityRecordingState state) {
    _restCompletionTimer?.cancel();
    _restCompletionTimer = null;
    final restEnd = state.restEndTime();
    if (restEnd == null) return;
    final now = DateTime.now().toUtc();
    final delayMillis =
        (restEnd.millisecondsSinceEpoch - now.millisecondsSinceEpoch)
            .clamp(0, 1 << 62);
    _restCompletionTimer = Timer(Duration(milliseconds: delayMillis), () {
      final current = _state.value;
      if (current.status == ActivityRecordingStatus.resting &&
          current.restStartedAt == state.restStartedAt) {
        _restCompletionTimer = null;
        _playRestTimerBellIfEnabled();
        _startNextRepetitionSet(current, DateTime.now().toUtc());
      }
    });
  }

  /// Kotlin `RestTimerBellVolume`.
  static const double _restTimerBellVolume = 0.42;

  /// The bell asks the OS to *duck* other audio rather than take it outright:
  /// `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` on Android, `duckOthers` on iOS. A
  /// runner listening to music or a podcast then hears it dip for the bell and
  /// come straight back, instead of having playback stopped as full audio focus
  /// (the audioplayers default) would do.
  static final AudioContext _duckingAudioContext = AudioContextConfig(
    focus: AudioContextConfigFocus.duckOthers,
  ).build();

  /// Kotlin `playRestTimerBellIfEnabled`: the same struck-bowl sample the
  /// mindfulness timer uses, not a spoken phrase.
  void _playRestTimerBellIfEnabled() {
    if (!preferencesRepository
        .activityRecordingPreferences()
        .restTimerBellEnabled) {
      return;
    }
    _playBell();
  }

  /// The bell itself, with no preference in front of it — the heart-rate-recovery cue
  /// rings whatever the rest-timer setting says, because a cue nobody heard has cost the
  /// rider the measurement they went out to take.
  void _playBell() {
    unawaited(() async {
      final player = AudioPlayer();
      try {
        player.onPlayerComplete.listen(
          (_) => player.dispose(),
          onError: (Object _) => player.dispose(),
        );
        // Duck other audio for the bell; disposing on completion releases the
        // transient focus and restores the music/podcast to full volume.
        await player.setAudioContext(_duckingAudioContext);
        await player.setVolume(_restTimerBellVolume);
        await player.play(AssetSource('sounds/bowl_struck.ogg'));
      } catch (_) {
        unawaited(player.dispose());
      }
    }());
  }

  // ── Persistence ──────────────────────────────────────────────────────────

  void _clearRecording() {
    _stopDeviceStreams();
    _restCompletionTimer?.cancel();
    _hrrPhaseTimer?.cancel();
    _hrrTargetHits = 0;
    _recordingGeneration += 1;
    _announcementTracker.reset();
    _lastNotificationSignature = null;
    _state.value = const ActivityRecordingState();
    unawaited(recordingStore.clear());
  }

  void _updateAndPersist(ActivityRecordingState state) {
    _state.value = state;
    unawaited(recordingStore.storeMetadata(state));
    _scheduleRestCompletion(state);
    // Kotlin's service observes the state flow and refreshes the notification
    // and voice announcer on every emission; this is that observer.
    _updateForegroundNotification(state);
    _maybeAnnounce(state);
  }

  ActivityRecordingKind _recordingKind(ActivityEntryType activityType) {
    if (activityType.supportsGpsRoute) return ActivityRecordingKind.gpsRoute;
    if (activityType.isRepetitionLike) return ActivityRecordingKind.repetition;
    return ActivityRecordingKind.timed;
  }
}
