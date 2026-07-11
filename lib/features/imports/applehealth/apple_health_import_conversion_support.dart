/// Shared conversion helpers: deterministic fingerprints, interval clamping,
/// sleep-session splitting, and the cross-source additive-overlap dedup index.
/// Ported from the Kotlin `AppleHealthImportConversionSupport.kt`.
library;

import 'dart:convert';

import 'package:crypto/crypto.dart';

import 'apple_health_import_models.dart';
import 'apple_health_import_records.dart';
import 'apple_health_import_types.dart';

class AppleInterval {
  const AppleInterval(this.start, this.end);

  final AppleDateTime start;
  final AppleDateTime end;
}

AppleInterval interval(AppleDateTime start, AppleDateTime end) {
  final adjustedEnd = end.instant.isAfter(start.instant)
      ? end
      : AppleDateTime(
          start.instant.add(const Duration(seconds: 1)),
          end.offset ?? start.offset,
        );
  return AppleInterval(start, adjustedEnd);
}

class SleepStageCandidate {
  const SleepStageCandidate({
    required this.record,
    required this.start,
    required this.end,
    required this.stage,
    required this.inBedOnly,
  });

  final AppleRecord record;
  final AppleDateTime start;
  final AppleDateTime end;
  final SleepStageType stage;
  final bool inBedOnly;
}

class AppleWorkoutOverlapCandidate {
  const AppleWorkoutOverlapCandidate({
    required this.type,
    required this.sourceName,
    required this.startDate,
    required this.endDate,
  });

  final String type;
  final String? sourceName;
  final AppleDateTime? startDate;
  final AppleDateTime? endDate;
}

class AppleAdditiveOverlapCandidate {
  const AppleAdditiveOverlapCandidate({
    required this.record,
    required this.start,
    required this.end,
    required this.sourceName,
    required this.sourcePriority,
  });

  final AppleRecord record;
  final DateTime start;
  final DateTime end;
  final String? sourceName;
  final int sourcePriority;
}

class BoundedWorkoutOverlapCandidates {
  const BoundedWorkoutOverlapCandidates(this.candidates, this.limitReached);

  final List<AppleWorkoutOverlapCandidate> candidates;
  final bool limitReached;
}

const Duration _sleepSessionGap = Duration(hours: 2);
const double _additiveOverlapCoverageThreshold = 0.8;
const Set<String> _workoutAppSourceHints = {
  'strava',
  'garmin',
  'polar',
  'fitbit',
  'wahoo',
  'zwift',
  'runkeeper',
  'komoot',
  'trainingpeaks',
};

DateTime _maxInstant(DateTime a, DateTime b) => a.isAfter(b) ? a : b;
DateTime _minInstant(DateTime a, DateTime b) => a.isBefore(b) ? a : b;

List<List<SleepStageCandidate>> splitSleepSessions(
  List<SleepStageCandidate> candidates,
) {
  final sessions = <List<SleepStageCandidate>>[];
  for (final candidate in candidates) {
    final current = sessions.isEmpty ? null : sessions.last;
    if (current == null) {
      sessions.add([candidate]);
      continue;
    }
    var maxEnd = current.first.end.instant;
    for (final c in current) {
      if (c.end.instant.isAfter(maxEnd)) maxEnd = c.end.instant;
    }
    if (candidate.start.instant.difference(maxEnd) > _sleepSessionGap) {
      sessions.add([candidate]);
    } else {
      current.add(candidate);
    }
  }
  return sessions;
}

const String _hexDigits = '0123456789abcdef';

String buildStableClientRecordId(String prefix, Object parts) {
  final bytes = sha256.convert(utf8.encode(parts.toString())).bytes;
  final buffer = StringBuffer();
  for (var index = 0; index < 16; index++) {
    final byte = bytes[index] & 0xFF;
    buffer.write(_hexDigits[byte >> 4]);
    buffer.write(_hexDigits[byte & 0x0F]);
  }
  return 'apple_health_${toStableIdSegment(prefix)}_$buffer';
}

extension AppleRecordFingerprints on AppleRecord {
  String stableClientRecordId(String prefix, [Object? extra]) =>
      buildStableClientRecordId(prefix, extra ?? stableParts());

  String get sourceFingerprint => stableParts();

  String stableParts() => [
        type,
        sourceName ?? '',
        sourceVersion ?? '',
        device ?? '',
        creationDate?.instant.toIso8601String() ?? '',
        startDate?.instant.toIso8601String() ?? '',
        endDate?.instant.toIso8601String() ?? '',
        unit ?? '',
        rawValue ?? '',
        correlationType ?? '',
        _sortedMetadata(metadata),
      ].join('|');

  AppleImportTimeRange? timeRangeOrNull() {
    final start = startDate?.instant;
    if (start == null) return null;
    final end = endDate?.instant ?? start;
    return AppleImportTimeRange(start, end);
  }

