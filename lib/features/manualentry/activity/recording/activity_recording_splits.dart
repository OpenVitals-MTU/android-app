import '../../../../core/geo/geo_distance.dart';
import '../../../../domain/model/activity_models.dart';

/// Port of the Kotlin `ActivityRecordingSplits.kt` — pure split/lap analytics
/// over a recorded route. No plugins.

enum ActivityRecordingTab { map, stats, intervals, byTime, byDistance }

class ActivityRecordingSplit {
  const ActivityRecordingSplit({
    required this.index,
    required this.startTime,
    required this.endTime,
    required this.startDistanceMeters,
    required this.endDistanceMeters,
    required this.distanceMeters,
    required this.elapsedMillis,
    required this.averageSpeedMetersPerSecond,
    required this.maxSpeedMetersPerSecond,
    required this.climbMeters,
  });

  final int index;
  final DateTime? startTime;
  final DateTime? endTime;
  final double startDistanceMeters;
  final double? endDistanceMeters;
  final double distanceMeters;
  final int elapsedMillis;
  final double averageSpeedMetersPerSecond;
  final double maxSpeedMetersPerSecond;
  final double climbMeters;
}

List<ActivityRecordingSplit> activityRecordingIntervalSplits(
  List<ExerciseRoutePoint> points,
  List<int> routeBreakIndexes,
) {
  final segments = toContinuousRouteSegments(points, routeBreakIndexes);
  final splits = <ActivityRecordingSplit>[];
  for (var index = 0; index < segments.length; index++) {
    final split = _toRouteSegmentSplit(segments[index], index + 1);
    if (split != null) splits.add(split);
  }
  return splits;
}

List<ActivityRecordingSplit> activityRecordingLapSplits({
  required List<ActivityRecordingLap> laps,
  required List<ExerciseRoutePoint> points,
  required List<int> routeBreakIndexes,
  required DateTime? recordingStartTime,
  DateTime? activeEndTime,
}) {
  if (recordingStartTime == null || laps.isEmpty) return const [];
  final sortedLaps = [...laps]..sort((a, b) => a.startTime.compareTo(b.startTime));
  final closedSplits = <ActivityRecordingSplit>[];
  for (var index = 0; index < sortedLaps.length; index++) {
    final split = _lapToSplit(
      sortedLaps[index],
      index + 1,
      points,
      routeBreakIndexes,
    );
    if (split != null) closedSplits.add(split);
  }
  final openStart =
      laps.reduce((a, b) => a.endTime.isAfter(b.endTime) ? a : b).endTime;
  final openEnd = activeEndTime;
  ActivityRecordingSplit? openSplit;
  if (openEnd != null && openStart.isBefore(openEnd)) {
    openSplit = _lapToSplit(
      ActivityRecordingLap(
        startTime: openStart,
        endTime: openEnd,
        distanceMeters: null,
      ),
      closedSplits.length + 1,
      points,
      routeBreakIndexes,
    );
  }
  return [...closedSplits, ?openSplit];
}

List<ActivityRecordingSplit> exerciseLapSplits(
  List<ExerciseLapData> laps,
  List<ExerciseRoutePoint> points, {
  List<int> routeBreakIndexes = const [],
}) {
  final sortedLaps = [...laps]..sort((a, b) => a.startTime.compareTo(b.startTime));
  final splits = <ActivityRecordingSplit>[];
  for (var index = 0; index < sortedLaps.length; index++) {
    final lap = sortedLaps[index];
    final split = _lapToSplit(
      ActivityRecordingLap(
        startTime: lap.startTime,
        endTime: lap.endTime,
        distanceMeters: lap.lengthMeters,
      ),
      index + 1,
      points,
      routeBreakIndexes,
    );
    if (split != null) splits.add(split);
  }
  return splits;
}

double activityRecordingRouteDistanceMeters({
  required List<ExerciseRoutePoint> points,
  required List<int> routeBreakIndexes,
  required DateTime startTime,
  required DateTime endTime,
}) =>
    _routeStatsBetween(points, routeBreakIndexes, startTime, endTime)
        .distanceMeters;

