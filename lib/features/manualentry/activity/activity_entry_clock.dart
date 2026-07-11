/// A minimal clock abstraction for the activity-entry feature, replacing the
/// Kotlin `java.time.Clock`. Provides the current instant plus a conversion of an
/// absolute instant to a wall-clock [DateTime] in the clock's zone.
class ActivityEntryClock {
  const ActivityEntryClock({required this.nowUtc, required this.toZone});

  /// The current absolute instant (UTC).
  final DateTime Function() nowUtc;

  /// Converts an absolute instant to a [DateTime] whose calendar fields are the
  /// wall-clock in the clock's zone.
  final DateTime Function(DateTime instant) toZone;

  /// The device clock in the local zone.
  factory ActivityEntryClock.system() => ActivityEntryClock(
        nowUtc: () => DateTime.now().toUtc(),
        toZone: (instant) => instant.toLocal(),
      );

  /// A fixed clock in UTC (used in tests).
  factory ActivityEntryClock.fixedUtc(DateTime instant) => ActivityEntryClock(
        nowUtc: () => instant.toUtc(),
        toZone: (value) => value.toUtc(),
      );

  DateTime nowInZone() => toZone(nowUtc());
}
