import 'package:xml/xml.dart';

import '../../../../domain/model/ble_sensor_models.dart';
import '../../../../domain/model/activity_models.dart';
import 'route_file_parser.dart';

/// Training Center XML — what Strava and Garmin export an INDOOR activity as.
///
/// The app could read GPX, KML and FIT, and a GPX cannot carry an indoor session
/// at all: it is a list of places, so a treadmill run has nothing to put in it,
/// and a routeless GPX is correctly refused for having no start, no duration and
/// no distance. TCX is the format that solves exactly that problem — its `Lap`
/// carries `TotalTimeSeconds`, `DistanceMeters` and `Calories`, and its
/// `Trackpoint` carries heart rate, cadence and speed with the `Position`
/// OPTIONAL. So a treadmill run is a first-class TCX document, and reporting one
/// as broken was the app's limitation, not the file's.
///
/// The route is therefore built from whichever trackpoints HAVE a position, and
/// an activity with none is still a complete activity. This is the same shape as
/// the FIT parser, for the same reason.
class TcxRouteParser {
  const TcxRouteParser._();

  /// Roughly: is this a TCX? Matched on the ROOT element rather than the
  /// extension, because the dispatcher sniffs content — a `.tcx` renamed to
  /// `.gpx` is still a TCX, and would otherwise die in the GPX parser with a
  /// message about location points.
  static bool looksLikeTcx(String text) =>
      text.contains('TrainingCenterDatabase');

  static RouteFileImport parse(String tcxText, {String? fileName}) {
    final document = XmlDocument.parse(tcxText);

    // `Activity` is a recorded session; `Course` is a planned route. Both hold
    // Laps and Tracks, and either may be what the user picked.
    final activities = [
      ...elementsByLocalName(document, 'Activity'),
      ...elementsByLocalName(document, 'Course'),
    ];
    if (activities.isEmpty) {
      throw const RouteImportException(
        'TCX file contains no activity or course.',
      );
    }
    final activity = activities.first;

    final points = <MutableRoutePoint>[];
    final heartRates = <BleHeartRateSample>[];
    final cadences = <(DateTime, int)>[];
    final speeds = <BleSpeedSample>[];

    final sport = activity.getAttribute('Sport');
    // TCX names the sport on the Activity and nowhere else, and its vocabulary is
    // three words wide: Running, Biking, Other. It cannot say "treadmill" — so an
    // indoor run imports as a run, which is what the file actually claims. Better
    // a true statement than a clever guess: a ride with no GPS is not necessarily
    // a trainer ride, it may be a ride whose GPS failed.
    final isCycling = (sport ?? '').toLowerCase().contains('bik');

    for (final trackpoint in elementsByLocalName(activity, 'Trackpoint')) {
      final time = _timeOrNull(directChildText(trackpoint, 'Time'));

      // Position is OPTIONAL, and its absence is the whole point of this parser.
      final position = _firstChild(trackpoint, 'Position');
      if (position != null) {
        points.add(
          MutableRoutePoint(
            latitude:
                double.tryParse(directChildText(position, 'LatitudeDegrees') ?? ''),
            longitude: double.tryParse(
                directChildText(position, 'LongitudeDegrees') ?? ''),
            elevationMeters: double.tryParse(
                directChildText(trackpoint, 'AltitudeMeters')?.trim() ?? ''),
            time: time,
          ),
        );
      }

      if (time == null) continue;

      // `<HeartRateBpm><Value>128</Value></HeartRateBpm>` — the value is a
      // child, not the text of the element.
      final heartRate = _valueOf(trackpoint, 'HeartRateBpm');
      if (heartRate != null && heartRate > 0) {
        heartRates.add(
          BleHeartRateSample(time: time, beatsPerMinute: heartRate.round()),
        );
      }
      final cadence =
          int.tryParse(directChildText(trackpoint, 'Cadence')?.trim() ?? '');
      if (cadence != null && cadence >= 0) cadences.add((time, cadence));

      // Speed and running cadence live in the vendor extension namespace
      // (`ns3:TPX`), which is where every exporter in practice puts them.
      final speed = _extensionValue(trackpoint, 'Speed');
      if (speed != null && speed >= 0) {
        speeds.add(
          BleSpeedSample(
            time: time,
            metersPerSecond: speed,
            isRunning: !isCycling,
          ),
        );
      }
      if (cadence == null) {
        final runCadence = _extensionValue(trackpoint, 'RunCadence');
        if (runCadence != null && runCadence >= 0) {
          cadences.add((time, runCadence.round()));
        }
      }
    }

    final summary = _summarize(activity);
    final routePoints = mutableToRoutePoints(points);

    final startTime = summary.startTime ??
        (routePoints.isNotEmpty ? routePoints.first.time : null) ??
        (heartRates.isNotEmpty ? heartRates.first.time : null);
    if (startTime == null) {
      throw const RouteImportException(
        'TCX file contains no timestamped activity data.',
      );
    }
    final duration = summary.durationSeconds;
    final candidateEnd = (duration != null && duration > 0)
        ? startTime.add(Duration(seconds: duration))
        : (routePoints.isNotEmpty ? routePoints.last.time : null);
    final endTime = (candidateEnd != null && startTime.isBefore(candidateEnd))
        ? candidateEnd
        : startTime.add(const Duration(seconds: 1));

    final bleSamples = BleRecordingSampleBuffer(
      heartRateSamples: heartRates,
      speedSamples: speeds,
      // Which record the cadence belongs in is decided by the sport, exactly as
      // the FIT parser decides it: pedalling cadence and step cadence are
      // different Health Connect record types, and `Cadence` is just "cadence".
      cyclingCadenceSamples: isCycling
          ? [
              for (final (time, rpm) in cadences)
                BleCyclingCadenceSample(time: time, rpm: rpm),
            ]
          : const [],
      stepsCadenceSamples: isCycling
          ? const []
          : [
              for (final (time, rpm) in cadences)
                // TCX writes RUNNING cadence as one foot: 85 means 170 steps a
                // minute, and every watch that reads it doubles it back.
                BleStepsCadenceSample(time: time, stepsPerMinute: rpm * 2),
            ],
    );

    final metadata = RouteFileMetadata(
      name: null,
      description: null,
      type: _sportName(sport),
    );

    if (routePoints.length >= minRoutePoints) {
      return buildRouteImport(
        fileName: fileName,
        points: routePoints,
        metadata: metadata,
      ).copyWith(
        distanceMeters: summary.distanceMeters ?? routeDistanceMeters(routePoints),
        elevationGainedMeters: routeElevationGainMeters(routePoints),
        totalCaloriesKcal: summary.caloriesKcal,
        startTime: startTime,
        endTime: endTime,
        durationSeconds: duration,
        bleSamples: bleSamples,
        originalPointCount: routePoints.length,
      );
    }

    // The indoor case. No route, and a complete activity all the same.
    return RouteFileImport(
      fileName: fileName,
      points: const <ExerciseRoutePoint>[],
      distanceMeters: summary.distanceMeters ?? 0.0,
      elevationGainedMeters: 0.0,
      // TCX `Calories` is the session TOTAL, and TCX has no active-calorie field
      // — so active is left unknown rather than invented. Filling it with an
      // estimate is exactly what made every routeless FIT file unsavable.
      totalCaloriesKcal: summary.caloriesKcal,
      startTime: startTime,
      endTime: endTime,
      durationSeconds: duration,
      name: null,
      description: null,
      type: _sportName(sport),
      bleSamples: bleSamples,
      originalPointCount: 0,
    );
  }

