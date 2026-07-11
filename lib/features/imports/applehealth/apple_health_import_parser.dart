/// Streaming Apple Health `export.xml` / `export.zip` parser, ported from the
/// Kotlin `AppleHealthImportParser.kt`.
///
/// Uses `package:xml`'s event API (`parseEvents`) so the document is consumed as
/// a stream of start/end/text events without materializing a DOM.
///
/// The ZIP is read **sequentially** from local file headers over an
/// `InputFileStream` — the Dart analogue of Kotlin's `ZipInputStream`, and the
/// reason a truncated export is recoverable at all. The previous
/// `ZipDecoder().decodeBytes()` was a whole-archive random-access decode that
/// needs the end-of-central-directory record, so a ZIP whose tail was missing
/// failed instantly (even for analysis) and every entry landed in RAM. Reading
/// forward instead means `export.xml` (extracted to a temp file, never a
/// `List<int>`) is usable even when the archive is cut short inside a later
/// `workout-routes/*.gpx` entry; see [AppleWorkoutRouteArchiveFailure].
///
/// Character repair and DTD stripping happen in
/// `apple_health_import_xml_support.dart` before any event is emitted.
library;

import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:xml/xml.dart';
import 'package:xml/xml_events.dart';

import 'apple_health_import_models.dart';
import 'apple_health_import_route_parser.dart';
import 'apple_health_import_types.dart';
import 'apple_health_import_xml_support.dart';

/// How many parsed elements go by between two [AppleHealthParseOptions.onElementsParsed]
/// ticks.
///
/// Deliberately a **count**, not a time interval: the parse is one tight
/// synchronous loop over every XML event in a file that can be several
/// gigabytes, and a `DateTime.now()` per element would itself show up in the
/// profile. 5 000 elements is fine enough for a smooth bar (a big export ticks
/// hundreds of times) and coarse enough that the modulo is free.
const int kAppleHealthParseProgressInterval = 5000;

class AppleHealthParseOptions {
  const AppleHealthParseOptions({
    this.parseRouteFiles = true,
    this.parseRecordDetails = true,
    this.shouldMaterializeRecord,
    this.onRecordSkipped,
    this.onElementsParsed,
  });

  final bool parseRouteFiles;
  final bool parseRecordDetails;

  /// Early-skip predicate, asked once per `<Record>` with only its Apple type
  /// (Kotlin hangs this off its streaming consumer; this port has none, so it
  /// rides on the parse options instead).
  ///
  /// Returning `false` means the record's dates, value and metadata are never
  /// parsed, no [AppleRecord] is allocated, and nothing is buffered — the single
  /// biggest win in this port, where the whole export is otherwise materialized
  /// in RAM. `null` (the default) materializes everything, so the analysis pass
  /// keeps seeing every record.
  final bool Function(String type)? shouldMaterializeRecord;

  /// Called for each record [shouldMaterializeRecord] rejected, so the caller can
  /// still book its totals (Kotlin `onRecordSkipped`).
  final void Function(String type)? onRecordSkipped;

  /// The running element total (records + workouts + correlations + activity
  /// summaries), reported every [kAppleHealthParseProgressInterval] elements.
  ///
  /// Without this the scan denominator is useless: the whole export is parsed in
  /// one blocking call, so the numerator would be 0 for the entire scan and the
  /// bar would sit at 0% for minutes before jumping. Kotlin gets the same ticks
  /// for free from its streaming consumer.
  final void Function(int parsedElements)? onElementsParsed;
}

/// Streaming consumer callbacks (Kotlin `AppleHealthXmlEventConsumer`).
abstract interface class AppleHealthXmlEventConsumer {
  void onParsedType(String type);
  void onRecord(AppleRecord record);
  void onWorkout(AppleWorkout workout);
  void onCorrelation(AppleCorrelation correlation);
  void onActivitySummary();
}

class AppleHealthImportParser {
  const AppleHealthImportParser._();

  /// Parses a staged export [file] — a `.zip` (read sequentially) or a bare
  /// `export.xml`. This is the production entry point; the export never has to
  /// exist as a `List<int>`.
  static AppleParsedExport parseFile(
    File file, {
    AppleHealthXmlEventConsumer? consumer,
    AppleHealthParseOptions options = const AppleHealthParseOptions(),
    Map<String, AppleWorkoutRouteFile> routeFiles = const {},
  }) {
    if (_fileHasZipHeader(file)) {
      return _parseZipFile(file, consumer, routeFiles, options);
    }
    final xml = utf8.decode(file.readAsBytesSync(), allowMalformed: true);
    return _parseXmlExport(xml, consumer, routeFiles, options);
  }

