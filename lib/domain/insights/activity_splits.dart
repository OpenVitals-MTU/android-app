import 'dart:math' as math;

import '../../core/geo/geo_distance.dart';
import '../model/activity_models.dart';
import '../model/heart_models.dart';

/// Per-segment splits ("laps") for a distance-based activity.
///
/// Pure arithmetic: no I/O, no Flutter, no plugins. The activity detail screen
/// feeds this whatever it managed to load (route points, speed samples, heart
/// rate samples) and renders [ActivitySplits.splits] with the provenance in
/// [ActivitySplits.source] spelled out in the header — a split derived from a
/// GPS route and a split guessed from the session average are NOT the same
/// claim, and the UI must not present them as if they were.

/// Where the splits came from, in descending order of trustworthiness. Lives on
/// the result rather than on each row: one computation yields one provenance.
enum SplitSource {
  /// The recording device/app wrote lap records. Shown as recorded — never
  /// re-cut to the split distance, because a lap is whatever the device called
  /// a lap (a track session's 400 m, a button press, an uneven interval).
  deviceLaps,

  /// Cut from the GPS route by accumulating haversine distance between fixes.
  route,

  /// Cut by integrating SpeedRecord samples over time — the treadmill case,
  /// where there is no route but the belt reports speed.
  speedSamples,

  /// Nothing per-time exists: total distance divided evenly over the duration.
  /// Every split necessarily shows the activity's average pace. Honest, but the
  /// UI MUST label it, or a flat line reads as a real (and eerily even) run.
  estimated,
}

/// One split row.
class ActivitySplit {
  const ActivitySplit({
    required this.index,
    required this.distanceMeters,
    required this.elapsed,
    required this.startTime,
    required this.endTime,
    required this.isPartial,
    this.averageHeartRateBpm,
    this.elevationGainMeters,
    this.elevationLossMeters,
    this.paceDeltaSecondsPerKilometer,
  });

  /// 1-based, as displayed.
  final int index;

  /// The split's own distance. The final split is usually a partial and keeps
  /// its real (short) distance — see [isPartial].
  final double distanceMeters;

  final Duration elapsed;
  final DateTime startTime;
  final DateTime endTime;

  /// True when this split is shorter than the requested split distance (the
  /// trailing remainder). Device laps are never marked partial: an uneven lap
  /// is not an incomplete one.
  final bool isPartial;

  /// Mean of the heart-rate samples inside `[startTime, endTime)`. Null when no
  /// sample falls in the window — never 0.
  final int? averageHeartRateBpm;

  /// Cumulative ascent/descent across the split, from the route's altitudes.
  /// Null (NOT 0) when the split has no altitude data — a treadmill run did not
  /// climb zero meters, it climbed an unknown number of them.
  final double? elevationGainMeters;
  final double? elevationLossMeters;

  /// This split's pace minus the whole activity's average pace, in seconds per
  /// kilometer (storage is metric; [paceDeltaSecondsPerUnit] converts at the
  /// display boundary). Negative = faster than the activity average.
  ///
  /// Null when either pace is undefined (zero distance or zero elapsed).
  final double? paceDeltaSecondsPerKilometer;

  /// Seconds per meter, or null when the split covered no distance or no time.
  double? get paceSecondsPerMeter {
    final seconds = elapsed.inMicroseconds / Duration.microsecondsPerSecond;
    if (distanceMeters <= 0 || seconds <= 0) return null;
    return seconds / distanceMeters;
  }

  /// The split's pace expressed per display unit (1000 m, or 1609.344 m).
  double? paceSecondsPerUnit(double unitMeters) {
    final perMeter = paceSecondsPerMeter;
    if (perMeter == null || unitMeters <= 0) return null;
    return perMeter * unitMeters;
  }

  /// [paceDeltaSecondsPerKilometer] re-expressed per display unit.
  double? paceDeltaSecondsPerUnit(double unitMeters) {
    final deltaPerKm = paceDeltaSecondsPerKilometer;
    if (deltaPerKm == null || unitMeters <= 0) return null;
    return deltaPerKm * unitMeters / 1000.0;
  }

