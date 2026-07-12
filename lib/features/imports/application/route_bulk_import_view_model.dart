import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../domain/usecase/write_imported_activity_use_case.dart';
import '../../activity/application/activities_view_model.dart';
import '../../dashboard/application/dashboard_view_model.dart';
import '../../manualentry/activity/activity_entry_clock.dart';
import '../../manualentry/activity/activity_entry_notifier.dart';
import '../../manualentry/activity/activity_entry_providers.dart';
import '../../manualentry/activity/activity_entry_write_request_builder.dart';
import '../../manualentry/activity/routeimport/route_file_parser.dart';

/// Per-file progress of a bulk route import. Port of the Kotlin
/// `RouteBulkImportProgress`.
@immutable
class RouteBulkImportProgress {
  const RouteBulkImportProgress({
    required this.totalFiles,
    this.importedFiles = 0,
    this.failedFiles = 0,
    this.currentFileIndex = 0,
  });

  final int totalFiles;
  final int importedFiles;
  final int failedFiles;
  final int currentFileIndex;

  @override
  bool operator ==(Object other) =>
      other is RouteBulkImportProgress &&
      other.totalFiles == totalFiles &&
      other.importedFiles == importedFiles &&
      other.failedFiles == failedFiles &&
      other.currentFileIndex == currentFileIndex;

  @override
  int get hashCode =>
      Object.hash(totalFiles, importedFiles, failedFiles, currentFileIndex);
}

/// The outcome of a finished bulk route import. Port of the Kotlin
/// `RouteBulkImportResult`.
@immutable
class RouteBulkImportResult {
  const RouteBulkImportResult({
    required this.totalFiles,
    required this.importedFiles,
    required this.failedFiles,
  });

  final int totalFiles;
  final int importedFiles;
  final int failedFiles;

  @override
  bool operator ==(Object other) =>
      other is RouteBulkImportResult &&
      other.totalFiles == totalFiles &&
      other.importedFiles == importedFiles &&
      other.failedFiles == failedFiles;

  @override
  int get hashCode => Object.hash(totalFiles, importedFiles, failedFiles);
}

/// Observable state of the settings route bulk importer. Mirrors the Kotlin
/// `SettingsUiState` route-import fields (`isImportingRouteFiles`,
/// `routeImportProgress`, `routeImportResult`, `routeImportError`).
@immutable
class RouteBulkImportState {
  const RouteBulkImportState({
    this.isImporting = false,
    this.progress,
    this.result,
    this.error,
  });

  final bool isImporting;
  final RouteBulkImportProgress? progress;
  final RouteBulkImportResult? result;
  final String? error;

  RouteBulkImportState copyWith({
    bool? isImporting,
    RouteBulkImportProgress? progress,
    RouteBulkImportResult? result,
    String? error,
    bool clearProgress = false,
    bool clearResult = false,
    bool clearError = false,
  }) =>
      RouteBulkImportState(
        isImporting: isImporting ?? this.isImporting,
        progress: clearProgress ? null : (progress ?? this.progress),
        result: clearResult ? null : (result ?? this.result),
        error: clearError ? null : (error ?? this.error),
      );
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
        (await writeImportedActivity(request)).orThrow();
        preferences.lastActivityExerciseType = request.exerciseType;
        importedFiles += 1;
      } catch (error) {
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

  String _describeError(Object error) {
    if (error is MissingActivityWritePermissionException) {
      return 'Activity import write permissions are missing.';
    }
    if (error is RouteImportException) return error.message;
    final message = error.toString();
    return message.isEmpty ? 'Route import failed.' : message;
  }
}

final routeBulkImportProvider =
    NotifierProvider<RouteBulkImportViewModel, RouteBulkImportState>(
  RouteBulkImportViewModel.new,
);