  /// Byte-oriented entry point kept for XML fixtures and callers that already
  /// hold the document. A zipped payload is spilled to a temp file so it goes
  /// through exactly the same sequential reader as [parseFile].
  static AppleParsedExport parse(
    List<int> bytes, {
    AppleHealthXmlEventConsumer? consumer,
    AppleHealthParseOptions options = const AppleHealthParseOptions(),
    Map<String, AppleWorkoutRouteFile> routeFiles = const {},
  }) {
    if (_hasZipHeader(bytes)) {
      final directory = Directory.systemTemp.createTempSync('apple_health_zip');
      final file = File('${directory.path}/export.zip')
        ..writeAsBytesSync(bytes);
      try {
        return _parseZipFile(file, consumer, routeFiles, options);
      } finally {
        try {
          directory.deleteSync(recursive: true);
        } catch (_) {
          // Best effort.
        }
      }
    }
    final xml = utf8.decode(bytes, allowMalformed: true);
    return _parseXmlExport(xml, consumer, routeFiles, options);
  }

  /// Walks the ZIP's local file headers front-to-back (Kotlin `ZipInputStream`),
  /// extracting `export.xml` to a temp file and streaming each route GPX.
  static AppleParsedExport _parseZipFile(
    File file,
    AppleHealthXmlEventConsumer? consumer,
    Map<String, AppleWorkoutRouteFile> routeFiles,
    AppleHealthParseOptions options,
  ) {
    final resolvedRouteFiles = Map<String, AppleWorkoutRouteFile>.of(routeFiles);
    final fileLength = file.lengthSync();
    final input = InputFileStream(file.path);
    final workspace = Directory.systemTemp.createTempSync('apple_health_export');
    final exportXml = File('${workspace.path}/export.xml');
    var foundExportXml = false;
    AppleWorkoutRouteArchiveFailure? routeArchiveFailure;

    try {
      var position = 0;
      while (position + _localHeaderMinimumBytes <= fileLength) {
        input.setPosition(position);
        if (input.readUint32() != _localFileHeaderSignature) break;
        final entry = _readLocalHeader(input, position, fileLength);
        if (entry == null) {
          // The header itself was cut short: we cannot even name the entry, so
          // this is never recoverable (Kotlin throws with entryName = null).
          throw AppleHealthZipReadException();
        }
        final truncated = entry.dataEnd > fileLength;
        if (_isAppleHealthExportXml(entry.name)) {
          if (truncated) {
            // Damage at or before export.xml is fatal — there is nothing to
            // import from a half-read document.
            throw AppleHealthZipReadException(entryName: entry.name);
          }
          _extractEntryToFile(input, entry, exportXml);
          foundExportXml = true;
        } else if (options.parseRouteFiles &&
            _isAppleWorkoutRouteFile(entry.name)) {
          final failure = _readRouteEntry(
            input,
            entry,
            fileLength,
            resolvedRouteFiles,
          );
          if (failure != null) {
            if (!foundExportXml) {
              throw AppleHealthZipReadException(
                entryName: failure.entryName,
                decompressedBytesRead: failure.decompressedBytesRead,
              );
            }
            // export.xml is already safe: keep the health records, drop this
            // route and every entry after it.
            routeArchiveFailure = failure;
            break;
          }
        }
        if (truncated) break;
        // Always strictly greater than `position`: an entry is at least a
        // 30-byte header, so the walk cannot stall.
        position = entry.nextEntryOffset;
      }

      if (!foundExportXml) {
        throw ArgumentError('Apple Health export.zip must contain export.xml.');
      }

      final xml = utf8.decode(exportXml.readAsBytesSync(), allowMalformed: true);
      final parsed = _parseXmlExport(xml, consumer, resolvedRouteFiles, options);
      return parsed.copyWithRouteArchiveFailure(routeArchiveFailure);
    } finally {
      input.closeSync();
      try {
        workspace.deleteSync(recursive: true);
      } catch (_) {
        // Best effort; the temp dir is reclaimed by the OS otherwise.
      }
    }
  }

