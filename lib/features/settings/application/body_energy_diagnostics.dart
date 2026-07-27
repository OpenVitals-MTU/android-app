import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../data/local/open_vitals_database.dart';
import '../../../data/repository/contract/body_energy_repository.dart';
import '../../../data/repository/impl/repository_time.dart';
import '../../../di/providers.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/insights/body_energy_timeline.dart';
import '../../../domain/insights/body_energy_watch_observations.dart';
import '../../../domain/model/health_source_totals.dart';
import '../../../domain/preferences/body_energy_calibration.dart';

/// How many days the calibration report covers.
///
/// Seven, not thirty. It matches the chain's settling window, so every day in
/// the report is still recompute-eligible and what is stored is what the CURRENT
/// constants produce — a longer window would mix in days frozen under older
/// gains and read as a constants problem that is really an archive. It also sits
/// safely inside Health Connect's 30-day horizon for an install without the
/// history grant, and it fits on one screen.
const int bodyEnergyDiagnosticsDays = 7;

/// Full watch resolution for the report.
///
/// [watchObservationBucket] is an hour because the calibration path must not let
/// one day of watch readings outvote months of the user's own check-ins. A
/// read-only report has no such worry and wants every sample it can pair.
const Duration bodyEnergyDiagnosticsBucket = Duration(minutes: 5);

/// How many downsample buckets a day's samples occupy — the ceiling on how
/// many observations can pair.
int _distinctBuckets(List<WatchBodyEnergySample> samples, Duration bucket) {
  final size = bucket.inMilliseconds;
  return samples
      .map((s) => s.time.toUtc().millisecondsSinceEpoch ~/ size)
      .toSet()
      .length;
}

/// The day totals the model actually consumed, as read from Health Connect.
///
/// Deliberately the *un-deduplicated* aggregate figures — the whole point is to
/// hold "what the model ate" next to "what each source served".
class ActivityProgressTotals {
  const ActivityProgressTotals({this.activeKcal, this.totalKcal, this.steps});

  final double? activeKcal;
  final double? totalKcal;
  final int? steps;
}

/// Folds one window of already-read facts into the Body Energy calibration
/// report.
///
/// Pure, and every argument is a plain read result — the provider does the I/O
/// and hands it in. That split is what lets the whole report be unit-tested with
/// no Health Connect, no database and no watch.
///
/// The report exists to separate two hypotheses the Body Energy screen cannot
/// tell apart: the drain CONSTANTS are too hot, or the calorie INPUT is doubled
/// because two apps write the same watch's data and Health Connect's aggregates
/// sum them. Every column is chosen to discriminate between those two.
BodyEnergyDiagnosticsReport buildBodyEnergyDiagnostics({
  required List<BodyEnergyTimeline> days,
  required Map<int, List<WatchBodyEnergySample>> watchSamplesByEpochDay,
  required Map<int, ActivityProgressTotals> modelInputByEpochDay,
  required List<SourceDayTotal> sourceTotals,
  required BodyEnergyCalibration calibration,
  int watchSampleCount = 0,
  Set<String> missingPermissions = const {},
  bool truncated = false,
  Duration bucket = bodyEnergyDiagnosticsBucket,
}) {
  final reportDays = <BodyEnergyDiagnosticsDay>[];
  final errorsByInfluence =
      <BodyEnergyPrimaryInfluence, List<double>>{};

  for (final timeline in days) {
    final epochDay = timeline.date.epochDay;
    final samples = watchSamplesByEpochDay[epochDay] ?? const [];
    final observations = buildWatchObservations(
      samples: samples,
      timeline: timeline,
      bucket: bucket,
    );
    for (final observation in observations) {
      errorsByInfluence
          .putIfAbsent(observation.dominantInfluence, () => <double>[])
          .add((observation.observedScore - observation.predictedScore)
              .toDouble());
    }

    final watch = _watchDayTotals(samples, bucket);
    reportDays.add(
      _dayReport(
        timeline: timeline,
        input: modelInputByEpochDay[epochDay] ?? const ActivityProgressTotals(),
        watch: watch,
        watchSampleCount: samples.length,
        // Buckets, not raw samples. The watch emits about one a minute and
        // `buildWatchObservations` keeps one per bucket, so comparing
        // observations against the raw count made full coverage read as an 80%
        // pairing failure.
        watchBucketCount: _distinctBuckets(samples, bucket),
        pairedSampleCount: observations.length,
      ),
    );
  }

  final influences = [
    for (final entry in errorsByInfluence.entries)
      BodyEnergyInfluenceError(
        influence: entry.key,
        observationCount: entry.value.length,
        meanSignedError:
            entry.value.reduce((a, b) => a + b) / entry.value.length,
        meanAbsoluteError: entry.value.map((e) => e.abs()).reduce((a, b) => a + b) /
            entry.value.length,
      ),
  ]..sort((a, b) =>
      b.meanSignedError.abs().compareTo(a.meanSignedError.abs()));

  final sources = [...sourceTotals]
    ..sort((a, b) => b.total.compareTo(a.total));

  return BodyEnergyDiagnosticsReport(
    days: reportDays..sort((a, b) => a.date.epochDay.compareTo(b.date.epochDay)),
    influences: influences,
    sources: sources,
    calibration: calibration,
    storedWatchSampleCount: watchSampleCount,
    missingPermissions: missingPermissions,
    truncated: truncated,
  );
}

