import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../../../../domain/model/activity_models.dart';
import '../../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../../domain/preferences/activity_recording_preferences.dart';
import 'activity_recording.dart';

/// Port of the Kotlin `ActivityRecordingStoreSerialization.kt` + the
/// `ActivityRecordingStore` — persists an in-progress [ActivityRecordingState] to
/// [SharedPreferences].
///
/// Fidelity note: the Kotlin store also mirrored route points into an
/// append-only CSV file for crash resilience. The Dart port persists route
/// points in the same `points` preference string only (path_provider file I/O
/// omitted); the encode/decode format is otherwise identical.
class ActivityRecordingStore {
  ActivityRecordingStore(this._prefs);

  final SharedPreferences _prefs;

  ActivityRecordingState restore() => restoreRecordingState(_prefs);

  Future<void> storeMetadata(ActivityRecordingState state) async {
    if (state.status == ActivityRecordingStatus.idle) {
      await clear();
    } else {
      await storeRecordingMetadata(_prefs, state);
    }
  }

  Future<void> clear() async {
    for (final key in _recordingKeys) {
      await _prefs.remove(key);
    }
  }
}

// ── State (de)serialization ──────────────────────────────────────────────────

ActivityRecordingState restoreRecordingState(SharedPreferences prefs) {
  final status = _statusFromName(prefs.getString(_keyStatus)) ??
      ActivityRecordingStatus.idle;
  if (status == ActivityRecordingStatus.idle) {
    return const ActivityRecordingState();
  }
  return ActivityRecordingState(
    status: status,
    recordingKind: _kindFromName(prefs.getString(_keyRecordingKind)) ??
        ActivityRecordingKind.gpsRoute,
    activityTypeId: prefs.getString(_keyActivityTypeId),
    exerciseType: _intOrNull(prefs, _keyExerciseType),
    startTime: _instantOrNull(prefs, _keyStartTime),
    endTime: _instantOrNull(prefs, _keyEndTime),
    pausedStartedAt: _instantOrNull(prefs, _keyPausedStartedAt),
    totalPausedMillis: prefs.getInt(_keyTotalPausedMillis) ?? 0,
    pauseIntervals: decodePauseIntervals(prefs.getString(_keyPauseIntervals) ?? ''),
    points: decodeRoutePoints(prefs.getString(_keyPoints) ?? ''),
    routeBreakIndexes: _decodeIntList(prefs.getString(_keyRouteBreakIndexes) ?? ''),
    manualLaps: _decodeLaps(prefs.getString(_keyManualLaps) ?? ''),
    markers: _decodeMarkers(prefs.getString(_keyMarkers) ?? ''),
    distanceMeters: _double(prefs, _keyDistanceMeters),
    elevationGainedMeters: _double(prefs, _keyElevationMeters),
    elevationLostMeters: _double(prefs, _keyElevationLostMeters),
    barometerElevationGainedMeters:
        _double(prefs, _keyBarometerElevationGainedMeters),
    barometerElevationLostMeters: _double(prefs, _keyBarometerElevationLostMeters),
    hasBarometerElevation: prefs.getBool(_keyHasBarometerElevation) ?? false,
    lastBarometerAltitudeMeters: _doubleOrNull(prefs, _keyLastBarometerAltitudeMeters),
    currentSpeedMetersPerSecond: _double(prefs, _keyCurrentSpeedMetersPerSecond),
    maxSpeedMetersPerSecond: _double(prefs, _keyMaxSpeedMetersPerSecond),
    gpsStatus: _gpsStatusFromName(prefs.getString(_keyGpsStatus)) ??
        ActivityGpsStatus.waitingForFix,
    keepScreenOnDuringRecording: prefs.getBool(_keyKeepScreenOnDuringRecording) ??
        ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording,
    autoIdleEnabled: prefs.getBool(_keyAutoIdleEnabled) ??
        ActivityRecordingPreferences.defaultAutoIdleEnabled,
    autoIdleTimeoutMillis: prefs.getInt(_keyAutoIdleTimeoutMillis) ??
        ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds * 1000,
    lastMovementAt: _instantOrNull(prefs, _keyLastMovementAt),
    totalIdleMillis: prefs.getInt(_keyTotalIdleMillis) ?? 0,
    repetitionCount: prefs.getInt(_keyRepetitionCount) ?? 0,
    currentSetRepetitionCount: prefs.getInt(_keyCurrentSetRepetitionCount) ?? 0,
    repetitionSets: _decodeRepetitionSets(prefs.getString(_keyRepetitionSets) ?? ''),
    repetitionRestSeconds: prefs.getInt(_keyRepetitionRestSeconds) ?? 0,
    currentSetStartedAt: _instantOrNull(prefs, _keyCurrentSetStartedAt),
    restStartedAt: _instantOrNull(prefs, _keyRestStartedAt),
    accumulatedRestMillis: prefs.getInt(_keyAccumulatedRestMillis) ?? 0,
    lastAccuracyMeters: _doubleOrNull(prefs, _keyLastAccuracyMeters),
    lastLocationTime: _instantOrNull(prefs, _keyLastLocationTime),
    droppedPointCount: prefs.getInt(_keyDroppedPointCount) ?? 0,
    errorMessage: prefs.getString(_keyErrorMessage),
    dashboardLayout: _restoreDashboardLayout(prefs),
  );
}

