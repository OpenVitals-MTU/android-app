/// Riverpod notifier driving the Settings "Apple Health import" card, ported
/// from the Kotlin `SettingsViewModel` Apple Health import wiring.
///
/// Analyze (scan + summarise by category without writing), toggle categories,
/// then import the selected set, persisting the shareable report through the
/// [AppleHealthImportReportStore].
///
/// The import itself runs where Kotlin runs it: **outside the UI**. Kotlin
/// enqueues a WorkManager worker; this port starts the app's foreground service
/// with `apple_health_import_task_handler.dart` as its task, so a multi-hour
/// import keeps running (with an ongoing progress notification) while the user
/// leaves the app — the promise the card's "Import continues in the background"
/// line already made. Progress, the result and any error come back over the
/// task-data port and drive exactly the same [AppleHealthImportUiState] the
/// in-process path used to.
///
/// Where no foreground service exists (tests, desktop, a missing plugin) the
/// import falls back to running in-process, through the very same
/// [runAppleHealthImportJob] the service isolate runs.
///
/// The picked export is never read into memory: it is staged to app-private
/// storage (with a verified byte count) in the *main* isolate — staging is
/// idempotent and the service isolate reuses that copy — and everything
/// downstream works against that [File]. Resume degrades gracefully: an import
/// that is killed resumes from its last committed batch.
library;

import 'dart:async';

import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../di/providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import 'apple_health_import_background.dart';
import 'apple_health_import_checkpoint_store.dart';
import 'apple_health_import_error_formatter.dart';
import 'apple_health_import_foreground_controller.dart';
import 'apple_health_import_models.dart';
import 'apple_health_import_report_store.dart';
import 'apple_health_import_service.dart';
import 'apple_health_import_staging_store.dart';

part 'apple_health_import_view_model.freezed.dart';

final appleHealthImportStagingStoreProvider =
    Provider<AppleHealthImportStagingStore>(
  (ref) => AppleHealthImportStagingStore(),
);

final appleHealthImportCheckpointStoreProvider =
    Provider<AppleHealthImportCheckpointStore>(
  (ref) => AppleHealthImportCheckpointStore(),
);

/// Shown when an activity recording already owns the app's single foreground
/// service (see [AppleHealthImportLaunch.serviceBusy]).
// TODO(l10n): Flutter-only string; the Kotlin app cannot hit this collision
// because WorkManager runs the import next to the recording service.
const String kAppleHealthImportServiceBusyError =
    'An activity recording is using the foreground service, so the Apple Health '
    'import cannot run in the background. Finish or discard the recording, then '
    'import again.';

/// Immutable UI state mirroring the Apple Health fields of the Kotlin
/// `SettingsUiState` consumed by `AppleHealthImportCard`.
///
/// Deliberately NOT a `CommandState`: the import's whole point is the report it
/// produces — per-record-type counts, duplicates, diagnostics — which it keeps
/// emitting *while* it runs, from another isolate. `CommandRunning` carries no
/// progress and `CommandSuccess` no partial outcome, so the rich progress/report
/// model below stays exactly as it was; only its hand-written copyWith is gone.
///
/// Every transition that must *clear* a nullable field still constructs a fresh
/// state rather than calling `copyWith` — the freezed copyWith could now pass an
/// explicit null, but the call sites read better as "this is the new state".
@freezed
abstract class AppleHealthImportUiState with _$AppleHealthImportUiState {
  const AppleHealthImportUiState._();

  const factory AppleHealthImportUiState({
    @Default(false) bool isAnalyzing,
    @Default(false) bool isImporting,
    AppleHealthImportProgress? analysisProgress,
    AppleHealthImportAnalysisResult? analysis,
    @Default(<AppleHealthImportCategory>{})
    Set<AppleHealthImportCategory> selectedCategories,
    AppleHealthImportProgress? progress,
    AppleHealthImportResult? result,
    String? error,
    @Default(false) bool permissionDenied,
  }) = _AppleHealthImportUiState;

  bool get isBusy => isAnalyzing || isImporting;
}

