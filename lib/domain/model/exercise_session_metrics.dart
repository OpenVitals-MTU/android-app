/// The sibling-record totals for one exercise session's window.
///
/// A Health Connect `ExerciseSessionRecord` carries almost nothing: a watch
/// writes the walk itself as a session, and its steps, distance, calories and
/// elevation as SEPARATE records covering the same span. Reading the session
/// alone therefore yields a duration and little else, which is why a recorded
/// walk could show "Steps: Not available" while its own step-cadence trace was
/// charted right below.
///
/// The only way to reattach them is to aggregate over the session's window, which
/// is what the native `readExerciseSessionMetrics` does. Every field here is null
/// when the metric was not asked for (no read permission) or when no such record
/// covers the window — "unknown", never "zero".
class ExerciseSessionMetrics {
  const ExerciseSessionMetrics({
    this.totalDistanceMeters,
    this.averageSpeedMetersPerSecond,
    this.steps,
    this.totalCaloriesKcal,
    this.activeCaloriesKcal,
    this.elevationGainedMeters,
    this.floorsClimbed,
    this.wheelchairPushes,
    this.averagePowerWatts,
  });

  /// Nothing was asked for, or nothing could be read.
  static const ExerciseSessionMetrics none = ExerciseSessionMetrics();

  final double? totalDistanceMeters;
  final double? averageSpeedMetersPerSecond;
  final int? steps;
  final double? totalCaloriesKcal;
  final double? activeCaloriesKcal;
  final double? elevationGainedMeters;
  final int? floorsClimbed;
  final int? wheelchairPushes;
  final double? averagePowerWatts;

  bool get isEmpty =>
      totalDistanceMeters == null &&
      averageSpeedMetersPerSecond == null &&
      steps == null &&
      totalCaloriesKcal == null &&
      activeCaloriesKcal == null &&
      elevationGainedMeters == null &&
      floorsClimbed == null &&
      wheelchairPushes == null &&
      averagePowerWatts == null;

  @override
  String toString() => 'ExerciseSessionMetrics(distance: $totalDistanceMeters, '
      'speed: $averageSpeedMetersPerSecond, steps: $steps, '
      'totalKcal: $totalCaloriesKcal, activeKcal: $activeCaloriesKcal, '
      'elevation: $elevationGainedMeters, floors: $floorsClimbed, '
      'pushes: $wheelchairPushes, power: $averagePowerWatts)';
}

/// One aggregate the native side can compute over a session window.
///
/// [wireName] is the string handed to the platform channel and MUST stay in step
/// with the `SESSION_METRICS` table in `ActivityHealthReader.kt`. A name the host
/// does not know is skipped there rather than throwing, so adding a value here
/// degrades gracefully against an older host instead of breaking the read.
enum ExerciseSessionMetric {
  distance('distance'),
  speed('speed'),
  steps('steps'),
  totalCalories('totalCalories'),
  activeCalories('activeCalories'),
  elevation('elevation'),
  floors('floors'),
  wheelchairPushes('wheelchairPushes'),
  power('power');

  const ExerciseSessionMetric(this.wireName);

  final String wireName;
}
