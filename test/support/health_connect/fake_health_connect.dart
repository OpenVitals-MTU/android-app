import 'package:health_connect_native/health_connect_native.dart';

import 'exhaustive_fake_host_api.dart';
import 'hc_fixture.dart';

/// Health Connect, answered from the fixture.
///
/// This sits at the Pigeon boundary — ABOVE the Kotlin readers — so it does not
/// reproduce Health Connect's raw behaviour. It reproduces what a CORRECT Kotlin
/// reader returns: samples already found, already clipped to the window, already
/// sorted. That is deliberate, and it is the one real design decision in this tier.
///
/// The alternative — emulating raw Health Connect, record-boundary filtering and
/// all — would mean `readRawHeartRateSamples` returns `[]` for the swallowed
/// workout. Every Dart test would then assert an EMPTY heart rate as the expected
/// answer, and the whole tier would encode the pre-fix behaviour as reality and
/// fight the fix. So the fake honours the contract the host actually delivers.
///
/// Which means: **this tier cannot prove the Kotlin is right.** Tier K does that,
/// against Google's own FakeHealthConnectClient, with the real readers. This tier
/// proves the DART is right, given a host that behaves.
///
/// Every method not answered here throws by name (see [ExhaustiveFakeHostApi]).
/// Returning an empty list instead would let a test pass while proving nothing,
/// which is exactly how 70 of the existing fake's 95 methods behave today.
class FakeHealthConnect extends ExhaustiveFakeHostApi {
  FakeHealthConnect({
    HcFixture? fixture,
    this.granted,
    this.sdkStatus = 3,
    this.syncEnabled = true,
  }) : fixture = fixture ?? HcFixture.load();

  final HcFixture fixture;

  /// Null = everything granted. A test that wants a degraded device names the
  /// permissions it has, rather than overriding a provider.
  final Set<String>? granted;
  final int sdkStatus;
  bool syncEnabled;

  /// Every call made, in order — so a test can assert the app asked for what it
  /// needed, over the window it needed, and not more.
  final List<String> calls = [];

  // ── device ──────────────────────────────────────────────────────────────────

  @override
  Future<int> getSdkStatus() async => sdkStatus;

  @override
  Future<HealthConnectAvailabilityDetail> availabilityDetail() async =>
      HealthConnectAvailabilityDetail(
        sdkStatus: sdkStatus,
        unsupportedProfile: false,
        standaloneNeedsPlayStore: false,
      );

  @override
  Future<bool> getSyncEnabled() async => syncEnabled;

  @override
  Future<void> setSyncEnabled(bool enabled) async => syncEnabled = enabled;

  @override
  Future<List<String>> getGrantedPermissions(List<String> permissions) async =>
      granted == null
          ? permissions
          : permissions.where(granted!.contains).toList();

  @override
  Future<List<String>> filterSupportedPermissions(List<String> permissions) async =>
      permissions;

  @override
  Future<bool> requestPermissions(List<String> permissions) async => true;

  @override
  Future<FeatureStatusMsg> getFeatureStatus(String feature) async =>
      FeatureStatusMsg.available;

  // ── heart ───────────────────────────────────────────────────────────────────

  @override
  Future<List<HeartRateSampleMsg>> readRawHeartRateSamples(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readRawHeartRateSamples');
    // What a CORRECT reader returns: every sample in the window, wherever the
    // writer chose to put it — including inside a 17-hour record that a windowed
    // read of the RECORDS could never have found. That recovery is Kotlin's job and
    // Tier K proves it; here it is the contract.
    final out = <HeartRateSampleMsg>[];
    for (final r in fixture.records('heartRate')) {
      final samples = _series(r);
      for (final s in samples) {
        if (s.$1 >= startEpochMs && s.$1 < endEpochMs) {
          out.add(HeartRateSampleMsg(
            timeEpochMs: s.$1,
            beatsPerMinute: s.$2.round(),
            source: r['writer']! as String,
          ));
        }
      }
    }
    out.sort((a, b) => a.timeEpochMs.compareTo(b.timeEpochMs));
    return out;
  }

  // ── activity ────────────────────────────────────────────────────────────────

  @override
  Future<List<ExerciseDataMsg>> readExerciseSessions(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readExerciseSessions');
    return _sessions(startEpochMs, endEpochMs);
  }

