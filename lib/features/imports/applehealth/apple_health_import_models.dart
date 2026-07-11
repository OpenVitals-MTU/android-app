/// Parsed-export and reporting models for the Apple Health importer, ported from
/// the Kotlin `AppleHealthImportModels.kt` (plus the progress/percent logic).
library;

import 'dart:math' as math;

import 'apple_health_import_records.dart';

/// An instant plus its wall-clock offset, mirroring the Kotlin `AppleDateTime`.
class AppleDateTime {
  const AppleDateTime(this.instant, this.offset);

  /// UTC instant.
  final DateTime instant;

  /// Offset from UTC (`java.time.ZoneOffset`), or `null` if unknown.
  final Duration? offset;

  AppleDateTime copyWith({DateTime? instant, Duration? offset}) =>
      AppleDateTime(instant ?? this.instant, offset ?? this.offset);

  @override
  String toString() => instant.toIso8601String();
}

/// A parsed `<Record>` element.
class AppleRecord {
  const AppleRecord({
    required this.type,
    this.sourceName,
    this.sourceVersion,
    this.device,
    this.unit,
    this.creationDate,
    this.startDate,
    this.endDate,
    this.rawValue,
    this.numericValue,
    this.metadata = const {},
    this.correlationType,
  });

  final String type;
  final String? sourceName;
  final String? sourceVersion;
  final String? device;
  final String? unit;
  final AppleDateTime? creationDate;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
  final String? rawValue;
  final double? numericValue;
  final Map<String, String> metadata;
  final String? correlationType;

  String? get valueForReport {
    final raw = rawValue;
    if (raw == null) return null;
    return raw.length <= 80 ? raw : raw.substring(0, 80);
  }

  AppleRecord copyWith({String? correlationType}) => AppleRecord(
        type: type,
        sourceName: sourceName,
        sourceVersion: sourceVersion,
        device: device,
        unit: unit,
        creationDate: creationDate,
        startDate: startDate,
        endDate: endDate,
        rawValue: rawValue,
        numericValue: numericValue,
        metadata: metadata,
        correlationType: correlationType ?? this.correlationType,
      );
}

class AppleWorkoutEvent {
  const AppleWorkoutEvent({
    this.type,
    this.date,
    this.duration,
    this.durationUnit,
  });

  final String? type;
  final AppleDateTime? date;
  final double? duration;
  final String? durationUnit;
}

class AppleWorkoutRoutePoint {
  const AppleWorkoutRoutePoint({
    required this.latitude,
    required this.longitude,
    this.altitudeMeters,
    this.horizontalAccuracyMeters,
    this.verticalAccuracyMeters,
  });

  final double latitude;
  final double longitude;
  final double? altitudeMeters;
  final double? horizontalAccuracyMeters;
  final double? verticalAccuracyMeters;
}

class AppleWorkoutRouteFile {
  const AppleWorkoutRouteFile({required this.path, required this.points});

  final String path;
  final List<AppleWorkoutRoutePoint> points;
}

/// A `workout-routes/*.gpx` entry that could not be read because the ZIP ended
/// part-way through it, *after* `export.xml` had already been read intact
/// (Kotlin `AppleWorkoutRouteArchiveFailure`). The health records still import;
/// this route and every later ZIP entry are dropped.
class AppleWorkoutRouteArchiveFailure {
  const AppleWorkoutRouteArchiveFailure({
    required this.entryName,
    this.decompressedBytesRead,
  });

  final String entryName;
  final int? decompressedBytesRead;

  String get detail {
    final buffer = StringBuffer('The ZIP ended unexpectedly while reading ')
      ..write(entryName);
    if (decompressedBytesRead != null) {
      buffer.write(' after $decompressedBytesRead decompressed byte(s)');
    }
    buffer.write(
      '. Health records were imported from the intact export.xml, but this '
      'route and any remaining ZIP entries were unavailable.',
    );
    return buffer.toString();
  }
}

