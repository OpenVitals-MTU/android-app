import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:xml/xml.dart';

import '../../../../domain/model/ble_sensor_models.dart';
import 'route_file_parser.dart';

/// The per-point series a GPX carries in its `<extensions>`: heart rate,
/// cadence, speed — the Garmin `gpxtpx:TrackPointExtension` every exporter
/// writes, and the only thing an indoor GPX has to say besides the time.
///
/// Read off the ROUTED files too, which is a fix in its own right: a GPX with a
/// track and a heart-rate extension used to import as a bare line on a map, its
/// heart rate thrown away at the parser.
class GpxSampleCollector {
  GpxSampleCollector({required this.isRunning});

  /// Decides which Health Connect record the cadence belongs in. Pedalling
  /// cadence and step cadence are different record types, and `cad` is just
  /// "cad".
  final bool isRunning;

  final List<BleHeartRateSample> _heartRates = [];
  final List<BleSpeedSample> _speeds = [];
  final List<(DateTime, int)> _cadences = [];

  void read(XmlElement point, DateTime time) {
    final heartRate = _extension(point, 'hr');
    if (heartRate != null && heartRate > 0) {
      _heartRates.add(
        BleHeartRateSample(time: time, beatsPerMinute: heartRate.round()),
      );
    }
    final cadence = _extension(point, 'cad');
    if (cadence != null && cadence >= 0) _cadences.add((time, cadence.round()));

    final speed = _extension(point, 'speed');
    if (speed != null && speed >= 0) {
      _speeds.add(
        BleSpeedSample(
          time: time,
          metersPerSecond: speed,
          isRunning: isRunning,
        ),
      );
    }
  }

  BleRecordingSampleBuffer get buffer => BleRecordingSampleBuffer(
        heartRateSamples: _heartRates,
        speedSamples: _speeds,
        cyclingCadenceSamples: isRunning
            ? const []
            : [
                for (final (time, rpm) in _cadences)
                  BleCyclingCadenceSample(time: time, rpm: rpm),
              ],
        stepsCadenceSamples: isRunning
            ? [
                for (final (time, rpm) in _cadences)
                  // Running cadence is written per FOOT, as in TCX: 85 means 170
                  // steps a minute.
                  BleStepsCadenceSample(time: time, stepsPerMinute: rpm * 2),
              ]
            : const [],
      );

  /// Matched by LOCAL name, so the namespace prefix (`gpxtpx:`, `ns3:`, none at
  /// all) does not matter — exporters disagree about it and none of them are
  /// wrong.
  static double? _extension(XmlElement point, String localName) {
    final element = point.descendantElements
        .where((e) => e.name.local == localName)
        .firstOrNull;
    return double.tryParse(element?.innerText.trim() ?? '');
  }
}

/// Port of the Kotlin `GpxRouteParser`.
class GpxRouteParser {
  const GpxRouteParser._();

  static const Set<String> _pointTags = {'trkpt', 'rtept'};

  static RouteFileImport parse(String gpxText, {String? fileName}) {
    final document = XmlDocument.parse(gpxText);
    final metadata = _routeMetadata(document);
    final mutablePoints = <MutableRoutePoint>[];
    // Every trackpoint that carries a TIME, whether or not it carries a place.
    // See [_routelessImport]: this is the indoor session.
    final timestamps = <DateTime>[];
    final samples = GpxSampleCollector(
      isRunning: !(metadata.type ?? '').toLowerCase().contains('bik') &&
          !(metadata.type ?? '').toLowerCase().contains('cycl'),
    );

    for (final tag in _pointTags) {
      for (final element in elementsByLocalName(document, tag)) {
        final time = _timeOrNull(directChildText(element, 'time'));
        mutablePoints.add(
          MutableRoutePoint(
            latitude: double.tryParse(element.getAttribute('lat') ?? ''),
            longitude: double.tryParse(element.getAttribute('lon') ?? ''),
            elevationMeters:
                double.tryParse(directChildText(element, 'ele')?.trim() ?? ''),
            time: time,
          ),
        );
        if (time != null) {
          timestamps.add(time);
          samples.read(element, time);
        }
      }
    }

    final routePoints = mutableToRoutePoints(mutablePoints);
    if (routePoints.length >= minRoutePoints) {
      return buildRouteImport(
        fileName: fileName,
        points: routePoints,
        metadata: metadata,
      ).copyWith(bleSamples: samples.buffer);
    }

    // No route. That is not the same as no activity — see below.
    if (timestamps.length >= minRoutePoints) {
      return _routelessImport(
        fileName: fileName,
        metadata: metadata,
        timestamps: timestamps,
        samples: samples,
      );
    }

    throw const RouteImportException(
      // Now genuinely empty: no places AND no times. A file with neither has
      // nothing in it to import, and this is the guard that keeps a corrupt XML
      // (or an HTML error page saved as .gpx) from arriving as a blank activity.
      'This GPX has nothing in it: no timestamped track points, with or '
      'without locations.',
    );
  }

