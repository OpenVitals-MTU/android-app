/// The Apple Health → import-record converter, ported from the Kotlin
/// `AppleHealthImportConverter.kt` and its per-type conversion modules.
///
/// The per-type conversions live in `part` files (mirroring the Kotlin
/// `*Conversions.kt` extension-function split) so they can share the converter's
/// private diagnostic/stats state within one Dart library.
library;

import 'dart:math' as math;

import '../../../core/geo/geo_distance.dart';
import 'apple_health_import_category_mappings.dart';
import 'apple_health_import_conversion_support.dart';
import 'apple_health_import_models.dart';
import '../../../domain/model/apple_health_import_records.dart';
import 'apple_health_import_types.dart';
import 'apple_health_import_unit_converters.dart';

part 'apple_health_import_single_record_conversions.dart';
part 'apple_health_import_record_conversions.dart';
part 'apple_health_import_sleep_conversions.dart';
part 'apple_health_import_nutrition_conversions.dart';
part 'apple_health_import_blood_pressure_conversions.dart';
part 'apple_health_import_workout_conversions.dart';

const int _diagnosticLimitUnbounded = 1 << 31;

class AppleHealthConversionResult {
  const AppleHealthConversionResult({
    required this.converted,
    required this.diagnostics,
    required this.typeStats,
  });

  final List<ConvertedAppleRecord> converted;
  final List<AppleHealthImportDiagnostic> diagnostics;
  final Map<String, MutableAppleImportTypeStats> typeStats;
}

class AppleHealthImportConverter {
  AppleHealthImportConverter({
    required this.mindfulnessAvailable,
    this.diagnosticLimit = _diagnosticLimitUnbounded,
    this.reportUnavailableWorkoutRoutes = false,
  });

  final bool mindfulnessAvailable;
  final int diagnosticLimit;

  /// Emit a `workout_route_unavailable` diagnostic per workout whose route GPX
  /// never made it out of the archive. Only meaningful when the workouts
  /// category is selected — otherwise the routes were deliberately never parsed
  /// (Kotlin 1.9.0 `a852d4e`) and every routed workout would look "unavailable".
  final bool reportUnavailableWorkoutRoutes;

  final List<AppleHealthImportDiagnostic> _diagnostics = [];
  final Map<String, AppleHealthImportDiagnosticSummary> _diagnosticSummaries = {};
  final Map<String, MutableAppleImportTypeStats> typeStats = {};

  int unsupportedCount = 0;
  int skippedCount = 0;
  int invalidCount = 0;

  final Set<String> consumedRecordFingerprints = {};

  /// Workout-overlap candidates gathered incrementally during a streaming import
  /// (Kotlin `workoutOverlapCandidates`), so [convertWorkouts] can protect a
  /// selected workout from additive samples that overlap it even though those
  /// samples stream past before the workout is converted. Bounded by
  /// [maxWorkoutOverlapCandidates]; the one-shot [convert] path uses its own local
  /// list instead.
  final List<AppleWorkoutOverlapCandidate> _workoutOverlapCandidates = [];
  bool _workoutOverlapCandidatesLimitReached = false;

  AppleHealthConversionResult convert(AppleParsedExport export) {
    export.parsedTypeCounts.forEach((type, count) {
      _stats(type).parsed += count;
    });

    final workoutOverlaps = toBoundedWorkoutOverlapCandidates(export.records);
    final overlapLimitReached = workoutOverlaps.limitReached;

    final converted = <ConvertedAppleRecord>[];
    converted.addAll(convertBloodPressureCorrelations(export.correlations));
    converted.addAll(convertStandaloneBloodPressure(export.records));
    converted.addAll(convertSleep(export.records));
    converted.addAll(convertNutrition(export.records));
    converted.addAll(
      convertWorkouts(
        export.workouts,
        workoutOverlaps.candidates,
        overlapLimitReached,
      ),
    );
    convertAdditiveOverlapSensitiveRecords(export.records, converted.add);
    for (final record in export.records) {
      if (consumedRecordFingerprints.contains(record.sourceFingerprint)) {
        continue;
      }
      final result = convertSingleRecord(record);
      if (result != null) converted.add(result);
    }
    for (final correlation in export.correlations) {
      if (correlation.type == appleBloodPressureCorrelation) continue;
      unsupported(
        correlation.type,
        'Correlation type has no direct Health Connect import mapping.',
        correlation.timeRangeOrNull()?.toString(),
      );
    }
    if (export.parsedActivitySummaries > 0) {
      unsupported(
        'ActivitySummary',
        'Apple activity rings and stand hours have no direct writable Health '
            'Connect record.',
        null,
      );
    }

    return AppleHealthConversionResult(
      converted: converted,
      diagnostics: List.of(_diagnostics),
      typeStats: typeStats,
    );
  }