Future<void> storeRecordingMetadata(
  SharedPreferences prefs,
  ActivityRecordingState state,
) async {
  await prefs.setString(_keyStatus, state.status.name);
  await prefs.setString(_keyRecordingKind, state.recordingKind.name);
  await _setStringOrRemove(prefs, _keyActivityTypeId, state.activityTypeId);
  await _setIntOrRemove(prefs, _keyExerciseType, state.exerciseType);
  await _setInstant(prefs, _keyStartTime, state.startTime);
  await _setInstant(prefs, _keyEndTime, state.endTime);
  await _setInstant(prefs, _keyPausedStartedAt, state.pausedStartedAt);
  await prefs.setInt(_keyTotalPausedMillis, state.totalPausedMillis);
  await prefs.setString(_keyPauseIntervals, encodePauseIntervals(state.pauseIntervals));
  await prefs.setString(_keyPoints, encodeRoutePoints(state.points));
  await prefs.setString(_keyRouteBreakIndexes, _encodeIntList(state.routeBreakIndexes));
  await prefs.setString(_keyManualLaps, _encodeLaps(state.manualLaps));
  await prefs.setString(_keyMarkers, _encodeMarkers(state.markers));
  await prefs.setDouble(_keyDistanceMeters, state.distanceMeters);
  await prefs.setDouble(_keyElevationMeters, state.elevationGainedMeters);
  await prefs.setDouble(_keyElevationLostMeters, state.elevationLostMeters);
  await prefs.setDouble(
      _keyBarometerElevationGainedMeters, state.barometerElevationGainedMeters);
  await prefs.setDouble(
      _keyBarometerElevationLostMeters, state.barometerElevationLostMeters);
  await prefs.setBool(_keyHasBarometerElevation, state.hasBarometerElevation);
  await _setDoubleOrRemove(
      prefs, _keyLastBarometerAltitudeMeters, state.lastBarometerAltitudeMeters);
  await prefs.setDouble(
      _keyCurrentSpeedMetersPerSecond, state.currentSpeedMetersPerSecond);
  await prefs.setDouble(_keyMaxSpeedMetersPerSecond, state.maxSpeedMetersPerSecond);
  await prefs.setString(_keyGpsStatus, state.gpsStatus.name);
  await prefs.setBool(
      _keyKeepScreenOnDuringRecording, state.keepScreenOnDuringRecording);
  await prefs.setBool(_keyAutoIdleEnabled, state.autoIdleEnabled);
  await prefs.setInt(_keyAutoIdleTimeoutMillis, state.autoIdleTimeoutMillis);
  await _setInstant(prefs, _keyLastMovementAt, state.lastMovementAt);
  await prefs.setInt(_keyTotalIdleMillis, state.totalIdleMillis);
  await prefs.setInt(_keyRepetitionCount, state.repetitionCount);
  await prefs.setInt(_keyCurrentSetRepetitionCount, state.currentSetRepetitionCount);
  await prefs.setString(
      _keyRepetitionSets, _encodeRepetitionSets(state.repetitionSets));
  await prefs.setInt(_keyRepetitionRestSeconds, state.repetitionRestSeconds);
  await _setInstant(prefs, _keyCurrentSetStartedAt, state.currentSetStartedAt);
  await _setInstant(prefs, _keyRestStartedAt, state.restStartedAt);
  await prefs.setInt(_keyAccumulatedRestMillis, state.accumulatedRestMillis);
  await _setDoubleOrRemove(prefs, _keyLastAccuracyMeters, state.lastAccuracyMeters);
  await _setInstant(prefs, _keyLastLocationTime, state.lastLocationTime);
  await prefs.setInt(_keyDroppedPointCount, state.droppedPointCount);
  await _setStringOrRemove(prefs, _keyErrorMessage, state.errorMessage);
  await prefs.setString(
      _keyDashboardTemplate, state.dashboardLayout.template.storageName);
  await prefs.setString(
      _keyDashboardFields, _encodeDashboardItems(state.dashboardLayout));
}

