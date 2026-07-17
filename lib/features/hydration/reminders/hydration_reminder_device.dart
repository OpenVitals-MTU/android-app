import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_controller.dart';

/// The hydration reminder's notification identity and copy. The behaviour lives
/// in the shared [LocalNotificationsReminderDevice].
///
/// [ReminderNotificationSpec.notificationId] must stay unique across features.
const hydrationReminderNotificationSpec = ReminderNotificationSpec(
  notificationId: 5001,
  // `_v2`: the original `hydration_reminders` channel was created at default
  // importance, which is locked once set. A new id lets existing installs get the
  // high-importance (heads-up) channel — see [ensureHydrationReminderChannel].
  channelId: 'hydration_reminders_v2',
  channelName: 'Hydration reminders',
  channelDescription: 'Reminders to drink water throughout the day.',
  title: 'Time to hydrate',
  androidIcon: 'ic_stat_hydration_reminder',
  scheduledBody: 'Log some water to stay on track with your daily goal.',
  body: _hydrationReminderBody,
);

/// The pre-`_v2` channel id, deleted when the high-importance channel is created.
const _legacyHydrationChannelId = 'hydration_reminders';

/// Creates the hydration reminder's high-importance channel and removes the
/// legacy default-importance one. Call before the first `show()` (startup + the
/// alarm isolate).
Future<void> ensureHydrationReminderChannel(
  FlutterLocalNotificationsPlugin plugin,
) =>
    ensureReminderChannel(
      plugin,
      hydrationReminderNotificationSpec,
      oldChannelId: _legacyHydrationChannelId,
    );

String _hydrationReminderBody(ReminderGoalProgress progress) =>
    progress.target > 0.0
    ? 'You have logged ${progress.current.toStringAsFixed(1)} L of '
        '${progress.target.toStringAsFixed(1)} L today.'
    : 'Log some water to stay hydrated.';

LocalNotificationsReminderDevice hydrationReminderDevice(
  FlutterLocalNotificationsPlugin plugin,
) =>
    LocalNotificationsReminderDevice(
      plugin: plugin,
      spec: hydrationReminderNotificationSpec,
    );
