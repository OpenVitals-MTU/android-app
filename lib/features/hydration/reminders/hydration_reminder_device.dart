import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_controller.dart';

/// The hydration reminder's notification identity and copy. The behaviour lives
/// in the shared [LocalNotificationsReminderDevice].
///
/// [ReminderNotificationSpec.notificationId] must stay unique across features.
const hydrationReminderNotificationSpec = ReminderNotificationSpec(
  notificationId: 5001,
  channelId: 'hydration_reminders',
  channelName: 'Hydration reminders',
  channelDescription: 'Reminders to drink water throughout the day.',
  title: 'Time to hydrate',
  scheduledBody: 'Log some water to stay on track with your daily goal.',
  body: _hydrationReminderBody,
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
