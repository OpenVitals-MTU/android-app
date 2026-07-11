import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/contract/apple_health_import_repository.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_background.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_checkpoint_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_error_formatter.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_records.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_report_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_service.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_staging_store.dart';

/// The order-of-operations log every fake appends to. The single assertion this
/// whole file exists for is that `resolveHealthAccess` lands *before* `import`.
late List<String> calls;

class _FakeRepository implements AppleHealthImportRepository {
  @override
  Future<Set<String>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async =>
      const {};

  @override
  Future<void> insertImportedRecords(List<ImportRecord> records) async {}

  @override
  bool isMindfulnessAvailable() => true;
}

const _result = AppleHealthImportResult(
  parsedRecords: 12,
  parsedWorkouts: 0,
  parsedCorrelations: 0,
  parsedActivitySummaries: 0,
  convertedRecords: 12,
  importedRecords: 9,
  duplicateSkippedRecords: 1,
  notSelectedRecords: 2,
  unsupportedElements: 3,
  skippedRecords: 0,
  failedRecords: 0,
  typeSummaries: [],
  diagnostics: [],
  shareableReportText: 'IMPORT_REPORT',
);

class FakeService extends AppleHealthImportService {
  FakeService({this.error, this.checkpointsPerBatch = 0})
      : super(_FakeRepository());

  final Object? error;

  /// How many batch checkpoints the import hands back before finishing.
  final int checkpointsPerBatch;

  AppleHealthImportCheckpoint? resumedWith;
  Set<AppleHealthImportCategory>? importedCategories;

  @override
  Future<AppleHealthImportResult> importAppleHealthExport(
    File file, {
    Set<AppleHealthImportCategory> selectedCategories =
        allAppleHealthImportCategories,
    AppleHealthImportProgressCallback? onProgress,
    AppleHealthImportCheckpoint? resumeCheckpoint,
    AppleHealthImportCheckpointCallback? onCheckpoint,
  }) async {
    calls.add('import');
    resumedWith = resumeCheckpoint;
    importedCategories = selectedCategories;
    onProgress?.call(const AppleHealthImportProgress(
      phase: AppleHealthImportPhase.writing,
      convertedRecords: 12,
    ));
    for (var batch = 1; batch <= checkpointsPerBatch; batch++) {
      onCheckpoint?.call(AppleHealthImportCheckpoint(
        sourceKey: resumeCheckpoint?.sourceKey ?? '',
        selectedCategories: selectedCategories,
        committedSelectedRecords: batch * 300,
        importedRecords: batch * 300,
      ));
    }
    final failure = error;
    if (failure != null) throw failure;
    return _result;
  }
}

class FakeStagingStore implements AppleHealthImportStagingStore {
  int clearCalls = 0;

  @override
  Future<bool> clear() async {
    calls.add('clearStaging');
    clearCalls++;
    return true;
  }

  @override
  Future<AppleHealthStagedExport> stage(AppleHealthExportSource source) =>
      throw UnimplementedError();

  @override
  Future<Directory> importDirectory() async => Directory.systemTemp;

  @override
  Future<File> stagedExportFile() async => File('staged');
}

class FakeCheckpointStore implements AppleHealthImportCheckpointStore {
  FakeCheckpointStore({this.stored});

  final AppleHealthImportCheckpoint? stored;
  final List<AppleHealthImportCheckpoint> saved = [];
  int clearCalls = 0;

  @override
  Future<AppleHealthImportCheckpoint?> load(
    String sourceKey,
    Set<AppleHealthImportCategory> selectedCategories,
  ) async {
    calls.add('loadCheckpoint');
    return stored;
  }

  @override
  Future<void> save(AppleHealthImportCheckpoint checkpoint) async {
    saved.add(checkpoint);
  }

  @override
  Future<void> clear() async {
    calls.add('clearCheckpoint');
    clearCalls++;
  }
}