// ── Route points ─────────────────────────────────────────────────────────────

String encodeRoutePoints(List<ExerciseRoutePoint> points) =>
    points.map(encodeRoutePoint).join('\n');

String encodeRoutePoint(ExerciseRoutePoint point) => [
      point.time.millisecondsSinceEpoch.toString(),
      point.latitude.toString(),
      point.longitude.toString(),
      point.altitudeMeters?.toString() ?? '',
      point.horizontalAccuracyMeters?.toString() ?? '',
      point.verticalAccuracyMeters?.toString() ?? '',
    ].join(',');

List<ExerciseRoutePoint> decodeRoutePoints(String text) {
  final result = <ExerciseRoutePoint>[];
  for (final line in const LineSplitter().convert(text)) {
    final parts = line.split(',');
    if (parts.length < 6) continue;
    final time = int.tryParse(parts[0]);
    final latitude = double.tryParse(parts[1]);
    final longitude = double.tryParse(parts[2]);
    if (time == null || latitude == null || longitude == null) continue;
    result.add(
      ExerciseRoutePoint(
        time: DateTime.fromMillisecondsSinceEpoch(time, isUtc: true),
        latitude: latitude,
        longitude: longitude,
        altitudeMeters: double.tryParse(parts[3]),
        horizontalAccuracyMeters: double.tryParse(parts[4]),
        verticalAccuracyMeters: double.tryParse(parts[5]),
      ),
    );
  }
  return result;
}

String encodePauseIntervals(List<ActivityPauseInterval> intervals) => intervals
    .map((i) =>
        '${i.startTime.millisecondsSinceEpoch},${i.endTime.millisecondsSinceEpoch}')
    .join('\n');

List<ActivityPauseInterval> decodePauseIntervals(String text) {
  final result = <ActivityPauseInterval>[];
  for (final line in const LineSplitter().convert(text)) {
    final parts = line.split(',');
    if (parts.length < 2) continue;
    final start = int.tryParse(parts[0]);
    final end = int.tryParse(parts[1]);
    if (start == null || end == null) continue;
    final interval = ActivityPauseInterval(
      startTime: DateTime.fromMillisecondsSinceEpoch(start, isUtc: true),
      endTime: DateTime.fromMillisecondsSinceEpoch(end, isUtc: true),
    );
    if (interval.startTime.isBefore(interval.endTime)) result.add(interval);
  }
  return result;
}

String _encodeRepetitionSets(List<ActivityRecordedRepetitionSet> sets) => sets
    .map((s) => '${s.repetitions},${s.restSeconds},${s.activeMillis}')
    .join('\n');