  @override
  Future<List<ExerciseDataMsg>> readExerciseSessionsWithMetrics(
    int startEpochMs,
    int endEpochMs,
    bool includeDistance,
    bool includeSpeed,
  ) async {
    calls.add('readExerciseSessionsWithMetrics');
    return _sessions(startEpochMs, endEpochMs);
  }

  @override
  Future<List<SpeedSampleMsg>> readSpeedSamples(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readSpeedSamples');
    final out = <SpeedSampleMsg>[];
    for (final r in fixture.records('speed')) {
      for (final s in _series(r)) {
        if (s.$1 >= startEpochMs && s.$1 < endEpochMs) {
          out.add(SpeedSampleMsg(
            timeEpochMs: s.$1,
            metersPerSecond: s.$2,
            source: r['writer']! as String,
          ));
        }
      }
    }
    out.sort((a, b) => a.timeEpochMs.compareTo(b.timeEpochMs));
    return out;
  }

  @override
  Future<List<ActivityCadenceSampleMsg>> readActivityCadenceSamples(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readActivityCadenceSamples');
    final out = <ActivityCadenceSampleMsg>[];
    for (final (kind, isCycling) in [
      ('stepsCadence', false),
      ('cyclingCadence', true),
    ]) {
      for (final r in fixture.records(kind)) {
        for (final s in _series(r)) {
          if (s.$1 >= startEpochMs && s.$1 < endEpochMs) {
            out.add(ActivityCadenceSampleMsg(
              timeEpochMs: s.$1,
              rate: s.$2,
              isCycling: isCycling,
              source: r['writer']! as String,
            ));
          }
        }
      }
    }
    out.sort((a, b) => a.timeEpochMs.compareTo(b.timeEpochMs));
    return out;
  }

  @override
  Future<ExerciseSessionMetricsMsg> readExerciseSessionMetrics(
    int startEpochMs,
    int endEpochMs,
    List<String> metrics,
  ) async {
    calls.add('readExerciseSessionMetrics');
    // "Only report a metric that was actually asked for": a total absent from an
    // aggregate the caller never requested is UNKNOWN, not zero. The app branches on
    // exactly that difference, so the fake must honour it or the branch is untested.
    double? total(String kind) =>
        _prorated(fixture.records(kind), startEpochMs, endEpochMs);
    double? avg(String kind) {
      final vals = <double>[];
      for (final r in fixture.records(kind)) {
        for (final s in _series(r)) {
          if (s.$1 >= startEpochMs && s.$1 < endEpochMs) vals.add(s.$2);
        }
      }
      if (vals.isEmpty) return null;
      return vals.reduce((a, b) => a + b) / vals.length;
    }

    return ExerciseSessionMetricsMsg(
      totalDistanceMeters: metrics.contains('distance') ? total('distance') : null,
      averageSpeedMetersPerSecond: metrics.contains('speed') ? avg('speed') : null,
      steps: metrics.contains('steps') ? total('steps')?.round() : null,
      totalCaloriesKcal:
          metrics.contains('totalCalories') ? total('totalCalories') : null,
      activeCaloriesKcal:
          metrics.contains('activeCalories') ? total('activeCalories') : null,
      elevationGainedMeters:
          metrics.contains('elevation') ? total('elevationGained') : null,
      floorsClimbed: null,
      wheelchairPushes: null,
      averagePowerWatts: metrics.contains('power') ? avg('power') : null,
    );
  }

  // ── sleep ───────────────────────────────────────────────────────────────────

  @override
  Future<List<SleepDataMsg>> readSleepSessionsRaw(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readSleepSessionsRaw');
    return [
      for (final r in fixture.records('sleep'))
        if ((r['start']! as int) >= startEpochMs &&
            (r['start']! as int) < endEpochMs)
          _sleepMsg(r),
    ];
  }

  @override
  Future<SleepDataMsg?> readSleepSessionById(String id) async {
    calls.add('readSleepSessionById');
    for (final r in fixture.records('sleep')) {
      if (r['id'] == id) return _sleepMsg(r);
    }
    return null;
  }

  // ── plumbing ────────────────────────────────────────────────────────────────