  /// The session totals, summed across the laps — a TCX writes one `Lap` per lap
  /// and the activity's distance/duration/calories are their sums, not any one
  /// of them.
  static _TcxSummary _summarize(XmlElement activity) {
    DateTime? startTime = _timeOrNull(directChildText(activity, 'Id'));
    var seconds = 0.0;
    var meters = 0.0;
    var calories = 0.0;
    var sawLap = false;

    for (final lap in elementsByLocalName(activity, 'Lap')) {
      sawLap = true;
      startTime ??= _timeOrNull(lap.getAttribute('StartTime'));
      seconds +=
          double.tryParse(directChildText(lap, 'TotalTimeSeconds') ?? '') ?? 0.0;
      meters +=
          double.tryParse(directChildText(lap, 'DistanceMeters') ?? '') ?? 0.0;
      calories += double.tryParse(directChildText(lap, 'Calories') ?? '') ?? 0.0;
    }

    return _TcxSummary(
      startTime: startTime,
      durationSeconds: (sawLap && seconds > 0) ? seconds.round() : null,
      distanceMeters: (sawLap && meters > 0) ? meters : null,
      caloriesKcal: (sawLap && calories > 0) ? calories : null,
    );
  }
}

class _TcxSummary {
  const _TcxSummary({
    this.startTime,
    this.durationSeconds,
    this.distanceMeters,
    this.caloriesKcal,
  });

  final DateTime? startTime;
  final int? durationSeconds;
  final double? distanceMeters;
  final double? caloriesKcal;
}

/// TCX's whole sport vocabulary, in the words the type inference reads.
String? _sportName(String? sport) {
  final value = (sport ?? '').toLowerCase();
  if (value.contains('bik') || value.contains('cycl')) return 'cycling';
  if (value.contains('run')) return 'running';
  if (value.contains('walk')) return 'walking';
  // "Other" says nothing, and saying nothing lets the file NAME speak — which
  // for a TCX is usually the only thing that can.
  return null;
}

XmlElement? _firstChild(XmlElement parent, String localName) =>
    parent.descendantElements
        .where((element) => element.name.local == localName)
        .firstOrNull;

/// `<HeartRateBpm><Value>128</Value></HeartRateBpm>`.
double? _valueOf(XmlElement trackpoint, String localName) {
  final element = _firstChild(trackpoint, localName);
  if (element == null) return null;
  return double.tryParse(directChildText(element, 'Value')?.trim() ?? '');
}

/// A value inside the vendor `Extensions` block (`ns3:TPX` → `Speed`, `Watts`,
/// `RunCadence`), matched by local name so the namespace prefix does not matter.
double? _extensionValue(XmlElement trackpoint, String localName) {
  final extensions = _firstChild(trackpoint, 'Extensions');
  if (extensions == null) return null;
  final element = extensions.descendantElements
      .where((e) => e.name.local == localName)
      .firstOrNull;
  return double.tryParse(element?.innerText.trim() ?? '');
}

DateTime? _timeOrNull(String? value) {
  if (value == null) return null;
  return DateTime.tryParse(value.trim())?.toUtc();
}
