import '../../core/time/local_date.dart';

/// Which raw record type a per-source diagnostic read is about.
///
/// [wireName] is the canonical Health Connect record-type name the host
/// switches on. A name the host does not recognise returns empty rather than
/// throwing, so a newer caller cannot break an older host — the same convention
/// the session-metric names use.
enum HealthRecordSourceMetric {
  activeCalories('ActiveCaloriesBurned', 'kcal'),
  totalCalories('TotalCaloriesBurned', 'kcal'),
  steps('Steps', 'steps'),
  distance('Distance', 'm');

  const HealthRecordSourceMetric(this.wireName, this.unit);

  final String wireName;

  /// The unit [SourceDayTotal.total] is expressed in, for display.
  final String unit;
}

/// One writing app's contribution to one record type on one local day.
///
/// Every other activity read in the app goes through a Health Connect
/// *aggregate*, and aggregates sum every contributing app into a single figure.
/// Two apps mirroring the same watch therefore read as one very active user, and
/// nothing downstream can tell — which is exactly how a doubled calorie feed
/// reaches the Body Energy drain unnoticed. This is the only read that keeps the
/// attribution.
class SourceDayTotal {
  const SourceDayTotal({
    required this.metric,
    required this.package,
    required this.date,
    required this.total,
    required this.recordCount,
    required this.manualEntryCount,
    required this.coveredMinutes,
    required this.firstStart,
    required this.lastEnd,
  });

  final HealthRecordSourceMetric metric;

  /// The `dataOrigin` package name, or `'unknown'` for a blank source.
  final String package;

  final LocalDate date;

  /// In [HealthRecordSourceMetric.unit].
  final double total;

  final int recordCount;

  /// How many of [recordCount] were entered by hand or by an import rather than
  /// recorded by a sensor.
  final int manualEntryCount;

  /// Summed duration of this app's records that day.
  ///
  /// The decisive figure when two apps appear on the same day: each covering
  /// most of a 1440-minute day means they are mirroring one feed, which neither
  /// the total nor the record count can establish on its own (one app may write
  /// a single daily record where another writes 1440 minute-long ones).
  final double coveredMinutes;

  final DateTime firstStart;
  final DateTime lastEnd;
}