  /// Convenience for the common metric case, and the name the spec uses.
  double? get paceDeltaSeconds => paceDeltaSecondsPerKilometer;

  @override
  bool operator ==(Object other) =>
      other is ActivitySplit &&
      other.index == index &&
      other.distanceMeters == distanceMeters &&
      other.elapsed == elapsed &&
      other.startTime == startTime &&
      other.endTime == endTime &&
      other.isPartial == isPartial &&
      other.averageHeartRateBpm == averageHeartRateBpm &&
      other.elevationGainMeters == elevationGainMeters &&
      other.elevationLossMeters == elevationLossMeters &&
      other.paceDeltaSecondsPerKilometer == paceDeltaSecondsPerKilometer;

  @override
  int get hashCode => Object.hash(
        index,
        distanceMeters,
        elapsed,
        startTime,
        endTime,
        isPartial,
        averageHeartRateBpm,
        elevationGainMeters,
        elevationLossMeters,
        paceDeltaSecondsPerKilometer,
      );

  @override
  String toString() => 'ActivitySplit(#$index, ${distanceMeters}m, $elapsed, '
      'partial: $isPartial)';
}

/// The splits plus the one thing the UI must not lose: where they came from.
class ActivitySplits {
  const ActivitySplits({required this.source, required this.splits});

  const ActivitySplits.none()
      : source = SplitSource.estimated,
        splits = const <ActivitySplit>[];

  final SplitSource source;
  final List<ActivitySplit> splits;

  bool get isEmpty => splits.isEmpty;
  bool get isNotEmpty => splits.isNotEmpty;

  @override
  bool operator ==(Object other) =>
      other is ActivitySplits &&
      other.source == source &&
      _listEquals(other.splits, splits);

  @override
  int get hashCode => Object.hash(source, Object.hashAll(splits));

  @override
  String toString() => 'ActivitySplits($source, ${splits.length} splits)';
}

bool _listEquals(List<ActivitySplit> a, List<ActivitySplit> b) {
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}

/// The default split distance: one kilometer. Storage is always metric.
const double kDefaultSplitDistanceMeters = 1000.0;

/// A trailing remainder shorter than this is dropped rather than shown as a
/// "0 m" split — a GPS route that overshoots 5 km by 40 cm did not run a sixth
/// split.
const double _minPartialMeters = 1.0;