  /// A GPX with no places, and an activity all the same.
  ///
  /// The app used to refuse this outright — "GPX route must contain at least 2
  /// timestamped location points" — on the theory that a GPX is a list of PLACES
  /// and an indoor session therefore cannot be written as one. That was wrong,
  /// and two real HealthFit exports say so: a strength session of 1931
  /// `<trkpt>`, and an indoor run of 1422, every one of them carrying a `<time>`
  /// and NO `lat`/`lon` at all. The GPX schema does require those attributes;
  /// real exporters omit them anyway, and the file that results is not corrupt —
  /// it is a timestamped series with the positions left out, which is exactly
  /// what an indoor activity is.
  ///
  /// So what a routeless GPX gives up is DISTANCE and CALORIES, not the session:
  /// the timestamps give the start, the end and the duration, and the extensions
  /// give the heart rate. Distance stays 0 (for a strength session there is
  /// nothing to be wrong about, and for a treadmill the file simply did not say),
  /// and calories are left for the entry form to estimate — which it does, from
  /// duration, precisely because nothing here was measured to contradict it.
  static RouteFileImport _routelessImport({
    required String? fileName,
    required RouteFileMetadata metadata,
    required List<DateTime> timestamps,
    required GpxSampleCollector samples,
  }) {
    final ordered = [...timestamps]..sort();
    final startTime = ordered.first;
    final last = ordered.last;
    final endTime =
        last.isAfter(startTime) ? last : startTime.add(const Duration(seconds: 1));

    return RouteFileImport(
      fileName: fileName,
      points: const [],
      distanceMeters: 0.0,
      elevationGainedMeters: 0.0,
      startTime: startTime,
      endTime: endTime,
      durationSeconds: endTime.difference(startTime).inSeconds,
      name: metadata.name,
      description: metadata.description,
      type: metadata.type,
      bleSamples: samples.buffer,
      originalPointCount: 0,
    );
  }

  static RouteFileMetadata _routeMetadata(XmlDocument document) {
    final routeElement = elementsByLocalName(document, 'trk').firstOrNull ??
        elementsByLocalName(document, 'rte').firstOrNull;
    final metadataElement = elementsByLocalName(document, 'metadata').firstOrNull;
    return RouteFileMetadata(
      name: cleanText(
            routeElement == null ? null : directChildText(routeElement, 'name'),
          ) ??
          cleanText(
            metadataElement == null
                ? null
                : directChildText(metadataElement, 'name'),
          ),
      description: cleanText(
            routeElement == null ? null : directChildText(routeElement, 'desc'),
          ) ??
          cleanText(
            metadataElement == null
                ? null
                : directChildText(metadataElement, 'desc'),
          ),
      type: cleanText(
        routeElement == null ? null : directChildText(routeElement, 'type'),
      ),
    );
  }
}

/// Port of the Kotlin `KmlRouteParser`.
class KmlRouteParser {
  const KmlRouteParser._();

  static const int _defaultSyntheticRouteDurationSeconds = 30 * 60;
  static const int _syntheticRoutePointSpacingSeconds = 10;
  static final DateTime _syntheticRouteStartTime =
      DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
  static final RegExp _whitespace = RegExp(r'\s+');