List<ActivityRecordedRepetitionSet> _decodeRepetitionSets(String text) {
  final result = <ActivityRecordedRepetitionSet>[];
  for (final line in const LineSplitter().convert(text)) {
    final parts = line.split(',');
    if (parts.length < 3) continue;
    final reps = int.tryParse(parts[0]);
    final rest = int.tryParse(parts[1]);
    final active = int.tryParse(parts[2]);
    if (reps == null || rest == null || active == null) continue;
    result.add(ActivityRecordedRepetitionSet(
      repetitions: reps < 0 ? 0 : reps,
      restSeconds: rest < 0 ? 0 : rest,
      activeMillis: active < 1 ? 1 : active,
    ));
  }
  return result;
}

String _encodeLaps(List<ActivityRecordingLap> laps) => laps
    .map((l) => [
          l.startTime.millisecondsSinceEpoch.toString(),
          l.endTime.millisecondsSinceEpoch.toString(),
          l.distanceMeters?.toString() ?? '',
        ].join(','))
    .join('\n');

List<ActivityRecordingLap> _decodeLaps(String text) {
  final result = <ActivityRecordingLap>[];
  for (final line in const LineSplitter().convert(text)) {
    final parts = line.split(',');
    if (parts.length < 3) continue;
    final start = int.tryParse(parts[0]);
    final end = int.tryParse(parts[1]);
    if (start == null || end == null) continue;
    final lap = ActivityRecordingLap(
      startTime: DateTime.fromMillisecondsSinceEpoch(start, isUtc: true),
      endTime: DateTime.fromMillisecondsSinceEpoch(end, isUtc: true),
      distanceMeters: double.tryParse(parts[2]),
    );
    if (lap.startTime.isBefore(lap.endTime)) result.add(lap);
  }
  return result;
}

String _encodeMarkers(List<ActivityRecordingMarker> markers) => markers
    .map((m) => [
          m.id,
          m.time.millisecondsSinceEpoch.toString(),
          m.latitude.toString(),
          m.longitude.toString(),
          m.altitudeMeters?.toString() ?? '',
          _encodeCompact(m.name),
          _encodeCompact(m.note),
          _encodeCompact(m.type),
        ].join(','))
    .join('\n');

List<ActivityRecordingMarker> _decodeMarkers(String text) {
  final result = <ActivityRecordingMarker>[];
  for (final line in const LineSplitter().convert(text)) {
    final parts = line.split(',');
    if (parts.length < 8) continue;
    if (parts[0].trim().isEmpty) continue;
    final time = int.tryParse(parts[1]);
    final latitude = double.tryParse(parts[2]);
    final longitude = double.tryParse(parts[3]);
    if (time == null || latitude == null || longitude == null) continue;
    final name = _decodeCompact(parts[5]);
    final type = _decodeCompact(parts[7]);
    result.add(ActivityRecordingMarker(
      id: parts[0],
      time: DateTime.fromMillisecondsSinceEpoch(time, isUtc: true),
      latitude: latitude,
      longitude: longitude,
      altitudeMeters: double.tryParse(parts[4]),
      name: name.trim().isEmpty ? 'Marker' : name,
      note: _decodeCompact(parts[6]),
      type: type.trim().isEmpty ? ActivityRecordingMarkerType.generic.value : type,
    ));
  }
  return result;
}

String _encodeCompact(String value) =>
    base64Url.encode(utf8.encode(value)).replaceAll('=', '');

String _decodeCompact(String value) {
  try {
    final padded = value.padRight((value.length + 3) & ~3, '=');
    return utf8.decode(base64Url.decode(padded));
  } catch (_) {
    return '';
  }
}

String _encodeIntList(List<int> values) => values.join(',');

List<int> _decodeIntList(String text) {
  final result = <int>[];
  for (final value in text.split(',')) {
    final parsed = int.tryParse(value);
    if (parsed != null && parsed > 0) result.add(parsed);
  }
  return result;
}

