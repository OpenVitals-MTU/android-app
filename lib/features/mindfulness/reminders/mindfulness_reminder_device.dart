import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_controller.dart';

/// The mindfulness reminder's notification identity and copy. The behaviour
/// lives in the shared [LocalNotificationsReminderDevice].
const mindfulnessReminderNotificationSpec = ReminderNotificationSpec(
  notificationId: 5002,
  // `_v2`: see [hydrationReminderNotificationSpec] — a new id upgrades existing
  // installs to the high-importance channel.
  channelId: 'mindfulness_reminders_v2',
  channelName: 'Mindfulness reminders',
  channelDescription: 'A daily nudge to take mindful minutes.',
  title: 'Take a mindful moment',
  androidIcon: 'ic_stat_mindfulness_reminder',
  scheduledBody: 'A few mindful minutes can reset your day.',
  body: _mindfulnessReminderBody,
);

/// The pre-`_v2` channel id, deleted when the high-importance channel is created.
const _legacyMindfulnessChannelId = 'mindfulness_reminders';

/// Creates the mindfulness reminder's high-importance channel and removes the
/// legacy default-importance one. Call before the first `show()`.
Future<void> ensureMindfulnessReminderChannel(
  FlutterLocalNotificationsPlugin plugin,
) =>
    ensureReminderChannel(
      plugin,
      mindfulnessReminderNotificationSpec,
      oldChannelId: _legacyMindfulnessChannelId,
    );

String _mindfulnessReminderBody(ReminderGoalProgress progress) => progress.target > 0.0
    ? 'You have ${progress.current.toStringAsFixed(0)} of '
        '${progress.target.toStringAsFixed(0)} mindful minutes today.'
    : 'Take a few mindful minutes.';

LocalNotificationsReminderDevice mindfulnessReminderDevice(
  FlutterLocalNotificationsPlugin plugin,
) =>
    LocalNotificationsReminderDevice(
      plugin: plugin,
      spec: mindfulnessReminderNotificationSpec,
    );
