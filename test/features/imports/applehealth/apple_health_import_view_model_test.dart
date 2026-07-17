import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/apple_health_import_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_checkpoint_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_error_formatter.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_foreground_controller.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_report_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_service.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_staging_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_view_model.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

class _FakeRepository implements AppleHealthImportRepository {
  @override
  Future<Result<Set<String>>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async =>
      const Ok({});

  @override
  Future<Result<void>> insertImportedRecords(List<ImportRecord> records) async =>
      const Ok(null);

  @override
  bool isMindfulnessAvailable() => true;
}

const _analysis = AppleHealthImportAnalysisResult(
  parsedRecords: 20,
  parsedWorkouts: 2,
  parsedCorrelations: 0,
  parsedActivitySummaries: 0,
  convertedRecords: 18,
  unsupportedElements: 2,
  skippedRecords: 0,
  failedRecords: 0,
  categorySummaries: [
    AppleHealthImportCategorySummary(
      category: AppleHealthImportCategory.activity,
      convertedRecords: 10,
    ),
    AppleHealthImportCategorySummary(
      category: AppleHealthImportCategory.vitals,
      convertedRecords: 8,
    ),
  ],
  typeSummaries: [],
  diagnostics: [],
  shareableReportText: 'ANALYSIS_REPORT',
);

const _result = AppleHealthImportResult(
  parsedRecords: 20,
  parsedWorkouts: 2,
  parsedCorrelations: 0,
  parsedActivitySummaries: 0,
  convertedRecords: 18,
  importedRecords: 15,
  duplicateSkippedRecords: 2,
  notSelectedRecords: 0,
  unsupportedElements: 2,
  skippedRecords: 0,
  failedRecords: 1,
  typeSummaries: [],
  diagnostics: [],
  shareableReportText: 'IMPORT_REPORT',
);

/// Answers with canned analysis/result (or throws), and records what the
/// view-model asked it to import.
class _FakeService extends AppleHealthImportService {
  _FakeService({this.analyzeError, this.importError}) : super(_FakeRepository());

  final Object? analyzeError;
  final Object? importError;
  Set<AppleHealthImportCategory>? importedCategories;

  @override
  Future<AppleHealthImportAnalysisResult> analyzeAppleHealthExport(
    File file, {
    AppleHealthImportProgressCallback? onProgress,
  }) async {
    onProgress?.call(const AppleHealthImportProgress(
      phase: AppleHealthImportPhase.parsing,
      parsedRecords: 5,
    ));
    final error = analyzeError;
    if (error != null) throw error;
    return _analysis;
  }

  @override
  Future<AppleHealthImportResult> importAppleHealthExport(
    File file, {
    Set<AppleHealthImportCategory> selectedCategories =
        allAppleHealthImportCategories,
    AppleHealthImportProgressCallback? onProgress,
    AppleHealthImportCheckpoint? resumeCheckpoint,
    AppleHealthImportCheckpointCallback? onCheckpoint,
  }) async {
    importedCategories = selectedCategories;
    onProgress?.call(const AppleHealthImportProgress(
      phase: AppleHealthImportPhase.writing,
      convertedRecords: 18,
      importedRecords: 15,
    ));
    final error = importError;
    if (error != null) throw error;
    return _result;
  }
}

/// No foreground service here (the desktop/test host), so the view-model runs
/// the import in-process — through the very same job the isolate runs.
class _NoServiceController implements AppleHealthImportServiceController {
  @override
  Future<AppleHealthImportLaunch> start(AppleHealthImportRequest request) async =>
      AppleHealthImportLaunch.unavailable;

  @override
  Future<bool> isImportRunning() async => false;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Directory tempDir;
  late File export;

  setUp(() {
    tempDir = Directory.systemTemp.createTempSync('apple_health_import_vm');
    export = File('${tempDir.path}/export.zip')
      ..writeAsBytesSync(List<int>.filled(64, 7));
  });

  tearDown(() => tempDir.deleteSync(recursive: true));

