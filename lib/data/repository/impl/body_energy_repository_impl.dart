import 'dart:async';
import 'dart:math' as math;

import '../../../core/time/local_date.dart';
import '../../prefs/preferences_repository.dart';
import '../body_energy_timeline_cache_store.dart';
import '../../../domain/dashboard/dashboard_aggregator.dart';
import '../../../domain/insights/body_energy_timeline.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/preferences/body_energy_calibration.dart';
import '../../../domain/preferences/body_profile.dart';
import '../contract/body_energy_repository.dart';
import '../contract/activity_repository.dart';
import '../contract/heart_repository.dart';
import '../contract/health_repository.dart';
import '../contract/sleep_repository.dart';
import '../contract/vitals_repository.dart';
import 'repository_time.dart';

/// Port of the Kotlin `BodyEnergyRepositoryImpl`.
///
/// Composes the heart / sleep / activity / vitals repositories to build a
/// per-day body-energy timeline via [calculateBodyEnergyTimeline], caching both
/// the timeline and the expensive 28-day baselines keyed by a
/// permission/calibration signature (mirroring the Kotlin repository).
class BodyEnergyRepositoryImpl implements BodyEnergyRepository {
  BodyEnergyRepositoryImpl({
    required HeartRepository heartRepository,
    required SleepRepository sleepRepository,
    required ActivityRepository activityRepository,
    required VitalsRepository vitalsRepository,
    required HealthRepository healthRepository,
    required PreferencesRepository preferencesRepository,
    required BodyEnergyTimelineCacheStore cacheStore,
    DateTime Function() now = DateTime.now,
  })  : _heart = heartRepository,
        _sleep = sleepRepository,
        _activity = activityRepository,
        _vitals = vitalsRepository,
        _health = healthRepository,
        _preferences = preferencesRepository,
        _cache = cacheStore,
        // ignore: prefer_initializing_formals
        _now = now;

  final HeartRepository _heart;
  final SleepRepository _sleep;
  final ActivityRepository _activity;
  final VitalsRepository _vitals;
  final HealthRepository _health;
  final PreferencesRepository _preferences;
  final BodyEnergyTimelineCacheStore _cache;
  final DateTime Function() _now;

  static const int _baselineDays = 28;
  // Kotlin `CurrentDayCacheMinutes` / `PastDayCacheHours` / `BaselineCacheHours`.
  static const int _currentDayCacheMinutes = 15;
  static const int _pastDayCacheHours = 24;
  static const int _baselineCacheHours = 24;

  @override
  Future<BodyEnergyTimelineResult> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async {
    final calibration = _preferences.bodyEnergyCalibrationListenable.value;
    final bodyProfile = _preferences.bodyProfileListenable.value;
    final permissionSignature = await _permissionSignature();

    final days = <BodyEnergyTimeline>[];
    var date = query.period.start;
    while (!date.isAfter(query.period.end)) {
      days.add(await _loadDay(
        date: date,
        refreshMode: query.refreshMode,
        permissionSignature: permissionSignature,
        calibration: calibration,
        bodyProfile: bodyProfile,
      ));
      date = date.plusDays(1);
    }
    return BodyEnergyTimelineResult(query: query, days: days);
  }

  Future<BodyEnergyTimeline> _loadDay({
    required LocalDate date,
    required RefreshMode refreshMode,
    required int permissionSignature,
    required BodyEnergyCalibration calibration,
    required BodyProfile bodyProfile,
  }) async {
    // Per-day signature: the body profile's age component varies by date, so
    // the signature is computed per day (Kotlin `loadDay`).
    final combinedSignature =
        '${calibration.signature()}|${bodyProfile.signature(today: date)}';
    final signature = _timelineSignature(combinedSignature, permissionSignature);

    final cached = _cache.load(date, signature);
    if (cached != null &&
        refreshMode == RefreshMode.normal &&
        !_timelineIsStale(cached, date)) {
      return cached;
    }

    final dayStart = localDayStart(date);
    final dayEnd = localDayEnd(date);
    final baselineStart = date.minusDays(_baselineDays);
    final baselineEnd = date.minusDays(1);

    final baselines = await _loadBaselines(
      date: date,
      baselineStart: baselineStart,
      baselineEnd: baselineEnd,
      dayStart: dayStart,
      signature: _baselineSignature(permissionSignature),
    );

    final heartRateSamples =
        await _heart.loadRawHeartRateSamplesForDayGraph(date);
    final hrvSamples = await _heart.loadHrvSamples(dayStart, dayEnd);
    final sleepSessions =
        await _sleep.loadSleepSessions(date.minusDays(1), date);
    final workouts = await _activity.loadWorkouts(date, date);
    // Kotlin loads respiratory only when a respiratory baseline exists (the
    // stress factor is inert without one).
    final List<RespiratoryRateEntry> respiratory =
        baselines.respiratoryRateBaseline != null
            ? await _vitals.loadRespiratoryRate(date, date)
            : const <RespiratoryRateEntry>[];
    final restingHr = await _heart.loadRestingHeartRate(date);
    // Kotlin seeds the day from the previous day's cached score.
    final previousEndScore =
        _cache.load(date.minusDays(1), signature)?.currentScore;

    final timeline = calculateBodyEnergyTimeline(
      BodyEnergyTimelineInputs(
        date: date,
        heartRateSamples: heartRateSamples,
        hrvSamples: hrvSamples,
        sleepSessions: sleepSessions,
        workouts: workouts,
        respiratoryRateSamples: respiratory,
        restingHeartRateBpm: restingHr,
        baselineRestingHeartRateBpm: baselines.baselineRestingHeartRateBpm,
        observedMaxHeartRateBpm: baselines.observedMaxHeartRateBpm,
        hrvBaselineRmssdMs: baselines.hrvBaselineRmssdMs,
        respiratoryRateBaseline: baselines.respiratoryRateBaseline,
        previousEndScore: previousEndScore,
        calibration: calibration,
        bodyProfile: bodyProfile,
      ),
    ).copyWith(signature: signature, generatedAt: _now());

    // Kotlin saves unconditionally, so a forced refresh also repopulates the
    // cache (and seeds the next day).
    await _cache.save(timeline);
    return timeline;
  }