class AppleHealthImportViewModel extends Notifier<AppleHealthImportUiState> {
  // Resolved ONCE in [build], never re-read afterwards.
  //
  // These used to be `ref.read(...)` getters, which made every use after an
  // `await` a latent crash: reading a `Ref` whose provider has been disposed
  // THROWS, and this view-model awaits file I/O and a multi-minute import job
  // between uses. The card can be closed — and, in tests, the container torn
  // down — at any point in those gaps.
  //
  // It was not theoretical. `_loadPersistedReports` reads two files in a row,
  // and the second `_reportStore` landed after the first `await`; closing the
  // Settings section in that window threw
  // "Cannot use the Ref of NotifierProvider<AppleHealthImportViewModel> after
  // it has been disposed" out of an unawaited future. On CI, whose disk is
  // slower and whose test order is randomized, it surfaced as a different
  // unrelated test failing "after it had already completed" — which is why two
  // previous attempts fixed the wrong file.
  //
  // Holding the instances removes the failure mode outright, rather than
  // needing an `if (!ref.mounted)` before all five of them forever.
  late AppleHealthImportService _service;
  late AppleHealthImportReportStore _reportStore;
  late AppleHealthImportStagingStore _stagingStore;
  late AppleHealthImportCheckpointStore _checkpointStore;
  late AppleHealthImportServiceController _serviceController;

  /// The most recently analyzed export, reused by [importSelected] (the Kotlin
  /// `pendingAppleHealthImportUri`). Only the *identity* of the pick is held —
  /// its bytes live in the staged file.
  AppleHealthExportSource? _pendingSource;
  String _lastReportText = '';
  String _lastFailureText = '';

  @override
  AppleHealthImportUiState build() {
    // Synchronously, before the first `await` below: after one, `ref` may
    // already be disposed. Reassigned rather than `late final` because `build`
    // re-runs on invalidation.
    _service = ref.read(appleHealthImportServiceProvider);
    _reportStore = ref.read(appleHealthImportReportStoreProvider);
    _stagingStore = ref.read(appleHealthImportStagingStoreProvider);
    _checkpointStore = ref.read(appleHealthImportCheckpointStoreProvider);
    _serviceController = ref.read(appleHealthImportServiceControllerProvider);

    // Read the last persisted report/failure back on card open so the Save
    // report action has content even before a fresh import runs this session.
    // The store is file-backed, so this is async; the card only needs it by the
    // time the user taps Save.
    unawaited(_loadPersistedReports());
    // Progress from the service isolate (and, on a relaunch, from an import that
    // is still in flight) arrives here.
    FlutterForegroundTask.addTaskDataCallback(_onTaskData);
    ref.onDispose(
      () => FlutterForegroundTask.removeTaskDataCallback(_onTaskData),
    );
    unawaited(_attachToRunningImport());
    return const AppleHealthImportUiState();
  }

  Future<void> _loadPersistedReports() async {
    _lastReportText = await _reportStore.readReport();
    _lastFailureText = await _reportStore.readFailure();
  }

  /// The best available report text for the Save action, favouring the live
  /// result, then a live error, then the last persisted report/failure.
  String get reportTextForSave {
    final result = state.result;
    if (result != null) return result.shareableReportText;
    final error = state.error;
    if (error != null && error.isNotEmpty) return error;
    if (_lastReportText.isNotEmpty) return _lastReportText;
    return _lastFailureText;
  }

  /// Stage the picked export into app-private storage (verifying the copied
  /// byte count), then scan + summarise it by category without writing anything,
  /// and auto-select the detected categories (Kotlin
  /// `analyzeStagedAppleHealthExport`).
  Future<void> analyze(AppleHealthExportSource source) async {
    if (state.isBusy) return;
    _pendingSource = source;
    state = const AppleHealthImportUiState(
      isAnalyzing: true,
      analysisProgress:
          AppleHealthImportProgress(phase: AppleHealthImportPhase.queued),
    );
    try {
      final staged = await _stagingStore.stage(source);
      final analysis = await _service.analyzeAppleHealthExport(
        staged.file,
        onProgress: (progress) {
          if (ref.mounted && state.isAnalyzing) {
            state = state.copyWith(analysisProgress: progress);
          }
        },
      );
      final detected = analysis.categorySummaries
          .map((summary) => summary.category)
          .toSet();
      // Analysis is minutes of work on a large export; the card can be gone by
      // now, and assigning `state` after disposal throws exactly as reading
      // `ref` does.
      if (!ref.mounted) return;
      state = AppleHealthImportUiState(
        analysis: analysis,
        selectedCategories: detected,
      );
    } catch (error) {
      _pendingSource = null;
      // Never let a retry reuse a copy we already know is bad. Deliberately
      // still run when unmounted: the bad staged copy must go regardless of
      // whether anyone is left to see the error.
      await _stagingStore.clear();
      if (!ref.mounted) return;
      state = AppleHealthImportUiState(
        error: AppleHealthImportErrorFormatter.details(error),
        permissionDenied:
            AppleHealthImportErrorFormatter.isPermissionDenied(error),
      );
    }
  }