/// Splits for [workout], cut every [splitDistanceMeters] — unless the workout
/// already carries device laps, which win outright.
///
/// Source priority: device laps > GPS route > speed samples > estimated. See
/// [SplitSource]. Returns an empty result when the activity has no distance at
/// all (a strength session has no splits).
ActivitySplits computeActivitySplits({
  required ExerciseData workout,
  required List<HeartRateSample> heartRateSamples,
  required List<SpeedSample> speedSamples,
  required double splitDistanceMeters,
}) {
  final unit =
      (splitDistanceMeters.isFinite && splitDistanceMeters > 0)
          ? splitDistanceMeters
          : kDefaultSplitDistanceMeters;

  // Defensive: nothing guarantees the caller's samples are time-ordered (a
  // Health Connect read merges several source apps), and every walk below
  // assumes they are.
  final heartRates = [...heartRateSamples]
    ..sort((a, b) => a.time.compareTo(b.time));
  final speeds = [...speedSamples]..sort((a, b) => a.time.compareTo(b.time));
  final routePoints = [...workout.route.points]
    ..sort((a, b) => a.time.compareTo(b.time));

  final laps = _usableLaps(workout);
  final routeNodes = _routeNodes(routePoints);
  final speedNodes = _speedNodes(speeds);
  final totalDistance = workout.totalDistanceMeters ?? 0.0;
  final lapDistance = laps.fold<double>(
    0.0,
    (sum, lap) => sum + math.max(0.0, lap.lengthMeters ?? 0.0),
  );

  final hasAnyDistance = _isPositive(totalDistance) ||
      _isPositive(lapDistance) ||
      _isPositive(routeNodes.isEmpty ? 0.0 : routeNodes.last.cumulativeMeters) ||
      _isPositive(speedNodes.isEmpty ? 0.0 : speedNodes.last.cumulativeMeters);
  if (!hasAnyDistance) return const ActivitySplits.none();

  final List<_RawSplit> raw;
  final SplitSource source;
  if (laps.isNotEmpty) {
    source = SplitSource.deviceLaps;
    raw = _lapSplits(laps, routeNodes);
  } else if (routeNodes.length >= 2 &&
      _isPositive(routeNodes.last.cumulativeMeters)) {
    source = SplitSource.route;
    raw = _cutNodes(routeNodes, unit);
  } else if (speedNodes.length >= 2 &&
      _isPositive(speedNodes.last.cumulativeMeters)) {
    source = SplitSource.speedSamples;
    raw = _cutNodes(speedNodes, unit);
  } else {
    source = SplitSource.estimated;
    raw = _estimatedSplits(workout, unit);
  }
  if (raw.isEmpty) return const ActivitySplits.none();

  // The yardstick for paceDelta is the WHOLE activity's average pace, not the
  // mean of the split paces: a 200 m partial should not drag the baseline.
  final activityPaceSecondsPerMeter = _activityPaceSecondsPerMeter(
    workout: workout,
    raw: raw,
  );

  final splits = <ActivitySplit>[];
  for (var i = 0; i < raw.length; i++) {
    final entry = raw[i];
    final elapsed = entry.endTime.difference(entry.startTime);
    final elapsedSeconds =
        elapsed.inMicroseconds / Duration.microsecondsPerSecond;
    final splitPaceSecondsPerMeter =
        (entry.distanceMeters > 0 && elapsedSeconds > 0)
            ? elapsedSeconds / entry.distanceMeters
            : null;
    final paceDeltaPerKm = (splitPaceSecondsPerMeter != null &&
            activityPaceSecondsPerMeter != null)
        ? (splitPaceSecondsPerMeter - activityPaceSecondsPerMeter) * 1000.0
        : null;

    splits.add(
      ActivitySplit(
        index: i + 1,
        distanceMeters: entry.distanceMeters,
        elapsed: elapsed.isNegative ? Duration.zero : elapsed,
        startTime: entry.startTime,
        endTime: entry.endTime,
        isPartial: entry.isPartial,
        averageHeartRateBpm: _averageHeartRate(
          heartRates,
          entry.startTime,
          entry.endTime,
        ),
        elevationGainMeters: entry.elevationGainMeters,
        elevationLossMeters: entry.elevationLossMeters,
        paceDeltaSecondsPerKilometer: paceDeltaPerKm,
      ),
    );
  }
  return ActivitySplits(source: source, splits: splits);
}

bool _isPositive(double value) => value.isFinite && value > 0;

/// Laps with a sane time window, oldest first. A lap that ends before it starts
/// is a source-app bug, not a lap.
List<ExerciseLapData> _usableLaps(ExerciseData workout) {
  final laps = workout.laps
      .where((lap) => !lap.endTime.isBefore(lap.startTime))
      .toList()
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  return laps;
}

/// The whole activity's average pace in seconds per meter — the baseline every
/// split's [ActivitySplit.paceDeltaSecondsPerKilometer] is measured against.
///
/// Prefers the recorded session totals; falls back to the sum of the splits
/// when the session has no distance/duration of its own (laps-only imports).
double? _activityPaceSecondsPerMeter({
  required ExerciseData workout,
  required List<_RawSplit> raw,
}) {
  final recordedDistance = workout.totalDistanceMeters ?? 0.0;
  final recordedSeconds = workout.durationMs / 1000.0;
  if (_isPositive(recordedDistance) && recordedSeconds > 0) {
    return recordedSeconds / recordedDistance;
  }
  final splitDistance =
      raw.fold<double>(0.0, (sum, entry) => sum + entry.distanceMeters);
  final splitSeconds = raw.fold<double>(
    0.0,
    (sum, entry) =>
        sum +
        entry.endTime.difference(entry.startTime).inMicroseconds /
            Duration.microsecondsPerSecond,
  );
  if (!_isPositive(splitDistance) || splitSeconds <= 0) return null;
  return splitSeconds / splitDistance;
}

