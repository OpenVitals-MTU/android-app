import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_controller.dart';
import '../../../navigation/app_routes.dart';

/// The hydration reminder's notification identity and copy. The scheduling
/// behaviour lives in the shared [BatchZonedNotificationReminderScheduler].
///
/// The id range `[6000, 6048)` is reserved for hydration's pre-scheduled batch;
/// it must not overlap the mindfulness range (`[6100, …)`).
const hydrationReminderNotificationSpec = ReminderNotificationSpec(
  baseNotificationId: 6000,
  // Enough upcoming reminders to cover ~1.5 days even at the 30-minute minimum
  // interval, so the chain keeps firing with no user interaction until the next
  // app foreground re-plans it. Kept under the iOS 64-pending cap once combined
  // with mindfulness's small batch.
  batchSize: 48,
  // `_v2`: the original `hydration_reminders` channel was created at default
  // importance, which is locked once set. A new id lets existing installs get the
  // high-importance (heads-up) channel — see [ensureHydrationReminderChannel].
  channelId: 'hydration_reminders_v2',
  channelName: 'Hydration reminders',
  channelDescription: 'Reminders to drink water throughout the day.',
  title: 'Time to hydrate',
  // The OpenVitals monochrome mark, shared by every notification the app posts.
  androidIcon: 'ic_launcher_monochrome',
  scheduledBody: 'Log some water to stay on track with your daily goal.',
  // Today's progress, e.g. "1.3 L / 2.0 L", shown on same-day reminders.
  body: _hydrationReminderBody,
  // Tapping the reminder opens the hydration entry screen to log a drink.
  tapRoute: AppRoutes.hydrationEntry,
);

String _hydrationReminderBody(ReminderGoalProgress progress) =>
    '${progress.current.toStringAsFixed(1)} L / '
    '${progress.target.toStringAsFixed(1)} L';

/// The pre-`_v2` channel id, deleted when the high-importance channel is created.
const _legacyHydrationChannelId = 'hydration_reminders';

/// Creates the hydration reminder's high-importance channel and removes the
/// legacy default-importance one. Call before the first schedule (startup).
Future<void> ensureHydrationReminderChannel(
  FlutterLocalNotificationsPlugin plugin,
) =>
    ensureReminderChannel(
      plugin,
      hydrationReminderNotificationSpec,
      oldChannelId: _legacyHydrationChannelId,
    );
