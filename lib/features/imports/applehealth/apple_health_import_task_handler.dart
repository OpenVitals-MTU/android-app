/// The foreground-service isolate that runs the Apple Health import — the
/// Flutter port of Kotlin's `AppleHealthImportWorker`.
///
/// The Kotlin importer runs inside a WorkManager `CoroutineWorker` promoted to a
/// foreground service (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`) so a multi-gigabyte
/// export keeps importing while the user leaves the app. Flutter has no
/// WorkManager equivalent, so the import runs in `flutter_foreground_task`'s
/// service isolate instead, with the same ongoing progress notification.
///
/// The isolate is fresh: no `main()` has run, no Riverpod container exists and
/// no drift database is open. It therefore registers the plugins itself and
/// builds the (small) import graph by hand — exactly like
/// `hydration_reminder_alarm.dart`. Inputs arrive through
/// `FlutterForegroundTask.saveData` (there is no payload parameter on
/// `startService`); progress, the result and any error go back to the UI through
/// `sendDataToMain`.
///
/// Everything the handler *does* lives in [runAppleHealthImportJob] — a plain
/// async function over injected collaborators, because a `TaskHandler` cannot be
/// unit-tested. This file is the shell: read inputs, build the graph, throttle
/// the notification, ship the outcome, stop the service. An exception escaping
/// here kills the service isolate outright, so every step is wrapped.
library;

import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../core/result/result.dart';
import '../../../data/repository/impl/apple_health_import_repository_impl.dart';
import '../../../data/repository/impl/health_repository_impl.dart';
import '../../../di/providers.dart' show openVitalsPackageName;
import '../../../data/source/health/health_data_source.dart';
import '../../../data/source/health/native/health_connect_native_data_source.dart';
import '../../../l10n/app_localizations.dart';
import 'apple_health_import_background.dart';
import 'apple_health_import_checkpoint_store.dart';
import 'apple_health_import_error_formatter.dart';
import 'apple_health_import_models.dart';
import 'apple_health_import_notification.dart';
import 'apple_health_import_report_store.dart';
import 'apple_health_import_service.dart';
import 'apple_health_import_staging_store.dart';

/// The Kotlin worker's `ForegroundNotificationUpdateMillis` /
/// `WorkManagerProgressUpdateMillis`: at most one update per second, plus one on
/// every phase change.
const Duration _progressUpdateInterval = Duration(seconds: 1);

/// The service isolate's entry point. Must be a top-level function annotated
/// with `@pragma('vm:entry-point')`, or tree-shaking drops it and the raw
/// callback handle the plugin stored will not resolve.
@pragma('vm:entry-point')
void appleHealthImportTaskCallback() {
  FlutterForegroundTask.setTaskHandler(AppleHealthImportTaskHandler());
}

