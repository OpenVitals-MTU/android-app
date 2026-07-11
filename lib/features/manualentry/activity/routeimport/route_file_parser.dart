import 'dart:convert';
import 'dart:typed_data';

import 'package:xml/xml.dart';

import '../../../../core/geo/geo_distance.dart';
import '../../../../domain/model/activity_models.dart';
import 'fit_route_parser.dart';
import 'gpx_kml_route_parser.dart';

/// Port of the Kotlin `RouteFileParser`, `RouteFileParsingCommon` and
/// `RouteFileImport`. Pure Dart route-file parsing shared by the GPX / KML / KMZ
/// / FIT parsers.

/// Thrown for expected, user-facing route-import failures (mirrors the Kotlin
/// `IllegalArgumentException` messages the parser produces).
class RouteImportException implements Exception {
  const RouteImportException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// Port of the Kotlin `RouteFileImport` data class.
class RouteFileImport {
  RouteFileImport({
    required this.fileName,
    required this.points,
    required this.distanceMeters,
    required this.elevationGainedMeters,
    this.activeCaloriesKcal,
    this.totalCaloriesKcal,
    required this.startTime,
    required this.endTime,
    this.durationSeconds,
    this.name,
    this.description,
    this.type,
    this.hasRecordedTimestamps = true,
    this.hasImportedTimeRange = true,
    int? originalPointCount,
  }) : originalPointCount = originalPointCount ?? points.length;

  final String? fileName;
  final List<ExerciseRoutePoint> points;
  final double distanceMeters;
  final double elevationGainedMeters;
  final double? activeCaloriesKcal;
  final double? totalCaloriesKcal;
  final DateTime startTime;
  final DateTime endTime;
  final int? durationSeconds;
  final String? name;
  final String? description;
  final String? type;
  final bool hasRecordedTimestamps;
  final bool hasImportedTimeRange;
  final int originalPointCount;

  RouteFileImport copyWith({
    List<ExerciseRoutePoint>? points,
    double? distanceMeters,
    double? elevationGainedMeters,
    double? activeCaloriesKcal,
    double? totalCaloriesKcal,
    DateTime? startTime,
    DateTime? endTime,
    int? durationSeconds,
    int? originalPointCount,
  }) =>
      RouteFileImport(
        fileName: fileName,
        points: points ?? this.points,
        distanceMeters: distanceMeters ?? this.distanceMeters,
        elevationGainedMeters:
            elevationGainedMeters ?? this.elevationGainedMeters,
        activeCaloriesKcal: activeCaloriesKcal ?? this.activeCaloriesKcal,
        totalCaloriesKcal: totalCaloriesKcal ?? this.totalCaloriesKcal,
        startTime: startTime ?? this.startTime,
        endTime: endTime ?? this.endTime,
        durationSeconds: durationSeconds ?? this.durationSeconds,
        name: name,
        description: description,
        type: type,
        hasRecordedTimestamps: hasRecordedTimestamps,
        hasImportedTimeRange: hasImportedTimeRange,
        originalPointCount: originalPointCount ?? this.originalPointCount,
      );
}

/// Port of the Kotlin `RouteFileMetadata`.
class RouteFileMetadata {
  const RouteFileMetadata({this.name, this.description, this.type});

  final String? name;
  final String? description;
  final String? type;
}

/// Port of the Kotlin `MutableRoutePoint`.
class MutableRoutePoint {
  MutableRoutePoint({
    this.latitude,
    this.longitude,
    this.elevationMeters,
    this.time,
  });

  final double? latitude;
  final double? longitude;
  double? elevationMeters;
  DateTime? time;

  MutableRoutePoint copyWith({DateTime? time}) => MutableRoutePoint(
        latitude: latitude,
        longitude: longitude,
        elevationMeters: elevationMeters,
        time: time ?? this.time,
      );
}

/// Port of the Kotlin `RouteFileParser` object.
class RouteFileParser {
  const RouteFileParser._();

  static RouteFileImport parseFile(Uint8List fileBytes, {String? fileName}) {
    if (fileBytes.length > maxRouteFileBytes) {
      throw const RouteImportException('Activity file is too large.');
    }
    try {
      if (isFitFile(fileBytes) || hasExtension(fileName, 'fit')) {
        return FitRouteParser.parse(fileBytes, fileName: fileName);
      }
      if (isZipArchive(fileBytes) || hasExtension(fileName, 'kmz')) {
        return KmzRouteParser.parse(fileBytes, fileName: fileName);
      }
      final routeText = utf8.decode(fileBytes, allowMalformed: true);
      if (hasExtension(fileName, 'kml') ||
          routeText.toLowerCase().contains('<kml')) {
        return KmlRouteParser.parse(routeText, fileName: fileName);
      }
      return parse(routeText, fileName: fileName);
    } on RouteImportException {
      rethrow;
    } catch (_) {
      throw const RouteImportException(
        'Activity file is not a valid GPX, KML, KMZ, or FIT file.',
      );
    }
  }

