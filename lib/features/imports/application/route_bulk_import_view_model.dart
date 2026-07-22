import 'package:flutter/foundation.dart'; // DIAGNOSTIC: debugPrint to logcat (also re-exports Uint8List)
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/apple_health_import_records.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../activity/application/activities_view_model.dart';
import '../../dashboard/application/dashboard_view_model.dart';
import '../../manualentry/activity/activity_entry_clock.dart';
import '../../manualentry/activity/activity_entry_view_model.dart';
import '../../manualentry/activity/activity_entry_providers.dart';
import '../../manualentry/activity/activity_entry_write_request_builder.dart';
import '../../manualentry/activity/routeimport/fit_route_parser.dart';
import '../../manualentry/activity/routeimport/route_file_parser.dart';
import '../fit/fit_wellness_import.dart';

part 'route_bulk_import_view_model.freezed.dart';

/// One file waiting its turn in a bulk import — opened only when the importer
/// reaches it.
///
/// The batch used to arrive as [ActivityRouteFileHandle]s, which carry BYTES, so
/// the caller had to read every file in the batch before the first one was
/// written. Picking four GPX tracks that way costs nothing. Pointing the
/// importer at a folder of four hundred FIT files would have held all four
/// hundred in memory at once and died before importing one. A source is a name
/// and a way to GET the bytes, so the importer holds one file's worth at a time.
class ActivityRouteFileSource {
  const ActivityRouteFileSource({required this.fileName, required this.read});

  /// Bytes already in hand — a single pick, a test fixture. Reading is a no-op.
  factory ActivityRouteFileSource.ofBytes({
    required Uint8List bytes,
    String? fileName,
  }) =>
      ActivityRouteFileSource(fileName: fileName, read: () async => bytes);

  final String? fileName;

  /// Opens the file. May throw, and the importer treats that as one failed file:
  /// a folder scanned a minute ago can name a file that has since been moved,
  /// and one unreadable file must not kill the batch.
  final Future<Uint8List> Function() read;
}