@visibleForTesting
class AppleHealthImportTaskHandler extends TaskHandler {
  AppLocalizations _l10n = lookupAppLocalizations(const Locale('en'));
  int _expectedParsedElements = 0;
  DateTime? _lastUpdate;
  AppleHealthImportPhase? _lastPhase;

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    try {
      // The isolate has no registered plugins of its own (shared_preferences and
      // path_provider register on the Dart side).
      DartPluginRegistrant.ensureInitialized();
      _l10n = appleHealthImportLocalizations();

      final stagedPath = await FlutterForegroundTask.getData<String>(
        key: kAppleHealthImportKeyStagedPath,
      );
      final sourceKey = await FlutterForegroundTask.getData<String>(
            key: kAppleHealthImportKeySourceKey,
          ) ??
          '';
      final selectedCategories = decodeAppleHealthImportCategories(
        await FlutterForegroundTask.getData<String>(
          key: kAppleHealthImportKeyCategories,
        ),
      );
      final expectedSelectedRecords = await FlutterForegroundTask.getData<int>(
            key: kAppleHealthImportKeyExpectedSelectedRecords,
          ) ??
          0;
      _expectedParsedElements = await FlutterForegroundTask.getData<int>(
            key: kAppleHealthImportKeyExpectedParsedElements,
          ) ??
          0;

      if (stagedPath == null || stagedPath.isEmpty) {
        throw AppleHealthImportException(
          'Missing staged Apple Health export path.',
        );
      }

      final HealthDataSource dataSource =
          HealthConnectNativeDataSource(appPackageName: openVitalsPackageName);
      final outcome = await runAppleHealthImportJob(
        AppleHealthImportJobInputs(
          service: AppleHealthImportService(
            AppleHealthImportRepositoryImpl(dataSource),
          ),
          stagingStore: AppleHealthImportStagingStore(),
          checkpointStore: AppleHealthImportCheckpointStore(),
          reportStore:
              AppleHealthImportReportStore(await SharedPreferences.getInstance()),
          // MUST resolve access before any write: `cachedAvailability` starts at
          // `notSupported` and the import repository gates every insert on it,
          // so without this the import writes nothing and still reports success.
          // The app gets this for free from `HealthConnectGate`; this isolate
          // has no widget tree.
          resolveHealthAccess: () async =>
              (await HealthRepositoryImpl(dataSource).refreshAvailability())
                  .orThrow(),
          stagedFile: File(stagedPath),
          sourceKey: sourceKey,
          selectedCategories: selectedCategories,
          expectedSelectedRecords: expectedSelectedRecords,
          expectedParsedElements: _expectedParsedElements,
        ),
        onProgress: _publishProgress,
      );

      final result = outcome.result;
      if (result != null) {
        _sendToMain(encodeAppleHealthImportProgress(
          appleHealthImportProgressOf(
            result,
            phase: AppleHealthImportPhase.complete,
            expectedSelectedRecords: expectedSelectedRecords,
            expectedParsedElements: _expectedParsedElements,
          ),
          event: kAppleHealthImportEventResult,
          workoutRoutesIncomplete: result.workoutRoutesIncomplete,
        ));
      } else {
        // The staged export and the checkpoint were kept by the job, so the next
        // run resumes from the last committed batch.
        _sendToMain(encodeAppleHealthImportError(outcome.error!));
      }
    } catch (error, stack) {
      debugPrint('Apple Health import task failed: $error\n$stack');
      _sendToMain(encodeAppleHealthImportError(error));
    } finally {
      await _finish();
    }
  }

  @override
  void onRepeatEvent(DateTime timestamp) {}

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {
    await _markInactive();
  }

  /// Kotlin's progress callback: throttled to one update per second (or an
  /// immediate one on a phase change), pushed to both the ongoing notification
  /// and the UI.
  void _publishProgress(AppleHealthImportProgress progress) {
    final now = DateTime.now();
    final last = _lastUpdate;
    final due = last == null ||
        now.difference(last) >= _progressUpdateInterval ||
        progress.phase != _lastPhase;
    if (!due) return;
    _lastUpdate = now;
    _lastPhase = progress.phase;

    // `runAppleHealthImportJob` re-seeds both expected totals onto every
    // progress it emits, so the snapshot is already self-describing.
    _sendToMain(encodeAppleHealthImportProgress(
      progress,
      event: kAppleHealthImportEventProgress,
    ));
    unawaited(FlutterForegroundTask.updateService(
      notificationTitle: _l10n.settingsAppleHealthImportNotificationTitle,
      notificationText: appleHealthImportNotificationText(_l10n, progress),
    ));
  }

  void _sendToMain(Object payload) {
    try {
      FlutterForegroundTask.sendDataToMain(payload);
    } catch (error) {
      // The UI may simply be gone; the report and the checkpoint are on disk.
      debugPrint('Apple Health import task could not reach the UI: $error');
    }
  }

  Future<void> _finish() async {
    await _markInactive();
    try {
      await FlutterForegroundTask.stopService();
    } catch (error) {
      debugPrint('Apple Health import service stop failed: $error');
    }
  }

  /// Releases the app's single foreground service back to the activity recorder
  /// (see `apple_health_import_foreground_controller.dart`).
  Future<void> _markInactive() async {
    try {
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyActive,
        value: false,
      );
    } catch (_) {
      // Best effort; `isRunningService` still gates the collision check.
    }
  }
}