  static RouteFileImport parse(String gpxText, {String? fileName}) =>
      GpxRouteParser.parse(gpxText, fileName: fileName);
}

// ── Common helpers ──────────────────────────────────────────────────────────

RouteFileImport buildRouteImport({
  required String? fileName,
  required List<ExerciseRoutePoint> points,
  required RouteFileMetadata metadata,
  bool hasRecordedTimestamps = true,
  bool hasImportedTimeRange = true,
}) {
  final sorted = [...points]..sort((a, b) => a.time.compareTo(b.time));
  final seen = <int>{};
  final sortedPoints = <ExerciseRoutePoint>[];
  for (final point in sorted) {
    if (seen.add(point.time.microsecondsSinceEpoch)) {
      sortedPoints.add(point);
    }
  }
  if (sortedPoints.length < minRoutePoints) {
    throw const RouteImportException(
      'Route must contain at least 2 unique location points.',
    );
  }
  final simplified = _simplifyRoutePoints(sortedPoints);
  return RouteFileImport(
    fileName: fileName,
    points: simplified,
    distanceMeters: routeDistanceMeters(sortedPoints),
    elevationGainedMeters: routeElevationGainMeters(sortedPoints),
    startTime: sortedPoints.first.time,
    endTime: sortedPoints.last.time,
    name: metadata.name,
    description: metadata.description,
    type: metadata.type,
    hasRecordedTimestamps: hasRecordedTimestamps,
    hasImportedTimeRange: hasImportedTimeRange,
    originalPointCount: sortedPoints.length,
  );
}

List<XmlElement> elementsByLocalName(XmlNode root, String localName) => root
    .descendants
    .whereType<XmlElement>()
    .where((e) => e.name.local == localName)
    .toList();

XmlElement? directChildElement(XmlElement element, String localName) {
  for (final child in element.childElements) {
    if (child.name.local == localName) return child;
  }
  return null;
}

List<String> directChildTexts(XmlElement element, String localName) {
  final result = <String>[];
  for (final child in element.childElements) {
    if (child.name.local == localName) {
      final text = cleanText(child.innerText);
      if (text != null) result.add(text);
    }
  }
  return result;
}

String? directChildText(XmlElement element, String localName) {
  final texts = directChildTexts(element, localName);
  return texts.isEmpty ? null : texts.first;
}

XmlElement? ancestorByLocalName(XmlElement element, String localName) {
  XmlNode? candidate = element.parent;
  while (candidate != null) {
    if (candidate is XmlElement && candidate.name.local == localName) {
      return candidate;
    }
    candidate = candidate.parent;
  }
  return null;
}

String? cleanText(String? value) {
  if (value == null) return null;
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}

/// Port of the Kotlin `String.toInstantOrNull`. Returns a UTC [DateTime].
DateTime? parseRouteInstant(String value) {
  final parsed = DateTime.tryParse(value.trim());
  return parsed?.toUtc();
}

List<ExerciseRoutePoint> mutableToRoutePoints(List<MutableRoutePoint> points) {
  final result = <ExerciseRoutePoint>[];
  for (final point in points) {
    final time = point.time;
    if (time == null) continue;
    final latitude = point.latitude;
    if (latitude == null || latitude < minLatitude || latitude > maxLatitude) {
      continue;
    }
    final longitude = point.longitude;
    if (longitude == null ||
        longitude < minLongitude ||
        longitude > maxLongitude) {
      continue;
    }
    result.add(
      ExerciseRoutePoint(
        time: time,
        latitude: latitude,
        longitude: longitude,
        altitudeMeters: point.elevationMeters,
        horizontalAccuracyMeters: null,
        verticalAccuracyMeters: null,
      ),
    );
  }
  return result;
}

bool isZipArchive(Uint8List bytes) =>
    bytes.length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4B;

bool hasExtension(String? name, String extension) {
  if (name == null) return false;
  final dot = name.lastIndexOf('.');
  if (dot < 0) return false;
  return name.substring(dot + 1).toLowerCase() == extension.toLowerCase();
}

double routeDistanceMeters(List<ExerciseRoutePoint> points) {
  var total = 0.0;
  for (var i = 0; i + 1 < points.length; i++) {
    total += haversineMeters(
      points[i].latitude,
      points[i].longitude,
      points[i + 1].latitude,
      points[i + 1].longitude,
    );
  }
  return total;
}

double routeElevationGainMeters(List<ExerciseRoutePoint> points) {
  var total = 0.0;
  for (var i = 0; i + 1 < points.length; i++) {
    final startAltitude = points[i].altitudeMeters;
    final endAltitude = points[i + 1].altitudeMeters;
    if (startAltitude != null && endAltitude != null) {
      final delta = endAltitude - startAltitude;
      total += delta > 0.0 ? delta : 0.0;
    }
  }
  return total;
}

List<ExerciseRoutePoint> _simplifyRoutePoints(List<ExerciseRoutePoint> points) {
  if (points.length <= _maxImportedRoutePoints) return points;
  final lastIndex = points.length - 1;
  final step = lastIndex / (_maxImportedRoutePoints - 1);
  final seen = <int>{};
  final result = <ExerciseRoutePoint>[];
  for (var index = 0; index < _maxImportedRoutePoints; index++) {
    final pickRaw = (index * step).toInt();
    final pick = pickRaw < 0
        ? 0
        : (pickRaw > lastIndex ? lastIndex : pickRaw);
    final point = points[pick];
    if (seen.add(point.time.microsecondsSinceEpoch)) {
      result.add(point);
    }
  }
  return result;
}

const int minRoutePoints = 2;
const int _maxImportedRoutePoints = 2000;
const int maxRouteFileBytes = 15 * 1024 * 1024;
const int maxKmzRouteEntryBytes = 15 * 1024 * 1024;
const double minLatitude = -90.0;
const double maxLatitude = 90.0;
const double minLongitude = -180.0;
const double maxLongitude = 180.0;