class AppleWorkout {
  const AppleWorkout({
    required this.workoutActivityType,
    this.sourceName,
    this.sourceVersion,
    this.device,
    this.creationDate,
    this.startDate,
    this.endDate,
    this.duration,
    this.durationUnit,
    this.totalDistance,
    this.totalDistanceUnit,
    this.totalEnergyBurned,
    this.totalEnergyBurnedUnit,
    this.metadata = const {},
    this.events = const [],
    this.routes = const [],
    this.routeReferencePaths = const [],
  });

  final String workoutActivityType;
  final String? sourceName;
  final String? sourceVersion;
  final String? device;
  final AppleDateTime? creationDate;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
  final double? duration;
  final String? durationUnit;
  final double? totalDistance;
  final String? totalDistanceUnit;
  final double? totalEnergyBurned;
  final String? totalEnergyBurnedUnit;
  final Map<String, String> metadata;
  final List<AppleWorkoutEvent> events;
  final List<AppleWorkoutRouteFile> routes;

  /// Every `<FileReference path=…>` this workout declared, normalized. Kotlin
  /// 1.9.0 carries the *paths* (not just a count) so an unreadable route can be
  /// named in the report for manual recovery.
  final List<String> routeReferencePaths;

  int get routeReferences => routeReferencePaths.length;

  /// Routes the workout declared but whose GPX never made it out of the ZIP.
  List<String> get unavailableRoutePaths {
    final available = routes.map((route) => route.path).toSet();
    return routeReferencePaths
        .where((path) => !available.contains(path))
        .toList();
  }
}

class AppleCorrelation {
  const AppleCorrelation({
    required this.type,
    this.sourceName,
    this.sourceVersion,
    this.device,
    this.creationDate,
    this.startDate,
    this.endDate,
    this.metadata = const {},
    this.records = const [],
  });

  final String type;
  final String? sourceName;
  final String? sourceVersion;
  final String? device;
  final AppleDateTime? creationDate;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
  final Map<String, String> metadata;
  final List<AppleRecord> records;
}

/// The result of a full export parse.
class AppleParsedExport {
  const AppleParsedExport({
    required this.records,
    required this.workouts,
    required this.correlations,
    required this.parsedRecords,
    required this.parsedWorkouts,
    required this.parsedCorrelations,
    required this.parsedActivitySummaries,
    required this.parsedTypeCounts,
    this.sanitizedControlChars = 0,
    this.sanitizedAmpersands = 0,
    this.workoutRouteArchiveFailure,
  });

  final List<AppleRecord> records;
  final List<AppleWorkout> workouts;
  final List<AppleCorrelation> correlations;
  final int parsedRecords;
  final int parsedWorkouts;
  final int parsedCorrelations;
  final int parsedActivitySummaries;
  final Map<String, int> parsedTypeCounts;

  /// Raw control characters removed because XML 1.0 forbids them as text.
  final int sanitizedControlChars;

  /// Bare `&` characters auto-escaped to `&amp;`.
  final int sanitizedAmpersands;

  /// Set when a truncated workout-route entry was ignored *after* export.xml had
  /// already been read intact.
  final AppleWorkoutRouteArchiveFailure? workoutRouteArchiveFailure;

  int get parsedElements =>
      parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries;

  AppleParsedExport copyWithRouteArchiveFailure(
    AppleWorkoutRouteArchiveFailure? failure,
  ) =>
      AppleParsedExport(
        records: records,
        workouts: workouts,
        correlations: correlations,
        parsedRecords: parsedRecords,
        parsedWorkouts: parsedWorkouts,
        parsedCorrelations: parsedCorrelations,
        parsedActivitySummaries: parsedActivitySummaries,
        parsedTypeCounts: parsedTypeCounts,
        sanitizedControlChars: sanitizedControlChars,
        sanitizedAmpersands: sanitizedAmpersands,
        workoutRouteArchiveFailure: failure ?? workoutRouteArchiveFailure,
      );
}