  static AppleParsedExport _parseXmlExport(
    String rawXml,
    AppleHealthXmlEventConsumer? consumer,
    Map<String, AppleWorkoutRouteFile> routeFiles,
    AppleHealthParseOptions options,
  ) {
    final sanitized = sanitizeAppleHealthXml(rawXml);
    final handler = _AppleHealthXmlHandler(consumer, routeFiles, options);
    try {
      for (final event in parseEvents(
        sanitized.text,
        validateNesting: true,
        withBuffer: true,
      )) {
        if (event is XmlStartElementEvent) {
          handler.startElement(event.name, event.attributes);
          if (event.isSelfClosing) handler.endElement(event.name);
        } else if (event is XmlEndElementEvent) {
          handler.endElement(event.name);
        }
      }
    } on XmlException catch (error) {
      final position = error is XmlParserException
          ? (error.position ?? sanitized.text.length)
          : sanitized.text.length;
      final start = position - 200 < 0 ? 0 : position - 200;
      final end = position > sanitized.text.length ? sanitized.text.length : position;
      throw AppleHealthXmlParseException(buildAppleHealthXmlParseMessage(
        location: 'position $position',
        causeMessage: error.message,
        recentContext: sanitized.text.substring(start, end),
        strippedControlChars: sanitized.strippedControlChars,
        escapedAmpersands: sanitized.escapedAmpersands,
      ));
    }
    return handler.result(
      sanitizedControlChars: sanitized.strippedControlChars,
      sanitizedAmpersands: sanitized.escapedAmpersands,
    );
  }
}

// ── Streaming date parsing ────────────────────────────────────────────────

final RegExp _appleDateTimeRegex = RegExp(
  r'^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?\s*'
  r'([+-]\d{2}:?\d{2}|[Zz])?$',
);

/// Parses an Apple export timestamp, preserving its wall-clock offset. Handles
/// `yyyy-MM-dd HH:mm:ss ±ZZZZ`, fractional seconds, local (no-offset), and ISO.
AppleDateTime? parseAppleDateTime(String value) {
  final match = _appleDateTimeRegex.firstMatch(value.trim());
  if (match != null) {
    final year = int.parse(match.group(1)!);
    final month = int.parse(match.group(2)!);
    final day = int.parse(match.group(3)!);
    final hour = int.parse(match.group(4)!);
    final minute = int.parse(match.group(5)!);
    final second = int.parse(match.group(6)!);
    final fraction = match.group(7);
    final millis =
        fraction == null ? 0 : int.parse('${fraction}000'.substring(0, 3));
    final offsetText = match.group(8);
    if (offsetText != null && offsetText.isNotEmpty) {
      final Duration offset;
      if (offsetText == 'Z' || offsetText == 'z') {
        offset = Duration.zero;
      } else {
        final sign = offsetText[0] == '-' ? -1 : 1;
        final digits = offsetText.substring(1).replaceAll(':', '');
        offset = Duration(
              hours: int.parse(digits.substring(0, 2)),
              minutes: int.parse(digits.substring(2, 4)),
            ) *
            sign;
      }
      final wall = DateTime.utc(year, month, day, hour, minute, second, millis);
      return AppleDateTime(wall.subtract(offset), offset);
    }
    final wall = DateTime(year, month, day, hour, minute, second, millis);
    return AppleDateTime(wall.toUtc(), wall.timeZoneOffset);
  }
  final parsed = DateTime.tryParse(value.trim());
  if (parsed != null) return AppleDateTime(parsed.toUtc(), Duration.zero);
  return null;
}

// ── Event handler ─────────────────────────────────────────────────────────

abstract class _MutableElement {
  final Map<String, String> metadata = {};
}

/// The stack marker for a `<Record>` the caller asked us not to materialize
/// (Kotlin's `object SkippedAppleRecord`).
///
/// Kotlin can make its sentinel's `metadata` *throw*, so a missed guard is a
/// crash. Dart cannot: `_MutableElement.metadata` is a final field with an
/// initializer, so this sentinel owns a perfectly usable (and, being a
/// singleton, **shared**) map. Every handler that writes through `_top` must
/// therefore check for it explicitly — see the `MetadataEntry` case — or a
/// skipped record's metadata would leak into the next one.
class _SkippedRecord extends _MutableElement {}

final _SkippedRecord _skippedRecord = _SkippedRecord();

class _MutableWorkoutRoute extends _MutableElement {
  final List<String> paths = [];
}

class _MutableRecord extends _MutableElement {
  _MutableRecord(Map<String, String> attrs, this.parentCorrelation, this.parseDetails)
      : type = attrs['type'] ?? 'Record',
        sourceName = attrs['sourceName'],
        sourceVersion = attrs['sourceVersion'],
        device = attrs['device'],
        unit = attrs['unit'],
        creationDate = _appleDate(attrs['creationDate'], parseDetails),
        startDate = _appleDate(attrs['startDate'], parseDetails),
        endDate = _appleDate(attrs['endDate'], parseDetails),
        rawValue = attrs['value'];

