import 'package:flutter/foundation.dart';

import '../../core/period/time_range.dart';
import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/local/open_vitals_database.dart';
import '../../data/prefs/preferences_repository.dart';
import '../../data/repository/contract/body_energy_repository.dart';
import '../insights/body_energy_calibration_fit.dart';
import '../insights/body_energy_watch_observations.dart';

/// Folds newly-synced watch Body Battery readings into the personal gains.
///
/// Follows the feel-check rule exactly: **each observation is counted once**.
///
/// The unit counted is an hour BUCKET, not a sample. A watermark records the
/// last bucket already fitted and only later buckets are considered, so an hour
/// contributes exactly one observation however many times the watch is synced
/// during it. Keying on the newest sample instead made the learning rate depend
/// on how often the user tapped Sync — ten syncs an hour taught the model ten
/// times as fast as one, from identical watch data.
///
/// Everything here is best-effort. Calibration is an enhancement, so a failure
/// to fit must never fail the sync that triggered it: the watermark simply does
/// not advance and the readings are retried next time.
class FitBodyEnergyFromWatchUseCase {
  const FitBodyEnergyFromWatchUseCase(
    this._dao,
    this._preferences,
    this._bodyEnergyRepository,
  );

  final GarminWellnessDao _dao;
  final PreferencesRepository _preferences;
  final BodyEnergyRepository _bodyEnergyRepository;

  /// How far back to look for unfitted samples on a first run, so an install
  /// with months of history does not try to fit all of it at once.
  static const Duration _maxLookback = Duration(days: 7);

  /// How long a day with no timeline is waited for before it is retired.
  ///
  /// The watermark is one scalar, so holding for a day that yields nothing holds
  /// every day after it too. Waiting is right when the reason is "the chain has
  /// not reached it yet", which resolves within a warm pass or two; it is wrong
  /// when the day has no heart data and never will, because then the wait never
  /// ends. Two days separates them about as well as anything can, and bounds the
  /// damage either way — where [_maxLookback] as the cutoff meant a single
  /// permanently-cold day inside the window blocked the whole refit behind it.
  static const Duration _coldDayGrace = Duration(days: 2);

  /// Returns how many observations were folded in.
  Future<int> call({DateTime? now}) async {
    final at = (now ?? DateTime.now()).toUtc();
    final bucketMs = watchObservationBucket.inMilliseconds;
    final fittedBucketStart = _preferences.bodyEnergyWatchFitWatermarkMillis;
    // Start of the first bucket not yet fitted.
    final from = fittedBucketStart > 0
        ? fittedBucketStart + bucketMs
        : at.subtract(_maxLookback).millisecondsSinceEpoch;

    final List<GarminWellnessSample> samples;
    try {
      samples = await _dao.samplesBetween(
        GarminWellnessMetric.bodyEnergy,
        from,
        at.millisecondsSinceEpoch + 1,
      );
    } catch (error) {
      debugPrint('[BODY-ENERGY-FIT] could not read samples: $error');
      return 0;
    }
    if (samples.isEmpty) return 0;

    // Group by the local day each sample belongs to: a timeline is computed per
    // day, and pairing needs the one covering the sample's own moment.
    final byDay = <LocalDate, List<WatchBodyEnergySample>>{};
    for (final sample in samples) {
      final time =
          DateTime.fromMillisecondsSinceEpoch(sample.timeMillis, isUtc: true)
              .toLocal();
      byDay
          .putIfAbsent(LocalDate.fromDateTime(time), () => [])
          .add(WatchBodyEnergySample(time: time, score: sample.value));
    }

    // Oldest first, because the watermark is a single scalar: it can only ever
    // say "everything before here is done", so the days have to be retired in
    // order for that to stay true.
    final days = byDay.keys.toList()..sort();
    final coldDayCutoff =
        LocalDate.fromDateTime(at.toLocal()).minusDays(_coldDayGrace.inDays);

    var fitted = 0;
    int? retiredThrough;
    for (final date in days) {
      final daySamples = byDay[date]!;
      final readings = await _observationsForDay(date, daySamples);

      if (readings.isEmpty) {
        // A day with no timeline yet — the chain has not reached it, or the
        // permissions were not there when it was asked. Its readings are not
        // unpairable, merely early, so stop and leave the whole remainder for
        // the next run.
        //
        // Advancing over it is what made the watermark lossy: it used to jump
        // to the newest bucket of every sample READ the moment any single day
        // fitted, so one warm day silently retired a week of evidence that had
        // never been examined. Combined with the algorithm-bump reset in
        // BodyEnergyRepositoryImpl, that left the gains pinned at 1.0 with
        // thousands of stored samples they were no longer allowed to see.
        if (date.isAfter(coldDayCutoff)) break;
        // Old enough that waiting has stopped being a bet on the chain catching
        // up, so retire it: it is the only thing that keeps the watermark moving
        // and lets the days behind it be read at all.
        retiredThrough = _newestBucket(daySamples, bucketMs);
        continue;
      }

      _preferences.setBodyEnergyCalibration(
        fitBodyEnergyGains(
          _preferences.bodyEnergyCalibration(),
          const [],
          watchReadings: readings,
        ),
      );
      fitted += readings.length;
      // Every bucket of a day that HAS a timeline is retired, not just the ones
      // that paired: within such a day a reading that found no point within the
      // pairing gap never will.
      retiredThrough = _newestBucket(daySamples, bucketMs);
    }

    if (retiredThrough != null) {
      _preferences.bodyEnergyWatchFitWatermarkMillis =
          retiredThrough * bucketMs;
    }
    if (fitted > 0) {
      debugPrint('[BODY-ENERGY-FIT] folded $fitted watch readings into the '
          'gains (${_preferences.bodyEnergyCalibration().watchObservationCount} '
          'total)');
    }
    return fitted;
  }

  int _newestBucket(List<WatchBodyEnergySample> samples, int bucketMs) => samples
      .map((s) => s.time.millisecondsSinceEpoch ~/ bucketMs)
      .reduce((a, b) => a > b ? a : b);

  Future<List<BodyEnergyWatchReading>> _observationsForDay(
    LocalDate date,
    List<WatchBodyEnergySample> samples,
  ) async {
    final result = await _bodyEnergyRepository.loadTimeline(
      BodyEnergyTimelineQuery(
        period: DatePeriod(date, date),
        range: TimeRange.day,
      ),
    );
    switch (result) {
      case Err():
        // No timeline for that day (missing permissions, no heart data) — the
        // readings simply have nothing to be compared against.
        return const [];
      case Ok(:final value):
        final timeline = value.days.where((d) => d.date == date).firstOrNull ??
            value.latestDay;
        if (timeline == null) return const [];
        return buildWatchObservations(samples: samples, timeline: timeline);
    }
  }
}