BodyEnergyDiagnosticsDay _dayReport({
  required BodyEnergyTimeline timeline,
  required ActivityProgressTotals input,
  required _WatchDayTotals watch,
  required int watchSampleCount,
  required int watchBucketCount,
  required int pairedSampleCount,
}) {
  var basal = 0.0;
  var stress = 0.0;
  var recoveryDebt = 0.0;
  var charge = 0.0;
  var fromEnergy = 0.0;
  var fromIntensity = 0.0;
  var energyWon = 0;
  var intensityWon = 0;
  var floorBuckets = 0;
  var ceilingBuckets = 0;
  DateTime? firstFloorTime;

  for (final point in timeline.points) {
    basal += point.basalDrain;
    stress += point.stressDrain;
    recoveryDebt += point.recoveryDebtDrain;
    charge += point.charge;

    // The applied activity drain is the STRONGER of the two estimates, never
    // their sum — summing them would double-count every bucket and is the
    // easiest mistake to make in this file.
    final applied = math.max(point.intensityDrain, point.activityEnergyDrain);
    if (applied > 0.0) {
      if (point.activityEnergyDrain >= point.intensityDrain) {
        fromEnergy += applied;
        energyWon += 1;
      } else {
        fromIntensity += applied;
        intensityWon += 1;
      }
    }

    if (point.score <= 0) {
      floorBuckets += 1;
      firstFloorTime ??= point.time;
    }
    if (point.score >= 100) ceilingBuckets += 1;
  }

  return BodyEnergyDiagnosticsDay(
    date: timeline.date,
    startScore: timeline.startScore,
    endScore: timeline.currentScore,
    charged: timeline.charged,
    drained: timeline.drained,
    ledgerOk: timeline.startScore + timeline.charged - timeline.drained ==
        timeline.currentScore,
    floorBuckets: floorBuckets,
    ceilingBuckets: ceilingBuckets,
    firstFloorTime: firstFloorTime,
    basalDrain: basal,
    activityFromEnergy: fromEnergy,
    activityFromIntensity: fromIntensity,
    energyWonBuckets: energyWon,
    intensityWonBuckets: intensityWon,
    stressDrain: stress,
    recoveryDebtDrain: recoveryDebt,
    chargeTotal: charge,
    activeKcal: input.activeKcal,
    totalKcal: input.totalKcal,
    steps: input.steps,
    watchSampleCount: watchSampleCount,
    watchBucketCount: watchBucketCount,
    pairedSampleCount: pairedSampleCount,
    watchStart: watch.start,
    watchEnd: watch.end,
    watchMin: watch.min,
    watchMax: watch.max,
    watchCharged: watch.charged,
    watchDrained: watch.drained,
  );
}

class _WatchDayTotals {
  const _WatchDayTotals({
    this.start,
    this.end,
    this.min,
    this.max,
    this.charged,
    this.drained,
  });

  final int? start;
  final int? end;
  final int? min;
  final int? max;
  final double? charged;
  final double? drained;
}