  final String type;
  final String? sourceName;
  final String? sourceVersion;
  final String? device;
  final String? unit;
  final AppleDateTime? creationDate;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
  final String? rawValue;
  final _MutableCorrelation? parentCorrelation;
  final bool parseDetails;

  AppleRecord toRecord() => AppleRecord(
        type: type,
        sourceName: sourceName,
        sourceVersion: sourceVersion,
        device: device,
        unit: unit,
        creationDate: creationDate,
        startDate: startDate,
        endDate: endDate,
        rawValue: rawValue,
        numericValue: parseDetails ? double.tryParse(rawValue ?? '') : null,
        metadata: Map.of(metadata),
        correlationType: parentCorrelation?.type,
      );
}

class _MutableWorkout extends _MutableElement {
  _MutableWorkout(Map<String, String> attrs, bool parseDetails)
      : workoutActivityType = attrs['workoutActivityType'] ?? 'Workout',
        sourceName = attrs['sourceName'],
        sourceVersion = attrs['sourceVersion'],
        device = attrs['device'],
        creationDate = _appleDate(attrs['creationDate'], parseDetails),
        startDate = _appleDate(attrs['startDate'], parseDetails),
        endDate = _appleDate(attrs['endDate'], parseDetails),
        duration = double.tryParse(attrs['duration'] ?? ''),
        durationUnit = attrs['durationUnit'],
        totalDistance = double.tryParse(attrs['totalDistance'] ?? ''),
        totalDistanceUnit = attrs['totalDistanceUnit'],
        totalEnergyBurned = double.tryParse(attrs['totalEnergyBurned'] ?? ''),
        totalEnergyBurnedUnit = attrs['totalEnergyBurnedUnit'] {
    _hasTotalDistanceAttribute = totalDistance != null;
    _hasTotalEnergyBurnedAttribute = totalEnergyBurned != null;
  }

  final String workoutActivityType;
  final String? sourceName;
  final String? sourceVersion;
  final String? device;
  final AppleDateTime? creationDate;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
  final double? duration;
  final String? durationUnit;
  double? totalDistance;
  String? totalDistanceUnit;
  double? totalEnergyBurned;
  String? totalEnergyBurnedUnit;
  late final bool _hasTotalDistanceAttribute;
  late final bool _hasTotalEnergyBurnedAttribute;
  final List<AppleWorkoutEvent> events = [];
  final List<AppleWorkoutRouteFile> routes = [];
  final List<String> routeReferencePaths = [];

  void addStatistic(Map<String, String> attrs) {
    final type = attrs['type'];
    if (type == null) return;
    final sum = double.tryParse(attrs['sum'] ?? '');
    if (sum == null) return;
    final unit = attrs['unit'];
    if (appleDistanceTypes.contains(type)) {
      if (!_hasTotalDistanceAttribute) {
        totalDistance = _addCompatible(totalDistance, sum, totalDistanceUnit, unit);
      }
      totalDistanceUnit ??= unit;
    } else if (type == appleActiveEnergyBurned) {
      if (!_hasTotalEnergyBurnedAttribute) {
        totalEnergyBurned =
            _addCompatible(totalEnergyBurned, sum, totalEnergyBurnedUnit, unit);
      }
      totalEnergyBurnedUnit ??= unit;
    }
  }

  void addRoute(AppleWorkoutRouteFile route) => routes.add(route);

  AppleWorkout toWorkout() {
    final deduped = <String, AppleWorkoutRouteFile>{};
    for (final route in routes) {
      deduped.putIfAbsent(route.path, () => route);
    }
    return AppleWorkout(
      workoutActivityType: workoutActivityType,
      sourceName: sourceName,
      sourceVersion: sourceVersion,
      device: device,
      creationDate: creationDate,
      startDate: startDate,
      endDate: endDate,
      duration: duration,
      durationUnit: durationUnit,
      totalDistance: totalDistance,
      totalDistanceUnit: totalDistanceUnit,
      totalEnergyBurned: totalEnergyBurned,
      totalEnergyBurnedUnit: totalEnergyBurnedUnit,
      metadata: Map.of(metadata),
      events: List.of(events),
      routes: deduped.values.toList(),
      routeReferencePaths: routeReferencePaths.toSet().toList(),
    );
  }
}

double _addCompatible(
  double? current,
  double value,
  String? currentUnit,
  String? valueUnit,
) {
  if (current == null || currentUnit == null || currentUnit == valueUnit) {
    return (current ?? 0.0) + value;
  }
  return current;
}