  void markParsed(String appleType) => _stats(appleType).parsed += 1;

  void markConverted(String appleType) => _stats(appleType).converted += 1;

  /// A compatible record the importer skipped *before* materializing it, because
  /// its category was not selected (Kotlin `markCompatibleNotSelected`).
  ///
  /// It never reaches the converter, so its per-type row would otherwise show
  /// `converted=0, notSelected=0` and the totals would silently shrink. Booking
  /// both here keeps every report row and every count identical to the
  /// materialize-everything path.
  void markCompatibleNotSelected(String appleType) {
    final stats = _stats(appleType);
    stats.converted += 1;
    stats.notSelected += 1;
  }

  // ── Streaming import surface (Kotlin's per-record consumer path) ────────────
  //
  // These let the import service convert the export as it is parsed, instead of
  // handing `convert()` a fully materialized [AppleParsedExport]. Records that
  // need cross-record context (grouping, overlap dedup) are buffered by the
  // caller and handed back through [convertBufferedGroups]; everything else
  // converts one at a time via [convertStreamingRecord].

  /// Records whose conversion depends on grouping with their neighbours (blood
  /// pressure pairing, sleep-session synthesis, nutrition aggregation) and so must
  /// be buffered until a flush (Kotlin `shouldBufferRecord`). Dietary water is a
  /// plain single-record conversion and is deliberately excluded.
  bool shouldBufferRecord(AppleRecord record) =>
      record.type == appleBloodPressureSystolic ||
      record.type == appleBloodPressureDiastolic ||
      record.type == appleSleepAnalysis ||
      (appleNutritionTypes.contains(record.type) &&
          record.type != appleDietaryWater);

  /// Additive samples (steps / distance / active energy) that take part in the
  /// cross-source overlap dedup, which needs the whole set sorted (Kotlin
  /// `shouldBufferForOverlapDedup`).
  bool shouldBufferForOverlapDedup(AppleRecord record) =>
      appleAdditiveOverlapSensitiveTypes.contains(record.type);

  /// Remembers a distance/active-energy sample as a workout-overlap candidate
  /// (Kotlin `noteWorkoutOverlap`), bounded by [maxWorkoutOverlapCandidates].
  void noteWorkoutOverlap(AppleRecord record) {
    final candidate = record.toWorkoutOverlapCandidate();
    if (candidate == null) return;
    if (_workoutOverlapCandidates.length < maxWorkoutOverlapCandidates) {
      _workoutOverlapCandidates.add(candidate);
    } else {
      _workoutOverlapCandidatesLimitReached = true;
    }
  }

  /// Converts a single non-buffered record immediately (Kotlin
  /// `convertStreamingRecord`).
  ConvertedAppleRecord? convertStreamingRecord(AppleRecord record) =>
      convertSingleRecord(record);

  /// Emit-based buffered-group conversion (Kotlin `convertBufferedGroups`).
  /// Converted records are handed to [emit] as they are produced rather than
  /// gathered into one list, so the caller can stream them into its batch writer;
  /// for step/energy-heavy exports the additive group alone can span hundreds of
  /// thousands of records.
  void convertBufferedGroups({
    required List<AppleRecord> records,
    required List<AppleWorkout> workouts,
    required List<AppleCorrelation> correlations,
    required int parsedActivitySummaries,
    required void Function(ConvertedAppleRecord) emit,
  }) {
    convertBloodPressureCorrelations(correlations).forEach(emit);
    convertStandaloneBloodPressure(records).forEach(emit);
    convertSleep(records, trackConsumedRecords: false).forEach(emit);
    convertNutrition(records, trackConsumedRecords: false).forEach(emit);
    convertWorkouts(
      workouts,
      _workoutOverlapCandidates,
      _workoutOverlapCandidatesLimitReached,
    ).forEach(emit);
    convertAdditiveOverlapSensitiveRecords(records, emit);
    for (final correlation in correlations) {
      if (correlation.type == appleBloodPressureCorrelation) continue;
      unsupported(
        correlation.type,
        'Correlation type has no direct Health Connect import mapping.',
        correlation.timeRangeOrNull()?.toString(),
      );
    }
    if (parsedActivitySummaries > 0) {
      unsupported(
        'ActivitySummary',
        'Apple activity rings and stand hours have no direct writable Health '
            'Connect record.',
        null,
      );
    }
  }

  List<AppleHealthImportDiagnostic> diagnosticsSnapshot() =>
      List.of(_diagnostics);

  List<AppleHealthImportDiagnosticSummary> diagnosticSummariesSnapshot() =>
      _diagnosticSummaries.values.toList();

  MutableAppleImportTypeStats _stats(String appleType) =>
      typeStats.putIfAbsent(appleType, MutableAppleImportTypeStats.new);

  // ── Diagnostic sinks (all return null so callers can `return invalid(...)`) ──