  static RouteFileImport parse(String kmlText, {String? fileName}) {
    final document = XmlDocument.parse(kmlText);
    final tracks = elementsByLocalName(document, 'Track');
    final trackMutablePoints = <MutableRoutePoint>[];
    for (final track in tracks) {
      final times = directChildTexts(track, 'when')
          .map((t) => _timeOrNull(t))
          .whereType<DateTime>()
          .toList();
      final coords = directChildTexts(track, 'coord');
      final pointCount = times.length < coords.length ? times.length : coords.length;
      for (var index = 0; index < pointCount; index++) {
        trackMutablePoints.add(_toKmlTrackPoint(coords[index], times[index]));
      }
    }
    final trackPoints = mutableToRoutePoints(trackMutablePoints);

    if (trackPoints.isNotEmpty) {
      if (trackPoints.length < minRoutePoints) {
        throw const RouteImportException(
          'This KML/KMZ has no gx:Track: it needs at least 2 timestamped location '
          'points.',
        );
      }
      return buildRouteImport(
        fileName: fileName,
        points: trackPoints,
        metadata: _routeMetadata(tracks.firstOrNull, document),
      );
    }

    final lineStrings = elementsByLocalName(document, 'LineString');
    final lineStringPoints = <MutableRoutePoint>[];
    for (final lineString in lineStrings) {
      lineStringPoints.addAll(
        _toKmlLineStringPoints(directChildText(lineString, 'coordinates') ?? ''),
      );
    }
    if (lineStringPoints.length < minRoutePoints) {
      throw const RouteImportException(
        'KML/KMZ route must contain a timestamped gx:Track or LineString with '
        'at least 2 coordinates.',
      );
    }

    final timeRange = _timeRange(lineStrings.firstOrNull, document);
    final startTime = timeRange?.start ?? _syntheticRouteStartTime;
    final endTime = timeRange?.end ??
        startTime.add(
          Duration(
            seconds: (lineStringPoints.length - 1 < 1
                    ? 1
                    : lineStringPoints.length - 1) *
                _syntheticRoutePointSpacingSeconds,
          ),
        );
    return buildRouteImport(
      fileName: fileName,
      points: mutableToRoutePoints(
        _withSyntheticTimes(lineStringPoints, startTime, endTime),
      ),
      metadata: _routeMetadata(lineStrings.firstOrNull, document),
      hasRecordedTimestamps: false,
      hasImportedTimeRange: timeRange != null,
    );
  }

  static RouteFileMetadata _routeMetadata(
    XmlElement? element,
    XmlDocument document,
  ) {
    final placemark =
        element == null ? null : ancestorByLocalName(element, 'Placemark');
    final documentElement = elementsByLocalName(document, 'Document').firstOrNull;
    return RouteFileMetadata(
      name: cleanText(
            placemark == null ? null : directChildText(placemark, 'name'),
          ) ??
          cleanText(
            documentElement == null
                ? null
                : directChildText(documentElement, 'name'),
          ),
      description: cleanText(
            placemark == null
                ? null
                : directChildText(placemark, 'description'),
          ) ??
          cleanText(
            documentElement == null
                ? null
                : directChildText(documentElement, 'description'),
          ),
      type: null,
    );
  }

  static _RouteTimeRange? _timeRange(XmlElement? element, XmlDocument document) {
    final placemark =
        element == null ? null : ancestorByLocalName(element, 'Placemark');
    final documentElement = elementsByLocalName(document, 'Document').firstOrNull;
    for (final candidate in [placemark, documentElement]) {
      if (candidate == null) continue;
      final timeSpan = directChildElement(candidate, 'TimeSpan');
      final timeStamp = directChildElement(candidate, 'TimeStamp');
      final start = (timeSpan == null
              ? null
              : _timeOrNull(directChildText(timeSpan, 'begin'))) ??
          (timeStamp == null
              ? null
              : _timeOrNull(directChildText(timeStamp, 'when')));
      final end = timeSpan == null
          ? null
          : _timeOrNull(directChildText(timeSpan, 'end'));
      final range = _RouteTimeRange.of(start, end);
      if (range != null) return range;
    }
    return null;
  }

  static MutableRoutePoint _toKmlTrackPoint(String coord, DateTime time) {
    final parts = coord.trim().split(_whitespace);
    return MutableRoutePoint(
      longitude: parts.isNotEmpty ? double.tryParse(parts[0]) : null,
      latitude: parts.length > 1 ? double.tryParse(parts[1]) : null,
      elevationMeters: parts.length > 2 ? double.tryParse(parts[2]) : null,
      time: time,
    );
  }

  static List<MutableRoutePoint> _toKmlLineStringPoints(String coordinates) {
    final result = <MutableRoutePoint>[];
    for (final coordinate in coordinates.trim().split(_whitespace)) {
      if (coordinate.isEmpty) continue;
      final parts = coordinate.split(',');
      final longitude = parts.isNotEmpty ? double.tryParse(parts[0]) : null;
      final latitude = parts.length > 1 ? double.tryParse(parts[1]) : null;
      if (longitude == null || latitude == null) continue;
      result.add(
        MutableRoutePoint(
          longitude: longitude,
          latitude: latitude,
          elevationMeters: parts.length > 2 ? double.tryParse(parts[2]) : null,
        ),
      );
    }
    return result;
  }