class _MutableCorrelation extends _MutableElement {
  _MutableCorrelation(Map<String, String> attrs, bool parseDetails)
      : type = attrs['type'] ?? 'Correlation',
        sourceName = attrs['sourceName'],
        sourceVersion = attrs['sourceVersion'],
        device = attrs['device'],
        creationDate = _appleDate(attrs['creationDate'], parseDetails),
        startDate = _appleDate(attrs['startDate'], parseDetails),
        endDate = _appleDate(attrs['endDate'], parseDetails);

  final String type;
  final String? sourceName;
  final String? sourceVersion;
  final String? device;
  final AppleDateTime? creationDate;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
  final List<AppleRecord> records = [];

  AppleCorrelation toCorrelation() => AppleCorrelation(
        type: type,
        sourceName: sourceName,
        sourceVersion: sourceVersion,
        device: device,
        creationDate: creationDate,
        startDate: startDate,
        endDate: endDate,
        metadata: Map.of(metadata),
        records: List.of(records),
      );
}

AppleDateTime? _appleDate(String? value, bool parseDetails) {
  if (!parseDetails || value == null) return null;
  return parseAppleDateTime(value);
}

class _AppleHealthXmlHandler {
  _AppleHealthXmlHandler(this._consumer, this._routeFiles, this._options);

  final AppleHealthXmlEventConsumer? _consumer;
  final Map<String, AppleWorkoutRouteFile> _routeFiles;
  final AppleHealthParseOptions _options;

  bool get _parseRecordDetails => _options.parseRecordDetails;

  final List<_MutableElement> _stack = [];
  final List<AppleRecord> _records = [];
  final List<AppleWorkout> _workouts = [];
  final List<AppleCorrelation> _correlations = [];
  final Map<String, int> _typeCounts = {};
  int _parsedRecords = 0;
  int _parsedWorkouts = 0;
  int _parsedCorrelations = 0;
  int _parsedActivitySummaries = 0;
  int _parsedElements = 0;

  _MutableElement? get _top => _stack.isEmpty ? null : _stack.last;

  /// One tick of the scan's numerator. Called once per top-level element in the
  /// parse's hot loop, so it stays a counter bump plus a modulo.
  void _countElement() {
    _parsedElements++;
    final onElementsParsed = _options.onElementsParsed;
    if (onElementsParsed != null &&
        _parsedElements % kAppleHealthParseProgressInterval == 0) {
      onElementsParsed(_parsedElements);
    }
  }

  void startElement(String name, List<XmlEventAttribute> rawAttributes) {
    switch (name) {
      case 'Record':
        _parsedRecords++;
        _countElement();
        final attrs = _attributes(rawAttributes);
        final type = attrs['type'] ?? 'Record';
        _countType(type);
        _consumer?.onParsedType(type);
        final parent = _top is _MutableCorrelation ? _top as _MutableCorrelation : null;
        // A correlation's children are ALWAYS materialized: the parent decides
        // whether the group converts (a blood-pressure correlation is systolic +
        // diastolic), so dropping a child would silently break it.
        final materialize = parent != null ||
            (_options.shouldMaterializeRecord?.call(type) ?? true);
        if (!materialize) {
          _options.onRecordSkipped?.call(type);
          _stack.add(_skippedRecord);
          return;
        }
        _stack.add(_MutableRecord(attrs, parent, _parseRecordDetails));
      case 'Workout':
        _parsedWorkouts++;
        _countElement();
        final attrs = _attributes(rawAttributes);
        final type = attrs['workoutActivityType'] ?? 'Workout';
        _countType(type);
        _consumer?.onParsedType(type);
        _stack.add(_MutableWorkout(attrs, _parseRecordDetails));
      case 'Correlation':
        _parsedCorrelations++;
        _countElement();
        final attrs = _attributes(rawAttributes);
        final type = attrs['type'] ?? 'Correlation';
        _countType(type);
        _consumer?.onParsedType(type);
        _stack.add(_MutableCorrelation(attrs, _parseRecordDetails));
      case 'MetadataEntry':
        if (!_parseRecordDetails) return;
        // The sentinel's metadata map is real and shared; writing to it would
        // corrupt the *next* skipped record (see [_SkippedRecord]).
        if (identical(_top, _skippedRecord)) return;
        final attrs = _attributes(rawAttributes);
        final key = attrs['key'];
        final value = attrs['value'];
        if (key != null && value != null) {
          _top?.metadata[key] = value;
        }
      case 'WorkoutEvent':
        final workout = _top;
        if (workout is! _MutableWorkout) return;
        final attrs = _attributes(rawAttributes);
        workout.events.add(AppleWorkoutEvent(
          type: attrs['type'],
          date: _appleDate(attrs['date'], _parseRecordDetails),
          duration: double.tryParse(attrs['duration'] ?? ''),
          durationUnit: attrs['durationUnit'],
        ));
      case 'WorkoutStatistics':
        final workout = _top;
        if (workout is! _MutableWorkout) return;
        workout.addStatistic(_attributes(rawAttributes));
      case 'WorkoutRoute':
        if (_top is _MutableWorkout) {
          _stack.add(_MutableWorkoutRoute());
        }
      case 'FileReference':
        final route = _top;
        if (route is! _MutableWorkoutRoute) return;
        final path = _attributes(rawAttributes)['path'];
        if (path != null) route.paths.add(path);
      case 'ActivitySummary':
        _parsedActivitySummaries++;
        _countElement();
        _countType('ActivitySummary');
        _consumer?.onParsedType('ActivitySummary');
        _consumer?.onActivitySummary();
    }
  }

