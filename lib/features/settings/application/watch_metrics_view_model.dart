import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/local/open_vitals_database.dart';
import '../../../di/providers.dart';

/// One stored watch metric, resolved to a value and the instant it was measured.
@immutable
class WatchMetricReading {
  const WatchMetricReading({required this.value, required this.time});

  final int value;
  final DateTime time;
}

/// Everything the app holds that Health Connect cannot: the latest value of each
/// watch-only metric, plus the day series for the two that have one.
///
/// Read-only and derived — nothing here decides what to store, only what the
/// device view and the watch-data screen can show. A metric the watch has never
/// sent is simply absent, which is what lets both screens omit it rather than
/// render a permanent blank row.
@immutable
class WatchMetrics {
  const WatchMetrics({
    this.latest = const {},
    this.stressToday = const [],
    this.bodyEnergyToday = const [],
    this.intensityMinutesWeek,
  });

  final Map<GarminWellnessMetric, WatchMetricReading> latest;

  /// Today's series, oldest first, for the metrics dense enough to draw.
  final List<WatchMetricReading> stressToday;
  final List<WatchMetricReading> bodyEnergyToday;

  /// Intensity minutes accumulated across the current week (Monday-anchored),
  /// vigorous counted double as Garmin counts them towards the weekly goal.
  /// Null when the watch has sent no intensity minutes at all. This is a
  /// week-long sum of daily finals, not the single latest reading: the watch
  /// stores a *running daily total* that resets each midnight, so the latest
  /// value is only today's — using it for the weekly goal understates the week.
  final int? intensityMinutesWeek;

  bool get isEmpty => latest.isEmpty;

  WatchMetricReading? operator [](GarminWellnessMetric metric) => latest[metric];

  int? valueOf(GarminWellnessMetric metric) => latest[metric]?.value;

  /// The metrics this watch has never sent, in declaration order — what the UI
  /// names once at the foot of the screen instead of showing empty rows.
  List<GarminWellnessMetric> missingFrom(List<GarminWellnessMetric> expected) =>
      [for (final m in expected) if (!latest.containsKey(m)) m];
}

/// Loads [WatchMetrics]. Invalidated by a sync so the screens refresh with it.
final watchMetricsProvider = FutureProvider.autoDispose<WatchMetrics>((ref) async {
  final dao = ref.watch(garminWellnessDaoProvider);

  final latest = <GarminWellnessMetric, WatchMetricReading>{};
  for (final metric in GarminWellnessMetric.values) {
    final row = await dao.latest(metric);
    if (row == null) continue;
    latest[metric] = WatchMetricReading(
      value: row.value,
      time: DateTime.fromMillisecondsSinceEpoch(row.timeMillis, isUtc: true)
          .toLocal(),
    );
  }

  final now = DateTime.now();
  final dayStart = DateTime(now.year, now.month, now.day);
  Future<List<WatchMetricReading>> series(GarminWellnessMetric metric) async {
    final rows = await dao.samplesBetween(
      metric,
      dayStart.toUtc().millisecondsSinceEpoch,
      dayStart.add(const Duration(days: 1)).toUtc().millisecondsSinceEpoch,
    );
    return [
      for (final r in rows)
        WatchMetricReading(
          value: r.value,
          time: DateTime.fromMillisecondsSinceEpoch(r.timeMillis, isUtc: true)
              .toLocal(),
        ),
    ];
  }

  // The weekly intensity-minutes total. The watch stores a running daily total
  // that resets at midnight, so the week's figure is the sum of each day's final
  // reading — never the single latest one, which is only today's.
  final weekStart = dayStart.subtract(Duration(days: dayStart.weekday - 1));
  Future<int> dailyFinalsSum(GarminWellnessMetric metric) async {
    final rows = await dao.samplesBetween(
      metric,
      weekStart.toUtc().millisecondsSinceEpoch,
      weekStart.add(const Duration(days: 7)).toUtc().millisecondsSinceEpoch,
    );
    // Rows arrive oldest-first, so the last value seen for a day is its final
    // running total. Key by local calendar day so a UTC-stored instant lands on
    // the day it was actually measured.
    final finalByDay = <int, int>{};
    for (final r in rows) {
      final local = DateTime.fromMillisecondsSinceEpoch(r.timeMillis, isUtc: true)
          .toLocal();
      final day = DateTime(local.year, local.month, local.day)
          .millisecondsSinceEpoch;
      finalByDay[day] = r.value;
    }
    return finalByDay.values.fold<int>(0, (sum, v) => sum + v);
  }

  int? intensityMinutesWeek;
  if (latest.containsKey(GarminWellnessMetric.moderateMinutes) ||
      latest.containsKey(GarminWellnessMetric.vigorousMinutes)) {
    final moderate = await dailyFinalsSum(GarminWellnessMetric.moderateMinutes);
    final vigorous = await dailyFinalsSum(GarminWellnessMetric.vigorousMinutes);
    intensityMinutesWeek = moderate + 2 * vigorous;
  }

  return WatchMetrics(
    latest: latest,
    stressToday: await series(GarminWellnessMetric.stress),
    bodyEnergyToday: await series(GarminWellnessMetric.bodyEnergy),
    intensityMinutesWeek: intensityMinutesWeek,
  );
});