  AppleWorkoutOverlapCandidate? toWorkoutOverlapCandidate() {
    if (appleDistanceTypes.contains(type) || type == appleActiveEnergyBurned) {
      return AppleWorkoutOverlapCandidate(
        type: type,
        sourceName: sourceName,
        startDate: startDate,
        endDate: endDate,
      );
    }
    return null;
  }

  AppleAdditiveOverlapCandidate? toAdditiveOverlapCandidate() {
    if (!appleAdditiveOverlapSensitiveTypes.contains(type)) return null;
    final start = startDate?.instant;
    if (start == null) return null;
    final rawEnd = endDate?.instant ?? start;
    final end = rawEnd.isAfter(start)
        ? rawEnd
        : start.add(const Duration(seconds: 1));
    return AppleAdditiveOverlapCandidate(
      record: this,
      start: start,
      end: end,
      sourceName: sourceName,
      sourcePriority: _additiveSourcePriority(),
    );
  }

  int _additiveSourcePriority() {
    final source = (sourceName ?? '').toLowerCase();
    if (source.contains('watch')) return 0;
    if (_workoutAppSourceHints.any(source.contains)) return 1;
    if (source.contains('iphone') || source.contains('ipad')) return 2;
    if (source.contains('apple')) return 3;
    return 4;
  }
}

extension AppleWorkoutFingerprints on AppleWorkout {
  String stableParts() => [
        workoutActivityType,
        sourceName ?? '',
        sourceVersion ?? '',
        device ?? '',
        creationDate?.instant.toIso8601String() ?? '',
        startDate?.instant.toIso8601String() ?? '',
        endDate?.instant.toIso8601String() ?? '',
        duration?.toString() ?? '',
        durationUnit ?? '',
        totalDistance?.toString() ?? '',
        totalDistanceUnit ?? '',
        totalEnergyBurned?.toString() ?? '',
        totalEnergyBurnedUnit ?? '',
        _sortedMetadata(metadata),
      ].join('|');
}

extension AppleCorrelationFingerprints on AppleCorrelation {
  String stableParts() => [
        type,
        sourceName ?? '',
        sourceVersion ?? '',
        device ?? '',
        creationDate?.instant.toIso8601String() ?? '',
        startDate?.instant.toIso8601String() ?? '',
        endDate?.instant.toIso8601String() ?? '',
        records.map((it) => it.stableParts()).join(';'),
      ].join('|');

  AppleImportTimeRange? timeRangeOrNull() {
    var start = startDate?.instant;
    if (start == null) {
      final starts =
          records.map((it) => it.startDate?.instant).whereType<DateTime>();
      if (starts.isEmpty) return null;
      start = starts.reduce(_minInstant);
    }
    var end = endDate?.instant;
    if (end == null) {
      final ends = records
          .map((it) => it.endDate?.instant ?? it.startDate?.instant)
          .whereType<DateTime>();
      end = ends.isEmpty ? start : ends.reduce(_maxInstant);
    }
    return AppleImportTimeRange(start, end);
  }
}

String _sortedMetadata(Map<String, String> metadata) {
  final keys = metadata.keys.toList()..sort();
  return keys.map((key) => '$key=${metadata[key]}').join(';');
}

BoundedWorkoutOverlapCandidates toBoundedWorkoutOverlapCandidates(
  List<AppleRecord> records,
) {
  final candidates = <AppleWorkoutOverlapCandidate>[];
  var limitReached = false;
  for (final record in records) {
    final candidate = record.toWorkoutOverlapCandidate();
    if (candidate == null) continue;
    if (candidates.length < maxWorkoutOverlapCandidates) {
      candidates.add(candidate);
    } else {
      limitReached = true;
      break;
    }
  }
  return BoundedWorkoutOverlapCandidates(candidates, limitReached);
}

class _AppleInstantRange {
  _AppleInstantRange(this.start, this.end);

  final DateTime start;
  DateTime end;
}

class AppleAdditiveOverlapIndex {
  final Map<String, Map<String, List<_AppleInstantRange>>>
      _rangesByTypeAndSource = {};

  bool isMostlyCovered(AppleAdditiveOverlapCandidate candidate) {
    final source = candidate.sourceName ?? '';
    final sourceRanges = _rangesByTypeAndSource[candidate.record.type];
    if (sourceRanges == null) return false;
    final overlaps = <_AppleInstantRange>[];
    sourceRanges.forEach((acceptedSource, ranges) {
      if (acceptedSource != source) {
        _collectOverlaps(ranges, candidate.start, candidate.end, overlaps);
      }
    });
    if (overlaps.isEmpty) return false;

    overlaps.sort((a, b) => a.start.compareTo(b.start));
    var coveredSeconds = 0;
    var currentStart = overlaps.first.start;
    var currentEnd = overlaps.first.end;
    for (var index = 1; index < overlaps.length; index++) {
      final range = overlaps[index];
      if (!range.start.isAfter(currentEnd)) {
        currentEnd = _maxInstant(currentEnd, range.end);
      } else {
        coveredSeconds += currentEnd.difference(currentStart).inSeconds;
        currentStart = range.start;
        currentEnd = range.end;
      }
    }
    coveredSeconds += currentEnd.difference(currentStart).inSeconds;

    final rawDuration = candidate.end.difference(candidate.start).inSeconds;
    final durationSeconds = rawDuration < 1 ? 1 : rawDuration;
    return coveredSeconds / durationSeconds >= _additiveOverlapCoverageThreshold;
  }