  Null invalidRecord(AppleRecord record, String detail) => invalid(
        record.type,
        detail,
        record.timeRangeOrNull()?.toString(),
        record.unit,
        record.valueForReport,
      );

  Null invalid(
    String appleType,
    String detail,
    String? timeRange, [
    String? unit,
    String? value,
  ]) {
    _addDiagnostic(AppleHealthImportDiagnostic(
      appleType: appleType,
      targetType: null,
      reasonCode: 'invalid',
      timeRange: timeRange,
      unit: unit,
      value: value,
      detail: detail,
    ));
    _stats(appleType).failed += 1;
    invalidCount += 1;
    return null;
  }

  Null unsupportedNull(AppleRecord record, String detail) => unsupported(
        record.type,
        detail,
        record.timeRangeOrNull()?.toString(),
        record.unit,
        record.valueForReport,
      );

  Null unsupported(
    String appleType,
    String detail,
    String? timeRange, [
    String? unit,
    String? value,
  ]) {
    _addDiagnostic(AppleHealthImportDiagnostic(
      appleType: appleType,
      targetType: null,
      reasonCode: 'unsupported',
      timeRange: timeRange,
      unit: unit,
      value: value,
      detail: detail,
    ));
    _stats(appleType).unsupported += 1;
    unsupportedCount += 1;
    return null;
  }

  Null skippedNull(AppleRecord record, String reasonCode, String detail) {
    skipped(
      record.type,
      reasonCode,
      detail,
      record.timeRangeOrNull()?.toString(),
      record.unit,
      record.valueForReport,
    );
    return null;
  }

  void skipped(
    String appleType,
    String reasonCode,
    String detail,
    String? timeRange, [
    String? unit,
    String? value,
  ]) {
    _addDiagnostic(AppleHealthImportDiagnostic(
      appleType: appleType,
      targetType: null,
      reasonCode: reasonCode,
      timeRange: timeRange,
      unit: unit,
      value: value,
      detail: detail,
    ));
    _stats(appleType).skipped += 1;
    skippedCount += 1;
  }

  void _addDiagnostic(AppleHealthImportDiagnostic diagnostic) {
    addToDiagnosticSummaries(_diagnosticSummaries, diagnostic);
    if (_diagnostics.length < diagnosticLimit) {
      _diagnostics.add(diagnostic);
    }
  }

  /// The additive-overlap dedup pass (steps / distance / active energy), which
  /// drops lower-priority samples already covered by another source. Records
  /// without a usable start date convert directly.
  void convertAdditiveOverlapSensitiveRecords(
    List<AppleRecord> records,
    void Function(ConvertedAppleRecord) emit,
  ) {
    final additiveRecords = records
        .where((it) => appleAdditiveOverlapSensitiveTypes.contains(it.type))
        .toList();
    if (additiveRecords.isEmpty) return;

    final candidates = <AppleAdditiveOverlapCandidate>[];
    for (final record in additiveRecords) {
      final candidate = record.toAdditiveOverlapCandidate();
      if (candidate == null) {
        consumedRecordFingerprints.add(record.sourceFingerprint);
        final result = convertSingleRecord(record);
        if (result != null) emit(result);
      } else {
        candidates.add(candidate);
      }
    }
    // Stable sort: Kotlin uses `sortWith(compareBy … thenBy …)` (stable), so a
    // tie on all four keys keeps the parse-order-first candidate.
    stableSort(candidates, (a, b) {
      var cmp = a.record.type.compareTo(b.record.type);
      if (cmp != 0) return cmp;
      cmp = a.sourcePriority.compareTo(b.sourcePriority);
      if (cmp != 0) return cmp;
      cmp = a.start.compareTo(b.start);
      if (cmp != 0) return cmp;
      return a.end.compareTo(b.end);
    });
    final accepted = AppleAdditiveOverlapIndex();
    for (final candidate in candidates) {
      consumedRecordFingerprints.add(candidate.record.sourceFingerprint);
      if (accepted.isMostlyCovered(candidate)) {
        skippedNull(
          candidate.record,
          'overlap_cross_source',
          'Skipped because another source already contributed an overlapping '
              'additive sample.',
        );
        continue;
      }
      accepted.add(candidate);
      final result = convertSingleRecord(candidate.record);
      if (result != null) emit(result);
    }
  }
}

/// The `Metadata.clientRecordId` for a single-record conversion: prefixed by the
/// target record type and derived from the record's stable fingerprint.
String _metadataClientRecordId(AppleRecord record, String targetType) =>
    buildStableClientRecordId(targetType, record.sourceFingerprint);

String _substringAfterLast(String value, String delimiter) {
  final index = value.lastIndexOf(delimiter);
  return index < 0 ? value : value.substring(index + delimiter.length);
}