ActivityRecordingDashboardLayout _restoreDashboardLayout(SharedPreferences prefs) {
  final template = ActivityRecordingDashboardTemplate.fromStorage(
        prefs.getString(_keyDashboardTemplate) ?? '',
      ) ??
      ActivityRecordingDashboardTemplate.largeTop;
  final items = _decodeDashboardItems(prefs.getString(_keyDashboardFields) ?? '');
  return ActivityRecordingDashboardLayout(
    template: template,
    fields: items.map((e) => e.$1).toList(),
    sizes: {
      for (final item in items)
        if (item.$2 != null) item.$1: item.$2!,
    },
  ).normalized();
}

String _encodeDashboardItems(ActivityRecordingDashboardLayout layout) =>
    layout.normalized().items
        .map((item) =>
            '${item.field.storageName}=${item.size.toPreferenceString()}')
        .join(',');

List<(ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize?)>
    _decodeDashboardItems(String text) {
  final result =
      <(ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize?)>[];
  for (final value in text.split(',')) {
    final sections = value.split('=');
    if (sections.isEmpty) continue;
    final field = ActivityRecordingDashboardField.fromStorage(sections.first);
    if (field == null) continue;
    final size = sections.length > 1
        ? ActivityRecordingDashboardItemSize.fromPreferenceString(sections[1])
        : null;
    result.add((field, size));
  }
  return result;
}

// ── Typed helpers ────────────────────────────────────────────────────────────

const int _missingSentinelInt = -1 << 62;

ActivityRecordingStatus? _statusFromName(String? name) => name == null
    ? null
    : ActivityRecordingStatus.values
        .where((e) => e.name == name)
        .cast<ActivityRecordingStatus?>()
        .firstOrNull;

ActivityRecordingKind? _kindFromName(String? name) => name == null
    ? null
    : ActivityRecordingKind.values
        .where((e) => e.name == name)
        .cast<ActivityRecordingKind?>()
        .firstOrNull;

ActivityGpsStatus? _gpsStatusFromName(String? name) => name == null
    ? null
    : ActivityGpsStatus.values
        .where((e) => e.name == name)
        .cast<ActivityGpsStatus?>()
        .firstOrNull;

int? _intOrNull(SharedPreferences prefs, String key) {
  final value = prefs.getInt(key);
  return (value == null || value == _missingSentinelInt) ? null : value;
}

DateTime? _instantOrNull(SharedPreferences prefs, String key) {
  final value = prefs.getInt(key);
  return (value == null || value == _missingSentinelInt)
      ? null
      : DateTime.fromMillisecondsSinceEpoch(value, isUtc: true);
}

double _double(SharedPreferences prefs, String key) => prefs.getDouble(key) ?? 0.0;

double? _doubleOrNull(SharedPreferences prefs, String key) => prefs.getDouble(key);

Future<void> _setStringOrRemove(
    SharedPreferences prefs, String key, String? value) async {
  if (value == null) {
    await prefs.remove(key);
  } else {
    await prefs.setString(key, value);
  }
}

Future<void> _setIntOrRemove(
    SharedPreferences prefs, String key, int? value) async {
  if (value == null) {
    await prefs.remove(key);
  } else {
    await prefs.setInt(key, value);
  }
}

Future<void> _setDoubleOrRemove(
    SharedPreferences prefs, String key, double? value) async {
  if (value == null) {
    await prefs.remove(key);
  } else {
    await prefs.setDouble(key, value);
  }
}

Future<void> _setInstant(
    SharedPreferences prefs, String key, DateTime? value) async {
  if (value == null) {
    await prefs.remove(key);
  } else {
    await prefs.setInt(key, value.millisecondsSinceEpoch);
  }
}