  void setCategorySelected(AppleHealthImportCategory category, bool selected) {
    final current = state.selectedCategories;
    final next = <AppleHealthImportCategory>{...current};
    if (selected) {
      next.add(category);
    } else {
      next.remove(category);
    }
    state = state.copyWith(selectedCategories: next);
  }

  /// Stage the pick, then hand the import to the foreground service (Kotlin
  /// `AppleHealthImportWorkController.enqueue`), falling back to an in-process
  /// import where no service can run.
  Future<void> importSelected() async {
    if (state.isBusy) return;
    final source = _pendingSource;
    final analysis = state.analysis;
    if (source == null || analysis == null) return;
    final selected = state.selectedCategories;
    if (selected.isEmpty) return;

    final expectedSelectedRecords = analysis.categorySummaries
        .where((summary) => selected.contains(summary.category))
        .fold<int>(0, (sum, summary) => sum + summary.convertedRecords);

    state = state.copyWith(
      isImporting: true,
      progress: AppleHealthImportProgress(
        phase: AppleHealthImportPhase.queued,
        expectedSelectedRecords: expectedSelectedRecords,
        expectedParsedElements: analysis.parsedElements,
      ),
    );

    // Staging stays in the main isolate: it is idempotent (an existing copy with
    // a matching fingerprint is reused), it is where the picker's stream lives,
    // and a copy failure must be reported before a service is ever started.
    final AppleHealthStagedExport staged;
    try {
      staged = await _stagingStore.stage(source);
    } catch (error) {
      await _publishFailure(error);
      return;
    }

    final launch = await _serviceController.start(AppleHealthImportRequest(
      stagedFilePath: staged.file.path,
      sourceKey: source.sourceKey,
      selectedCategories: selected,
      expectedSelectedRecords: expectedSelectedRecords,
      expectedParsedElements: analysis.parsedElements,
    ));
    switch (launch) {
      case AppleHealthImportLaunch.started:
      case AppleHealthImportLaunch.alreadyImporting:
        // The task isolate owns the import now; [_onTaskData] drives the UI.
        return;
      case AppleHealthImportLaunch.serviceBusy:
        // Refused, not failed: the staged copy stays put, so importing again
        // once the recording ends reuses it.
        if (!ref.mounted) return;
        state = AppleHealthImportUiState(
          analysis: analysis,
          selectedCategories: selected,
          error: kAppleHealthImportServiceBusyError,
        );
        return;
      case AppleHealthImportLaunch.unavailable:
        break;
    }

    final outcome = await runAppleHealthImportJob(
      AppleHealthImportJobInputs(
        service: _service,
        stagingStore: _stagingStore,
        checkpointStore: _checkpointStore,
        reportStore: _reportStore,
        resolveHealthAccess: _resolveHealthAccessInProcess,
        stagedFile: staged.file,
        sourceKey: source.sourceKey,
        selectedCategories: selected,
        expectedSelectedRecords: expectedSelectedRecords,
        expectedParsedElements: analysis.parsedElements,
      ),
      onProgress: (progress) {
        if (ref.mounted && state.isImporting) {
          state = state.copyWith(progress: progress);
        }
      },
    );
    final result = outcome.result;
    if (result != null) {
      _lastReportText = result.shareableReportText;
      if (!ref.mounted) return;
      // The import just landed months of data the widgets have never seen —
      // refresh them now instead of waiting out the 30-minute alarm.
      // Fire-and-forget: the import outcome must not ride on a widget push.
      unawaited(ref.read(homeWidgetRefresherProvider).refreshIfPlaced());
      state = AppleHealthImportUiState(
        analysis: analysis,
        selectedCategories: selected,
        result: result,
      );
      return;
    }
    // The job kept the staged copy and the checkpoint, so a retry resumes.
    final error = outcome.error!;
    _lastFailureText = await _reportStore.readFailure();
    if (!ref.mounted) return;
    state = AppleHealthImportUiState(
      analysis: analysis,
      selectedCategories: selected,
      error: AppleHealthImportErrorFormatter.details(error),
      permissionDenied: AppleHealthImportErrorFormatter.isPermissionDenied(error),
    );
  }