List<ActivityRecordingSplit> activityRecordingTimeSplits({
  required List<ExerciseRoutePoint> points,
  required List<int> routeBreakIndexes,
  required int splitMillis,
}) {
  if (splitMillis <= 0) return const [];
  if (points.isEmpty) return const [];
  final firstTime = points.first.time;
  final buckets = <int, _MutableSplitStats>{};

  for (final segment in toContinuousRouteSegments(points, routeBreakIndexes)) {
    for (var i = 0; i + 1 < segment.length; i++) {
      final start = segment[i];
      final end = segment[i + 1];
      final elapsedMillis = _millisBetween(start.time, end.time);
      if (elapsedMillis <= 0) continue;

      final pairStartOffset = _atLeast0(_millisBetween(firstTime, start.time));
      final pairEndOffset =
          _atLeast(_millisBetween(firstTime, end.time), pairStartOffset);
      final pairStart = pairStartOffset.toDouble();
      var cursor = pairStart;
      final pairEnd = pairEndOffset.toDouble();
      final distanceMeters = _distanceMetersTo(start, end);
      final climbMeters = _climbMetersTo(start, end);
      final speedMetersPerSecond =
          _speedFor(distanceMeters, elapsedMillis.toDouble());

      while (cursor < pairEnd - _splitEpsilon) {
        final bucketIndex = (cursor / splitMillis).floor();
        final bucketEnd = _min(
          (bucketIndex + 1) * splitMillis.toDouble(),
          pairEnd,
        );
        final fraction = (bucketEnd - cursor) / (pairEnd - pairStart);
        buckets.putIfAbsent(bucketIndex, _MutableSplitStats.new).add(
              distanceMeters: distanceMeters * fraction,
              elapsedMillis: elapsedMillis * fraction,
              climbMeters: climbMeters * fraction,
              speedMetersPerSecond: speedMetersPerSecond,
            );
        cursor = bucketEnd;
      }
    }
  }

  final result = <ActivityRecordingSplit>[];
  buckets.forEach((bucketIndex, stats) {
    final startOffset = bucketIndex * splitMillis;
    result.add(
      ActivityRecordingSplit(
        index: bucketIndex + 1,
        startTime: firstTime.add(Duration(milliseconds: startOffset)),
        endTime: firstTime.add(Duration(milliseconds: startOffset + splitMillis)),
        startDistanceMeters: 0.0,
        endDistanceMeters: null,
        distanceMeters: stats.distanceMeters,
        elapsedMillis: stats.elapsedMillisRounded(),
        averageSpeedMetersPerSecond: stats.averageSpeedMetersPerSecond(),
        maxSpeedMetersPerSecond: stats.maxSpeedMetersPerSecond,
        climbMeters: stats.climbMeters,
      ),
    );
  });
  return result;
}

List<ActivityRecordingSplit> activityRecordingDistanceSplits({
  required List<ExerciseRoutePoint> points,
  required List<int> routeBreakIndexes,
  required double splitMeters,
}) {
  if (splitMeters <= 0.0 || !splitMeters.isFinite) return const [];
  final buckets = <int, _MutableSplitStats>{};
  var routeDistanceMeters = 0.0;

  for (final segment in toContinuousRouteSegments(points, routeBreakIndexes)) {
    for (var i = 0; i + 1 < segment.length; i++) {
      final start = segment[i];
      final end = segment[i + 1];
      final elapsedMillis = _millisBetween(start.time, end.time);
      if (elapsedMillis <= 0) continue;

      final distanceMeters = _distanceMetersTo(start, end);
      if (distanceMeters <= _splitEpsilon) continue;

      final climbMeters = _climbMetersTo(start, end);
      final speedMetersPerSecond =
          _speedFor(distanceMeters, elapsedMillis.toDouble());
      var consumedMeters = 0.0;
      while (consumedMeters < distanceMeters - _splitEpsilon) {
        final absoluteDistance = routeDistanceMeters + consumedMeters;
        final bucketIndex = (absoluteDistance / splitMeters).floor();
        final bucketEndDistance = _min(
          (bucketIndex + 1) * splitMeters,
          routeDistanceMeters + distanceMeters,
        );
        final distancePortion = bucketEndDistance - absoluteDistance;
        if (distancePortion <= _splitEpsilon) break;

        final fraction = distancePortion / distanceMeters;
        buckets.putIfAbsent(bucketIndex, _MutableSplitStats.new).add(
              distanceMeters: distancePortion,
              elapsedMillis: elapsedMillis * fraction,
              climbMeters: climbMeters * fraction,
              speedMetersPerSecond: speedMetersPerSecond,
            );
        consumedMeters += distancePortion;
      }
      routeDistanceMeters += distanceMeters;
    }
  }

  final result = <ActivityRecordingSplit>[];
  buckets.forEach((bucketIndex, stats) {
    result.add(
      ActivityRecordingSplit(
        index: bucketIndex + 1,
        startTime: null,
        endTime: null,
        startDistanceMeters: bucketIndex * splitMeters,
        endDistanceMeters: (bucketIndex + 1) * splitMeters,
        distanceMeters: stats.distanceMeters,
        elapsedMillis: stats.elapsedMillisRounded(),
        averageSpeedMetersPerSecond: stats.averageSpeedMetersPerSecond(),
        maxSpeedMetersPerSecond: stats.maxSpeedMetersPerSecond,
        climbMeters: stats.climbMeters,
      ),
    );
  });
  return result;
}

