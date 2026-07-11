import 'package:flutter_foreground_task/flutter_foreground_task.dart';

/// Notification-action command ids, the port of the `ActionPause` /
/// `ActionResume` / `ActionDiscard` service intents that Kotlin's
/// `ActivityRecordingService.onStartCommand` handles. The foreground-service
/// isolate relays a pressed button's id to the main isolate, where the
/// recording controller executes the matching command.
const String kActivityRecordingActionPause = 'pause_activity_recording';
const String kActivityRecordingActionResume = 'resume_activity_recording';
const String kActivityRecordingActionDiscard = 'discard_activity_recording';

@pragma('vm:entry-point')
void activityRecordingTaskCallback() {
  FlutterForegroundTask.setTaskHandler(_ActivityRecordingTaskHandler());
}

/// Runs in the foreground-service isolate. The recorder itself lives in the
/// main isolate (driven by the geolocator/sensor streams there), so the only
/// job here is relaying notification-button presses across.
class _ActivityRecordingTaskHandler extends TaskHandler {
  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {}

  @override
  void onRepeatEvent(DateTime timestamp) {}

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {}

  @override
  void onNotificationButtonPressed(String id) {
    FlutterForegroundTask.sendDataToMain(id);
  }
}