/// The watch's own charge and drain for a day, as DELTA SUMS rather than
/// `start - end`.
///
/// Both models clip at 0 and 100, so comparing endpoints understates a day that
/// pinned. Summing the ups and downs separately is unaffected by clipping as
/// long as the series is not pinned — which [_WatchDayTotals.min] and `.max`
/// let the reader check.
///
/// Downsampled with the same first-in-bucket rule [buildWatchObservations] uses,
/// so the totals and the per-influence errors describe the same samples.
_WatchDayTotals _watchDayTotals(
  List<WatchBodyEnergySample> samples,
  Duration bucket,
) {
  if (samples.isEmpty) return const _WatchDayTotals();
  final sorted = [...samples]..sort((a, b) => a.time.compareTo(b.time));
  final bucketMs = bucket.inMilliseconds;
  final kept = <WatchBodyEnergySample>[];
  int? lastBucket;
  for (final sample in sorted) {
    final index = sample.time.toUtc().millisecondsSinceEpoch ~/ bucketMs;
    if (index == lastBucket) continue;
    lastBucket = index;
    kept.add(sample);
  }

  var charged = 0.0;
  var drained = 0.0;
  for (var i = 1; i < kept.length; i++) {
    final delta = kept[i].score - kept[i - 1].score;
    if (delta > 0) {
      charged += delta;
    } else {
      drained += -delta;
    }
  }

  return _WatchDayTotals(
    start: kept.first.score,
    end: kept.last.score,
    min: kept.map((s) => s.score).reduce(math.min),
    max: kept.map((s) => s.score).reduce(math.max),
    charged: charged,
    drained: drained,
  );
}

/// One day's model output, its inputs, and the watch's view of the same day.
class BodyEnergyDiagnosticsDay {
  const BodyEnergyDiagnosticsDay({
    required this.date,
    required this.startScore,
    required this.endScore,
    required this.charged,
    required this.drained,
    required this.ledgerOk,
    required this.floorBuckets,
    required this.ceilingBuckets,
    required this.firstFloorTime,
    required this.basalDrain,
    required this.activityFromEnergy,
    required this.activityFromIntensity,
    required this.energyWonBuckets,
    required this.intensityWonBuckets,
    required this.stressDrain,
    required this.recoveryDebtDrain,
    required this.chargeTotal,
    required this.activeKcal,
    required this.totalKcal,
    required this.steps,
    required this.watchSampleCount,
    required this.watchBucketCount,
    required this.pairedSampleCount,
    required this.watchStart,
    required this.watchEnd,
    required this.watchMin,
    required this.watchMax,
    required this.watchCharged,
    required this.watchDrained,
  });

  final LocalDate date;

  final int startScore;
  final int endScore;
  final int charged;
  final int drained;

  /// `start + charged - drained == end`. False means the accounting has drifted.
  final bool ledgerOk;

  /// Buckets pinned at 0 and at 100.
  ///
  /// The headline `drained` cannot show over-draining any more: since the totals
  /// became applied rather than gross, a day that wants 250 points of drain and
  /// one that wants 60 read identically once they hit the floor. This is what
  /// makes the difference visible again.
  final int floorBuckets;
  final int ceilingBuckets;
  final DateTime? firstFloorTime;

  final double basalDrain;

  /// The applied activity drain, split by which estimate won the per-bucket
  /// `max`. Their SUM is the applied activity drain; neither is a share of it.
  final double activityFromEnergy;
  final double activityFromIntensity;
  final int energyWonBuckets;
  final int intensityWonBuckets;

  final double stressDrain;
  final double recoveryDebtDrain;
  final double chargeTotal;

  /// What the model consumed, from the un-deduplicated Health Connect aggregate.
  final double? activeKcal;
  final double? totalKcal;
  final int? steps;

  final int watchSampleCount;

  /// How many downsample buckets those samples fall into — the number that can
  /// actually pair, since `buildWatchObservations` keeps one sample per bucket.
  final int watchBucketCount;

  /// How many buckets paired with a timeline point. A low yield against
  /// [watchBucketCount] means the influence table is speaking for a fraction of
  /// the day; against [watchSampleCount] it would only mean the watch samples
  /// faster than the buckets.
  final int pairedSampleCount;

  final int? watchStart;
  final int? watchEnd;
  final int? watchMin;
  final int? watchMax;
  final double? watchCharged;
  final double? watchDrained;

