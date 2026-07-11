import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';

part 'data_confidence.freezed.dart';

enum DataConfidenceLevel {
  high,
  medium,
  low,
}

enum DataSourceConsistency {
  notAvailable,
  singleSource,
  mixedSources,
}

enum DataValueKind {
  measured,
  aggregated,
  calculated,
  estimated,
  mixed,
}

enum DataConfidenceWarning {
  lowCoverage,
  sparseData,
  mixedSources,
  manualEntries,
  calculatedValue,
  noSourceDetails,
}

@freezed
abstract class DataConfidence with _$DataConfidence {
  const factory DataConfidence({
    required DataConfidenceLevel level,
    required int expectedDays,
    required int trackedDays,
    required int sampleCount,
    required int coveragePercent,
    required List<String> sources,
    required DataSourceConsistency sourceConsistency,
    required DataValueKind valueKind,
    required int manualEntryCount,
    required List<DataConfidenceWarning> warnings,
  }) = _DataConfidence;
}

DataConfidence dataConfidence(
  DatePeriod period,
  Iterable<LocalDate> trackedDates,
  int sampleCount, {
  Iterable<String> sources = const <String>[],
  DataValueKind valueKind = DataValueKind.measured,
  int manualEntryCount = 0,
  int minSamplesForTrend = 3,
}) {
  final expectedDays =
      math.max(1, (period.end.epochDay - period.start.epochDay) + 1);
  final trackedDays = trackedDates
      .where((date) =>
          !date.isBefore(period.start) && !date.isAfter(period.end))
      .toSet()
      .length;
  final coveragePercent =
      ((trackedDays.toDouble() / expectedDays.toDouble()) * 100.0).round();
  final normalizedSources = sources
      .map((source) => source.trim())
      .where((source) => source.isNotEmpty)
      .toSet()
      .toList()
    ..sort();
  final DataSourceConsistency sourceConsistency;
  switch (normalizedSources.length) {
    case 0:
      sourceConsistency = DataSourceConsistency.notAvailable;
    case 1:
      sourceConsistency = DataSourceConsistency.singleSource;
    default:
      sourceConsistency = DataSourceConsistency.mixedSources;
  }
  final sparseData = (sampleCount >= 1 && sampleCount < minSamplesForTrend) ||
      (expectedDays > 1 &&
          trackedDays >= 1 &&
          trackedDays < math.min(minSamplesForTrend, expectedDays));
  final lowCoverage = expectedDays > 1 && coveragePercent < 60;
  final warnings = <DataConfidenceWarning>[];
  if (lowCoverage) warnings.add(DataConfidenceWarning.lowCoverage);
  if (sparseData) warnings.add(DataConfidenceWarning.sparseData);
  if (sourceConsistency == DataSourceConsistency.mixedSources) {
    warnings.add(DataConfidenceWarning.mixedSources);
  }
  if (manualEntryCount > 0) warnings.add(DataConfidenceWarning.manualEntries);
  if (valueKind == DataValueKind.calculated ||
      valueKind == DataValueKind.estimated) {
    warnings.add(DataConfidenceWarning.calculatedValue);
  }
  if (sourceConsistency == DataSourceConsistency.notAvailable) {
    warnings.add(DataConfidenceWarning.noSourceDetails);
  }
  final DataConfidenceLevel level;
  if (sampleCount <= 0) {
    level = DataConfidenceLevel.low;
  } else if (expectedDays > 1 && coveragePercent < 25) {
    level = DataConfidenceLevel.low;
  } else if (sparseData) {
    level = DataConfidenceLevel.low;
  } else if (warnings.isNotEmpty) {
    level = DataConfidenceLevel.medium;
  } else {
    level = DataConfidenceLevel.high;
  }

  return DataConfidence(
    level: level,
    expectedDays: expectedDays,
    trackedDays: trackedDays,
    sampleCount: math.max(0, sampleCount),
    coveragePercent: coveragePercent,
    sources: normalizedSources,
    sourceConsistency: sourceConsistency,
    valueKind: valueKind,
    manualEntryCount: math.max(0, manualEntryCount),
    warnings: warnings,
  );
}
