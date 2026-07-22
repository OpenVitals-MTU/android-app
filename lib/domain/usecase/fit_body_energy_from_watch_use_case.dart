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

    var fitted = 0;
    for (final entry in byDay.entries) {
      final readings = await _observationsForDay(entry.key, entry.value);
      if (readings.isEmpty) continue;
      _preferences.setBodyEnergyCalibration(
        fitBodyEnergyGains(
          _preferences.bodyEnergyCalibration(),
          const [],
          watchReadings: readings,
        ),
      );
      fitted += readings.length;
    }

    if (fitted > 0) {
      // Advanced past every bucket examined, not merely the ones that paired,
      // so unpairable readings are not reconsidered forever. Only reached when
      // something WAS fitted: a run that produced nothing (no timeline yet)
      // leaves the watermark alone so those readings get another chance once
      // the day's data fills in.
      final newestBucket = samples
          .map((s) => s.timeMillis ~/ bucketMs)
          .reduce((a, b) => a > b ? a : b);
      _preferences.bodyEnergyWatchFitWatermarkMillis = newestBucket * bucketMs;
      debugPrint('[BODY-ENERGY-FIT] folded $fitted watch readings into the '
          'gains (${_preferences.bodyEnergyCalibration().watchObservationCount} '
          'total)');
    }
    return fitted;
  }

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