const String _keyStatus = 'status';
const String _keyRecordingKind = 'recording_kind';
const String _keyActivityTypeId = 'activity_type_id';
const String _keyExerciseType = 'exercise_type';
const String _keyStartTime = 'start_time';
const String _keyEndTime = 'end_time';
const String _keyPausedStartedAt = 'paused_started_at';
const String _keyTotalPausedMillis = 'total_paused_millis';
const String _keyPauseIntervals = 'pause_intervals';
const String _keyPoints = 'points';
const String _keyRouteBreakIndexes = 'route_break_indexes';
const String _keyManualLaps = 'manual_laps';
const String _keyMarkers = 'markers';
const String _keyDistanceMeters = 'distance_meters';
const String _keyElevationMeters = 'elevation_meters';
const String _keyElevationLostMeters = 'elevation_lost_meters';
const String _keyBarometerElevationGainedMeters =
    'barometer_elevation_gained_meters';
const String _keyBarometerElevationLostMeters = 'barometer_elevation_lost_meters';
const String _keyHasBarometerElevation = 'has_barometer_elevation';
const String _keyLastBarometerAltitudeMeters = 'last_barometer_altitude_meters';
const String _keyCurrentSpeedMetersPerSecond = 'current_speed_meters_per_second';
const String _keyMaxSpeedMetersPerSecond = 'max_speed_meters_per_second';
const String _keyGpsStatus = 'gps_status';
const String _keyKeepScreenOnDuringRecording = 'keep_screen_on_during_recording';
const String _keyAutoIdleEnabled = 'auto_idle_enabled';
const String _keyAutoIdleTimeoutMillis = 'auto_idle_timeout_millis';
const String _keyLastMovementAt = 'last_movement_at';
const String _keyTotalIdleMillis = 'total_idle_millis';
const String _keyRepetitionCount = 'repetition_count';
const String _keyCurrentSetRepetitionCount = 'current_set_repetition_count';
const String _keyRepetitionSets = 'repetition_sets';
const String _keyRepetitionRestSeconds = 'repetition_rest_seconds';
const String _keyCurrentSetStartedAt = 'current_set_started_at';
const String _keyRestStartedAt = 'rest_started_at';
const String _keyAccumulatedRestMillis = 'accumulated_rest_millis';
const String _keyLastAccuracyMeters = 'last_accuracy_meters';
const String _keyLastLocationTime = 'last_location_time';
const String _keyDroppedPointCount = 'dropped_point_count';
const String _keyErrorMessage = 'error_message';
const String _keyDashboardTemplate = 'dashboard_template';
const String _keyDashboardFields = 'dashboard_fields';

const List<String> _recordingKeys = [
  _keyStatus,
  _keyRecordingKind,
  _keyActivityTypeId,
  _keyExerciseType,
  _keyStartTime,
  _keyEndTime,
  _keyPausedStartedAt,
  _keyTotalPausedMillis,
  _keyPauseIntervals,
  _keyPoints,
  _keyRouteBreakIndexes,
  _keyManualLaps,
  _keyMarkers,
  _keyDistanceMeters,
  _keyElevationMeters,
  _keyElevationLostMeters,
  _keyBarometerElevationGainedMeters,
  _keyBarometerElevationLostMeters,
  _keyHasBarometerElevation,
  _keyLastBarometerAltitudeMeters,
  _keyCurrentSpeedMetersPerSecond,
  _keyMaxSpeedMetersPerSecond,
  _keyGpsStatus,
  _keyKeepScreenOnDuringRecording,
  _keyAutoIdleEnabled,
  _keyAutoIdleTimeoutMillis,
  _keyLastMovementAt,
  _keyTotalIdleMillis,
  _keyRepetitionCount,
  _keyCurrentSetRepetitionCount,
  _keyRepetitionSets,
  _keyRepetitionRestSeconds,
  _keyCurrentSetStartedAt,
  _keyRestStartedAt,
  _keyAccumulatedRestMillis,
  _keyLastAccuracyMeters,
  _keyLastLocationTime,
  _keyDroppedPointCount,
  _keyErrorMessage,
  _keyDashboardTemplate,
  _keyDashboardFields,
];

extension _FirstOrNull<E> on Iterable<E> {
  E? get firstOrNull {
    final iterator = this.iterator;
    return iterator.moveNext() ? iterator.current : null;
  }
}