class AppleImportTimeRange {
  const AppleImportTimeRange(this.start, this.end);

  final DateTime start;
  final DateTime end;

  @override
  String toString() =>
      start == end ? start.toIso8601String() : '${start.toIso8601String()}..${end.toIso8601String()}';
}

/// One converted record ready for import (Kotlin `ConvertedAppleRecord`).
class ConvertedAppleRecord {
  const ConvertedAppleRecord({
    required this.appleType,
    required this.targetType,
    required this.fingerprint,
    required this.record,
    required this.sourceTimeRange,
    this.unit,
    this.value,
  });

  final String appleType;
  final String targetType;

  /// Deterministic import fingerprint used for in-file dedup fallback.
  final String fingerprint;
  final ImportRecord record;
  final AppleImportTimeRange sourceTimeRange;
  final String? unit;
  final String? value;

  /// The record class name used to group duplicate-check queries.
  String get recordType => targetType;

  /// The record's `clientRecordId`, or `null` when blank.
  String? get clientRecordId =>
      record.clientRecordId.isNotEmpty ? record.clientRecordId : null;
}

enum AppleHealthImportPhase {
  queued,
  parsing,
  converting,
  checkingDuplicates,
  writing,
  finishing,
  buildingReport,
  complete,
}

const int _selectedRecordsPercentCeiling = 88;

class AppleHealthImportProgress {
  const AppleHealthImportProgress({
    this.phase = AppleHealthImportPhase.queued,
    this.parsedRecords = 0,
    this.parsedWorkouts = 0,
    this.parsedCorrelations = 0,
    this.parsedActivitySummaries = 0,
    this.convertedRecords = 0,
    this.importedRecords = 0,
    this.duplicateSkippedRecords = 0,
    this.notSelectedRecords = 0,
    this.unsupportedElements = 0,
    this.skippedRecords = 0,
    this.failedRecords = 0,
    this.expectedSelectedRecords = 0,
    this.expectedParsedElements = 0,
  });

  final AppleHealthImportPhase phase;
  final int parsedRecords;
  final int parsedWorkouts;
  final int parsedCorrelations;
  final int parsedActivitySummaries;
  final int convertedRecords;
  final int importedRecords;
  final int duplicateSkippedRecords;
  final int notSelectedRecords;
  final int unsupportedElements;
  final int skippedRecords;
  final int failedRecords;
  final int expectedSelectedRecords;

  /// The element total measured by the analysis pass (Kotlin
  /// `expectedParsedElements`): the denominator of the *scan* percent, which is
  /// the only one that advances while the importer streams past long stretches
  /// of unselected records.
  final int expectedParsedElements;

  AppleHealthImportProgress copyWith({
    AppleHealthImportPhase? phase,
    int? parsedRecords,
    int? convertedRecords,
    int? notSelectedRecords,
    int? expectedSelectedRecords,
    int? expectedParsedElements,
  }) =>
      AppleHealthImportProgress(
        phase: phase ?? this.phase,
        parsedRecords: parsedRecords ?? this.parsedRecords,
        parsedWorkouts: parsedWorkouts,
        parsedCorrelations: parsedCorrelations,
        parsedActivitySummaries: parsedActivitySummaries,
        convertedRecords: convertedRecords ?? this.convertedRecords,
        importedRecords: importedRecords,
        duplicateSkippedRecords: duplicateSkippedRecords,
        notSelectedRecords: notSelectedRecords ?? this.notSelectedRecords,
        unsupportedElements: unsupportedElements,
        skippedRecords: skippedRecords,
        failedRecords: failedRecords,
        expectedSelectedRecords:
            expectedSelectedRecords ?? this.expectedSelectedRecords,
        expectedParsedElements:
            expectedParsedElements ?? this.expectedParsedElements,
      );

  int get parsedElements =>
      parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries;

  int get selectedPreparedRecords {
    final v = convertedRecords - notSelectedRecords;
    return v < 0 ? 0 : v;
  }

