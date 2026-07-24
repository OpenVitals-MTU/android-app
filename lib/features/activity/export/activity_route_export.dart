/// Builds GPX and KMZ files from a workout's GPS route, the port of the Kotlin
/// `ActivityRouteExport.kt` writers. Pure Dart (no Flutter imports) so the
/// output bytes are unit-testable; the SAF save dialog and the "open in map
/// app" launch live with the activity-detail UI.
library;

import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:intl/intl.dart';
import 'package:xml/xml.dart';

import '../../../domain/model/activity_models.dart';

/// Kotlin `ActivityRouteExportFormat`.
enum ActivityRouteExportFormat {
  gpx(mimeType: 'application/gpx+xml', extension: 'gpx'),
  kmz(mimeType: 'application/vnd.google-earth.kmz', extension: 'kmz');

  const ActivityRouteExportFormat({
    required this.mimeType,
    required this.extension,
  });

  final String mimeType;
  final String extension;
}

const String _gpxNamespace = 'http://www.topografix.com/GPX/1/1';
const String _kmlNamespace = 'http://www.opengis.net/kml/2.2';
const String _kmlGxNamespace = 'http://www.google.com/kml/ext/2.2';
const String _kmzDocumentFileName = 'doc.kml';
const int _maxRouteFileNamePrefixLength = 48;

/// Kotlin `ExerciseData.sortedRoutePointsForExport`: the points ordered by
/// time. Throws [StateError] when the route is empty — callers gate the
/// export buttons on a non-empty route, so this only guards misuse.
List<ExerciseRoutePoint> sortedRoutePointsForExport(ExerciseData workout) {
  final routePoints = [...workout.route.points]
    ..sort((a, b) => a.time.compareTo(b.time));
  if (routePoints.isEmpty) {
    throw StateError('Activity has no route points to export.');
  }
  return routePoints;
}

/// Kotlin `writeActivityRouteExport`: the selected format's bytes.
Uint8List buildActivityRouteExport(
  ExerciseData workout,
  List<ExerciseRoutePoint> routePoints,
  ActivityRouteExportFormat format,
) =>
    switch (format) {
      ActivityRouteExportFormat.gpx =>
        Uint8List.fromList(utf8.encode(buildActivityRouteGpx(workout, routePoints))),
      ActivityRouteExportFormat.kmz =>
        buildActivityRouteKmz(workout, routePoints),
    };

/// Kotlin `writeActivityRouteGpx`: a GPX 1.1 track with the workout title and
/// notes as `name`/`desc`, one `trkpt` per route point.
String buildActivityRouteGpx(
  ExerciseData workout,
  List<ExerciseRoutePoint> routePoints,
) {
  final title = _blankToNull(workout.title);
  final notes = _blankToNull(workout.notes);
  final builder = XmlBuilder();
  builder.declaration(encoding: 'UTF-8');
  builder.element('gpx', attributes: {
    'version': '1.1',
    'creator': 'OpenVitals',
    'xmlns': _gpxNamespace,
  }, nest: () {
    builder.element('trk', nest: () {
      if (title != null) builder.element('name', nest: title);
      if (notes != null) builder.element('desc', nest: notes);
      builder.element('trkseg', nest: () {
        for (final point in routePoints) {
          builder.element('trkpt', attributes: {
            'lat': _routeCoordinate(point.latitude),
            'lon': _routeCoordinate(point.longitude),
          }, nest: () {
            final altitude = point.altitudeMeters;
            if (altitude != null) {
              builder.element('ele', nest: _routeDecimal(altitude));
            }
            builder.element('time', nest: _routeInstant(point.time));
          });
        }
      });
    });
  });
  return builder.buildDocument().toXmlString();
}

/// Kotlin `writeActivityRouteKmz`: a zip holding a single `doc.kml` whose
/// `gx:Track` carries the timestamped coordinates.
Uint8List buildActivityRouteKmz(
  ExerciseData workout,
  List<ExerciseRoutePoint> routePoints,
) {
  final kmlBytes = utf8.encode(_buildActivityRouteKml(workout, routePoints));
  final archive = Archive()
    ..addFile(ArchiveFile(_kmzDocumentFileName, kmlBytes.length, kmlBytes));
  return Uint8List.fromList(ZipEncoder().encode(archive));
}