  Future<ProviderContainer> container({_FakeService? service}) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    Future<Directory> directory() async => tempDir;
    final result = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        appleHealthImportServiceProvider
            .overrideWithValue(service ?? _FakeService()),
        appleHealthImportReportStoreProvider.overrideWithValue(
          AppleHealthImportReportStore(directoryResolver: directory),
        ),
        appleHealthImportStagingStoreProvider.overrideWithValue(
          AppleHealthImportStagingStore(directory: directory),
        ),
        appleHealthImportCheckpointStoreProvider.overrideWithValue(
          AppleHealthImportCheckpointStore(directory: directory),
        ),
        appleHealthImportServiceControllerProvider
            .overrideWithValue(_NoServiceController()),
        healthConnectAvailabilityProvider
            .overrideWith((ref) async => HealthConnectAvailability.available),
      ],
    );
    addTearDown(result.dispose);
    return result;
  }

  AppleHealthExportSource source() => AppleHealthExportSource.file(export);

  test('analyze summarises the export and pre-selects what it found', () async {
    final harness = await container();

    await harness.read(appleHealthImportProvider.notifier).analyze(source());

    final state = harness.read(appleHealthImportProvider);
    expect(state.isAnalyzing, isFalse);
    expect(state.analysis, same(_analysis));
    expect(
      state.selectedCategories,
      {AppleHealthImportCategory.activity, AppleHealthImportCategory.vitals},
    );
    expect(state.error, isNull);
  });

  test('a failed analysis reports the error and forgets the staged pick',
      () async {
    final harness = await container(
      service: _FakeService(
        analyzeError: AppleHealthImportPermissionException('no read access'),
      ),
    );

    await harness.read(appleHealthImportProvider.notifier).analyze(source());

    final state = harness.read(appleHealthImportProvider);
    expect(state.analysis, isNull);
    expect(state.error, contains('no read access'));
    expect(state.permissionDenied, isTrue);
    expect(state.isBusy, isFalse);

    // The pending source is gone, so importing cannot reuse a bad staged copy.
    await harness.read(appleHealthImportProvider.notifier).importSelected();
    expect(harness.read(appleHealthImportProvider).result, isNull);
  });

  test('importing the selected categories keeps the whole report', () async {
    final service = _FakeService();
    final harness = await container(service: service);
    final notifier = harness.read(appleHealthImportProvider.notifier);

    await notifier.analyze(source());
    notifier.setCategorySelected(AppleHealthImportCategory.vitals, false);
    await notifier.importSelected();

    final state = harness.read(appleHealthImportProvider);
    expect(service.importedCategories, {AppleHealthImportCategory.activity});
    expect(state.isImporting, isFalse);
    expect(state.result?.importedRecords, 15);
    expect(state.result?.duplicateSkippedRecords, 2);
    expect(state.result?.failedRecords, 1);
    expect(state.result?.shareableReportText, 'IMPORT_REPORT');
    // The analysis (and the selection it was run with) survives the import.
    expect(state.analysis, same(_analysis));
    expect(state.selectedCategories, {AppleHealthImportCategory.activity});
    // The report the Save action offers is the import's, not the analysis's.
    expect(notifier.reportTextForSave, 'IMPORT_REPORT');
  });

  test('a failed import reports the failure and offers it for saving',
      () async {
    final harness = await container(
      service: _FakeService(
        importError: AppleHealthImportException('write batch exploded'),
      ),
    );
    final notifier = harness.read(appleHealthImportProvider.notifier);

    await notifier.analyze(source());
    await notifier.importSelected();

    final state = harness.read(appleHealthImportProvider);
    expect(state.result, isNull);
    expect(state.isImporting, isFalse);
    expect(state.error, contains('write batch exploded'));
    expect(state.permissionDenied, isFalse);
    expect(notifier.reportTextForSave, contains('write batch exploded'));
    // The staged export is kept, so a retry resumes instead of re-copying.
    expect(File('${tempDir.path}/staged_export.bin').existsSync(), isTrue);
  });

  test('importing without an analysis does nothing', () async {
    final harness = await container();

    await harness.read(appleHealthImportProvider.notifier).importSelected();

    expect(harness.read(appleHealthImportProvider), const AppleHealthImportUiState());
  });
}
