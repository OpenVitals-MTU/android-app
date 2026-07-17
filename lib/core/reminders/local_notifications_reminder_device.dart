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
        // High so the reminder heads-up instead of appearing silently in the
        // shade. Must match the channel created by [ensureReminderChannel] — once
        // Android creates a channel its importance is fixed, and details cannot
        // raise it above the channel's level.
        importance: Importance.high,
        priority: Priority.high,
      ),
      iOS: const DarwinNotificationDetails(),
    );

/// Creates [spec]'s Android notification channel with high importance so the
/// reminder heads-up, deleting a superseded [oldChannelId] first.
///
/// A channel's importance is locked once Android creates it, so raising it for
/// existing installs — which may already hold the old default-importance channel —
/// requires a NEW channel id and deleting the old one. Best-effort, idempotent,
/// and Android-only (a no-op elsewhere, and safe to call from a background
/// isolate). Run it before the first `show()` on either isolate.
Future<void> ensureReminderChannel(
  FlutterLocalNotificationsPlugin plugin,
  ReminderNotificationSpec spec, {
  String? oldChannelId,
}) async {
  try {
    final android = plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android == null) return;
    if (oldChannelId != null && oldChannelId != spec.channelId) {
      await android.deleteNotificationChannel(channelId: oldChannelId);
    }
    await android.createNotificationChannel(
      AndroidNotificationChannel(
        spec.channelId,
        spec.channelName,
        description: spec.channelDescription,
        importance: Importance.high,
      ),
    );
  } catch (_) {
    // Best-effort: a host without the channel API (a unit test) must not break
    // startup. The channel is also created lazily on first show() as a fallback.
  }
}

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