String _buildActivityRouteKml(
  ExerciseData workout,
  List<ExerciseRoutePoint> routePoints,
) {
  final title = _blankToNull(workout.title);
  final notes = _blankToNull(workout.notes);
  final kml = StringBuffer()
    ..writeln('<?xml version="1.0" encoding="UTF-8"?>')
    ..writeln('<kml xmlns="$_kmlNamespace" xmlns:gx="$_kmlGxNamespace">')
    ..writeln('  <Document>');
  if (title != null) {
    kml.writeln('    <name>${_xmlEscaped(title)}</name>');
  }
  kml.writeln('    <Placemark>');
  if (title != null) {
    kml.writeln('      <name>${_xmlEscaped(title)}</name>');
  }
  if (notes != null) {
    kml.writeln('      <description>${_xmlEscaped(notes)}</description>');
  }
  kml.writeln('      <gx:Track>');
  for (final point in routePoints) {
    kml
      ..writeln('        <when>${_routeInstant(point.time)}</when>')
      ..writeln('        <gx:coord>${_routeCoordinate(point.longitude)} '
          '${_routeCoordinate(point.latitude)} '
          '${_routeDecimal(point.altitudeMeters ?? 0.0)}</gx:coord>');
  }
  kml
    ..writeln('      </gx:Track>')
    ..writeln('    </Placemark>')
    ..writeln('  </Document>')
    ..writeln('</kml>');
  return kml.toString();
}

/// Kotlin `ExerciseData.routeExportFileName`:
/// `<sanitized-title>-yyyyMMdd-HHmm.<ext>` in the device's local time zone,
/// falling back to `activity-route` for blank/unsafe titles.
String activityRouteExportFileName(
  ExerciseData workout,
  ActivityRouteExportFormat format,
) {
  final titlePart = _sanitizeRouteFileName(workout.title);
  final timePart =
      DateFormat('yyyyMMdd-HHmm').format(workout.startTime.toLocal());
  return '$titlePart-$timePart.${format.extension}';
}

String _sanitizeRouteFileName(String? title) {
  final sanitized = (title ?? '')
      .trim()
      .toLowerCase()
      .replaceAll(RegExp('[^a-z0-9._-]+'), '-')
      .replaceAll(RegExp(r'^-+|-+$'), '');
  final clipped = sanitized.length > _maxRouteFileNamePrefixLength
      ? sanitized.substring(0, _maxRouteFileNamePrefixLength)
      : sanitized;
  return clipped.isEmpty ? 'activity-route' : clipped;
}

/// Kotlin `takeIf { it.isNotBlank() }`.
String? _blankToNull(String? value) =>
    value == null || value.trim().isEmpty ? null : value;

/// Kotlin wrote `Instant.toString()`, which drops a zero fractional second
/// (`...T08:30:00Z`, never `...T08:30:00.000Z`); Dart's `toIso8601String`
/// always prints milliseconds, so strip the redundant `.000` to match (same
/// convention as `appleInstantToStableString` in the Apple Health import).
String _routeInstant(DateTime time) {
  final iso = time.toUtc().toIso8601String();
  return iso.endsWith('.000Z')
      ? '${iso.substring(0, iso.length - '.000Z'.length)}Z'
      : iso;
}

String _routeCoordinate(double value) => value.toStringAsFixed(7);

String _routeDecimal(double value) => value.toStringAsFixed(2);

String _xmlEscaped(String value) {
  final buffer = StringBuffer();
  for (final rune in value.runes) {
    buffer.write(switch (rune) {
      0x26 => '&amp;',
      0x3C => '&lt;',
      0x3E => '&gt;',
      0x22 => '&quot;',
      0x27 => '&apos;',
      _ => String.fromCharCode(rune),
    });
  }
  return buffer.toString();
}