int? _averageHeartRate(
  List<HeartRateSample> samples,
  DateTime start,
  DateTime end,
) {
  var sum = 0;
  var count = 0;
  for (final sample in samples) {
    // Half-open [start, end): a sample on a boundary belongs to exactly one
    // split, never to both.
    if (sample.time.isBefore(start)) continue;
    if (!sample.time.isBefore(end)) break;
    sum += sample.beatsPerMinute;
    count++;
  }
  if (count == 0) return null;
  return (sum / count).round();
}

/// A split before heart rate / pace delta are attached.
class _RawSplit {
  const _RawSplit({
    required this.startTime,
    required this.endTime,
    required this.distanceMeters,
    required this.isPartial,
    this.elevationGainMeters,
    this.elevationLossMeters,
  });

  final DateTime startTime;
  final DateTime endTime;
  final double distanceMeters;
  final bool isPartial;
  final double? elevationGainMeters;
  final double? elevationLossMeters;
}

/// A point on a monotone distance-over-time curve, whatever produced it: a GPS
/// fix, a speed sample, or an interpolated split boundary. Sharing one node
/// type is what lets the route and speed-sample cutters be the same code.
class _Node {
  const _Node({
    required this.time,
    required this.cumulativeMeters,
    this.altitudeMeters,
  });

  final DateTime time;
  final double cumulativeMeters;
  final double? altitudeMeters;
}

/// GPS fixes → cumulative haversine distance. Non-finite or backwards segments
/// contribute 0 rather than corrupting the curve.
List<_Node> _routeNodes(List<ExerciseRoutePoint> points) {
  if (points.isEmpty) return const <_Node>[];
  final nodes = <_Node>[
    _Node(
      time: points.first.time,
      cumulativeMeters: 0.0,
      altitudeMeters: points.first.altitudeMeters,
    ),
  ];
  var cumulative = 0.0;
  for (var i = 1; i < points.length; i++) {
    final previous = points[i - 1];
    final current = points[i];
    final segment = haversineMeters(
      previous.latitude,
      previous.longitude,
      current.latitude,
      current.longitude,
    );
    if (segment.isFinite && segment > 0) cumulative += segment;
    nodes.add(
      _Node(
        time: current.time,
        cumulativeMeters: cumulative,
        altitudeMeters: current.altitudeMeters,
      ),
    );
  }
  return nodes;
}

/// Speed samples → cumulative distance by trapezoidal integration of v·dt.
/// No altitude: a SpeedRecord says nothing about the ground going up.
List<_Node> _speedNodes(List<SpeedSample> samples) {
  if (samples.isEmpty) return const <_Node>[];
  final nodes = <_Node>[
    _Node(time: samples.first.time, cumulativeMeters: 0.0),
  ];
  var cumulative = 0.0;
  for (var i = 1; i < samples.length; i++) {
    final previous = samples[i - 1];
    final current = samples[i];
    final seconds = current.time.difference(previous.time).inMicroseconds /
        Duration.microsecondsPerSecond;
    final v0 = math.max(0.0, previous.metersPerSecond);
    final v1 = math.max(0.0, current.metersPerSecond);
    if (seconds > 0 && v0.isFinite && v1.isFinite) {
      cumulative += (v0 + v1) / 2.0 * seconds;
    }
    nodes.add(_Node(time: current.time, cumulativeMeters: cumulative));
  }
  return nodes;
}

