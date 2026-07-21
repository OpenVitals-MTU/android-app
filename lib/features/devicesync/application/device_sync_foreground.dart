/// Foreground-service keep-alive for a phone-to-phone sync transfer.
///
/// Unlike the Apple Health import (which runs its whole job inside the
/// foreground-service isolate), the sync transfer MUST stay in the main isolate:
/// the `bluetooth_sync_native` plugin is attached to the main Flutter engine, so
/// its RFCOMM byte events (`onBytesReceived`) can only be delivered there. This
/// service therefore does no work of its own — its task handler is a no-op. It
/// exists solely to promote the process to the foreground for the duration of a
/// transfer so the OS does not kill the app if the user glances away mid-sync.
///
/// The app declares a single foreground service, so if one is already running
/// (e.g. a GPS activity recording) [startDeviceSyncForegroundService] returns
/// false and the transfer proceeds in-process without one.
library;

import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';

import '../../../l10n/app_localizations.dart';

const String _channelId = 'openvitals_device_sync';

/// Registered as the service isolate entry point; must carry the pragma or
/// tree-shaking drops it and the service fails to start.
@pragma('vm:entry-point')
void deviceSyncTaskCallback() {
  FlutterForegroundTask.setTaskHandler(_DeviceSyncTaskHandler());
}

/// A deliberately inert handler — see the library doc: the transfer runs in the
/// main isolate; this service only keeps the process foregrounded.
class _DeviceSyncTaskHandler extends TaskHandler {
  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {}

  @override
  void onRepeatEvent(DateTime timestamp) {}

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {}
}

AppLocalizations _l10n() {
  try {
    return lookupAppLocalizations(PlatformDispatcher.instance.locale);
  } catch (_) {
    return lookupAppLocalizations(const Locale('en'));
  }
}

/// Whether THIS module started the currently-running foreground service. The app
/// has a single FGS slot shared with activity recording, so the stop side must
/// never tear down a service it didn't start (e.g. a live GPS recording).
bool _startedByUs = false;

/// Starts the keep-alive foreground service. Returns false (and starts nothing)
/// if a foreground service is already running or startup fails; the caller then
/// runs the transfer in-process.
Future<bool> startDeviceSyncForegroundService() async {
  _startedByUs = false;
  try {
    if (await FlutterForegroundTask.isRunningService) return false;
    final l10n = _l10n();
    // init + startService must stay together (startService throws
    // ServiceNotInitializedException otherwise).
    FlutterForegroundTask.init(
      androidNotificationOptions: AndroidNotificationOptions(
        channelId: _channelId,
        channelName: l10n.deviceSyncNotificationChannel,
        channelImportance: NotificationChannelImportance.LOW,
        priority: NotificationPriority.LOW,
        onlyAlertOnce: true,
      ),
      iosNotificationOptions: const IOSNotificationOptions(
        showNotification: true,
        playSound: false,
      ),
      foregroundTaskOptions: ForegroundTaskOptions(
        eventAction: ForegroundTaskEventAction.nothing(),
        allowWakeLock: true,
        autoRunOnBoot: false,
      ),
    );
    final result = await FlutterForegroundTask.startService(
      serviceTypes: const [
        ForegroundServiceTypes.dataSync,
        ForegroundServiceTypes.connectedDevice,
      ],
      notificationTitle: l10n.deviceSyncNotificationTitle,
      notificationText: l10n.deviceSyncNotificationText,
      callback: deviceSyncTaskCallback,
    );
    _startedByUs = result is! ServiceRequestFailure;
    return _startedByUs;
  } catch (e) {
    debugPrint('[devicesync] foreground service start failed: $e');
    return false;
  }
}

Future<void> stopDeviceSyncForegroundService() async {
  // Only stop a service WE started. Otherwise a sync that ran in-process (because
  // a GPS recording already held the single FGS slot) would, on teardown, kill
  // that unrelated recording service.
  if (!_startedByUs) return;
  try {
    if (await FlutterForegroundTask.isRunningService) {
      await FlutterForegroundTask.stopService();
    }
  } catch (_) {
    // best effort
  } finally {
    _startedByUs = false;
  }
}