Future<AppleHealthImportJobOutcome> run(
  FakeService service,
  FakeStagingStore staging,
  FakeCheckpointStore checkpoints,
  AppleHealthImportReportStore reports, {
  Set<AppleHealthImportCategory> selected = const {
    AppleHealthImportCategory.activity,
  },
  int expectedSelectedRecords = 12,
  int expectedParsedElements = 0,
  void Function(AppleHealthImportProgress)? onProgress,
}) =>
    runAppleHealthImportJob(
      AppleHealthImportJobInputs(
        service: service,
        stagingStore: staging,
        checkpointStore: checkpoints,
        reportStore: reports,
        resolveHealthAccess: () async => calls.add('resolveHealthAccess'),
        stagedFile: File('staged-export.bin'),
        sourceKey: 'export.zip|export.zip|1024',
        selectedCategories: selected,
        expectedSelectedRecords: expectedSelectedRecords,
        expectedParsedElements: expectedParsedElements,
      ),
      onProgress: onProgress,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeStagingStore staging;
  late FakeCheckpointStore checkpoints;
  late AppleHealthImportReportStore reports;

  Future<void> seedReportStore() async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    reports = AppleHealthImportReportStore(await SharedPreferences.getInstance());
  }

  setUp(() async {
    calls = [];
    staging = FakeStagingStore();
    checkpoints = FakeCheckpointStore();
    await seedReportStore();
  });

  test('resolves Health Connect access BEFORE the import runs', () async {
    final service = FakeService();

    final outcome = await run(service, staging, checkpoints, reports);

    expect(outcome.isSuccess, isTrue);
    // The recurring bug this pins: `cachedAvailability` starts at `notSupported`,
    // so an import that runs before access is resolved writes nothing at all and
    // still reports success.
    expect(
      calls.indexOf('resolveHealthAccess') < calls.indexOf('import'),
      isTrue,
      reason: 'availability must be resolved before a single record is written',
    );
  });

  test('resumes from a checkpoint stored for the same source and selection',
      () async {
    checkpoints = FakeCheckpointStore(
      stored: const AppleHealthImportCheckpoint(
        sourceKey: 'export.zip|export.zip|1024',
        selectedCategories: {AppleHealthImportCategory.activity},
        committedSelectedRecords: 600,
        importedRecords: 580,
        duplicateSkippedRecords: 20,
      ),
    );
    final service = FakeService();

    await run(service, staging, checkpoints, reports);

    expect(service.resumedWith?.committedSelectedRecords, 600);
    expect(service.resumedWith?.importedRecords, 580);
    expect(calls.indexOf('loadCheckpoint') < calls.indexOf('import'), isTrue);
  });

  test('starts clean when no checkpoint is stored', () async {
    final service = FakeService();

    await run(service, staging, checkpoints, reports);

    expect(service.resumedWith?.committedSelectedRecords, 0);
    expect(service.resumedWith?.sourceKey, 'export.zip|export.zip|1024');
    expect(
      service.resumedWith?.selectedCategories,
      {AppleHealthImportCategory.activity},
    );
  });

  test('saves a checkpoint for every batch the import commits', () async {
    final service = FakeService(checkpointsPerBatch: 3);

    await run(service, staging, checkpoints, reports);

    expect(checkpoints.saved.length, 3);
    expect(
      checkpoints.saved.map((c) => c.committedSelectedRecords),
      [300, 600, 900],
    );
  });

  test('a successful import writes the report and clears staging + checkpoint',
      () async {
    final service = FakeService(checkpointsPerBatch: 1);
    final progress = <AppleHealthImportPhase>[];

    final outcome = await run(
      service,
      staging,
      checkpoints,
      reports,
      onProgress: (p) => progress.add(p.phase),
    );

    expect(outcome.result?.importedRecords, 9);
    expect(reports.readReport(), 'IMPORT_REPORT');
    expect(staging.clearCalls, 1);
    expect(checkpoints.clearCalls, 1);
    // The last checkpoint write must land before the checkpoint is cleared, or a
    // stale checkpoint outlives the import it belonged to.
    expect(calls.indexOf('clearCheckpoint') < calls.indexOf('clearStaging'), isTrue);
    expect(progress.last, AppleHealthImportPhase.complete);
  });

  test('a failed import KEEPS staging + checkpoint so the next run resumes',
      () async {
    final service = FakeService(
      checkpointsPerBatch: 2,
      error: AppleHealthImportException('boom'),
    );

    final outcome = await run(service, staging, checkpoints, reports);

    expect(outcome.isSuccess, isFalse);
    expect(outcome.error, isA<AppleHealthImportException>());
    expect(staging.clearCalls, 0);
    expect(checkpoints.clearCalls, 0);
    // The batches that *did* commit are still on record.
    expect(checkpoints.saved.length, 2);
    // The failure report replaces the worker's `Result.failure` output data.
    expect(reports.readFailure(), contains('Status: failed'));
    expect(reports.readFailure(), contains('boom'));
    expect(reports.readReport(), isEmpty);
  });

  test('a failure while resolving Health Connect access never imports', () async {
    final service = FakeService();
    final outcome = await runAppleHealthImportJob(
      AppleHealthImportJobInputs(
        service: service,
        stagingStore: staging,
        checkpointStore: checkpoints,
        reportStore: reports,
        resolveHealthAccess: () async => throw StateError('no health connect'),
        stagedFile: File('staged-export.bin'),
        sourceKey: 'key',
        selectedCategories: const {AppleHealthImportCategory.activity},
      ),
    );

    expect(outcome.isSuccess, isFalse);
    expect(calls, isNot(contains('import')));
    expect(staging.clearCalls, 0);
  });

  group('isolate payloads', () {
    test('category set survives the saveData round trip', () {
      const selected = {
        AppleHealthImportCategory.activity,
        AppleHealthImportCategory.workouts,
      };
      final encoded = encodeAppleHealthImportCategories(selected);

      expect(encoded, 'activity,workouts');
      expect(decodeAppleHealthImportCategories(encoded), selected);
      // Kotlin's `selectedCategoriesFromData` default.
      expect(
        decodeAppleHealthImportCategories(null),
        allAppleHealthImportCategories,
      );
    });

    test('progress survives the port round trip', () {
      const progress = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.writing,
        parsedRecords: 100,
        convertedRecords: 80,
        importedRecords: 40,
        notSelectedRecords: 5,
        expectedSelectedRecords: 75,
        expectedParsedElements: 200,
      );

      final decoded = decodeAppleHealthImportProgress(
        encodeAppleHealthImportProgress(
          progress,
          event: kAppleHealthImportEventProgress,
        ),
      );

      expect(decoded?.phase, AppleHealthImportPhase.writing);
      expect(decoded?.importedRecords, 40);
      expect(decoded?.expectedSelectedRecords, 75);
      // The scan denominator was written to the port but never read back, so the
      // main isolate could not see it and the card could never show the scan
      // variant. It has to survive the trip like every other counter.
      expect(decoded?.expectedParsedElements, 200);
      expect(decoded?.percent, progress.percent);
      expect(decodeAppleHealthImportProgress('not a payload'), isNull);
    });

    test('the job re-seeds both expected totals onto every progress it emits',
        () async {
      final progresses = <AppleHealthImportProgress>[];
      await run(
        FakeService(),
        staging,
        checkpoints,
        reports,
        expectedSelectedRecords: 75,
        expectedParsedElements: 200,
        onProgress: progresses.add,
      );

      expect(progresses, isNotEmpty);
      for (final progress in progresses) {
        expect(progress.expectedSelectedRecords, 75);
        expect(progress.expectedParsedElements, 200);
      }
    });

    test('the result payload carries the counters, the store carries the report',
        () {
      final payload = encodeAppleHealthImportProgress(
        appleHealthImportProgressOf(
          _result,
          phase: AppleHealthImportPhase.complete,
          expectedSelectedRecords: 12,
        ),
        event: kAppleHealthImportEventResult,
        workoutRoutesIncomplete: true,
      );

      final decoded = decodeAppleHealthImportResult(payload, 'REPORT_FROM_STORE');

      expect(decoded?.importedRecords, 9);
      expect(decoded?.duplicateSkippedRecords, 1);
      expect(decoded?.workoutRoutesIncomplete, isTrue);
      expect(decoded?.shareableReportText, 'REPORT_FROM_STORE');
    });

    test('the error payload carries the details and the permission flag', () {
      final payload = encodeAppleHealthImportError(
        AppleHealthImportException('boom'),
      );

      expect(payload[kAppleHealthImportEventKey], kAppleHealthImportEventError);
      expect('${payload['error']}', contains('boom'));
      expect(payload['permissionDenied'], isFalse);
    });
  });
}
