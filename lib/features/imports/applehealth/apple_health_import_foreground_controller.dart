/// Starts (and detects) the Apple Health import's foreground service — the
/// Flutter analogue of Kotlin's `AppleHealthImportWorkController`, which
/// enqueues the unique `apple_health_import` work and observes its `WorkInfo`.
///
/// WorkManager can run any number of workers side by side. `flutter_foreground_task`
/// cannot: the app declares exactly ONE `ForegroundService` in the manifest, and
/// `startService` throws `ServiceAlreadyStartedException` when it is already up.
/// So an Apple Health import and a GPS activity recording can never hold it at
/// the same time, and the collision has to be refused explicitly in both
/// directions:
///
///   * an import refuses to start while a recording owns the service
///     ([AppleHealthImportLaunch.serviceBusy]);
///   * the recording controller keeps its own `startService` path untouched — it
///     already refreshes an existing service instead of double-starting, and the
///     import never steals a service it did not start.
///
/// [kAppleHealthImportKeyActive] is what tells the two apart: it is written
/// before the import's service comes up and cleared when its task ends, so a
/// *running* service with the flag set is our import (re-attach), and one
/// without it belongs to something else (refuse).
library;

import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'apple_health_import_background.dart';
import 'apple_health_import_models.dart';
import 'apple_health_import_notification.dart';
import 'apple_health_import_task_handler.dart';

/// The outcome of asking for the import's foreground service.
enum AppleHealthImportLaunch {
  /// The service is up; progress now arrives through the task-data callback.
  started,

  /// No foreground service on this platform (tests, desktop, a missing plugin).
  /// The caller falls back to importing in-process.
  unavailable,

  /// Another feature (an activity recording) owns the app's single service.
  serviceBusy,

  /// Our own import is already running; the caller re-attaches to it.
  alreadyImporting,
}

/// What the service isolate needs to run one import.
class AppleHealthImportRequest {
  const AppleHealthImportRequest({
    required this.stagedFilePath,
    required this.sourceKey,
    required this.selectedCategories,
    required this.expectedSelectedRecords,
    required this.expectedParsedElements,
  });

  final String stagedFilePath;
  final String sourceKey;
  final Set<AppleHealthImportCategory> selectedCategories;
  final int expectedSelectedRecords;
  final int expectedParsedElements;
}

/// `true` when the app's single foreground service is currently running an Apple
/// Health import.
///
/// Read by the *activity recorder* before it starts (or refreshes) the service:
/// without this it would mistake a running import's service for its own
/// process-death restart, overwrite the import's notification with the
/// recording's, and then lose the service entirely when the import finishes and
/// calls `stopService`.
Future<bool> appleHealthImportOwnsForegroundService() async {
  try {
    if (!await FlutterForegroundTask.isRunningService) return false;
    return await FlutterForegroundTask.getData<bool>(
          key: kAppleHealthImportKeyActive,
        ) ==
        true;
  } catch (_) {
    // No plugin (tests / non-Android): nothing owns anything.
    return false;
  }
}

/// Seam over the foreground service, so the notifier's collision handling and
/// its in-process fallback are testable without the plugin.
abstract class AppleHealthImportServiceController {
  Future<AppleHealthImportLaunch> start(AppleHealthImportRequest request);

  /// `true` when a foreground-service import is in flight right now (used to
  /// re-attach the UI after the app is relaunched mid-import).
  Future<bool> isImportRunning();
}

/// The real controller, over `flutter_foreground_task`.
class ForegroundAppleHealthImportServiceController
    implements AppleHealthImportServiceController {
  const ForegroundAppleHealthImportServiceController();

  @override
  Future<bool> isImportRunning() => appleHealthImportOwnsForegroundService();

  @override
  Future<AppleHealthImportLaunch> start(AppleHealthImportRequest request) async {
    final bool serviceRunning;
    try {
      serviceRunning = await FlutterForegroundTask.isRunningService;
    } catch (_) {
      return AppleHealthImportLaunch.unavailable;
    }
    if (serviceRunning) {
      // The app has a single ForegroundService: either it is already ours, or an
      // activity recording is holding it and this import must wait.
      return await isImportRunning()
          ? AppleHealthImportLaunch.alreadyImporting
          : AppleHealthImportLaunch.serviceBusy;
    }

    try {
      final l10n = appleHealthImportLocalizations();
      // `startService` takes no payload, so the task's inputs are handed over
      // through the plugin's (SharedPreferences-backed) store. `saveData` only
      // persists primitives — the category set travels as a joined String.
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyStagedPath,
        value: request.stagedFilePath,
      );
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeySourceKey,
        value: request.sourceKey,
      );
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyCategories,
        value: encodeAppleHealthImportCategories(request.selectedCategories),
      );
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyExpectedSelectedRecords,
        value: request.expectedSelectedRecords,
      );
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyExpectedParsedElements,
        value: request.expectedParsedElements,
      );
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyActive,
        value: true,
      );

      // `startService` throws `ServiceNotInitializedException` unless `init` ran
      // first, so the two must stay together.
      FlutterForegroundTask.init(
        androidNotificationOptions: AndroidNotificationOptions(
          channelId: kAppleHealthImportChannelId,
          channelName: l10n.settingsAppleHealthImportNotificationChannel,
          channelImportance: NotificationChannelImportance.LOW,
          priority: NotificationPriority.LOW,
          onlyAlertOnce: true,
        ),
        iosNotificationOptions: const IOSNotificationOptions(
          showNotification: true,
          playSound: false,
        ),
        foregroundTaskOptions: ForegroundTaskOptions(
          // The import drives itself from `onStart`; it needs no repeat events.
          eventAction: ForegroundTaskEventAction.nothing(),
          allowWakeLock: true,
          autoRunOnBoot: false,
        ),
      );
      final result = await FlutterForegroundTask.startService(
        // Kotlin's FOREGROUND_SERVICE_TYPE_DATA_SYNC.
        serviceTypes: const [ForegroundServiceTypes.dataSync],
        notificationTitle: l10n.settingsAppleHealthImportNotificationTitle,
        notificationText: appleHealthImportNotificationText(
          l10n,
          AppleHealthImportProgress(
            expectedSelectedRecords: request.expectedSelectedRecords,
            expectedParsedElements: request.expectedParsedElements,
          ),
        ),
        callback: appleHealthImportTaskCallback,
      );
      if (result is ServiceRequestFailure) {
        await _markInactive();
        debugPrint(
          'Apple Health import foreground service failed: ${result.error}',
        );
        return AppleHealthImportLaunch.unavailable;
      }
      return AppleHealthImportLaunch.started;
    } catch (error) {
      // The import still works in-process while the app is open, so degrade
      // rather than failing the import outright.
      await _markInactive();
      debugPrint('Apple Health import foreground service failed: $error');
      return AppleHealthImportLaunch.unavailable;
    }
  }

  Future<void> _markInactive() async {
    try {
      await FlutterForegroundTask.saveData(
        key: kAppleHealthImportKeyActive,
        value: false,
      );
    } catch (_) {
      // Best effort.
    }
  }
}

final appleHealthImportServiceControllerProvider =
    Provider<AppleHealthImportServiceController>(
  (ref) => const ForegroundAppleHealthImportServiceController(),
);
