import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/import_write_repository.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/features/imports/csv/csv_column_mapping.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';
import 'package:openvitals/features/imports/csv/csv_import_metric.dart';
import 'package:openvitals/features/imports/csv/csv_import_models.dart';
import 'package:openvitals/features/imports/csv/csv_import_service.dart';
import 'package:openvitals/features/imports/csv/csv_table_reader.dart';

/// Hand-written, per AGENTS.md — this repo has no mocking library.
class FakeImportWriteRepository implements ImportWriteRepository {
  FakeImportWriteRepository({
    this.existingIds = const {},
    this.failEveryBatch = false,
    this.rejectRecordWhere,
    this.rateLimitAfterBatches,
  });

  final Set<String> existingIds;

  /// Refuses any multi-record insert, forcing the single-record retry path.
  final bool failEveryBatch;

  /// Refuses one specific record during the single-record retry.
  final bool Function(ImportRecord record)? rejectRecordWhere;

  /// Rate-limits once this many batches have been attempted.
  final int? rateLimitAfterBatches;

  final List<ImportRecord> inserted = [];
  int batchCalls = 0;
  int lookupCalls = 0;

  @override
  bool isMindfulnessAvailable() => true;

  @override
  Future<Result<void>> insertImportedRecords(List<ImportRecord> records) async {
    batchCalls++;
    if (rateLimitAfterBatches != null && batchCalls > rateLimitAfterBatches!) {
      return const Err(RateLimitFailure('quota exceeded'));
    }
    if (records.length > 1 && failEveryBatch) {
      return const Err(UnexpectedFailure('batch refused'));
    }
    if (records.length == 1 && (rejectRecordWhere?.call(records.single) ?? false)) {
      return const Err(UnexpectedFailure('record refused'));
    }
    inserted.addAll(records);
    return const Ok(null);
  }

  @override
  Future<Result<Set<String>>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async {
    lookupCalls++;
    return Ok(wantedIds.intersection(existingIds));
  }
}