/// Cut a distance-over-time curve every [unit] meters.
///
/// The crossing is INTERPOLATED between the two bracketing nodes, not snapped
/// to the next one: at a 5 s GPS cadence, snapping quantises every split's
/// elapsed time to ±5 s, which on a 1 km split is a visible ~8 s/km pace error.
List<_RawSplit> _cutNodes(List<_Node> nodes, double unit) {
  final total = nodes.last.cumulativeMeters;
  if (!_isPositive(total)) return const <_RawSplit>[];

  final splits = <_RawSplit>[];
  var splitStart = nodes.first;
  var splitStartIndex = 0;
  var boundary = unit;
  // Guards a pathological (or hostile) preference: a 1 cm split distance over a
  // marathon would otherwise try to build 4.2 million rows.
  const maxSplits = 500;

  for (var i = 1; i < nodes.length && splits.length < maxSplits; i++) {
    final previous = nodes[i - 1];
    final current = nodes[i];
    final segment = current.cumulativeMeters - previous.cumulativeMeters;
    if (segment <= 0) continue;

    while (boundary <= current.cumulativeMeters && splits.length < maxSplits) {
      final fraction = (boundary - previous.cumulativeMeters) / segment;
      final crossing = _interpolate(previous, current, fraction, boundary);
      final elevation = _elevationBetween(
        nodes,
        splitStartIndex,
        i,
        splitStart,
        crossing,
      );
      splits.add(
        _RawSplit(
          startTime: splitStart.time,
          endTime: crossing.time,
          distanceMeters:
              crossing.cumulativeMeters - splitStart.cumulativeMeters,
          isPartial: false,
          elevationGainMeters: elevation?.gain,
          elevationLossMeters: elevation?.loss,
        ),
      );
      splitStart = crossing;
      // The crossing sits between nodes[i-1] and nodes[i]. `splitStartIndex` is
      // by convention the index of the node at-or-before the split start, so
      // the next split's interior nodes begin at i (and nodes[i] is NOT lost).
      splitStartIndex = i - 1;
      boundary += unit;
    }
  }

  // The trailing remainder: a real, shorter split, kept and flagged.
  final last = nodes.last;
  final remainder = last.cumulativeMeters - splitStart.cumulativeMeters;
  if (remainder >= _minPartialMeters && splits.length < maxSplits) {
    final elevation = _elevationBetween(
      nodes,
      splitStartIndex,
      nodes.length - 1,
      splitStart,
      last,
    );
    splits.add(
      _RawSplit(
        startTime: splitStart.time,
        endTime: last.time,
        distanceMeters: remainder,
        isPartial: true,
        elevationGainMeters: elevation?.gain,
        elevationLossMeters: elevation?.loss,
      ),
    );
  }
  return splits;
}

/// The node at [targetDistance], [fraction] of the way from [from] to [to].
_Node _interpolate(_Node from, _Node to, double fraction, double targetDistance) {
  final clamped = fraction.isFinite ? fraction.clamp(0.0, 1.0) : 0.0;
  final spanMicros =
      to.time.difference(from.time).inMicroseconds.toDouble();
  final time = from.time.add(
    Duration(microseconds: (spanMicros * clamped).round()),
  );
  final fromAltitude = from.altitudeMeters;
  final toAltitude = to.altitudeMeters;
  final altitude = (fromAltitude != null && toAltitude != null)
      ? fromAltitude + (toAltitude - fromAltitude) * clamped
      : (fromAltitude ?? toAltitude);
  return _Node(
    time: time,
    cumulativeMeters: targetDistance,
    altitudeMeters: altitude,
  );
}

class _Elevation {
  const _Elevation(this.gain, this.loss);

  final double gain;
  final double loss;
}

/// Cumulative ascent/descent from [start] through the nodes strictly inside
/// `(startIndex, endIndex]`… and on to [end]. Null when no two consecutive
/// points in the window both carry an altitude — a split with no altitude data
/// gained an UNKNOWN amount of elevation, not zero.
_Elevation? _elevationBetween(
  List<_Node> nodes,
  int startIndex,
  int endIndex,
  _Node start,
  _Node end,
) {
  final walk = <_Node>[
    start,
    for (var i = startIndex + 1; i < endIndex; i++) nodes[i],
    end,
  ];
  var gain = 0.0;
  var loss = 0.0;
  var sawPair = false;
  for (var i = 1; i < walk.length; i++) {
    final from = walk[i - 1].altitudeMeters;
    final to = walk[i].altitudeMeters;
    if (from == null || to == null || !from.isFinite || !to.isFinite) continue;
    sawPair = true;
    final delta = to - from;
    if (delta > 0) {
      gain += delta;
    } else {
      loss += -delta;
    }
  }
  if (!sawPair) return null;
  return _Elevation(gain, loss);
}