  /// Delta-decoded samples: `(timeMs, value)`.
  List<(int, double)> _series(Map<String, Object?> r) {
    final out = <(int, double)>[];
    var t = r['t0']! as int;
    final values = (r['bpm'] ?? r['v'])! as List;
    out.add((t, (values[0] as num).toDouble()));
    final dt = (r['dt']! as List).cast<int>();
    for (var i = 0; i < dt.length; i++) {
      t += dt[i];
      out.add((t, (values[i + 1] as num).toDouble()));
    }
    return out;
  }

  /// An interval total, pro-rated by how much of the record is inside the window.
  /// Same rule as the Kotlin aggregating fake — and the same caveat: UNCALIBRATED
  /// against a real device.
  double? _prorated(List<Map<String, Object?>> records, int start, int end) {
    var sum = 0.0;
    for (final r in records) {
      final rs = r['start']! as int;
      final re = r['end']! as int;
      if (rs >= end || re <= start) continue;
      final span = re - rs;
      if (span <= 0) continue;
      final overlap = (re < end ? re : end) - (rs > start ? rs : start);
      final value = (r['v'] ?? r['count'])! as num;
      sum += value.toDouble() * (overlap / span);
    }
    return sum > 0 ? sum : null;
  }

  List<ExerciseDataMsg> _sessions(int startEpochMs, int endEpochMs) => [
        for (final r in fixture.records('exercise'))
          if ((r['start']! as int) >= startEpochMs &&
              (r['start']! as int) < endEpochMs)
            ExerciseDataMsg(
              id: r['id']! as String,
              title: r['title'] as String?,
              exerciseType: r['exerciseType']! as int,
              startEpochMs: r['start']! as int,
              endEpochMs: r['end']! as int,
              source: r['writer']! as String,
              notes: r['notes'] as String?,
              clientRecordId: r['clientRecordId'] as String?,
              plannedExerciseSessionId: null,
              device: null,
              segments: const [],
              laps: const [],
              route: ExerciseRouteMsg(
                status: (r['route']! as List).isEmpty
                    ? ExerciseRouteStatusMsg.noData
                    : ExerciseRouteStatusMsg.data,
                points: [
                  for (final p in (r['route']! as List).cast<Map<String, Object?>>())
                    ExerciseRoutePointMsg(
                      timeEpochMs: p['t']! as int,
                      latitude: (p['lat']! as num).toDouble(),
                      longitude: (p['lon']! as num).toDouble(),
                      altitudeMeters: (p['alt'] as num?)?.toDouble(),
                      horizontalAccuracyMeters: (p['acc'] as num?)?.toDouble(),
                      verticalAccuracyMeters: null,
                    ),
                ],
              ),
              isOpenVitalsEntry:
                  (r['writer']! as String).startsWith('tech.mmarca.openvitals'),
              totalDistanceMeters: null,
              averageSpeedMetersPerSecond: null,
              // The provenance. Dropped from the message for months, so every
              // record read null: the manual-entry count was always zero and the
              // dedup tie-break was always a draw.
              startZoneOffsetSeconds: r['startZoneOffsetSeconds'] as int?,
              endZoneOffsetSeconds: r['endZoneOffsetSeconds'] as int?,
              lastModifiedEpochMs: r['lastModified'] as int?,
              clientRecordVersion: r['clientRecordVersion'] as int?,
              recordingMethod: r['recordingMethod'] as int?,
            ),
      ];

  SleepDataMsg _sleepMsg(Map<String, Object?> r) => SleepDataMsg(
        id: r['id']! as String,
        startEpochMs: r['start']! as int,
        endEpochMs: r['end']! as int,
        source: r['writer']! as String,
        title: r['title'] as String?,
        notes: r['notes'] as String?,
        clientRecordId: r['clientRecordId'] as String?,
        device: null,
        stages: [
          for (final s in (r['stages']! as List).cast<Map<String, Object?>>())
            SleepStageMsg(
              startEpochMs: s['start']! as int,
              endEpochMs: s['end']! as int,
              stageType: s['type']! as int,
            ),
        ],
        startZoneOffsetSeconds: r['startZoneOffsetSeconds'] as int?,
        endZoneOffsetSeconds: r['endZoneOffsetSeconds'] as int?,
        lastModifiedEpochMs: r['lastModified'] as int?,
        clientRecordVersion: r['clientRecordVersion'] as int?,
        recordingMethod: r['recordingMethod'] as int?,
      );
}
