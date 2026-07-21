import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_controller.dart';

/// The mindfulness reminder's notification identity and copy. The scheduling
/// behaviour lives in the shared [BatchZonedNotificationReminderScheduler].
///
/// A single daily time pre-schedules two weeks of reminders, so it keeps firing
/// through a long absence (the old 3-day batch went silent over a weekend +
/// couple of days away). The id range `[6100, 6114)` must not overlap hydration's
/// `[6000, 6048)`, and 14 + hydration's 48 stays under the iOS 64-pending cap.
const mindfulnessReminderNotificationSpec = ReminderNotificationSpec(
  baseNotificationId: 6100,
  batchSize: 14,
  // `_v2`: see [hydrationReminderNotificationSpec] — a new id upgrades existing
  // installs to the high-importance channel.
  channelId: 'mindfulness_reminders_v2',
  channelName: 'Mindfulness reminders',
  channelDescription: 'A daily nudge to take mindful minutes.',
  title: 'Take a mindful moment',
  // The OpenVitals monochrome mark, shared by every notification the app posts.
  androidIcon: 'ic_launcher_monochrome',
  scheduledBody: 'A few mindful minutes can reset your day.',
  // Today's progress, e.g. "5 / 10 min", shown on same-day reminders.
  body: _mindfulnessReminderBody,
);

String _mindfulnessReminderBody(ReminderGoalProgress progress) =>
    '${progress.current.toStringAsFixed(0)} / '
    '${progress.target.toStringAsFixed(0)} min';

/// The pre-`_v2` channel id, deleted when the high-importance channel is created.
const _legacyMindfulnessChannelId = 'mindfulness_reminders';

/// Creates the mindfulness reminder's high-importance channel and removes the
/// legacy default-importance one. Call before the first schedule.
Future<void> ensureMindfulnessReminderChannel(
  FlutterLocalNotificationsPlugin plugin,
) =>
    ensureReminderChannel(
      plugin,
      mindfulnessReminderNotificationSpec,
      oldChannelId: _legacyMindfulnessChannelId,
    );