  /// The determinate percent, or `null` while no denominator is known.
  ///
  /// The scan percent (raw elements scanned against the total measured during
  /// analysis) *wins* over the selected-record percent whenever the export's
  /// element total is known: it is the only one that keeps moving while the
  /// importer streams past a long section of unselected records, where
  /// [selectedPreparedRecords] does not change at all.
  int? get percent {
    if (phase == AppleHealthImportPhase.complete) return 100;
    final scanPercent = expectedParsedElements > 0
        ? (math.min(parsedElements, expectedParsedElements) /
                expectedParsedElements *
                _selectedRecordsPercentCeiling)
            .round()
        : null;
    final selectedPercent = expectedSelectedRecords > 0
        ? (math.min(selectedPreparedRecords, expectedSelectedRecords) /
                expectedSelectedRecords *
                _selectedRecordsPercentCeiling)
            .round()
        : null;
    final workPercent = scanPercent ?? selectedPercent;
    if (workPercent == null) return null;
    final selectedProgressComplete = expectedSelectedRecords > 0 &&
        selectedPreparedRecords >= expectedSelectedRecords;
    final scanProgressComplete = expectedParsedElements > 0 &&
        parsedElements >= expectedParsedElements;
    final workComplete = scanProgressComplete || selectedProgressComplete;
    final phaseFloor = switch (phase) {
      AppleHealthImportPhase.queued ||
      AppleHealthImportPhase.parsing ||
      AppleHealthImportPhase.converting =>
        0,
      AppleHealthImportPhase.checkingDuplicates => workComplete ? 88 : 0,
      AppleHealthImportPhase.writing => workComplete ? 92 : 0,
      AppleHealthImportPhase.finishing => 95,
      AppleHealthImportPhase.buildingReport => 98,
      AppleHealthImportPhase.complete => 100,
    };
    return math.max(workPercent, phaseFloor).clamp(0, 99);
  }
}

class AppleHealthImportTypeSummary {
  const AppleHealthImportTypeSummary({
    required this.appleType,
    required this.parsed,
    required this.converted,
    required this.imported,
    required this.duplicateSkipped,
    required this.notSelected,
    required this.unsupported,
    required this.skipped,
    required this.failed,
  });

  final String appleType;
  final int parsed;
  final int converted;
  final int imported;
  final int duplicateSkipped;
  final int notSelected;
  final int unsupported;
  final int skipped;
  final int failed;
}

enum AppleHealthImportCategory {
  workouts,
  activity,
  heart,
  sleep,
  body,
  vitals,
  nutrition,
  hydration,
  mindfulness,
  cycle,
}

extension AppleHealthImportCategoryReport on AppleHealthImportCategory {
  String get reportName => switch (this) {
        AppleHealthImportCategory.workouts => 'Workouts and routes',
        AppleHealthImportCategory.activity => 'Activity metrics',
        AppleHealthImportCategory.heart => 'Heart',
        AppleHealthImportCategory.sleep => 'Sleep',
        AppleHealthImportCategory.body => 'Body measurements',
        AppleHealthImportCategory.vitals => 'Vitals',
        AppleHealthImportCategory.nutrition => 'Nutrition',
        AppleHealthImportCategory.hydration => 'Hydration',
        AppleHealthImportCategory.mindfulness => 'Mindfulness',
        AppleHealthImportCategory.cycle => 'Cycle tracking',
      };
}

const Set<AppleHealthImportCategory> allAppleHealthImportCategories = {
  AppleHealthImportCategory.workouts,
  AppleHealthImportCategory.activity,
  AppleHealthImportCategory.heart,
  AppleHealthImportCategory.sleep,
  AppleHealthImportCategory.body,
  AppleHealthImportCategory.vitals,
  AppleHealthImportCategory.nutrition,
  AppleHealthImportCategory.hydration,
  AppleHealthImportCategory.mindfulness,
  AppleHealthImportCategory.cycle,
};