  /// The in-process import's `resolveHealthAccess`.
  ///
  /// [healthConnectAvailabilityProvider] *is* `refreshAvailability()` on the
  /// singleton data source, and the card already awaits it (the import button
  /// stays disabled until it reports `available`), so awaiting it here resolves
  /// access exactly once, before the first write — the same invariant the
  /// isolate enforces with its own `HealthRepositoryImpl.refreshAvailability()`,
  /// which it must build by hand because it has no provider graph and no gate.
  ///
  /// The one `ref.read` that cannot be hoisted into [build]: eagerly holding
  /// the future would leave it unawaited whenever no in-process import runs,
  /// and a failed availability check would then surface as an unhandled async
  /// error — trading one crash for another. It is read lazily and guarded
  /// instead, so a card closed mid-import fails the JOB (which
  /// [runAppleHealthImportJob] reports through its outcome) rather than
  /// throwing out of an unawaited future.
  Future<void> _resolveHealthAccessInProcess() {
    if (!ref.mounted) {
      throw StateError(
        'The Apple Health card closed before health access was resolved; '
        'the in-process import cannot continue without it.',
      );
    }
    return ref.read(healthConnectAvailabilityProvider.future);
  }

  // ── Foreground-service import ───────────────────────────────────────────────

  /// Re-attaches the card to an import that is still running after the app was
  /// backgrounded (or killed and relaunched) — the Kotlin card re-observes the
  /// unique work's `WorkInfo` for the same reason.
  Future<void> _attachToRunningImport() async {
    if (!await _serviceController.isImportRunning()) return;
    if (!ref.mounted || state.isBusy) return;
    state = state.copyWith(
      isImporting: true,
      progress: const AppleHealthImportProgress(
        phase: AppleHealthImportPhase.queued,
      ),
    );
  }

  /// Progress / result / error from the service isolate.
  void _onTaskData(Object data) {
    if (data is! Map) return;
    switch (data[kAppleHealthImportEventKey]) {
      case kAppleHealthImportEventProgress:
        final progress = decodeAppleHealthImportProgress(data);
        if (progress == null || !ref.mounted) return;
        state = state.copyWith(isImporting: true, progress: progress);
      case kAppleHealthImportEventResult:
        unawaited(_onImportResult(data));
      case kAppleHealthImportEventError:
        unawaited(_onImportError(data));
    }
  }

  Future<void> _onImportResult(Map<Object?, Object?> data) async {
    // Render the counts from the payload right away — they do not depend on the
    // report text, and reading that back is file I/O we should not make the
    // result wait on.
    final analysis = state.analysis;
    final selectedCategories = state.selectedCategories;
    final immediate = decodeAppleHealthImportResult(data, '');
    if (immediate == null || !ref.mounted) return;
    state = AppleHealthImportUiState(
      analysis: analysis,
      selectedCategories: selectedCategories,
      result: immediate,
    );

    // The report was written to a file by the *other* isolate; the filesystem is
    // shared, so reading it back needs no reload. Patch it onto the result so
    // Copy/Save have the full text.
    _lastReportText = await _reportStore.readReport();
    if (_lastReportText.isEmpty || !ref.mounted) return;
    state = AppleHealthImportUiState(
      analysis: analysis,
      selectedCategories: selectedCategories,
      result: decodeAppleHealthImportResult(data, _lastReportText),
    );
  }

  Future<void> _onImportError(Map<Object?, Object?> data) async {
    // Show the error right away; the failure report (for Save) loads after.
    if (!ref.mounted) return;
    state = AppleHealthImportUiState(
      analysis: state.analysis,
      selectedCategories: state.selectedCategories,
      error: '${data['error']}',
      permissionDenied: data['permissionDenied'] == true,
    );
    _lastFailureText = await _reportStore.readFailure();
  }

  /// A staging failure: nothing has been imported, so this is reported exactly
  /// like an import failure (the Kotlin worker stages inside `doWork`, so a copy
  /// failure lands in the same `Result.failure` path).
  Future<void> _publishFailure(Object error) async {
    final failureText = buildAppleHealthFailureReportText(error);
    try {
      await _reportStore.writeFailure(failureText);
    } catch (_) {
      // A report we could not persist must not mask the failure itself.
    }
    _lastFailureText = failureText;
    if (!ref.mounted) return;
    state = AppleHealthImportUiState(
      analysis: state.analysis,
      selectedCategories: state.selectedCategories,
      error: AppleHealthImportErrorFormatter.details(error),
      permissionDenied: AppleHealthImportErrorFormatter.isPermissionDenied(error),
    );
  }
}

final appleHealthImportProvider =
    NotifierProvider<AppleHealthImportViewModel, AppleHealthImportUiState>(
  AppleHealthImportViewModel.new,
);
