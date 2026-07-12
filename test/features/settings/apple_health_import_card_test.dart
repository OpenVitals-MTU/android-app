import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/apple_health_import_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_background.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_error_formatter.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_foreground_controller.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_checkpoint_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_view_model.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_service.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_staging_store.dart';
import 'package:openvitals/features/settings/presentation/cards/apple_health_import_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

// ── Fakes ────────────────────────────────────────────────────────────────────

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

final _cannedAnalysis = AppleHealthImportAnalysisResult(
  parsedRecords: 12,
  parsedWorkouts: 0,
  parsedCorrelations: 0,
  parsedActivitySummaries: 0,
  convertedRecords: 12,
  unsupportedElements: 3,
  skippedRecords: 0,
  failedRecords: 0,
  categorySummaries: const [
    AppleHealthImportCategorySummary(
      category: AppleHealthImportCategory.activity,
      convertedRecords: 10,
    ),
    AppleHealthImportCategorySummary(
      category: AppleHealthImportCategory.workouts,
      convertedRecords: 2,
      routeSessions: 1,
    ),
  ],
  typeSummaries: const [],
  diagnostics: const [],
  shareableReportText: 'ANALYSIS_REPORT',
);

const _cannedResult = AppleHealthImportResult(
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

const _cannedIncompleteRoutesResult = AppleHealthImportResult(
  parsedRecords: 12,
  parsedWorkouts: 1,
  parsedCorrelations: 0,
  parsedActivitySummaries: 0,
  convertedRecords: 12,
  importedRecords: 9,
  duplicateSkippedRecords: 1,
  notSelectedRecords: 2,
  unsupportedElements: 3,
  skippedRecords: 1,
  failedRecords: 0,
  workoutRoutesIncomplete: true,
  typeSummaries: [],
  diagnostics: [],
  shareableReportText: 'IMPORT_REPORT',
);

class _FakeService extends AppleHealthImportService {
  _FakeService({this.analysisError, this.result = _cannedResult})
      : super(_FakeRepository());

  final Object? analysisError;
  final AppleHealthImportResult result;

  Set<AppleHealthImportCategory>? importedCategories;
  int analyzeCalls = 0;
  int importCalls = 0;

  File? analyzedFile;
  File? importedFile;

  @override
  Future<AppleHealthImportAnalysisResult> analyzeAppleHealthExport(
    File file, {
    AppleHealthImportProgressCallback? onProgress,
  }) async {
    analyzeCalls++;
    analyzedFile = file;
    final error = analysisError;
    if (error != null) throw error;
    return _cannedAnalysis;
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
    importCalls++;
    importedFile = file;
    importedCategories = selectedCategories;
    return result;
  }
}

// ── Harness ──────────────────────────────────────────────────────────────────

late Directory cardTempDirectory;

/// A picked export backed by a real temp file.
AppleHealthExportSource fakePickedExport() {
  final file = File('${cardTempDirectory.path}/picked-export.xml')
    ..writeAsStringSync('<HealthData />');
  return AppleHealthExportSource.file(file);
}

/// The real stores do async `dart:io` work, which never completes under
/// `testWidgets`' FakeAsync clock. The staging/checkpoint stores have their own
/// tests against real temp directories; here they are faked so the card's
/// orchestration (stage → import → clear) is what gets asserted.
class FakeStagingStore implements AppleHealthImportStagingStore {
  int stageCalls = 0;
  int clearCalls = 0;
  AppleHealthExportSource? stagedSource;

  @override
  Future<AppleHealthStagedExport> stage(AppleHealthExportSource source) async {
    stageCalls++;
    stagedSource = source;
    final file = File('${cardTempDirectory.path}/staged-export.bin')
      ..writeAsStringSync('<HealthData />');
    return AppleHealthStagedExport(
      file: file,
      bytes: file.lengthSync(),
      reused: false,
    );
  }

  @override
  Future<bool> clear() async {
    clearCalls++;
    return true;
  }

  @override
  Future<Directory> importDirectory() async => Directory.systemTemp;

  @override
  Future<File> stagedExportFile() async =>
      File('${cardTempDirectory.path}/staged-export.bin');
}

class FakeCheckpointStore implements AppleHealthImportCheckpointStore {
  AppleHealthImportCheckpoint? stored;
  final List<AppleHealthImportCheckpoint> saved = [];
  int clearCalls = 0;

  @override
  Future<AppleHealthImportCheckpoint?> load(
    String sourceKey,
    Set<AppleHealthImportCategory> selectedCategories,
  ) async =>
      stored;

  @override
  Future<void> save(AppleHealthImportCheckpoint checkpoint) async =>
      saved.add(checkpoint);

  @override
  Future<void> clear() async => clearCalls++;
}

/// The app has a single `ForegroundService`, so the import can only run in the
/// background when nothing else (an activity recording) holds it. This fake is
/// what decides that, standing in for `flutter_foreground_task`.
class FakeImportServiceController implements AppleHealthImportServiceController {
  FakeImportServiceController({
    this.launch = AppleHealthImportLaunch.unavailable,
    this.running = false,
  });

  AppleHealthImportLaunch launch;
  bool running;
  AppleHealthImportRequest? request;
  int startCalls = 0;

  @override
  Future<AppleHealthImportLaunch> start(AppleHealthImportRequest request) async {
    startCalls++;
    this.request = request;
    return launch;
  }

  @override
  Future<bool> isImportRunning() async => running;
}

late FakeStagingStore stagingStore;
late FakeCheckpointStore checkpointStore;
late FakeImportServiceController serviceController;
late SharedPreferences prefs;

/// Delivers a payload the way the service isolate's `sendDataToMain` would.
void emitTaskData(Object payload) {
  for (final callback in FlutterForegroundTask.dataCallbacks.toList()) {
    callback(payload);
  }
}

Future<Widget> _bootstrap(
  _FakeService service, {
  Set<String> granted = const {},
  bool grantAll = false,
  Future<AppleHealthExportSource?> Function()? pickExportSource,
  Future<bool> Function(String, String)? saveReportFile,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      appleHealthImportServiceProvider.overrideWithValue(service),
      appleHealthImportStagingStoreProvider.overrideWithValue(stagingStore),
      appleHealthImportCheckpointStoreProvider
          .overrideWithValue(checkpointStore),
      appleHealthImportServiceControllerProvider
          .overrideWithValue(serviceController),
      healthConnectAvailabilityProvider.overrideWith(
        (ref) async => HealthConnectAvailability.available,
      ),
      grantedHealthPermissionsProvider.overrideWith(
        (ref) async => grantAll
            ? ref.watch(healthRepositoryProvider).dataImportWritePermissions
            : granted,
      ),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: SingleChildScrollView(
          child: AppleHealthImportCard(
            pickExportSource: pickExportSource,
            saveReportFile: saveReportFile,
          ),
        ),
      ),
    ),
  );
}

