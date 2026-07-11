import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/data_confidence.dart';

void main() {
  test('confidence calculates coverage inside the selected period', () {
    final period = DatePeriod(LocalDate(2026, 5, 1), LocalDate(2026, 5, 7));

    final confidence = dataConfidence(
      period,
      [
        LocalDate(2026, 4, 30),
        LocalDate(2026, 5, 1),
        LocalDate(2026, 5, 3),
        LocalDate(2026, 5, 3),
        LocalDate(2026, 5, 8),
      ],
      5,
      sources: const ['com.example.watch'],
    );

    expect(confidence.expectedDays, 7);
    expect(confidence.trackedDays, 2);
    expect(confidence.coveragePercent, 29);
  });

  test('mixed sources are reported as medium confidence', () {
    final confidence = dataConfidence(
      DatePeriod(LocalDate(2026, 5, 1), LocalDate(2026, 5, 1)),
      [LocalDate(2026, 5, 1)],
      12,
      sources: const ['com.example.watch', 'com.example.scale'],
    );

    expect(confidence.level, DataConfidenceLevel.medium);
    expect(confidence.sourceConsistency, DataSourceConsistency.mixedSources);
    expect(confidence.warnings, contains(DataConfidenceWarning.mixedSources));
  });

  test('calculated values include a calculated warning', () {
    final confidence = dataConfidence(
      DatePeriod(LocalDate(2026, 5, 1), LocalDate(2026, 5, 1)),
      [LocalDate(2026, 5, 1)],
      1,
      sources: const ['com.example.scale'],
      valueKind: DataValueKind.calculated,
    );

    expect(confidence.level, DataConfidenceLevel.low);
    expect(confidence.warnings, contains(DataConfidenceWarning.calculatedValue));
  });

  test('empty samples are low confidence', () {
    final confidence = dataConfidence(
      DatePeriod(LocalDate(2026, 5, 1), LocalDate(2026, 5, 31)),
      const [],
      0,
    );

    expect(confidence.level, DataConfidenceLevel.low);
    expect(confidence.warnings, contains(DataConfidenceWarning.noSourceDetails));
  });

  test('manual entries are reported', () {
    final confidence = dataConfidence(
      DatePeriod(LocalDate(2026, 5, 1), LocalDate(2026, 5, 3)),
      [
        LocalDate(2026, 5, 1),
        LocalDate(2026, 5, 2),
        LocalDate(2026, 5, 3),
      ],
      3,
      sources: const ['com.example.app'],
      manualEntryCount: 1,
    );

    expect(confidence.level, DataConfidenceLevel.medium);
    expect(confidence.warnings, contains(DataConfidenceWarning.manualEntries));
  });
}
