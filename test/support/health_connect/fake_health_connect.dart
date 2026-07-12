import 'dart:convert';

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

  // ── heart: HRV, resting, aggregated buckets ─────────────────────────────────

  @override
  Future<List<HrvSampleMsg>> readHrvSamples(int startEpochMs, int endEpochMs) async {
    calls.add('readHrvSamples');
    return [
      for (final r in _instantsIn('hrv', startEpochMs, endEpochMs))
        HrvSampleMsg(
          timeEpochMs: r['time']! as int,
          rmssdMs: (r['v']! as num).toDouble(),
          source: r['writer']! as String,
        ),
    ];
  }

  @override
  Future<List<DailyHrvMsg>> readDailyHRV(int startEpochMs, int endEpochMs) async {
    calls.add('readDailyHRV');
    return [
      for (final e in _dailyMean('hrv', startEpochMs, endEpochMs).entries)
        DailyHrvMsg(dateEpochMs: e.key, rmssdMs: e.value),
    ];
  }

  @override
  Future<List<RestingHeartRateSampleMsg>> readRestingHeartRateSamples(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readRestingHeartRateSamples');
    return [
      for (final r in _instantsIn('restingHeartRate', startEpochMs, endEpochMs))
        RestingHeartRateSampleMsg(
          timeEpochMs: r['time']! as int,
          beatsPerMinute: (r['v']! as num).round(),
          source: r['writer']! as String,
        ),
    ];
  }

  @override
  Future<List<DailyRestingHRMsg>> readDailyRestingHR(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readDailyRestingHR');
    return [
      for (final e
          in _dailyMean('restingHeartRate', startEpochMs, endEpochMs).entries)
        DailyRestingHRMsg(dateEpochMs: e.key, bpm: e.value.round()),
    ];
  }

  @override
  Future<List<HeartRateAggBucketMsg>> readHeartRateAggregatedBuckets(
    int startEpochMs,
    int endEpochMs,
    int bucketMs,
  ) async {
    calls.add('readHeartRateAggregatedBuckets');
    // Slices by TIME, not by record — which is the whole reason aggregation can see
    // inside a 17-hour record that a windowed read of the RECORDS cannot.
    final out = <HeartRateAggBucketMsg>[];
    for (var t = startEpochMs; t < endEpochMs; t += bucketMs) {
      final end = t + bucketMs > endEpochMs ? endEpochMs : t + bucketMs;
      final vals = <double>[];
      for (final r in fixture.records('heartRate')) {
        for (final sample in _series(r)) {
          if (sample.$1 >= t && sample.$1 < end) vals.add(sample.$2);
        }
      }
      // Health Connect OMITS an empty bucket rather than returning a zero one, and
      // the app branches on the difference.
      if (vals.isEmpty) continue;
      out.add(HeartRateAggBucketMsg(
        startEpochMs: t,
        avgBpm: (vals.reduce((a, b) => a + b) / vals.length).round(),
      ));
    }
    return out;
  }

  // ── hydration ───────────────────────────────────────────────────────────────

  @override
  Future<List<HydrationEntryMsg>> readHydrationEntries(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readHydrationEntries');
    return [
      for (final r in fixture.records('hydration'))
        if ((r['start']! as int) >= startEpochMs &&
            (r['start']! as int) < endEpochMs)
          HydrationEntryMsg(
            startEpochMs: r['start']! as int,
            endEpochMs: r['end']! as int,
            liters: (r['v']! as num).toDouble(),
            source: r['writer']! as String,
            id: r['id']! as String,
            clientRecordId: r['clientRecordId'] as String?,
            isOpenVitalsEntry:
                (r['writer']! as String).startsWith('tech.mmarca.openvitals'),
          ),
    ];
  }

  @override
  Future<List<DailyHydrationMsg>> readDailyHydration(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readDailyHydration');
    final byDay = <int, double>{};
    for (final r in fixture.records('hydration')) {
      final t = r['start']! as int;
      if (t < startEpochMs || t >= endEpochMs) continue;
      byDay.update(_dayKey(t), (v) => v + (r['v']! as num).toDouble(),
          ifAbsent: () => (r['v']! as num).toDouble());
    }
    return [
      for (final e in byDay.entries)
        DailyHydrationMsg(dateEpochMs: e.key, liters: e.value),
    ];
  }

  // ── record types the fixture genuinely has none of ──────────────────────────
  //
  // Answering EMPTY here is honest: the export contains no nutrition, blood
  // pressure, glucose, body-temperature or respiratory-rate records in the golden
  // week, so "there are none" is the truth and the app's no-data branches are what
  // gets exercised. This is NOT the same as the base class's refusal, which exists
  // for a method nobody has thought about.

  @override
  Future<List<NutritionEntryMsg>> readNutritionEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<DailyNutritionMsg>> readDailyNutrition(
    int startEpochMs,
    int endEpochMs,
    bool includeHydration,
    bool includeCalories,
    bool includeEstimatedCalories,
  ) async =>
      const [];

  @override
  Future<List<BloodPressureEntryMsg>> readBloodPressureEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<BloodGlucoseEntryMsg>> readBloodGlucoseEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<BodyTempEntryMsg>> readBodyTemperatureEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<RespiratoryRateEntryMsg>> readRespiratoryRateEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<SpO2EntryMsg>> readSpO2Entries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<Vo2MaxEntryMsg>> readVo2MaxEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      const [];

  @override
  Future<List<HeartRateSummaryMsg>> readDailyHeartRateSummaries(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readDailyHeartRateSummaries');
    final byDay = <int, List<double>>{};
    for (final r in fixture.records('heartRate')) {
      for (final sample in _series(r)) {
        if (sample.$1 < startEpochMs || sample.$1 >= endEpochMs) continue;
        byDay.putIfAbsent(_dayKey(sample.$1), () => []).add(sample.$2);
      }
    }
    return [
      for (final e in byDay.entries)
        HeartRateSummaryMsg(
          dateEpochMs: e.key,
          avgBpm: (e.value.reduce((a, b) => a + b) / e.value.length).round(),
          minBpm: e.value.reduce((a, b) => a < b ? a : b).round(),
          maxBpm: e.value.reduce((a, b) => a > b ? a : b).round(),
        ),
    ];
  }

  // ── body: BMR is real; the rest the golden week genuinely has none of ────────

  @override
  Future<List<BmrEntryMsg>> readBmrEntries(
    int startEpochMs,
    int endEpochMs,
  ) async {
    calls.add('readBmrEntries');
    return [
      for (final r in _instantsIn('basalMetabolicRate', startEpochMs, endEpochMs))
        BmrEntryMsg(
          timeEpochMs: r['time']! as int,
          kcalPerDay: (r['v']! as num).toDouble(),
          source: r['writer']! as String,
        ),
    ];
  }

  @override
  Future<BmrEntryMsg?> readLatestBmr() async {
    calls.add('readLatestBmr');
    final all = fixture.records('basalMetabolicRate');
    if (all.isEmpty) return null;
    final latest = all.reduce(
        (a, b) => (a['time']! as int) >= (b['time']! as int) ? a : b);
    return BmrEntryMsg(
      timeEpochMs: latest['time']! as int,
      kcalPerDay: (latest['v']! as num).toDouble(),
      source: latest['writer']! as String,
    );
  }

  // The golden week has no weight, height, body-fat or body-composition records.
  // "There are none" is the truth, and it is what exercises the body screens'
  // no-data branches — which is a scenario, not a gap.
  @override
  Future<List<WeightEntryMsg>> readWeightEntries(int s, int e) async => const [];

  @override
  Future<WeightEntryMsg?> readLatestWeight() async => null;

  @override
  Future<List<HeightEntryMsg>> readHeightEntries(int s, int e) async => const [];

  @override
  Future<HeightEntryMsg?> readLatestHeightEntry() async => null;

  @override
  Future<List<BodyFatEntryMsg>> readBodyFatEntries(int s, int e) async => const [];

  @override
  Future<BodyFatEntryMsg?> readLatestBodyFat() async => null;

  @override
  Future<List<BodyMassEntryMsg>> readLeanBodyMassEntries(int s, int e) async =>
      const [];

  @override
  Future<List<BodyMassEntryMsg>> readBoneMassEntries(int s, int e) async => const [];

  @override
  Future<List<BodyMassEntryMsg>> readBodyWaterMassEntries(int s, int e) async =>
      const [];

  @override
  Future<List<DailyMacrosMsg>> readDailyMacros(int s, int e) async => const [];

  // ── cycle: DROPPED from the fixture on purpose ──────────────────────────────
  //
  // Menstruation, ovulation, cervical mucus, intermenstrual bleeding, sexual
  // activity and basal body temperature are not scrubbed out of the fixture -- they
  // are not in it. This repository is public, and no amount of value-scrubbing makes
  // it acceptable to derive a public artefact from a real person's records of those.
  //
  // So the cycle reads answer EMPTY, and that is honest rather than a shortcut: the
  // cycle screens' no-data path is genuinely what runs here. Their WITH-data paths
  // are the one part of this app the fixture cannot cover, and pretending otherwise
  // would be worse than saying so.

  @override
  Future<List<MenstruationFlowEntryMsg>> readMenstruationFlowEntries(
          int s, int e) async =>
      const [];

  @override
  Future<List<MenstruationPeriodEntryMsg>> readMenstruationPeriods(
          int s, int e) async =>
      const [];

  @override
  Future<List<OvulationTestEntryMsg>> readOvulationTests(int s, int e) async =>
      const [];

  @override
  Future<List<CervicalMucusEntryMsg>> readCervicalMucusEntries(
          int s, int e) async =>
      const [];

  @override
  Future<List<BasalBodyTemperatureEntryMsg>> readBasalBodyTemperatureEntries(
          int s, int e) async =>
      const [];

  @override
  Future<List<IntermenstrualBleedingEntryMsg>> readIntermenstrualBleedingEntries(
          int s, int e) async =>
      const [];

  @override
  Future<List<SexualActivityEntryMsg>> readSexualActivityEntries(
          int s, int e) async =>
      const [];

  // ── aggregation ─────────────────────────────────────────────────────────────

  @override
  Future<List<String>> aggregateGroupByDurationJson(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
    int bucketMinutes,
  ) async {
    calls.add('aggregateGroupByDurationJson');
    final bucketMs = bucketMinutes * 60 * 1000;
    final out = <String>[];
    for (var t = startEpochMs; t < endEpochMs; t += bucketMs) {
      final end = t + bucketMs > endEpochMs ? endEpochMs : t + bucketMs;
      final values = <String, double>{};
      for (final metric in aggregateMetrics) {
        final kind = _metricToFixtureKind[metric];
        if (kind == null) continue;
        final v = _prorated(fixture.records(kind), t, end);
        if (v != null) values[metric] = v;
      }
      // Sparse: HC omits a bucket with no data. The app distinguishes "no data"
      // from "zero" and several screens branch on it.
      if (values.isEmpty) continue;
      out.add(jsonEncode({
        'startEpochMs': t,
        'endEpochMs': end,
        'values': values,
      }));
    }
    return out;
  }

  /// Wire metric name -> the fixture list that backs it. A name absent here
  /// aggregates to nothing, which mirrors the native side skipping a metric key it
  /// does not know.
  static const _metricToFixtureKind = {
    'Steps.count': 'steps',
    'Distance.distance': 'distance',
    'ActiveCaloriesBurned.energy': 'activeCalories',
    'TotalCaloriesBurned.energy': 'totalCalories',
    'ElevationGained.elevation': 'elevationGained',
  };

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

  /// Instantaneous records inside the window (HRV, resting heart rate).
  List<Map<String, Object?>> _instantsIn(String kind, int start, int end) => [
        for (final r in fixture.records(kind))
          if ((r['time']! as int) >= start && (r['time']! as int) < end) r,
      ];

  /// The mean of an instantaneous record type, per local day.
  Map<int, double> _dailyMean(String kind, int start, int end) {
    final byDay = <int, List<double>>{};
    for (final r in _instantsIn(kind, start, end)) {
      byDay
          .putIfAbsent(_dayKey(r['time']! as int), () => [])
          .add((r['v']! as num).toDouble());
    }
    return {
      for (final e in byDay.entries)
        e.key: e.value.reduce((a, b) => a + b) / e.value.length,
    };
  }

  static int _dayKey(int epochMs) {
    final t = DateTime.fromMillisecondsSinceEpoch(epochMs, isUtc: true);
    return DateTime.utc(t.year, t.month, t.day).millisecondsSinceEpoch;
  }

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