  double get activityApplied => activityFromEnergy + activityFromIntensity;

  /// Model drain minus the watch's. Positive means this app drained harder.
  double? get drainError =>
      watchDrained == null ? null : drained - watchDrained!;

  double? get chargeError =>
      watchCharged == null ? null : charged - watchCharged!;
}

/// Mean model-vs-watch error for one influence.
///
/// A caveat worth stating: a paired error at time T is the ACCUMULATED
/// divergence since the two models last agreed, not an instantaneous
/// attribution to that bucket's influence. That is exactly what
/// `fitBodyEnergyGains` assumes, so this table explains why the gains drifted
/// where they did — it is not a per-component residual and must not be read as
/// one.
class BodyEnergyInfluenceError {
  const BodyEnergyInfluenceError({
    required this.influence,
    required this.observationCount,
    required this.meanSignedError,
    required this.meanAbsoluteError,
  });

  final BodyEnergyPrimaryInfluence influence;
  final int observationCount;

  /// `observed - predicted`. Negative means the model sat below the watch, i.e.
  /// it drained harder than the watch did.
  final double meanSignedError;
  final double meanAbsoluteError;
}

class BodyEnergyDiagnosticsReport {
  const BodyEnergyDiagnosticsReport({
    required this.days,
    required this.influences,
    required this.sources,
    required this.calibration,
    required this.storedWatchSampleCount,
    required this.missingPermissions,
    required this.truncated,
  });

  final List<BodyEnergyDiagnosticsDay> days;
  final List<BodyEnergyInfluenceError> influences;

  /// Per-source day totals, biggest first.
  final List<SourceDayTotal> sources;

  final BodyEnergyCalibration calibration;

  /// Watch body-energy samples held locally. Zero distinguishes "the watch
  /// disagrees" from "the watch has never synced".
  final int storedWatchSampleCount;

  /// Read permissions the report needed and did not have. Without this, "the app
  /// wrote nothing" and "you never granted it" render identically.
  final Set<String> missingPermissions;

  /// Whether the raw per-source read hit its record cap. Paging is time-ordered,
  /// so a truncated read drops whole days off one end and skews the proportions
  /// — they must not be presented as fact.
  final bool truncated;

  /// The packages that wrote active calories, per day, where more than one did.
  Iterable<SourceDayTotal> get activeCalorieSources => sources
      .where((s) => s.metric == HealthRecordSourceMetric.activeCalories);

  bool get hasMultipleCalorieSources {
    final byDay = <int, Set<String>>{};
    for (final source in activeCalorieSources) {
      byDay.putIfAbsent(source.date.epochDay, () => <String>{}).add(source.package);
    }
    return byDay.values.any((packages) => packages.length > 1);
  }

  /// Active kcal attributable to every package but the largest, across the
  /// window — the size of the double count, if it is one.
  double get secondarySourceActiveKcal {
    final byPackage = <String, double>{};
    for (final source in activeCalorieSources) {
      byPackage[source.package] = (byPackage[source.package] ?? 0.0) + source.total;
    }
    if (byPackage.length < 2) return 0.0;
    final totals = byPackage.values.toList()..sort();
    return totals.take(totals.length - 1).fold(0.0, (a, b) => a + b);
  }