  void endElement(String name) {
    switch (name) {
      case 'Record':
        // Pop UNCONDITIONALLY: `_removeTop` leaves a non-matching top in place,
        // which would strand the sentinel on the stack and corrupt every later
        // lookup (correlation parenting, workout events, route files).
        if (_stack.isEmpty) return;
        final popped = _stack.removeLast();
        if (identical(popped, _skippedRecord)) return;
        if (popped is! _MutableRecord) return;
        final element = popped;
        final record = element.toRecord();
        final parent = _top is _MutableCorrelation ? _top as _MutableCorrelation : null;
        if (parent != null) {
          parent.records.add(record.copyWith(correlationType: parent.type));
        } else if (_consumer != null) {
          _consumer.onRecord(record);
        } else {
          _records.add(record);
        }
      case 'Workout':
        final element = _removeTop<_MutableWorkout>();
        if (element == null) return;
        final workout = element.toWorkout();
        if (_consumer != null) {
          _consumer.onWorkout(workout);
        } else {
          _workouts.add(workout);
        }
      case 'Correlation':
        final element = _removeTop<_MutableCorrelation>();
        if (element == null) return;
        final correlation = element.toCorrelation();
        if (_consumer != null) {
          _consumer.onCorrelation(correlation);
        } else {
          _correlations.add(correlation);
        }
      case 'WorkoutRoute':
        final route = _removeTop<_MutableWorkoutRoute>();
        if (route == null) return;
        final workout = _top;
        if (workout is! _MutableWorkout) return;
        final referencedPaths = <String>{};
        for (final path in route.paths) {
          referencedPaths.add(normalizedAppleWorkoutRoutePath(path));
        }
        workout.routeReferencePaths.addAll(referencedPaths);
        for (final path in referencedPaths) {
          final routeFile = _routeFiles[path];
          if (routeFile != null) workout.addRoute(routeFile);
        }
    }
  }

  T? _removeTop<T extends _MutableElement>() {
    if (_stack.isEmpty || _stack.last is! T) return null;
    return _stack.removeLast() as T;
  }

  void _countType(String type) => _typeCounts[type] = (_typeCounts[type] ?? 0) + 1;

  AppleParsedExport result({
    required int sanitizedControlChars,
    required int sanitizedAmpersands,
  }) =>
      AppleParsedExport(
        records: _records,
        workouts: _workouts,
        correlations: _correlations,
        parsedRecords: _parsedRecords,
        parsedWorkouts: _parsedWorkouts,
        parsedCorrelations: _parsedCorrelations,
        parsedActivitySummaries: _parsedActivitySummaries,
        parsedTypeCounts: _typeCounts,
        sanitizedControlChars: sanitizedControlChars,
        sanitizedAmpersands: sanitizedAmpersands,
      );
}

Map<String, String> _attributes(List<XmlEventAttribute> attributes) {
  final result = <String, String>{};
  for (final attribute in attributes) {
    final value = attribute.value;
    if (value.trim().isNotEmpty) result[attribute.name] = value;
  }
  return result;
}

// ── Sequential ZIP reading ────────────────────────────────────────────────
//
// The Dart analogue of Kotlin's `ZipInputStream`: walk the local file headers
// forward instead of seeking to the end-of-central-directory record, so a ZIP
// whose tail is missing is still readable up to the point of damage.

const int _localFileHeaderSignature = 0x04034b50;
const int _centralDirectorySignature = 0x02014b50;
const int _dataDescriptorSignature = 0x08074b50;
const int _localHeaderMinimumBytes = 30;
const int _zip64ExtraFieldId = 0x0001;
const int _dataDescriptorFlag = 0x08;
const int _compressionDeflate = 8;
const int _sizeUnknown = 0xFFFFFFFF;

