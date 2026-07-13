import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:xml/xml.dart';

import 'route_file_parser.dart';

/// Port of the Kotlin `GpxRouteParser`.
class GpxRouteParser {
  const GpxRouteParser._();

  static const Set<String> _pointTags = {'trkpt', 'rtept'};

  static RouteFileImport parse(String gpxText, {String? fileName}) {
    final document = XmlDocument.parse(gpxText);
    final metadata = _routeMetadata(document);
    final mutablePoints = <MutableRoutePoint>[];
    for (final tag in _pointTags) {
      for (final element in elementsByLocalName(document, tag)) {
        mutablePoints.add(
          MutableRoutePoint(
            latitude: double.tryParse(element.getAttribute('lat') ?? ''),
            longitude: double.tryParse(element.getAttribute('lon') ?? ''),
            elevationMeters:
                double.tryParse(directChildText(element, 'ele')?.trim() ?? ''),
            time: _timeOrNull(directChildText(element, 'time')),
          ),
        );
      }
    }
    final routePoints = mutableToRoutePoints(mutablePoints);
    if (routePoints.length < minRoutePoints) {
      throw const RouteImportException(
        // Says what to DO. A GPX is a list of places — its trackpoints REQUIRE a
        // latitude and a longitude — so an indoor activity cannot be expressed in
        // one, and this refusal used to read as though the file were corrupt when
        // the file was simply the wrong format for what the user did. TCX and FIT
        // both carry a session (duration, distance, calories) with no route at
        // all, and the app now reads both.
        'This GPX has no track: it needs at least 2 timestamped location '
        'points. An indoor activity has no GPS, so export it as TCX or FIT '
        'instead — those formats can carry a session with no route.',
      );
    }
    return buildRouteImport(
      fileName: fileName,
      points: routePoints,
      metadata: metadata,
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