  void add(AppleAdditiveOverlapCandidate candidate) {
    final ranges = _rangesByTypeAndSource
        .putIfAbsent(candidate.record.type, () => {})
        .putIfAbsent(candidate.sourceName ?? '', () => []);
    _addMerged(ranges, candidate.start, candidate.end);
  }
}

void _collectOverlaps(
  List<_AppleInstantRange> ranges,
  DateTime start,
  DateTime end,
  List<_AppleInstantRange> destination,
) {
  var index = _indexOfFirstEndingAfter(ranges, start);
  while (index < ranges.length) {
    final range = ranges[index];
    if (!range.start.isBefore(end)) break;
    final overlapStart = _maxInstant(range.start, start);
    final overlapEnd = _minInstant(range.end, end);
    if (overlapEnd.isAfter(overlapStart)) {
      destination.add(_AppleInstantRange(overlapStart, overlapEnd));
    }
    index++;
  }
}

int _indexOfFirstEndingAfter(List<_AppleInstantRange> ranges, DateTime start) {
  var low = 0;
  var high = ranges.length;
  while (low < high) {
    final mid = (low + high) >> 1;
    if (!ranges[mid].end.isAfter(start)) {
      low = mid + 1;
    } else {
      high = mid;
    }
  }
  return low;
}

void _addMerged(List<_AppleInstantRange> ranges, DateTime start, DateTime end) {
  if (ranges.isEmpty) {
    ranges.add(_AppleInstantRange(start, end));
    return;
  }

  final last = ranges.last;
  if (!start.isBefore(last.start)) {
    if (!start.isAfter(last.end)) {
      last.end = _maxInstant(last.end, end);
    } else {
      ranges.add(_AppleInstantRange(start, end));
    }
    return;
  }

  ranges.add(_AppleInstantRange(start, end));
  ranges.sort((a, b) => a.start.compareTo(b.start));
  var writeIndex = 0;
  for (var readIndex = 1; readIndex < ranges.length; readIndex++) {
    final current = ranges[readIndex];
    final merged = ranges[writeIndex];
    if (!current.start.isAfter(merged.end)) {
      merged.end = _maxInstant(merged.end, current.end);
    } else {
      writeIndex++;
      ranges[writeIndex] = current;
    }
  }
  ranges.removeRange(writeIndex + 1, ranges.length);
}

bool hasOverlapping(
  List<AppleWorkoutOverlapCandidate> candidates,
  AppleWorkout workout,
  Set<String> types,
) {
  final workoutStart = workout.startDate?.instant;
  final workoutEnd = workout.endDate?.instant;
  if (workoutStart == null || workoutEnd == null) return false;
  return candidates.any((record) {
    if (!types.contains(record.type)) return false;
    final recordStart = record.startDate?.instant;
    if (recordStart == null || !recordStart.isBefore(workoutEnd)) return false;
    final recordEnd = record.endDate?.instant ?? recordStart;
    return recordEnd.isAfter(workoutStart);
  });
}

final RegExp _stableIdSegmentRegex = RegExp('[^a-z0-9]+');

String toStableIdSegment(String value) {
  final segment = value
      .toLowerCase()
      .replaceAll(_stableIdSegmentRegex, '_')
      .replaceAll(RegExp(r'^_+|_+$'), '');
  return segment.isEmpty ? 'record' : segment;
}

/// Composite diagnostic-summary key (Kotlin `AppleHealthDiagnosticSummaryKey`).
String diagnosticSummaryKey(AppleHealthImportDiagnostic diagnostic) =>
    '${diagnostic.appleType} ${diagnostic.targetType ?? ''}'
    ' ${diagnostic.reasonCode} ${diagnostic.detail}';

/// Adds [diagnostic] to a grouped-summary map keyed by [diagnosticSummaryKey],
/// incrementing the count when the group already exists (Kotlin `MutableMap.add`).
void addToDiagnosticSummaries(
  Map<String, AppleHealthImportDiagnosticSummary> summaries,
  AppleHealthImportDiagnostic diagnostic,
) {
  final key = diagnosticSummaryKey(diagnostic);
  final existing = summaries[key];
  if (existing != null) {
    existing.count += 1;
  } else {
    summaries[key] = AppleHealthImportDiagnosticSummary(
      appleType: diagnostic.appleType,
      targetType: diagnostic.targetType,
      reasonCode: diagnostic.reasonCode,
      detail: diagnostic.detail,
      count: 1,
      exampleTimeRange: diagnostic.timeRange,
      exampleUnit: diagnostic.unit,
      exampleValue: diagnostic.value,
    );
  }
}
