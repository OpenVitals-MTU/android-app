import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';

/// One app or device contributing data to Health Connect, as seen across the
/// metrics openvitals reads. Pure attribution: `package` is the writing app's
/// package name from each record's `dataOrigin`.
///
/// This is a **diagnostic**, not a registry device — a WearOS watch reaches the
/// phone through its vendor app (Samsung Health, …), so what shows up here is
/// the bridging app's package, not the watch itself. It is how we tell, after
/// pairing a watch, whether its data is actually landing in Health Connect.
class HealthConnectSource {
  const HealthConnectSource({
    required this.package,
    required this.recordCount,
    required this.lastSeen,
    required this.metrics,
  });

  /// The `dataOrigin` package name, or `'unknown'` for a blank source.
  final String package;

  final int recordCount;

  /// The most recent record seen from this package, across [metrics].
  final DateTime lastSeen;

  /// Which of the scanned metrics this package contributed (e.g. `heart rate`).
  final Set<String> metrics;

  /// A friendly name for the well-known contributors, else the raw package.
  String get displayName => _friendlyNames[package] ?? package;
}

const Map<String, String> _friendlyNames = {
  'tech.mmarca.openvitals': 'OpenVitals (this app)',
  'com.sec.android.app.shealth': 'Samsung Health',
  'com.google.android.apps.healthdata': 'Health Connect',
  'com.google.android.apps.fitness': 'Google Fit',
  'com.fitbit.FitbitMobile': 'Fitbit',
  'com.garmin.android.apps.connectmobile': 'Garmin Connect',
  'unknown': 'Unknown source',
};

/// Folds `(source, time)` observations grouped per metric into a source list,
/// most-recent contribution first. Pure — the provider feeds it real reads.
List<HealthConnectSource> aggregateHealthConnectSources(
  Map<String, List<(String source, DateTime time)>> byMetric,
) {
  final acc =
      <String, ({int count, DateTime lastSeen, Set<String> metrics})>{};
  for (final entry in byMetric.entries) {
    final metric = entry.key;
    for (final (source, time) in entry.value) {
      final key = source.trim().isEmpty ? 'unknown' : source.trim();
      final existing = acc[key];
      if (existing == null) {
        acc[key] = (count: 1, lastSeen: time, metrics: {metric});
      } else {
        acc[key] = (
          count: existing.count + 1,
          lastSeen: time.isAfter(existing.lastSeen) ? time : existing.lastSeen,
          metrics: {...existing.metrics, metric},
        );
      }
    }
  }
  return [
    for (final e in acc.entries)
      HealthConnectSource(
        package: e.key,
        recordCount: e.value.count,
        lastSeen: e.value.lastSeen,
        metrics: e.value.metrics,
      ),
  ]..sort((a, b) => b.lastSeen.compareTo(a.lastSeen));
}

/// The contributors seen in the last week of heart-rate and sleep data — the
/// two metrics a watch most reliably writes. Read-only; empty when nothing has
/// been read (no permission, or nothing has synced yet).
final healthConnectSourcesProvider =
    FutureProvider.autoDispose<List<HealthConnectSource>>((ref) async {
  final hc = ref.watch(healthDataSourceProvider);
  final now = DateTime.now();
  final start = now.subtract(const Duration(days: 7));
  final heartRate = await hc.readHeartRateSamples(start, now);
  final sleep = await hc.readSleepSessions(start, now);
  return aggregateHealthConnectSources({
    'heart rate': [for (final s in heartRate) (s.source, s.time)],
    'sleep': [for (final s in sleep) (s.source, s.endTime)],
  });
});
