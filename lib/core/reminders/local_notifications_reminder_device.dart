import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;

import 'reminder_controller.dart';

/// The per-feature identity and copy of a reminder notification. Everything a
/// feature needs to customize about how its reminder looks lives here, so the
/// device adapters themselves stay shared.
class ReminderNotificationSpec {
  const ReminderNotificationSpec({
    required this.baseNotificationId,
    required this.batchSize,
    required this.channelId,
    required this.channelName,
    required this.channelDescription,
    required this.title,
    required this.androidIcon,
    required this.scheduledBody,
    required this.body,
    this.tapRoute,
  });

  /// The first id of this feature's reserved, contiguous notification-id range
  /// `[baseNotificationId, baseNotificationId + batchSize)`. Ranges must not
  /// overlap between features — a batch (re)schedule cancels its whole range, so
  /// an overlap would let one reminder wipe another's pending notifications.
  final int baseNotificationId;

  /// How many upcoming reminders may be pre-scheduled at once — bounds the id
  /// range above and the per-(re)schedule work. Keep the sum across all features
  /// under the iOS 64-pending-notification cap.
  final int batchSize;

  final String channelId;
  final String channelName;
  final String channelDescription;
  final String title;

  /// The `res/drawable` name of the monochrome status-bar icon — the shared
  /// OpenVitals mark (`ic_launcher_monochrome`) for every reminder. Android tints
  /// the small icon from its alpha channel, so this must be an alpha-only
  /// silhouette; a full-color launcher icon here renders as a blank white square.
  final String androidIcon;

  /// The body for entries that cannot show live progress: future-day reminders
  /// (whose value on that day is unknown at schedule time) and reminders with no
  /// goal set.
  final String scheduledBody;

  /// Today's-progress body, e.g. "1.3 L / 2.0 L". Used only for SAME-DAY entries
  /// when a goal is set — it reflects progress as of scheduling, which is why it
  /// is never used for a future day (whose numbers would be stale after midnight).
  final String Function(ReminderGoalProgress progress) body;

  /// The go_router location to open when the notification is tapped (carried as
  /// the notification payload), or null to just bring the app forward.
  final String? tapRoute;
}

NotificationDetails _detailsFor(
  ReminderNotificationSpec spec, {
  int? maxProgress,
  int? progress,
}) =>
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
        // A plain determinate bar showing today's progress toward the goal.
        showProgress: maxProgress != null,
        maxProgress: maxProgress ?? 0,
        progress: progress ?? 0,
      ),
      iOS: const DarwinNotificationDetails(),
    );

/// Creates [spec]'s Android notification channel with high importance so the
/// reminder heads-up, deleting a superseded [oldChannelId] first.
///
/// A channel's importance is locked once Android creates it, so raising it for
/// existing installs — which may already hold the old default-importance channel —
/// requires a NEW channel id and deleting the old one. Best-effort, idempotent,
/// and Android-only (a no-op elsewhere). Run it before the first schedule.
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
    // startup. The channel is also created lazily on first schedule as a fallback.
  }
}

/// Schedules a feature's upcoming reminders as a batch of pre-scheduled
/// `flutter_local_notifications` entries, one per plan instant, over the feature's
/// reserved id range.
///
/// This is the single scheduler for every platform. Pre-scheduled notifications
/// are re-armed by the OS across reboot *and* app update (via the plugin's
/// `ScheduledNotificationBootReceiver`, which listens for both `BOOT_COMPLETED`
/// and `MY_PACKAGE_REPLACED`), which is why the reminder chain now survives a
/// nightly update — unlike the old one-shot alarm, which Android cancelled on
/// package replace with nothing to re-arm it.
///
/// Requires `tz.local` to be set; see `initializeReminderTimeZone`. Without it
/// `tz.local` is UTC and reminders fire at the wrong wall-clock time.
class BatchZonedNotificationReminderScheduler implements ReminderScheduler {
  const BatchZonedNotificationReminderScheduler({
    required this.plugin,
    required this.spec,
    this.canScheduleExact,
    this.now = DateTime.now,
  });

  final FlutterLocalNotificationsPlugin plugin;
  final ReminderNotificationSpec spec;

  /// Used to decide which triggers fire "today" (and so may show today's live
  /// progress). Injectable for tests.
  final DateTime Function() now;

  /// Scales litres/minutes to the integer progress bar (0.01 resolution).
  static const int _progressScale = 100;

  /// Resolves whether an EXACT alarm may be used right now — SCHEDULE_EXACT_ALARM
  /// (Android 12+) granted, or implicit below 12. Consulted on EVERY (re)schedule,
  /// never cached, because the permission can be revoked between runs. Null or
  /// false → inexact, Doze-surviving delivery (a window rather than the instant);
  /// never dropped.
  final Future<bool> Function()? canScheduleExact;

  @override
  Future<void> scheduleAll(
    List<DateTime> triggers,
    ReminderGoalProgress progress,
  ) async {
    // Cancel the whole reserved range FIRST: a plan shorter than last time (e.g.
    // the goal was met, so today is now empty) must not leave stale entries from
    // the previous, longer plan firing later.
    await cancel();
    final exact = canScheduleExact != null && await canScheduleExact!();
    final mode = exact
        ? AndroidScheduleMode.exactAllowWhileIdle
        : AndroidScheduleMode.inexactAllowWhileIdle;

    final today = now();
    final hasGoal = progress.target > 0;
    final maxProgress = hasGoal ? (progress.target * _progressScale).round() : 0;
    final currentProgress = hasGoal
        ? (progress.current * _progressScale).round().clamp(0, maxProgress)
        : 0;

    final count =
        triggers.length < spec.batchSize ? triggers.length : spec.batchSize;
    for (var i = 0; i < count; i++) {
      final trigger = triggers[i];
      // Live progress only makes sense for a reminder that fires today: a future
      // day's reminder would otherwise show today's numbers after midnight. The
      // next app foreground re-plans those days with their own fresh progress.
      final firesToday = trigger.year == today.year &&
          trigger.month == today.month &&
          trigger.day == today.day;
      final showProgress = firesToday && hasGoal;
      await plugin.zonedSchedule(
        id: spec.baseNotificationId + i,
        title: spec.title,
        body: showProgress ? spec.body(progress) : spec.scheduledBody,
        scheduledDate: tz.TZDateTime.from(trigger, tz.local),
        notificationDetails: showProgress
            ? _detailsFor(spec, maxProgress: maxProgress, progress: currentProgress)
            : _detailsFor(spec),
        androidScheduleMode: mode,
        payload: spec.tapRoute,
      );
    }
  }

  @override
  Future<void> cancel() async {
    for (var i = 0; i < spec.batchSize; i++) {
      await plugin.cancel(id: spec.baseNotificationId + i);
    }
  }
}