class _ZipLocalEntry {
  const _ZipLocalEntry({
    required this.name,
    required this.compressionMethod,
    required this.dataOffset,
    required this.compressedSize,
    required this.nextEntryOffset,
  });

  final String name;
  final int compressionMethod;
  final int dataOffset;
  final int compressedSize;

  /// Where the *next* local header starts (past any data descriptor).
  final int nextEntryOffset;

  int get dataEnd => dataOffset + compressedSize;
}

/// Reads one local file header, with [input] positioned just past its signature.
/// Returns `null` when the header (or its name/extra field) runs past the end of
/// a truncated file.
_ZipLocalEntry? _readLocalHeader(
  InputFileStream input,
  int headerOffset,
  int fileLength,
) {
  input.readUint16(); // version needed
  final flags = input.readUint16();
  final compressionMethod = input.readUint16();
  input
    ..readUint16() // modification time
    ..readUint16() // modification date
    ..readUint32(); // crc32
  var compressedSize = input.readUint32();
  var uncompressedSize = input.readUint32();
  final nameLength = input.readUint16();
  final extraLength = input.readUint16();
  final headerEnd =
      headerOffset + _localHeaderMinimumBytes + nameLength + extraLength;
  if (headerEnd > fileLength) return null;

  final name = input.readString(size: nameLength, utf8: true);
  final extra = input.readBytes(extraLength).toUint8List();
  if (compressedSize == _sizeUnknown || uncompressedSize == _sizeUnknown) {
    final zip64 = _readZip64Sizes(extra);
    if (zip64 != null) {
      uncompressedSize = zip64.$1;
      compressedSize = zip64.$2;
    }
  }

  final dataOffset = headerEnd;
  if (compressedSize > 0) {
    return _ZipLocalEntry(
      name: name,
      compressionMethod: compressionMethod,
      dataOffset: dataOffset,
      compressedSize: compressedSize,
      nextEntryOffset: dataOffset + compressedSize,
    );
  }

  // A streaming ZIP writer leaves the sizes at zero and appends a data
  // descriptor after the payload. There is no central directory to consult
  // (that is the whole point of reading sequentially), so bound the entry by
  // the next signature in the file.
  final hasDataDescriptor = (flags & _dataDescriptorFlag) != 0;
  final boundary = _findNextSignature(input, dataOffset, fileLength);
  if (boundary == null) {
    // Nothing follows. An intact archive always has a central directory, so
    // either this is a genuinely empty entry, or the file was cut short — in
    // which case the caller sees dataEnd > fileLength and reports the damage.
    final isEmptyEntry = uncompressedSize == 0 && !hasDataDescriptor;
    return _ZipLocalEntry(
      name: name,
      compressionMethod: compressionMethod,
      dataOffset: dataOffset,
      compressedSize: isEmptyEntry ? 0 : fileLength - dataOffset + 1,
      nextEntryOffset: fileLength,
    );
  }
  final descriptorLength = hasDataDescriptor
      ? _dataDescriptorLength(input, boundary, dataOffset)
      : 0;
  return _ZipLocalEntry(
    name: name,
    compressionMethod: compressionMethod,
    dataOffset: dataOffset,
    compressedSize: boundary - descriptorLength - dataOffset,
    nextEntryOffset: boundary,
  );
}

/// The zip64 extended-information extra field: `(uncompressedSize, compressedSize)`.
(int, int)? _readZip64Sizes(Uint8List extra) {
  var index = 0;
  while (index + 4 <= extra.length) {
    final id = extra[index] | (extra[index + 1] << 8);
    final size = extra[index + 2] | (extra[index + 3] << 8);
    final body = index + 4;
    if (id == _zip64ExtraFieldId && size >= 16 && body + 16 <= extra.length) {
      return (
        _readUint64(extra, body),
        _readUint64(extra, body + 8),
      );
    }
    index = body + size;
  }
  return null;
}

int _readUint64(Uint8List bytes, int offset) {
  var value = 0;
  for (var index = 7; index >= 0; index--) {
    value = (value << 8) | bytes[offset + index];
  }
  return value;
}

