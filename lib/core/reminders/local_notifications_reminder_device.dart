import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;

import 'reminder_controller.dart';

/// The per-feature identity and copy of a reminder notification. Everything a
/// feature needs to customize about how its reminder looks lives here, so the
/// device adapters themselves stay shared.
class ReminderNotificationSpec {
  const ReminderNotificationSpec({
    required this.notificationId,
    required this.channelId,
    required this.channelName,
    required this.channelDescription,
    required this.title,
    required this.androidIcon,
    required this.scheduledBody,
    required this.body,
  });

  /// Must be unique per feature — two reminders sharing an id overwrite and
  /// cancel each other.
  final int notificationId;
  final String channelId;
  final String channelName;
  final String channelDescription;
  final String title;

  /// The `res/drawable` name of this reminder's monochrome status-bar icon.
  /// Android tints the small icon from its alpha channel, so this must be an
  /// alpha-only silhouette (e.g. `ic_stat_hydration_reminder`); a full-color
  /// launcher icon here renders as a blank white square.
  final String androidIcon;

  /// Used only by [ZonedNotificationReminderScheduler], which posts a
  /// notification ahead of time and so cannot know the user's progress.
  final String scheduledBody;

  /// Used by [LocalNotificationsReminderDevice.show], which is called from the
  /// alarm callback and does know today's progress.
  final String Function(ReminderGoalProgress progress) body;
}

NotificationDetails _detailsFor(ReminderNotificationSpec spec) =>
    NotificationDetails(
      android: AndroidNotificationDetails(
        spec.channelId,
        spec.channelName,
        channelDescription: spec.channelDescription,
        icon: spec.androidIcon,
        importance: Importance.defaultImportance,
        priority: Priority.defaultPriority,
      ),
      iOS: const DarwinNotificationDetails(),
    );

/// Posts and clears a reminder notification via `flutter_local_notifications`.
/// A feature wires one of these with its own [ReminderNotificationSpec].
class LocalNotificationsReminderDevice implements ReminderNotifier {
  const LocalNotificationsReminderDevice({
    required this.plugin,
    required this.spec,
  });

  final FlutterLocalNotificationsPlugin plugin;
  final ReminderNotificationSpec spec;

  @override
  Future<void> show(ReminderGoalProgress progress) => plugin.show(
        id: spec.notificationId,
        title: spec.title,
        body: spec.body(progress),
        notificationDetails: _detailsFor(spec),
      );

  @override
  Future<void> cancel() => plugin.cancel(id: spec.notificationId);
}

/// Posts the reminder as a notification scheduled ahead of time.
///
/// The fallback for platforms with no alarm manager (iOS). It cannot re-read
/// progress at fire time, so it shows [ReminderNotificationSpec.scheduledBody]
/// and will notify even if the user has since met the goal. On Android prefer
/// `AlarmManagerReminderScheduler`, which wakes the app and re-checks — the
/// model the Kotlin app uses.
///
/// Requires `tz.local` to be set; see `initializeReminderTimeZone`. Without it
/// `tz.local` is UTC and the reminder fires at the wrong wall-clock time.
class ZonedNotificationReminderScheduler implements ReminderScheduler {
  const ZonedNotificationReminderScheduler({
    required this.plugin,
    required this.spec,
  });

  final FlutterLocalNotificationsPlugin plugin;
  final ReminderNotificationSpec spec;

  @override
  Future<void> schedule(DateTime triggerAt) => plugin.zonedSchedule(
        id: spec.notificationId,
        title: spec.title,
        body: spec.scheduledBody,
        scheduledDate: tz.TZDateTime.from(triggerAt, tz.local),
        notificationDetails: _detailsFor(spec),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      );

  @override
  Future<void> cancel() => plugin.cancel(id: spec.notificationId);
}