  /// The whole report as plain text, for the clipboard.
  ///
  /// Every date and number is formatted explicitly — never a bare
  /// `DateTime.toString()` — so the output is locale- and timezone-stable and
  /// can be asserted against a literal in a test.
  String toReportText() {
    final out = StringBuffer()
      ..writeln('Body Energy calibration report')
      ..writeln('algorithm v$bodyEnergyTimelineAlgorithmVersion, '
          '${days.length} day(s), bucket '
          '${bodyEnergyDiagnosticsBucket.inMinutes}m')
      ..writeln('gains: sleep ${_g(calibration.sleepChargeGain)} '
          'activity ${_g(calibration.activityDrainGain)} '
          'basal ${_g(calibration.basalDrainGain)} '
          'stress ${_g(calibration.stressDrainGain)}')
      ..writeln('checks: ${calibration.watchObservationCount} watch; '
          '$storedWatchSampleCount watch samples stored');
    if (missingPermissions.isNotEmpty) {
      final missing = missingPermissions.toList()..sort();
      out.writeln('MISSING PERMISSIONS: ${missing.join(', ')}');
    }
    if (truncated) {
      out.writeln('WARNING: per-source read hit its cap; '
          'the source split below is truncated, not proportional');
    }

    out.writeln('');
    out.writeln('per day');
    for (final day in days) {
      out
        ..writeln('  ${day.date}  start ${day.startScore} '
            '+${day.charged} -${day.drained} end ${day.endScore}'
            '${day.ledgerOk ? '' : '  LEDGER MISMATCH'}')
        ..writeln('    floor ${day.floorBuckets}b'
            '${day.firstFloorTime == null ? '' : ' from '
                '${_hm(day.firstFloorTime!)}'}'
            '  ceiling ${day.ceilingBuckets}b')
        ..writeln('    drain: basal ${_n(day.basalDrain)}'
            '  activity ${_n(day.activityApplied)}'
            ' (kcal ${_n(day.activityFromEnergy)}/${day.energyWonBuckets}b,'
            ' zone ${_n(day.activityFromIntensity)}/${day.intensityWonBuckets}b)'
            '  stress ${_n(day.stressDrain)}'
            '  debt ${_n(day.recoveryDebtDrain)}'
            '  charge ${_n(day.chargeTotal)}')
        ..writeln('    input: activeKcal ${_o(day.activeKcal)}'
            '  totalKcal ${_o(day.totalKcal)}'
            '  steps ${day.steps ?? '--'}')
        ..writeln('    watch: ${day.watchStart ?? '--'}->${day.watchEnd ?? '--'}'
            ' (min ${day.watchMin ?? '--'} max ${day.watchMax ?? '--'})'
            '  +${_o(day.watchCharged)} -${_o(day.watchDrained)}'
            '  paired ${day.pairedSampleCount}/${day.watchBucketCount}b'
            ' of ${day.watchSampleCount} samples'
            '  drainErr ${_o(day.drainError)}');
    }

    out
      ..writeln('')
      ..writeln('per influence (observed - predicted)');
    if (influences.isEmpty) {
      out.writeln('  none paired');
    }
    for (final influence in influences) {
      out.writeln('  ${influence.influence.storageName}: '
          'mean ${_n(influence.meanSignedError)} '
          'abs ${_n(influence.meanAbsoluteError)} '
          'n=${influence.observationCount}');
    }

    out
      ..writeln('')
      ..writeln('per source');
    if (sources.isEmpty) {
      out.writeln('  none');
    }
    for (final source in sources) {
      // `coveredMinutes` SUMS each record's in-day span, and the native reader
      // clips every record to the day. So a figure above the day's own length
      // is arithmetic proof that this app's records overlap each other — the
      // day is being counted more than once, and no aggregate above will say
      // so. Naming it here is the difference between a number that looks large
      // and a number that is wrong.
      final overlap = source.coveredMinutes - _minutesInDay;
      out.writeln('  ${source.date} ${source.metric.wireName} '
          '${source.package}: ${_n(source.total)} ${source.metric.unit}, '
          '${source.recordCount} rec (${source.manualEntryCount} manual), '
          '${_n(source.coveredMinutes)} min covered '
          '${_hm(source.firstStart)}->${_hm(source.lastEnd)}'
          '${overlap > 1.0 ? '  OVERLAP +${_n(overlap)} min' : ''}');
    }
    if (hasMultipleCalorieSources) {
      out.writeln('  => MULTIPLE apps wrote active calories; '
          '${_n(secondarySourceActiveKcal)} kcal came from other than the '
          'largest source');
    }
    return out.toString();
  }

  /// A nominal day. DST makes a real one 23 or 25 hours, which only shifts the
  /// threshold this compares against by an hour — far below the margin that
  /// makes an overlap worth reporting.
  static const double _minutesInDay = 1440.0;

  static String _g(double value) => value.toStringAsFixed(2);
  static String _n(double value) => value.toStringAsFixed(1);
  static String _o(double? value) => value == null ? '--' : _n(value);
  static String _hm(DateTime time) {
    final local = time.toLocal();
    final hh = local.hour.toString().padLeft(2, '0');
    final mm = local.minute.toString().padLeft(2, '0');
    return '$hh:$mm';
  }
}