class AppleHealthImportCategorySummary {
  const AppleHealthImportCategorySummary({
    required this.category,
    required this.convertedRecords,
    this.routeSessions = 0,
  });

  final AppleHealthImportCategory category;
  final int convertedRecords;
  final int routeSessions;
}

class AppleHealthImportDiagnostic {
  const AppleHealthImportDiagnostic({
    required this.appleType,
    required this.targetType,
    required this.reasonCode,
    required this.timeRange,
    required this.unit,
    required this.value,
    required this.detail,
  });

  final String appleType;
  final String? targetType;
  final String reasonCode;
  final String? timeRange;
  final String? unit;
  final String? value;
  final String detail;
}

class AppleHealthImportDiagnosticSummary {
  AppleHealthImportDiagnosticSummary({
    required this.appleType,
    required this.targetType,
    required this.reasonCode,
    required this.detail,
    required this.count,
    required this.exampleTimeRange,
    required this.exampleUnit,
    required this.exampleValue,
  });

  final String appleType;
  final String? targetType;
  final String reasonCode;
  final String detail;
  int count;
  final String? exampleTimeRange;
  final String? exampleUnit;
  final String? exampleValue;
}

class AppleHealthImportResult {
  const AppleHealthImportResult({
    required this.parsedRecords,
    required this.parsedWorkouts,
    required this.parsedCorrelations,
    required this.parsedActivitySummaries,
    required this.convertedRecords,
    required this.importedRecords,
    required this.duplicateSkippedRecords,
    required this.notSelectedRecords,
    required this.unsupportedElements,
    required this.skippedRecords,
    required this.failedRecords,
    required this.typeSummaries,
    required this.diagnostics,
    required this.shareableReportText,
    this.workoutRoutesIncomplete = false,
  });

  final int parsedRecords;
  final int parsedWorkouts;
  final int parsedCorrelations;
  final int parsedActivitySummaries;
  final int convertedRecords;
  final int importedRecords;
  final int duplicateSkippedRecords;
  final int notSelectedRecords;
  final int unsupportedElements;
  final int skippedRecords;
  final int failedRecords;

  /// A workout-route entry was unreadable because the ZIP ended unexpectedly;
  /// health records still imported (Kotlin `workoutRoutesIncomplete`).
  final bool workoutRoutesIncomplete;
  final List<AppleHealthImportTypeSummary> typeSummaries;
  final List<AppleHealthImportDiagnostic> diagnostics;
  final String shareableReportText;

  int get unsupportedRecords => unsupportedElements;
}

class AppleHealthImportAnalysisResult {
  const AppleHealthImportAnalysisResult({
    required this.parsedRecords,
    required this.parsedWorkouts,
    required this.parsedCorrelations,
    required this.parsedActivitySummaries,
    required this.convertedRecords,
    required this.unsupportedElements,
    required this.skippedRecords,
    required this.failedRecords,
    required this.categorySummaries,
    required this.typeSummaries,
    required this.diagnostics,
    required this.shareableReportText,
  });

  final int parsedRecords;
  final int parsedWorkouts;
  final int parsedCorrelations;
  final int parsedActivitySummaries;
  final int convertedRecords;
  final int unsupportedElements;
  final int skippedRecords;
  final int failedRecords;
  final List<AppleHealthImportCategorySummary> categorySummaries;
  final List<AppleHealthImportTypeSummary> typeSummaries;
  final List<AppleHealthImportDiagnostic> diagnostics;
  final String shareableReportText;

  int get parsedElements =>
      parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries;
}

/// Mutable per-Apple-type accounting used during conversion/import (Kotlin
/// `MutableAppleImportTypeStats`).
class MutableAppleImportTypeStats {
  int parsed = 0;
  int converted = 0;
  int imported = 0;
  int duplicateSkipped = 0;
  int notSelected = 0;
  int unsupported = 0;
  int skipped = 0;
  int failed = 0;
}