void main() {
  setUp(() {
    cardTempDirectory =
        Directory.systemTemp.createTempSync('apple_health_card_test');
    stagingStore = FakeStagingStore();
    checkpointStore = FakeCheckpointStore();
    // Default: no foreground service (the desktop/test case), so the import runs
    // in-process exactly as it did before it was moved to a service isolate.
    serviceController = FakeImportServiceController();
  });

  tearDown(() {
    if (cardTempDirectory.existsSync()) {
      cardTempDirectory.deleteSync(recursive: true);
    }
  });

  testWidgets('renders header, permission line and analyze action',
      (tester) async {
    final service = _FakeService();
    await tester.pumpWidget(await _bootstrap(service));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Apple Health Importer'), findsOneWidget);
    expect(find.textContaining('import permissions granted'), findsOneWidget);
    expect(find.text('Analyze Apple Health export'), findsOneWidget);
  });

  testWidgets('analyze populates the analysis result and category checklist',
      (tester) async {
    final service = _FakeService();
    await tester.pumpWidget(
      await _bootstrap(service, pickExportSource: () async => fakePickedExport()),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();

    expect(service.analyzeCalls, 1);
    expect(find.textContaining('Found 12 compatible records'), findsOneWidget);
    // Per-category checklist with two detected categories.
    expect(find.text('Activity metrics'), findsOneWidget);
    expect(find.text('Workouts and routes'), findsOneWidget);
    expect(find.byType(Checkbox), findsNWidgets(2));
    // The route-aware count line for the workouts category.
    expect(find.textContaining('2 records, 1 with routes'), findsOneWidget);
    // After an analysis the action flips to "choose another".
    expect(find.text('Choose another Apple Health export'), findsOneWidget);
    expect(find.text('Import selected categories'), findsOneWidget);
  });

  testWidgets('toggling a category then importing calls import with the '
      'selected set and shows the result', (tester) async {
    final service = _FakeService();
    await tester.pumpWidget(
      await _bootstrap(
        service,
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();

    // Both categories auto-selected; deselect workouts (the second checkbox).
    await tester.tap(find.byType(Checkbox).at(1));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    expect(service.importCalls, 1);
    expect(service.importedCategories, {AppleHealthImportCategory.activity});

    // Result summary (six counters) + report actions.
    expect(find.textContaining('Imported 9'), findsOneWidget);
    expect(find.text('Copy report'), findsOneWidget);
    expect(find.text('Download full report'), findsOneWidget);
  });

  testWidgets('save report action invokes the save seam and confirms',
      (tester) async {
    final service = _FakeService();
    String? savedContent;
    await tester.pumpWidget(
      await _bootstrap(
        service,
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
        saveReportFile: (content, name) async {
          savedContent = content;
          return true;
        },
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Download full report'));
    await tester.pumpAndSettle();

    expect(savedContent, 'IMPORT_REPORT');
    expect(find.text('Import report saved.'), findsOneWidget);
  });

  testWidgets('analysis failure shows the error text', (tester) async {
    final service = _FakeService(
      analysisError: AppleHealthImportException('boom'),
    );
    await tester.pumpWidget(
      await _bootstrap(service, pickExportSource: () async => fakePickedExport()),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Import failed'), findsOneWidget);
    expect(find.text('Copy error'), findsOneWidget);
    // A failed analysis must never leave a staged copy a retry could reuse.
    expect(stagingStore.clearCalls, 1);
  });

  testWidgets('a successful import stages the pick, then clears the staged '
      'export and its checkpoint', (tester) async {
    final service = _FakeService();
    await tester.pumpWidget(
      await _bootstrap(
        service,
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    // The picked export is never read into memory; the service only ever sees
    // the staged file.
    expect(stagingStore.stageCalls, 2); // analyze, then import (reused copy)
    expect(service.analyzedFile?.path, endsWith('staged-export.bin'));
    expect(service.importedFile?.path, endsWith('staged-export.bin'));
    expect(stagingStore.clearCalls, 1);
    expect(checkpointStore.clearCalls, 1);
  });

  testWidgets('an incomplete workout-route archive shows the warning row',
      (tester) async {
    final service = _FakeService(result: _cannedIncompleteRoutesResult);
    await tester.pumpWidget(
      await _bootstrap(
        service,
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('some workout routes were unavailable'),
      findsOneWidget,
    );
  });

  // ── Foreground-service import ───────────────────────────────────────────────

  testWidgets('the import is handed to the foreground service, not run in the '
      'UI isolate', (tester) async {
    serviceController.launch = AppleHealthImportLaunch.started;
    final service = _FakeService();
    await tester.pumpWidget(
      await _bootstrap(
        service,
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    // The service isolate owns the import; nothing runs here.
    expect(service.importCalls, 0);
    expect(serviceController.startCalls, 1);
    // It is handed the staged copy (never the pick) plus the checkpoint identity
    // and the two progress denominators.
    final request = serviceController.request!;
    expect(request.stagedFilePath, endsWith('staged-export.bin'));
    expect(request.sourceKey, isNotEmpty);
    expect(request.selectedCategories, {
      AppleHealthImportCategory.activity,
      AppleHealthImportCategory.workouts,
    });
    expect(request.expectedSelectedRecords, 12);
    expect(request.expectedParsedElements, 12);
    // The card keeps promising the background import while the service runs.
    expect(
      find.text('Import continues in the background while you leave the app.'),
      findsOneWidget,
    );
  });

  testWidgets('progress and the result from the service isolate drive the card',
      (tester) async {
    serviceController.launch = AppleHealthImportLaunch.started;
    await tester.pumpWidget(
      await _bootstrap(
        _FakeService(),
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    emitTaskData(encodeAppleHealthImportProgress(
      const AppleHealthImportProgress(
        phase: AppleHealthImportPhase.writing,
        parsedRecords: 12,
        convertedRecords: 12,
        importedRecords: 5,
        expectedSelectedRecords: 12,
        expectedParsedElements: 20,
      ),
      event: kAppleHealthImportEventProgress,
    ));
    await tester.pumpAndSettle();
    expect(find.textContaining('imported 5'), findsOneWidget);
    // The scan variant: the export's element total came over the port, so the
    // line names the same denominator the percent is computed from.
    expect(find.textContaining('Scanned 12/20 items'), findsOneWidget);

    // The report is written by the *other* isolate, so it comes back from the
    // store, not over the port.
    await prefs.setString('apple_health_import_report', 'BG_REPORT');
    emitTaskData(encodeAppleHealthImportProgress(
      const AppleHealthImportProgress(
        phase: AppleHealthImportPhase.complete,
        parsedRecords: 12,
        convertedRecords: 12,
        importedRecords: 9,
        duplicateSkippedRecords: 1,
        notSelectedRecords: 2,
        unsupportedElements: 3,
        expectedSelectedRecords: 12,
      ),
      event: kAppleHealthImportEventResult,
    ));
    await tester.pumpAndSettle();

    expect(find.textContaining('Imported 9'), findsOneWidget);
    expect(find.text('Copy report'), findsOneWidget);
  });

  testWidgets('an error from the service isolate shows in the card',
      (tester) async {
    serviceController.launch = AppleHealthImportLaunch.started;
    await tester.pumpWidget(
      await _bootstrap(
        _FakeService(),
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    emitTaskData(encodeAppleHealthImportError(AppleHealthImportException('boom')));
    await tester.pumpAndSettle();

    expect(find.textContaining('boom'), findsOneWidget);
    expect(find.text('Copy error'), findsOneWidget);
  });

  testWidgets('a running activity recording refuses the import instead of '
      'crashing on the single foreground service', (tester) async {
    serviceController.launch = AppleHealthImportLaunch.serviceBusy;
    final service = _FakeService();
    await tester.pumpWidget(
      await _bootstrap(
        service,
        grantAll: true,
        pickExportSource: () async => fakePickedExport(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Analyze Apple Health export'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Import selected categories'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(service.importCalls, 0);
    expect(find.textContaining('activity recording'), findsOneWidget);
    // Refused, not failed: the staged copy stays for the retry.
    expect(stagingStore.clearCalls, 0);
    expect(checkpointStore.clearCalls, 0);
    // The card is idle again, so the user can retry once the recording ends.
    expect(find.text('Import selected categories'), findsOneWidget);
  });

  testWidgets('an import still running on relaunch re-attaches to the card',
      (tester) async {
    serviceController.running = true;
    await tester.pumpWidget(await _bootstrap(_FakeService()));
    // Not `pumpAndSettle`: the re-attached (percent-less) progress bar animates
    // forever, which is precisely the point — the import is still in flight.
    await tester.pump();
    await tester.pump();

    expect(
      find.text('Import continues in the background while you leave the app.'),
      findsOneWidget,
    );
    expect(find.byType(LinearProgressIndicator), findsOneWidget);
  });
}
