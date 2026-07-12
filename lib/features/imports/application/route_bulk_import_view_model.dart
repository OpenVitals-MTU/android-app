import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../activity/application/activities_view_model.dart';
import '../../dashboard/application/dashboard_view_model.dart';
import '../../manualentry/activity/activity_entry_clock.dart';
import '../../manualentry/activity/activity_entry_view_model.dart';
import '../../manualentry/activity/activity_entry_providers.dart';
import '../../manualentry/activity/activity_entry_write_request_builder.dart';
import '../../manualentry/activity/routeimport/route_file_parser.dart';

part 'route_bulk_import_view_model.freezed.dart';

/// Per-file progress of a bulk route import. Port of the Kotlin
/// `RouteBulkImportProgress`.
@freezed
abstract class RouteBulkImportProgress with _$RouteBulkImportProgress {
  const factory RouteBulkImportProgress({
    required int totalFiles,
    @Default(0) int importedFiles,
    @Default(0) int failedFiles,
    @Default(0) int currentFileIndex,
  }) = _RouteBulkImportProgress;
}

/// The outcome of a finished bulk route import. Port of the Kotlin
/// `RouteBulkImportResult`.
@freezed
abstract class RouteBulkImportResult with _$RouteBulkImportResult {
  const factory RouteBulkImportResult({
    required int totalFiles,
    required int importedFiles,
    required int failedFiles,
  }) = _RouteBulkImportResult;
}

/// Observable state of the settings route bulk importer. Mirrors the Kotlin
/// `SettingsUiState` route-import fields (`isImportingRouteFiles`,
/// `routeImportProgress`, `routeImportResult`, `routeImportError`).
///
/// Deliberately NOT a `CommandState`: a finished bulk import is not "success"
/// or "failure" — it is `importedFiles` of `totalFiles`, with the last failure's
/// text alongside a positive import count. The run is also observable while it
/// runs ([progress] counts files as they land), which [CommandRunning] carries
/// nothing to express. The among-many progress model is the state.
@freezed
abstract class RouteBulkImportState with _$RouteBulkImportState {
  const factory RouteBulkImportState({
    @Default(false) bool isImporting,
    RouteBulkImportProgress? progress,
    RouteBulkImportResult? result,

    /// The *last* file's failure, rendered by the Settings card through
    /// `l10n.settingsRouteImportError`. A String, not a [ScreenError], because
    /// one failed file in a tolerated batch is a line of feedback, not the
    /// screen's error state.
    String? error,
  }) = _RouteBulkImportState;
}

/// Drives the direct-write bulk route import. Faithful port of the Kotlin
/// `SettingsViewModel.importRouteFiles`: for each picked file it parses the
/// route, folds it into a fresh activity form state, builds the write request,
/// checks the write permission and writes it to Health Connect. One bad file
/// does not abort the batch (per-file error tolerance).
class RouteBulkImportViewModel extends Notifier<RouteBulkImportState> {
  @override
  RouteBulkImportState build() => const RouteBulkImportState();

  Future<void> importRouteFiles(
    List<ActivityRouteFileHandle> handles,
    UnitSystem unitSystem,
  ) async {
    if (handles.isEmpty || state.isImporting) return;

    final importer = ref.read(routeFileImporterProvider);
    final writeImportedActivity = ref.read(writeImportedActivityUseCaseProvider);
    final preferences = ref.read(preferencesRepositoryProvider);
    final writePermissions =
        ref.read(readActivityWritePermissionsUseCaseProvider)();
    final clock = ActivityEntryClock.system();

    final totalFiles = handles.length;
    var importedFiles = 0;
    var failedFiles = 0;
    String? lastError;

    state = RouteBulkImportState(
      isImporting: true,
      progress: RouteBulkImportProgress(totalFiles: totalFiles),
    );

    for (var index = 0; index < handles.length; index++) {
      state = state.copyWith(
        progress: RouteBulkImportProgress(
          totalFiles: totalFiles,
          importedFiles: importedFiles,
          failedFiles: failedFiles,
          currentFileIndex: index + 1,
        ),
      );

      try {
        final routeImport = await importer.import(handles[index]);
        final routeState = activityStateWithRouteImport(
          initialActivityEntryState(
            clock,
            writePermissions,
            selectedActivityType: preferredActivityEntryType(
              preferences,
              requireGpsRoute: routeImport.points.isNotEmpty,
            ),
          ),
          routeImport,
          unitSystem,
          clock,
        );
        final request = buildWriteRequest(routeState, unitSystem);
        if (request == null) {
          throw const RouteImportException(
            'Imported route could not be converted into an activity.',
          );
        }
        // Checks the permissions THIS record needs, then writes it — a file with
        // a route needs more than a bare track does. See
        // [WriteImportedActivityUseCase].
        switch (await writeImportedActivity(request)) {
          case Ok():
            preferences.lastActivityExerciseType = request.exerciseType;
            importedFiles += 1;
          case Err(:final failure):
            // Per-file tolerance: a refused write — including a permission the
            // record needs and does not have — fails one file, never the batch.
            failedFiles += 1;
            lastError = _describeFailure(failure);
        }
      } catch (error) {
        // The route parser still throws (it is not a repository), so a malformed
        // file lands here.
        failedFiles += 1;
        lastError = _describeError(error);
      }
    }

    state = RouteBulkImportState(
      isImporting: false,
      result: RouteBulkImportResult(
        totalFiles: totalFiles,
        importedFiles: importedFiles,
        failedFiles: failedFiles,
      ),
      error: failedFiles > 0 ? lastError : null,
    );

    // Newly-written activities must surface on the dashboard and activities
    // list, which the settings screen never navigates through. Kotlin marks the
    // dashboard dirty; the Riverpod analogue is invalidating the read models.
    if (importedFiles > 0) {
      ref.invalidate(dashboardProvider);
      ref.invalidate(activitiesProvider);
    }
  }

  /// A failed write, at the presentation boundary: the use-case's [AppFailure]
  /// becomes a [ScreenError] and then the line of text the card renders. A
  /// permission refusal is *this record's* missing permission (the use case
  /// carries `MissingActivityWritePermissionException` as the failure's cause),
  /// which the user must be told about — it will fail every remaining file for
  /// the same reason.
  String _describeFailure(AppFailure failure) =>
      switch (failure.toScreenError(fallback: _fallbackError)) {
        ScreenErrorPermissionDenied() =>
          'Activity import write permissions are missing.',
        ScreenErrorHealthConnectUnavailable() =>
          'Health Connect is unavailable.',
        ScreenErrorMessage(:final text) => text,
        ScreenErrorNotFound() || ScreenErrorMissingArgument() => _fallbackError,
      };

  /// A thrown parse/conversion failure (the route parser is not a repository and
  /// still throws).
  String _describeError(Object error) {
    if (error is RouteImportException) return error.message;
    final message = error.toString();
    return message.isEmpty ? _fallbackError : message;
  }
}

const String _fallbackError = 'Route import failed.';

final routeBulkImportProvider =
    NotifierProvider<RouteBulkImportViewModel, RouteBulkImportState>(
  RouteBulkImportViewModel.new,
);