ActivityRecordingSplit? _lapToSplit(
  ActivityRecordingLap lap,
  int index,
  List<ExerciseRoutePoint> points,
  List<int> routeBreakIndexes,
) {
  if (!lap.startTime.isBefore(lap.endTime)) return null;
  final elapsedMillis = _atLeast0(_millisBetween(lap.startTime, lap.endTime));
  if (elapsedMillis <= 0) return null;
  final stats =
      _routeStatsBetween(points, routeBreakIndexes, lap.startTime, lap.endTime);
  final double splitDistanceMeters;
  if (stats.distanceMeters > 0.0) {
    splitDistanceMeters = stats.distanceMeters;
  } else if (lap.distanceMeters != null) {
    splitDistanceMeters = lap.distanceMeters!;
  } else {
    splitDistanceMeters = 0.0;
  }
  return ActivityRecordingSplit(
    index: index,
    startTime: lap.startTime,
    endTime: lap.endTime,
    startDistanceMeters: 0.0,
    endDistanceMeters: null,
    distanceMeters: splitDistanceMeters,
    elapsedMillis: elapsedMillis,
    averageSpeedMetersPerSecond:
        _speedFor(splitDistanceMeters, elapsedMillis.toDouble()),
    maxSpeedMetersPerSecond: stats.maxSpeedMetersPerSecond,
    climbMeters: stats.climbMeters,
  );
}

ActivityRecordingSplit? _toRouteSegmentSplit(
  List<ExerciseRoutePoint> segment,
  int index,
) {
  if (segment.length < 2) return null;
  final stats = _MutableSplitStats();
  for (var i = 0; i + 1 < segment.length; i++) {
    final start = segment[i];
    final end = segment[i + 1];
    final elapsedMillis = _millisBetween(start.time, end.time);
    if (elapsedMillis <= 0) continue;
    final distanceMeters = _distanceMetersTo(start, end);
    stats.add(
      distanceMeters: distanceMeters,
      elapsedMillis: elapsedMillis.toDouble(),
      climbMeters: _climbMetersTo(start, end),
      speedMetersPerSecond: _speedFor(distanceMeters, elapsedMillis.toDouble()),
    );
  }
  if (stats.elapsedMillis <= 0.0) return null;
  return ActivityRecordingSplit(
    index: index,
    startTime: segment.first.time,
    endTime: segment.last.time,
    startDistanceMeters: 0.0,
    endDistanceMeters: null,
    distanceMeters: stats.distanceMeters,
    elapsedMillis: stats.elapsedMillisRounded(),
    averageSpeedMetersPerSecond: stats.averageSpeedMetersPerSecond(),
    maxSpeedMetersPerSecond: stats.maxSpeedMetersPerSecond,
    climbMeters: stats.climbMeters,
  );
}