  static List<MutableRoutePoint> _withSyntheticTimes(
    List<MutableRoutePoint> points,
    DateTime startTime,
    DateTime endTime,
  ) {
    if (points.isEmpty) return const [];
    final rawTotal = endTime.difference(startTime).inMilliseconds;
    final totalMillis = rawTotal < points.length ? points.length : rawTotal;
    final lastOffset = (totalMillis - 1) < 0 ? 0 : (totalMillis - 1);
    final result = <MutableRoutePoint>[];
    for (var index = 0; index < points.length; index++) {
      final offset =
          points.length == 1 ? 0 : (lastOffset * index) ~/ (points.length - 1);
      result.add(
        points[index].copyWith(
          time: startTime.add(Duration(milliseconds: offset)),
        ),
      );
    }
    return result;
  }
}

/// Port of the Kotlin `KmzRouteParser`.
class KmzRouteParser {
  const KmzRouteParser._();

  static RouteFileImport parse(Uint8List kmzBytes, {String? fileName}) {
    final candidates = _zipRouteCandidates(kmzBytes);
    if (candidates.isEmpty) {
      throw const RouteImportException(
        'KMZ file must contain a .gpx or .kml route file.',
      );
    }
    candidates.sort(_compareCandidates);
    final failures = <Object>[];
    for (final candidate in candidates) {
      try {
        final text = utf8.decode(candidate.bytes, allowMalformed: true);
        if (candidate.name.toLowerCase().endsWith('.gpx')) {
          return GpxRouteParser.parse(text, fileName: fileName ?? candidate.name);
        }
        return KmlRouteParser.parse(text, fileName: fileName ?? candidate.name);
      } catch (error) {
        failures.add(error);
      }
    }
    final firstMessage = failures.isNotEmpty && failures.first is RouteImportException
        ? (failures.first as RouteImportException).message
        : 'KMZ file must contain at least 2 route coordinates.';
    throw RouteImportException(firstMessage);
  }

  static List<_ZipRouteCandidate> _zipRouteCandidates(Uint8List bytes) {
    final archive = ZipDecoder().decodeBytes(bytes);
    final candidates = <_ZipRouteCandidate>[];
    for (final file in archive.files) {
      if (file.isFile &&
          (hasExtension(file.name, 'gpx') || hasExtension(file.name, 'kml'))) {
        if (file.size > maxKmzRouteEntryBytes) {
          throw const RouteImportException('KMZ route entry is too large.');
        }
        final Uint8List data = file.content;
        if (data.length > maxKmzRouteEntryBytes) {
          throw const RouteImportException('KMZ route entry is too large.');
        }
        candidates.add(_ZipRouteCandidate(file.name, data));
      }
    }
    return candidates;
  }

  static int _compareCandidates(_ZipRouteCandidate a, _ZipRouteCandidate b) {
    int rankDoc(_ZipRouteCandidate c) =>
        c.name.toLowerCase() == 'doc.kml' ? 0 : 1;
    int rankGpx(_ZipRouteCandidate c) =>
        c.name.toLowerCase().endsWith('.gpx') ? 0 : 1;
    final byDoc = rankDoc(a).compareTo(rankDoc(b));
    if (byDoc != 0) return byDoc;
    final byGpx = rankGpx(a).compareTo(rankGpx(b));
    if (byGpx != 0) return byGpx;
    return a.name.compareTo(b.name);
  }
}

class _ZipRouteCandidate {
  const _ZipRouteCandidate(this.name, this.bytes);

  final String name;
  final Uint8List bytes;
}

class _RouteTimeRange {
  const _RouteTimeRange(this.start, this.end);

  final DateTime start;
  final DateTime end;

  static _RouteTimeRange? of(DateTime? start, DateTime? end) {
    if (start == null) return null;
    final rangeEnd = end ??
        start.add(
          const Duration(
            seconds: KmlRouteParser._defaultSyntheticRouteDurationSeconds,
          ),
        );
    if (!start.isBefore(rangeEnd)) return null;
    return _RouteTimeRange(start, rangeEnd);
  }
}

DateTime? _timeOrNull(String? value) {
  if (value == null) return null;
  return parseRouteInstant(value);
}

extension _FirstOrNull<E> on List<E> {
  E? get firstOrNull => isEmpty ? null : first;
}
