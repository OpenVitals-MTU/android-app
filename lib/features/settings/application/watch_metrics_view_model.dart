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
  });

  final Map<GarminWellnessMetric, WatchMetricReading> latest;

  /// Today's series, oldest first, for the metrics dense enough to draw.
  final List<WatchMetricReading> stressToday;
  final List<WatchMetricReading> bodyEnergyToday;

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

  return WatchMetrics(
    latest: latest,
    stressToday: await series(GarminWellnessMetric.stress),
    bodyEnergyToday: await series(GarminWellnessMetric.bodyEnergy),
  );
});