/// The Body Energy calibration report for the last [bodyEnergyDiagnosticsDays]
/// days.
///
/// Same shape as `healthConnectSourcesProvider`: every read happens here and the
/// folding happens in [buildBodyEnergyDiagnostics], so the report itself stays
/// testable without a device.
///
/// Deliberately NOT auto-loaded — the card fires it on a button. A cold run is
/// on the order of sixty Health Connect calls and the platform charges quota per
/// call, which is not something a settings screen should do on open.
final bodyEnergyDiagnosticsProvider =
    FutureProvider.autoDispose<BodyEnergyDiagnosticsReport>((ref) async {
  final repository = ref.watch(bodyEnergyRepositoryProvider);
  final dao = ref.watch(garminWellnessDaoProvider);
  final health = ref.watch(healthDataSourceProvider);
  final calibration =
      ref.watch(preferencesRepositoryProvider).bodyEnergyCalibration();

  final today = LocalDate.now();
  final from = today.minusDays(bodyEnergyDiagnosticsDays - 1);
  final windowStart = localDayStart(from);
  final windowEnd = localDayEnd(today);

  // Every reader degrades a missing permission to empty, so without this probe
  // "nothing wrote calories" and "you never granted it" render identically.
  final granted = await health.grantedPermissions();
  final missing = {
    for (final permission in [
      HcPermissions.readActiveCalories,
      HcPermissions.readTotalCalories,
      HcPermissions.readSteps,
      HcPermissions.readHeartRate,
    ])
      if (!granted.contains(permission)) permission.split('.').last,
  };

  // RefreshMode.normal is load-bearing: settled days serve from SQLite instead
  // of costing ~8 Health Connect reads each.
  final result = await repository.loadTimeline(
    BodyEnergyTimelineQuery(
      period: DatePeriod(from, today),
      range: TimeRange.week,
      refreshMode: RefreshMode.normal,
    ),
  );
  final days = switch (result) {
    Ok(:final value) => value.days,
    Err() => const <BodyEnergyTimeline>[],
  };

  final samples = await dao.samplesBetween(
    GarminWellnessMetric.bodyEnergy,
    windowStart.millisecondsSinceEpoch,
    windowEnd.millisecondsSinceEpoch,
  );
  final watchByDay = <int, List<WatchBodyEnergySample>>{};
  for (final sample in samples) {
    final time = DateTime.fromMillisecondsSinceEpoch(sample.timeMillis);
    watchByDay
        .putIfAbsent(LocalDate.fromDateTime(time).epochDay, () => [])
        .add(WatchBodyEnergySample(time: time, score: sample.value));
  }

  final inputByDay = <int, ActivityProgressTotals>{};
  for (var date = from; !date.isAfter(today); date = date.plusDays(1)) {
    final progress = await health.readRawActivityProgress(date);
    if (progress.isEmpty) continue;
    final last = progress.last;
    inputByDay[date.epochDay] = ActivityProgressTotals(
      activeKcal: last.totalActiveCaloriesKcal,
      totalKcal: last.totalCaloriesBurnedKcal,
      steps: last.totalSteps,
    );
  }

  final sources = <SourceDayTotal>[];
  for (final metric in const [
    HealthRecordSourceMetric.activeCalories,
    HealthRecordSourceMetric.totalCalories,
    HealthRecordSourceMetric.steps,
  ]) {
    sources.addAll(
      await health.readSourceDayTotals(metric, windowStart, windowEnd),
    );
  }

  return buildBodyEnergyDiagnostics(
    days: days,
    watchSamplesByEpochDay: watchByDay,
    modelInputByEpochDay: inputByDay,
    sourceTotals: sources,
    calibration: calibration,
    watchSampleCount:
        await dao.countFor(GarminWellnessMetric.bodyEnergy),
    missingPermissions: missing,
    // Paging is time-ordered, so hitting the cap drops whole days off one end.
    truncated: sources.fold<int>(0, (sum, s) => sum + s.recordCount) >=
        bodyEnergySourceRecordCap,
  );
});

/// Mirrors the host's own record cap. If the summed record count reaches it the
/// per-source split is truncated rather than proportional, and must be labelled
/// as such rather than read as fact.
const int bodyEnergySourceRecordCap = 20000;