/// Per-file progress of a bulk route import. Port of the Kotlin
/// `RouteBulkImportProgress`.
@freezed
abstract class RouteBulkImportProgress with _$RouteBulkImportProgress {
  const factory RouteBulkImportProgress({
    required int totalFiles,
    @Default(0) int importedFiles,
    @Default(0) int failedFiles,
    @Default(0) int skippedFiles,
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
    @Default(0) int skippedFiles,
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
    List<ActivityRouteFileSource> files,
    UnitSystem unitSystem,
  ) async {
    if (files.isEmpty || state.isImporting) return;

    final importer = ref.read(routeFileImporterProvider);
    final writeImportedActivity = ref.read(writeImportedActivityUseCaseProvider);
    final writeImportedActivities =
        ref.read(writeImportedActivitiesUseCaseProvider);
    // Wellness FIT types (sleep, …) are not activities: they carry no route and
    // write to their own Health Connect records through the generic import path
    // the Apple Health importer uses, not the exercise-session path.
    final healthDataSource = ref.read(healthDataSourceProvider);
    final preferences = ref.read(preferencesRepositoryProvider);
    final writePermissions =
        ref.read(readActivityWritePermissionsUseCaseProvider)();
    final clock = ActivityEntryClock.system();

    final totalFiles = files.length;
    var importedFiles = 0;
    var failedFiles = 0;
    // Non-activity FIT files with no Health Connect-mappable data (Garmin stress,
    // Body Battery, metrics, sleep-disruption). Skipped, not failed: nothing is
    // wrong with them, they simply have no Health Connect home.
    var skippedFiles = 0;
    String? lastError;

    state = RouteBulkImportState(
      isImporting: true,
      progress: RouteBulkImportProgress(totalFiles: totalFiles),
    );

    // Parsed activities waiting to be written together. Health Connect charges
    // its rate limit per API CALL, not per record, so writing these one at a time
    // would spend a unit of quota per file — a folder of a couple of thousand
    // then dies on "API call quota exceeded" partway through. See
    // [WriteImportedActivitiesUseCase].
    final pending = <ActivityWriteRequest>[];
    // Peak memory is now the BATCH, not the file, so the batch is bounded by the
    // GPS points it is carrying and not just by how many files it holds.
    var pendingRoutePoints = 0;
    // Set when Health Connect says the quota is gone. Not a bad file: the data is
    // fine and the quota refills, so the run STOPS instead of marching through the
    // rest of the folder failing every remaining file for the same reason and
    // reporting them all as broken.
    var rateLimited = false;

    // Parsed wellness records waiting to be written together, same per-call-quota
    // reasoning as the activity batch. Grouped by file — a monitoring file yields
    // both a resting-HR and a BMR record — so the file count stays right whether a
    // batch succeeds whole or is retried file by file.
    final wellnessPending = <List<ImportRecord>>[];

    Future<void> writeWellnessPending() async {
      if (wellnessPending.isEmpty) return;
      final groups = List<List<ImportRecord>>.of(wellnessPending);
      wellnessPending.clear();
      try {
        await healthDataSource
            .insertImportedRecords([for (final g in groups) ...g]);
        importedFiles += groups.length;
      } catch (_) {
        // insertImportedRecords throws rather than returning a Result; a batch is
        // all-or-nothing, so retry one FILE at a time to keep the good ones and
        // count only the guilty file.
        for (final group in groups) {
          try {
            await healthDataSource.insertImportedRecords(group);
            importedFiles += 1;
          } catch (error) {
            failedFiles += 1;
            lastError = _describeError(error);
            debugPrint('[FIT] wellness write FAILED: $lastError');
          }
        }
      }
      state = state.copyWith(
        progress: RouteBulkImportProgress(
          totalFiles: totalFiles,
          importedFiles: importedFiles,
          failedFiles: failedFiles,
          skippedFiles: skippedFiles,
          currentFileIndex: state.progress?.currentFileIndex ?? 0,
        ),
      );
    }

    Future<void> writePending() async {
      if (pending.isEmpty || rateLimited) return;
      final batch = List<ActivityWriteRequest>.of(pending);
      pending.clear();
      pendingRoutePoints = 0;

      switch (await writeImportedActivities(batch)) {
        case Ok():
          importedFiles += batch.length;
          preferences.lastActivityExerciseType = batch.last.exerciseType;
        case Err(:final RateLimitFailure failure):
          rateLimited = true;
          lastError = _describeFailure(failure);
          debugPrint('[FIT] batch rate-limited: $lastError',
        );
        case Err():
          // The batch is ATOMIC: Health Connect wrote none of it, and the failure
          // does not say which record it choked on. Retry the files one by one to
          // find the bad one — the good ones still get written, exactly as they
          // did before batching, and only the guilty file is counted as failed.
          debugPrint('[FIT] batch write refused; retrying ${batch.length} files singly',
        );
          for (final request in batch) {
            if (rateLimited) break;
            switch (await writeImportedActivity(request)) {
              case Ok():
                preferences.lastActivityExerciseType = request.exerciseType;
                importedFiles += 1;
              case Err(:final RateLimitFailure failure):
                rateLimited = true;
                lastError = _describeFailure(failure);
              case Err(:final failure):
                // Per-file tolerance: a refused write — including a permission the
                // record needs and does not have — fails one file, never the batch.
                failedFiles += 1;
                lastError = _describeFailure(failure);
                debugPrint('[FIT] write FAILED for one file: $lastError',
        );
            }
          }
      }

      state = state.copyWith(
        progress: RouteBulkImportProgress(
          totalFiles: totalFiles,
          importedFiles: importedFiles,
          failedFiles: failedFiles,
          skippedFiles: skippedFiles,
          currentFileIndex: state.progress?.currentFileIndex ?? 0,
        ),
      );
    }

    for (var index = 0; index < files.length; index++) {
      if (rateLimited) break;

      state = state.copyWith(
        progress: RouteBulkImportProgress(
          totalFiles: totalFiles,
          importedFiles: importedFiles,
          failedFiles: failedFiles,
          skippedFiles: skippedFiles,
          currentFileIndex: index + 1,
        ),
      );

      try {
        // Opened HERE, one file at a time — see [ActivityRouteFileSource]. The
        // bytes go out of scope with the iteration, so a folder of four hundred
        // files costs the heap one file, not four hundred. Only the parsed write
        // request is held on for the batch, which is small.
        final file = files[index];
        final bytes = await file.read();

        // Wellness FIT types take the import-records path, not the activity path.
        // A type-49 (sleep) / type-68 (HRV) file has no session or route, so the
        // activity parser would (correctly) reject it — here it becomes Health
        // Connect records. One decode pass yields whichever the file carried.
        if (isFitFile(bytes)) {
          final wellness =
              FitRouteParser.parseWellness(bytes, fileName: file.fileName);
          final records = <ImportRecord>[
            if (wellness.sleep != null) ...fitSleepImportRecords(wellness.sleep!),
            if (wellness.hrv != null) ...fitHrvImportRecords(wellness.hrv!),
            if (wellness.monitoring != null)
              ...fitMonitoringImportRecords(wellness.monitoring!),
            if (wellness.metrics != null)
              ...fitMetricsImportRecords(wellness.metrics!),
            ...fitNapImportRecords(wellness.naps),
            if (wellness.healthSnapshot != null)
              ...fitHealthSnapshotImportRecords(wellness.healthSnapshot!),
          ];
          if (records.isNotEmpty) {
            wellnessPending.add(records);
            debugPrint('[FIT] queued wellness file=${file.fileName ?? "?"} '
                'records=${records.length}');
            if (wellnessPending.length >= _maxPendingFiles) {
              await writeWellnessPending();
            }
          }
          // The activity type decides the path, NOT whether wellness data was
          // found. An activity file carries wellness messages of its own — a
          // Garmin writes VO2 max and recovery time into the workout it just
          // recorded — so branching on "did this yield wellness records" sent a
          // real 6.5 KB workout down the wellness path and dropped it.
          if (!wellness.isActivityType) {
            if (records.isEmpty) {
              // Decoded fine, but nothing Health Connect holds — a metrics file
              // carrying only sleep pressure, say, whose numbers the app keeps
              // in its own table. A skip, not a failure.
              skippedFiles += 1;
              debugPrint('[FIT] skipped file=${file.fileName ?? "?"} '
                  'type=${wellness.fileType} (nothing for Health Connect)');
            }
            continue;
          }
        }

        final routeImport = await importer.import(
          ActivityRouteFileHandle(bytes: bytes, fileName: file.fileName),
        );
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
        pending.add(request);
        pendingRoutePoints += request.routePoints.length;
        // DIAGNOSTIC: a file that parsed and became a write request — the success
        // path the UI never itemises.
        debugPrint('[FIT] queued file=${files[index].fileName ?? "?"} '
          'routePoints=${request.routePoints.length}',
        );
        if (pending.length >= _maxPendingFiles ||
            pendingRoutePoints >= _maxPendingRoutePoints) {
          await writePending();
        }
      } catch (error) {
        // The route parser still throws (it is not a repository), so a malformed
        // file lands here — and so does one that could not be OPENED, which a
        // folder import can hit when a file moves between the scan and its turn.
        failedFiles += 1;
        lastError = _describeError(error);
        // DIAGNOSTIC: the per-file failure reason the UI collapses into a single
        // "last error" line. One line per rejected file, attributable by name.
        debugPrint('[FIT] FAILED file=${files[index].fileName ?? "?"} reason="$lastError"',
        );
      }
    }

    await writePending();
    await writeWellnessPending();

    // DIAGNOSTIC: the run's bottom line, matching the card's "Imported X. …".
    debugPrint('[FIT] run complete: total=$totalFiles imported=$importedFiles '
      'skipped=$skippedFiles failed=$failedFiles rateLimited=$rateLimited '
      'lastError="$lastError"',
        );

    state = RouteBulkImportState(
      isImporting: false,
      result: RouteBulkImportResult(
        totalFiles: totalFiles,
        importedFiles: importedFiles,
        failedFiles: failedFiles,
        skippedFiles: skippedFiles,
      ),
      // Shown whenever anything went wrong, not only when a FILE failed: a run cut
      // short by the quota can have zero failed files and still owe the user an
      // explanation for the ones it never got to.
      error: lastError,
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
  String _describeFailure(AppFailure failure) {
    // Health Connect's own words here are a wall of stack trace ("IllegalStateException:
    // ... availableQuota: 0.70882547 requested: 1"). What the user needs to know is
    // that nothing is wrong with their files and the import can simply be resumed.
    if (failure is RateLimitFailure) {
      return 'Health Connect has hit its limit on how much can be written at once. '
          'The files that remain are fine — run the import again later to continue.';
    }
    return switch (failure.toScreenError(fallback: _fallbackError)) {
      ScreenErrorPermissionDenied() =>
        'Activity import write permissions are missing.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() || ScreenErrorMissingArgument() => _fallbackError,
    };
  }

  /// A thrown parse/conversion failure (the route parser is not a repository and
  /// still throws).
  String _describeError(Object error) {
    if (error is RouteImportException) return error.message;
    final message = error.toString();
    return message.isEmpty ? _fallbackError : message;
  }
}

const String _fallbackError = 'Route import failed.';

/// Activities per Health Connect call.
///
/// The quota is charged per CALL, so bigger is cheaper: twenty-five files cost one
/// unit of quota instead of twenty-five, which turns a folder of a few thousand from
/// "exhausts the daily allowance partway through" into a few dozen calls.
///
/// It cannot simply be raised, though, and the limit is MEMORY, not quota. The
/// importer holds every batched activity until the batch is written, and a route
/// file's GPS track is the fat part of it — so the batch, not the file, is now what
/// has to fit in memory. See [_maxPendingRoutePoints], which is the real bound; this
/// is only the ceiling for files that carry no route at all.
const int _maxPendingFiles = 25;

/// Route points per Health Connect call.
///
/// The honest bound on a batch. A folder of FIT files is not twenty-five uniform
/// things: one ride can carry ten thousand GPS points and the next a hundred. Sizing
/// the batch by file count alone would make peak memory a function of whichever files
/// the user happened to pick — which is exactly the OOM the one-file-at-a-time reader
/// was written to avoid (see [ActivityRouteFileSource]).
///
/// So the batch flushes on whichever bound comes first. It also keeps the payload of
/// a single call well clear of Health Connect's own per-call size ceiling.
const int _maxPendingRoutePoints = 50000;

final routeBulkImportProvider =
    NotifierProvider<RouteBulkImportViewModel, RouteBulkImportState>(
  RouteBulkImportViewModel.new,
);