/// Scans forward for the next local-header / central-directory signature, which
/// bounds an entry whose size the local header did not record.
int? _findNextSignature(InputFileStream input, int from, int fileLength) {
  const chunkSize = 64 * 1024;
  var position = from;
  while (position + 4 <= fileLength) {
    final length = (fileLength - position).clamp(0, chunkSize);
    input.setPosition(position);
    final chunk = input.readBytes(length).toUint8List();
    for (var index = 0; index + 4 <= chunk.length; index++) {
      if (chunk[index] != 0x50 || chunk[index + 1] != 0x4B) continue;
      final signature = chunk[index] |
          (chunk[index + 1] << 8) |
          (chunk[index + 2] << 16) |
          (chunk[index + 3] << 24);
      if (signature == _localFileHeaderSignature ||
          signature == _centralDirectorySignature) {
        return position + index;
      }
    }
    if (length < chunkSize) break;
    // Overlap by three bytes so a signature straddling a chunk edge is seen.
    position += chunkSize - 3;
  }
  return null;
}

/// How many bytes of data descriptor sit between the entry data and [boundary].
/// Only called when the local header's data-descriptor flag was set, so one is
/// definitely there: 16 bytes with the optional signature, 12 without.
int _dataDescriptorLength(InputFileStream input, int boundary, int dataOffset) {
  if (boundary - 16 >= dataOffset) {
    input.setPosition(boundary - 16);
    if (input.readUint32() == _dataDescriptorSignature) return 16;
  }
  if (boundary - 12 >= dataOffset) return 12;
  return 0;
}

/// Extracts a (never-truncated) entry to [destination] without holding it in RAM.
void _extractEntryToFile(
  InputFileStream input,
  _ZipLocalEntry entry,
  File destination,
) {
  final data = input.subset(
    position: entry.dataOffset,
    length: entry.compressedSize,
  );
  final output = OutputFileStream(destination.path);
  try {
    if (entry.compressionMethod == _compressionDeflate) {
      ZLibDecoder().decodeStream(data, output, raw: true);
    } else {
      output.writeStream(data);
    }
  } finally {
    output.closeSync();
  }
}

/// Reads one `workout-routes/*.gpx` entry into [routeFiles]. Returns a failure
/// when the entry could not be read because the archive ends inside it (or its
/// GPX is cut short) — the caller decides whether that is fatal.
AppleWorkoutRouteArchiveFailure? _readRouteEntry(
  InputFileStream input,
  _ZipLocalEntry entry,
  int fileLength,
  Map<String, AppleWorkoutRouteFile> routeFiles,
) {
  final truncated = entry.dataEnd > fileLength;
  final available = truncated ? fileLength - entry.dataOffset : entry.compressedSize;
  final data = input.subset(
    position: entry.dataOffset,
    length: available < 0 ? 0 : available,
  );
  final output = OutputMemoryStream();
  var decompressionFailed = false;
  try {
    if (entry.compressionMethod == _compressionDeflate) {
      // The pure-Dart inflater yields whatever it managed to decode before the
      // stream ran out, which is exactly the byte count Kotlin reports.
      Inflate.stream(data, output: output);
    } else {
      output.writeStream(data);
    }
  } catch (_) {
    decompressionFailed = true;
  }
  final bytes = output.getBytes();
  if (truncated || decompressionFailed) {
    return AppleWorkoutRouteArchiveFailure(
      entryName: entry.name,
      decompressedBytesRead: bytes.length,
    );
  }
  final gpx = utf8.decode(bytes, allowMalformed: true);
  try {
    final routeFile = AppleHealthImportRouteParser.parse(entry.name, gpx);
    if (routeFile != null) routeFiles[routeFile.path] = routeFile;
  } catch (_) {
    // A GPX that does not parse as XML at all means the entry's bytes are not
    // what the archive claimed (Kotlin maps an unexpected XML end inside an
    // archived entry onto the same ZIP-read failure).
    return AppleWorkoutRouteArchiveFailure(
      entryName: entry.name,
      decompressedBytesRead: bytes.length,
    );
  }
  return null;
}

bool _fileHasZipHeader(File file) {
  final handle = file.openSync();
  try {
    final header = handle.readSync(2);
    return header.length >= 2 && header[0] == 0x50 && header[1] == 0x4B;
  } finally {
    handle.closeSync();
  }
}

bool _hasZipHeader(List<int> bytes) =>
    bytes.length >= 2 && bytes[0] == 0x50 && bytes[1] == 0x4B;

bool _isAppleHealthExportXml(String name) {
  final normalized = name.replaceAll('\\', '/');
  final base = normalized.substring(normalized.lastIndexOf('/') + 1).toLowerCase();
  return base == 'export.xml';
}

bool _isAppleWorkoutRouteFile(String name) {
  final path = normalizedAppleWorkoutRoutePath(name);
  return path.startsWith('workout-routes/') && path.endsWith('.gpx');
}