List<List<ExerciseRoutePoint>> toContinuousRouteSegments(
  List<ExerciseRoutePoint> points,
  List<int> routeBreakIndexes,
) {
  if (points.isEmpty) return const [];
  final breakIndexes = <int>{
    for (final index in routeBreakIndexes)
      if (index >= 1 && index < points.length) index,
  };
  final segments = <List<ExerciseRoutePoint>>[];
  for (var index = 0; index < points.length; index++) {
    if (index == 0 || breakIndexes.contains(index)) {
      segments.add(<ExerciseRoutePoint>[points[index]]);
    } else if (segments.isNotEmpty) {
      segments.last.add(points[index]);
    }
  }
  return segments;
}

_MutableSplitStats _routeStatsBetween(
  List<ExerciseRoutePoint> points,
  List<int> routeBreakIndexes,
  DateTime startTime,
  DateTime endTime,
) {
  final stats = _MutableSplitStats();
  if (!startTime.isBefore(endTime)) return stats;
  for (final segment in toContinuousRouteSegments(points, routeBreakIndexes)) {
    for (var i = 0; i + 1 < segment.length; i++) {
      final start = segment[i];
      final end = segment[i + 1];
      final elapsedMillis = _millisBetween(start.time, end.time);
      if (elapsedMillis <= 0) continue;
      final overlapStart = start.time.isAfter(startTime) ? start.time : startTime;
      final overlapEnd = end.time.isBefore(endTime) ? end.time : endTime;
      if (!overlapStart.isBefore(overlapEnd)) continue;
      final overlapMillis = _atLeast0(_millisBetween(overlapStart, overlapEnd));
      if (overlapMillis <= 0) continue;
      final fraction = overlapMillis.toDouble() / elapsedMillis.toDouble();
      final distanceMeters = _distanceMetersTo(start, end);
      stats.add(
        distanceMeters: distanceMeters * fraction,
        elapsedMillis: overlapMillis.toDouble(),
        climbMeters: _climbMetersTo(start, end) * fraction,
        speedMetersPerSecond: _speedFor(distanceMeters, elapsedMillis.toDouble()),
      );
    }
  }
  return stats;
}

class _MutableSplitStats {
  double distanceMeters = 0.0;
  double elapsedMillis = 0.0;
  double climbMeters = 0.0;
  double maxSpeedMetersPerSecond = 0.0;

  void add({
    required double distanceMeters,
    required double elapsedMillis,
    required double climbMeters,
    required double speedMetersPerSecond,
  }) {
    this.distanceMeters += distanceMeters < 0.0 ? 0.0 : distanceMeters;
    this.elapsedMillis += elapsedMillis < 0.0 ? 0.0 : elapsedMillis;
    this.climbMeters += climbMeters < 0.0 ? 0.0 : climbMeters;
    if (speedMetersPerSecond > maxSpeedMetersPerSecond) {
      maxSpeedMetersPerSecond = speedMetersPerSecond;
    }
  }

  int elapsedMillisRounded() {
    final value = elapsedMillis.toInt();
    return value < 0 ? 0 : value;
  }

  double averageSpeedMetersPerSecond() => _speedFor(distanceMeters, elapsedMillis);
}

double _speedFor(double distanceMeters, double elapsedMillis) {
  final elapsedSeconds = elapsedMillis / 1000.0;
  return (distanceMeters > 0.0 && elapsedSeconds > 0.0)
      ? distanceMeters / elapsedSeconds
      : 0.0;
}

double _climbMetersTo(ExerciseRoutePoint from, ExerciseRoutePoint to) {
  final startAltitude = from.altitudeMeters;
  final endAltitude = to.altitudeMeters;
  if (startAltitude == null || endAltitude == null) return 0.0;
  final delta = endAltitude - startAltitude;
  return delta >= _minSplitClimbMeters ? delta : 0.0;
}

double _distanceMetersTo(ExerciseRoutePoint from, ExerciseRoutePoint to) =>
    haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude);

int _millisBetween(DateTime start, DateTime end) =>
    end.millisecondsSinceEpoch - start.millisecondsSinceEpoch;

int _atLeast0(int value) => value < 0 ? 0 : value;
int _atLeast(int value, int min) => value < min ? min : value;
double _min(double a, double b) => a < b ? a : b;

const double _minSplitClimbMeters = 1.0;
const double _splitEpsilon = 0.000001;