  /// Kotlin `loadBaselines`: reuse a fresh cached baseline (this day or an
  /// adjacent one), else recompute the 28-day medians + observed max and cache.
  Future<BodyEnergyBaselineCacheEntry> _loadBaselines({
    required LocalDate date,
    required LocalDate baselineStart,
    required LocalDate baselineEnd,
    required DateTime dayStart,
    required String signature,
  }) async {
    final reusable = _loadReusableBaseline(date, signature);
    if (reusable != null && !_baselineIsStale(reusable)) return reusable;

    final baselineResting = DashboardAggregator.medianLongOrNull(
      (await _heart.loadDailyRestingHR(baselineStart, baselineEnd))
          .map((e) => e.bpm)
          .where((v) => v > 0)
          .toList(),
    );
    final baselineHrv = DashboardAggregator.medianDoubleValuesOrNull(
      (await _heart.loadDailyHRV(baselineStart, baselineEnd))
          .map((e) => e.rmssdMs)
          .where((v) => v > 0)
          .toList(),
    );
    // Observed max is taken over the whole baseline window (Kotlin), not just
    // the current day's samples.
    final baselineSamples = await _heart.loadHeartRateSamplesInstant(
      localDayStart(baselineStart),
      dayStart,
    );
    final observedMax = baselineSamples.isEmpty
        ? null
        : baselineSamples
            .map((s) => s.beatsPerMinute)
            .reduce((a, b) => math.max(a, b));

    final baseline = BodyEnergyBaselineCacheEntry(
      baselineRestingHeartRateBpm: baselineResting?.round(),
      observedMaxHeartRateBpm: observedMax,
      hrvBaselineRmssdMs: baselineHrv,
      respiratoryRateBaseline: reusable?.respiratoryRateBaseline,
      generatedAt: _now(),
    );
    await _cache.saveBaseline(date, signature, baseline);
    return baseline;
  }

  BodyEnergyBaselineCacheEntry? _loadReusableBaseline(
    LocalDate date,
    String signature,
  ) {
    final exact = _cache.loadBaseline(date, signature);
    if (exact != null && !_baselineIsStale(exact)) return exact;

    for (final adjacentDate in [date.minusDays(1), date.plusDays(1)]) {
      final adjacent = _cache.loadBaseline(adjacentDate, signature);
      if (adjacent != null && !_baselineIsStale(adjacent)) {
        unawaited(_cache.saveBaseline(date, signature, adjacent));
        return adjacent;
      }
    }
    return null;
  }

  Future<int> _permissionSignature() async {
    try {
      if (_health.availability() != HealthConnectAvailability.available) {
        return 0;
      }
      final granted = (await _health.grantedPermissions()).toList()..sort();
      return granted.join(',').hashCode;
    } catch (_) {
      return 0;
    }
  }

  String _timelineSignature(String combinedSignature, int permissionSignature) =>
      'v$bodyEnergyTimelineAlgorithmVersion'
      '|${combinedSignature.hashCode}|$permissionSignature';

  String _baselineSignature(int permissionSignature) =>
      'v$bodyEnergyTimelineAlgorithmVersion|baseline|$permissionSignature';

  bool _timelineIsStale(BodyEnergyTimeline timeline, LocalDate date) {
    final generatedAt = timeline.generatedAt ?? _now();
    final age = _now().difference(generatedAt);
    return date == LocalDate.fromDateTime(_now())
        ? age.inMinutes >= _currentDayCacheMinutes
        : age.inHours >= _pastDayCacheHours;
  }

  bool _baselineIsStale(BodyEnergyBaselineCacheEntry baseline) =>
      _now().difference(baseline.generatedAt).inHours >= _baselineCacheHours;
}
