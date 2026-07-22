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
/// A watermark records the newest sample already fitted, and only strictly
/// newer ones are fed in — so re-syncing an overlapping window, which a watch
/// does constantly, cannot re-teach the model the same lesson twice.
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
    final watermark = _preferences.bodyEnergyWatchFitWatermarkMillis;
    final from = watermark > 0
        ? watermark
        : at.subtract(_maxLookback).millisecondsSinceEpoch;

    final List<GarminWellnessSample> samples;
    try {
      samples = await _dao.samplesBetween(
        GarminWellnessMetric.bodyEnergy,
        // Strictly newer than the watermark: the boundary sample was already
        // counted by the previous run.
        from + 1,
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
    var newestFitted = watermark;
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
      for (final reading in readings) {
        final ms = reading.time.toUtc().millisecondsSinceEpoch;
        if (ms > newestFitted) newestFitted = ms;
      }
    }

    if (fitted > 0) {
      // Advanced past every sample examined in this run, not merely the ones
      // that paired, so unpairable readings are not reconsidered forever. Only
      // reached when something WAS fitted: a run that produced nothing (no
      // timeline available yet) leaves the watermark alone so those readings
      // get another chance once the day's data fills in.
      final newestSeen = samples
          .map((s) => s.timeMillis)
          .reduce((a, b) => a > b ? a : b);
      _preferences.bodyEnergyWatchFitWatermarkMillis =
          newestSeen > newestFitted ? newestSeen : newestFitted;
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