/// Device laps, shown as recorded. A lap's length is what the device wrote; if
/// it wrote none, the route (if any) supplies it, and failing that the lap has
/// no distance to show.
List<_RawSplit> _lapSplits(List<ExerciseLapData> laps, List<_Node> routeNodes) {
  final splits = <_RawSplit>[];
  for (final lap in laps) {
    final recorded = lap.lengthMeters;
    final fromRoute = _routeSpan(routeNodes, lap.startTime, lap.endTime);
    final distance = (recorded != null && recorded.isFinite && recorded >= 0)
        ? recorded
        : (fromRoute?.distanceMeters ?? 0.0);
    splits.add(
      _RawSplit(
        startTime: lap.startTime,
        endTime: lap.endTime,
        distanceMeters: distance,
        // A device lap is never "partial": an uneven lap is not a truncated
        // one, it is simply the lap the device recorded.
        isPartial: false,
        elevationGainMeters: fromRoute?.elevation?.gain,
        elevationLossMeters: fromRoute?.elevation?.loss,
      ),
    );
  }
  return splits;
}

class _RouteSpan {
  const _RouteSpan(this.distanceMeters, this.elevation);

  final double distanceMeters;
  final _Elevation? elevation;
}

/// Route distance + elevation between two times, for a lap that spans part of
/// a recorded route.
_RouteSpan? _routeSpan(List<_Node> nodes, DateTime start, DateTime end) {
  if (nodes.length < 2 || !end.isAfter(start)) return null;
  final inside = <_Node>[
    for (final node in nodes)
      if (!node.time.isBefore(start) && !node.time.isAfter(end)) node,
  ];
  if (inside.length < 2) return null;
  final distance =
      inside.last.cumulativeMeters - inside.first.cumulativeMeters;
  var gain = 0.0;
  var loss = 0.0;
  var sawPair = false;
  for (var i = 1; i < inside.length; i++) {
    final from = inside[i - 1].altitudeMeters;
    final to = inside[i].altitudeMeters;
    if (from == null || to == null || !from.isFinite || !to.isFinite) continue;
    sawPair = true;
    final delta = to - from;
    if (delta > 0) {
      gain += delta;
    } else {
      loss += -delta;
    }
  }
  return _RouteSpan(
    math.max(0.0, distance),
    sawPair ? _Elevation(gain, loss) : null,
  );
}

/// The last resort: total distance spread evenly over the duration. Every split
/// gets the same pace by construction — which is exactly why the UI labels this
/// source as estimated instead of drawing a suspiciously flat bar chart and
/// letting the user believe it.
List<_RawSplit> _estimatedSplits(ExerciseData workout, double unit) {
  final total = workout.totalDistanceMeters ?? 0.0;
  if (!_isPositive(total)) return const <_RawSplit>[];
  final durationMicros = workout.durationMs <= 0
      ? 0
      : workout.durationMs * Duration.microsecondsPerMillisecond;

  final splits = <_RawSplit>[];
  const maxSplits = 500;
  var covered = 0.0;
  var index = 0;
  while (covered < total && index < maxSplits) {
    final distance = math.min(unit, total - covered);
    // Below the partial floor the remainder is noise, not a split.
    if (distance < _minPartialMeters && index > 0) break;
    final startFraction = covered / total;
    final endFraction = (covered + distance) / total;
    splits.add(
      _RawSplit(
        startTime: workout.startTime.add(
          Duration(microseconds: (durationMicros * startFraction).round()),
        ),
        endTime: workout.startTime.add(
          Duration(microseconds: (durationMicros * endFraction).round()),
        ),
        distanceMeters: distance,
        isPartial: distance < unit,
        // No route: elevation is unknown, and unknown is not zero.
      ),
    );
    covered += distance;
    index++;
  }
  return splits;
}