CsvImportMapping weightMapping() => CsvImportMapping(
      columns: const [
        CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        CsvColumnMapping(
          columnIndex: 1,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.weight,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
      ],
      dateTime: const CsvDateTimeSettings(
        format: CsvDateTimeFormat.yearFirst,
        zone: CsvTimeZoneMode.utc,
      ),
    );

const CsvDialect dialect = CsvDialect(fieldDelimiter: ',', eol: '\n');

void main() {
  late Directory dir;

  setUp(() => dir = Directory.systemTemp.createTempSync('csv_service_test'));
  tearDown(() => dir.deleteSync(recursive: true));

  /// [days] daily weigh-ins starting 2026-07-01.
  File writeCsv(String name, int days) {
    final buffer = StringBuffer('Date,Weight\n');
    for (var day = 0; day < days; day++) {
      final date = DateTime.utc(2026, 7, 1).add(Duration(days: day));
      final stamp = '${date.year}-${date.month.toString().padLeft(2, '0')}-'
          '${date.day.toString().padLeft(2, '0')} 08:12:00';
      buffer.writeln('$stamp,${(78 + day % 5).toStringAsFixed(1)}');
    }
    return File('${dir.path}/$name')..writeAsStringSync(buffer.toString());
  }

  test('every row of a clean file is written and the run completes', () async {
    final repository = FakeImportWriteRepository();
    final file = writeCsv('clean.csv', 120);

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.outcome, CsvImportOutcome.completed);
    expect(result.progress.rowsRead, 120);
    expect(result.progress.written, 120);
    expect(result.progress.rejected, 0);
    expect(repository.inserted, hasLength(120));
  });

  test(
    'records already in Health Connect are counted as present and still '
    'written, so a corrected value upserts',
    () async {
      final file = writeCsv('again.csv', 3);
      // Pre-seed with the ids the file will produce.
      final probe = FakeImportWriteRepository();
      await CsvImportService(probe).run(
        path: file.path,
        dialect: dialect,
        mapping: weightMapping(),
      );
      final existing = probe.inserted.map((it) => it.clientRecordId).toSet();

      final repository = FakeImportWriteRepository(existingIds: existing);
      final result = await CsvImportService(repository).run(
        path: file.path,
        dialect: dialect,
        mapping: weightMapping(),
      );

      expect(result.progress.alreadyPresent, 3);
      // Still inserted: the id excludes the value, so only a write can carry a
      // correction through.
      expect(repository.inserted, hasLength(3));
      expect(result.progress.written, 3);
    },
  );

  test('a duplicated row inside one file is written once', () async {
    final file = File('${dir.path}/dupe.csv')
      ..writeAsStringSync(
        'Date,Weight\n'
        '2026-07-01 08:12:00,78.4\n'
        '2026-07-01 08:12:00,78.4\n'
        '2026-07-02 08:12:00,78.6\n',
      );
    final repository = FakeImportWriteRepository();

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.progress.rowsRead, 3);
    expect(repository.inserted, hasLength(2));
  });

  test(
    'a refused batch is retried record by record and only the bad record is '
    'counted as rejected',
    () async {
      final file = writeCsv('bad.csv', 5);
      final repository = FakeImportWriteRepository(
        failEveryBatch: true,
        rejectRecordWhere: (record) =>
            record is WeightImportRecord && record.time.day == 3,
      );

      final result = await CsvImportService(repository).run(
        path: file.path,
        dialect: dialect,
        mapping: weightMapping(),
      );

      expect(result.outcome, CsvImportOutcome.completed);
      expect(result.progress.written, 4);
      expect(result.progress.rejected, 1);
      expect(
        result.diagnosticCounts[CsvImportDiagnosticReason.writeFailed],
        1,
      );
    },
  );

  test('a rate-limited run stops and reports how far it got', () async {
    final file = writeCsv('ratelimit.csv', 900);
    // Batch size is 300, so the second flush is refused.
    final repository = FakeImportWriteRepository(rateLimitAfterBatches: 1);

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.outcome, CsvImportOutcome.rateLimited);
    expect(result.error, contains('quota'));
    expect(result.progress.written, kCsvWriteBatchSize);
    expect(result.progress.rowsRead, lessThan(900));
  });

  test('cancelling mid-run keeps what was written and stops reading', () async {
    final file = writeCsv('cancel.csv', 2000);
    final repository = FakeImportWriteRepository();
    final cancellation = CsvImportCancellation();

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
      cancellation: cancellation,
      onProgress: (progress) {
        if (progress.rowsRead >= 50) cancellation.cancel();
      },
    );

    expect(result.outcome, CsvImportOutcome.cancelled);
    expect(result.progress.rowsRead, lessThan(2000));
    expect(repository.inserted.length, result.progress.written);
  });

  test('a malformed row is skipped with a diagnostic and the rest imports',
      () async {
    final file = File('${dir.path}/malformed.csv')
      ..writeAsStringSync(
        'Date,Weight\n'
        '2026-07-01 08:12:00,78.4\n'
        'not a date,78.5\n'
        '2026-07-03 08:12:00,78.6\n',
      );
    final repository = FakeImportWriteRepository();

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.outcome, CsvImportOutcome.completed);
    expect(result.progress.written, 2);
    expect(
      result.diagnosticCounts[CsvImportDiagnosticReason.unparsableTimestamp],
      1,
    );
  });

  test('a missing file fails the run instead of throwing', () async {
    final repository = FakeImportWriteRepository();

    final result = await CsvImportService(repository).run(
      path: '${dir.path}/nope.csv',
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.outcome, CsvImportOutcome.failed);
    expect(result.error, isNotNull);
    expect(result.wroteNothing, isTrue);
  });

  test('a file with only a header writes nothing and still completes', () async {
    final file = File('${dir.path}/headeronly.csv')
      ..writeAsStringSync('Date,Weight\n');
    final repository = FakeImportWriteRepository();

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.outcome, CsvImportOutcome.completed);
    expect(result.progress.written, 0);
    expect(repository.batchCalls, 0);
  });

  test('the retained diagnostic log is capped while the counts stay complete',
      () async {
    final buffer = StringBuffer('Date,Weight\n');
    for (var i = 0; i < kCsvMaxRetainedDiagnostics + 50; i++) {
      buffer.writeln('not a date,78.4');
    }
    final file = File('${dir.path}/allbad.csv')
      ..writeAsStringSync(buffer.toString());
    final repository = FakeImportWriteRepository();

    final result = await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
    );

    expect(result.diagnostics, hasLength(kCsvMaxRetainedDiagnostics));
    expect(
      result.diagnosticCounts[CsvImportDiagnosticReason.unparsableTimestamp],
      kCsvMaxRetainedDiagnostics + 50,
    );
  });

  test('progress reports a fraction of the file once bytes are known', () async {
    final file = writeCsv('progress.csv', 200);
    final repository = FakeImportWriteRepository();
    final fractions = <double>[];

    await CsvImportService(repository).run(
      path: file.path,
      dialect: dialect,
      mapping: weightMapping(),
      onProgress: (progress) {
        final fraction = progress.fraction;
        if (fraction != null) fractions.add(fraction);
      },
    );

    expect(fractions, isNotEmpty);
    expect(fractions.every((it) => it >= 0 && it <= 1), isTrue);
  });
}
