import 'dart:ui' show Color, Locale, PlatformDispatcher;

import 'package:flutter/foundation.dart' show FlutterError;
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;

import '../../domain/preferences/app_language.dart';
import '../../l10n/app_localizations.dart';
import 'reminder_controller.dart';

/// The notification copy in the app's language, resolved at (re)schedule time.
///
/// Notifications are PRE-scheduled, so their text is baked when the batch is
/// written, not when it fires — every (re)schedule (startup, resume, settings,
/// a log, a quick-add) re-bakes it, so a language change reaches the shade on
/// the next re-plan. Falls back to the spec's English when no resolver is
/// available (a test without catalogs).
class ReminderLocalizedCopy {
  const ReminderLocalizedCopy({
    required this.title,
    required this.scheduledBody,
    required this.channelName,
    required this.channelDescription,
  });

  final String title;
  final String scheduledBody;
  final String channelName;
  final String channelDescription;
}

/// The catalog for the selected [AppLanguage] (`system` follows the OS locale),
/// resolvable without a BuildContext — usable from the background isolates that
/// reschedule after a quick-add.
AppLocalizations reminderLocalizationsFor(AppLanguage language) {
  final tag = language.languageTag;
  final locale = tag != null ? Locale(tag) : PlatformDispatcher.instance.locale;
  try {
    return lookupAppLocalizations(locale);
  } on FlutterError {
    return lookupAppLocalizations(const Locale('en'));
  }
}

ReminderLocalizedCopy _copyOf(
  ReminderNotificationSpec spec,
  AppLocalizations? l10n,
) {
  final localized = l10n != null ? spec.localizedCopy?.call(l10n) : null;
  return localized ??
      ReminderLocalizedCopy(
        title: spec.title,
        scheduledBody: spec.scheduledBody,
        channelName: spec.channelName,
        channelDescription: spec.channelDescription,
      );
}

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
    this.localizedCopy,
    this.tapRoute,
    this.accentColor,
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

  /// The copy above in the app's language. The plain String fields stay as the
  /// English fallback — see [ReminderLocalizedCopy].
  final ReminderLocalizedCopy Function(AppLocalizations l10n)? localizedCopy;

  /// The go_router location to open when the notification is tapped (carried as
  /// the notification payload), or null to just bring the app forward.
  final String? tapRoute;

  /// The feature's accent, applied as the Android notification color: it tints
  /// the small icon, the progress bar and — on most skins — the action-button
  /// text, which is what makes the quick-add actions read as buttons rather
  /// than plain text. Null keeps the system default.
  final Color? accentColor;
}

NotificationDetails _detailsFor(
  ReminderNotificationSpec spec,
  ReminderLocalizedCopy copy, {
  int? maxProgress,
  int? progress,
  List<AndroidNotificationAction> actions = const [],
}) =>
    NotificationDetails(
      android: AndroidNotificationDetails(
        spec.channelId,
        copy.channelName,
        channelDescription: copy.channelDescription,
        icon: spec.androidIcon,
        // High so the reminder heads-up instead of appearing silently in the
        // shade. Must match the channel created by [ensureReminderChannel] — once
        // Android creates a channel its importance is fixed, and details cannot
        // raise it above the channel's level.
        importance: Importance.high,
        priority: Priority.high,
        color: spec.accentColor,
        // A plain determinate bar showing today's progress toward the goal.
        showProgress: maxProgress != null,
        maxProgress: maxProgress ?? 0,
        progress: progress ?? 0,
        actions: actions,
      ),
      iOS: const DarwinNotificationDetails(),
    );

/// Posts [spec]'s notification right now — the debug-diagnostics "test this
/// reminder" path. Same channel, details, body, progress bar and actions as a
/// scheduled fire, so what it shows is exactly what a real reminder shows.
///
/// Uses the first id of the reserved range, so the next batch (re)schedule
/// cancels it like any other entry rather than leaving a stray test
/// notification behind.
Future<void> showReminderNotificationNow(
  FlutterLocalNotificationsPlugin plugin,
  ReminderNotificationSpec spec, {
  AppLocalizations? l10n,
  ReminderGoalProgress? progress,
  List<AndroidNotificationAction> actions = const [],
}) async {
  final copy = _copyOf(spec, l10n);
  final hasGoal = progress != null && progress.target > 0;
  final maxProgress = hasGoal
      ? (progress.target * BatchZonedNotificationReminderScheduler._progressScale)
          .round()
      : 0;
  final currentProgress = hasGoal
      ? (progress.current * BatchZonedNotificationReminderScheduler._progressScale)
          .round()
          .clamp(0, maxProgress)
      : 0;
  await plugin.show(
    id: spec.baseNotificationId,
    title: copy.title,
    body: hasGoal ? spec.body(progress) : copy.scheduledBody,
    notificationDetails: hasGoal
        ? _detailsFor(
            spec,
            copy,
            maxProgress: maxProgress,
            progress: currentProgress,
            actions: actions,
          )
        : _detailsFor(spec, copy, actions: actions),
    payload: spec.tapRoute,
  );
}

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
  AppLocalizations? l10n,
  String? oldChannelId,
}) async {
  final copy = _copyOf(spec, l10n);
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
        copy.channelName,
        description: copy.channelDescription,
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
    this.buildActions,
    this.localizations,
    this.now = DateTime.now,
  });

  final FlutterLocalNotificationsPlugin plugin;
  final ReminderNotificationSpec spec;

  /// The catalog the batch's copy is baked from, resolved once per
  /// (re)schedule so a language change reaches the shade on the next re-plan.
  /// Null (or a failure) falls back to the spec's English.
  final AppLocalizations Function()? localizations;

  /// Android action buttons stamped onto every entry of the batch, resolved
  /// once per (re)schedule so they reflect current state (e.g. the hydration
  /// quick-add reads the last used cup sizes). Null or a failure → no actions.
  /// Android-only: iOS action categories are fixed at plugin initialization and
  /// cannot carry per-schedule labels.
  final List<AndroidNotificationAction> Function()? buildActions;

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

    // Best-effort, like the exact-alarm probe: a failing actions builder must
    // cost the buttons, not the whole reminder chain.
    List<AndroidNotificationAction> actions;
    try {
      actions = buildActions?.call() ?? const [];
    } catch (_) {
      actions = const [];
    }

    ReminderLocalizedCopy copy;
    try {
      copy = _copyOf(spec, localizations?.call());
    } catch (_) {
      copy = _copyOf(spec, null);
    }

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
        title: copy.title,
        body: showProgress ? spec.body(progress) : copy.scheduledBody,
        scheduledDate: tz.TZDateTime.from(trigger, tz.local),
        notificationDetails: showProgress
            ? _detailsFor(
                spec,
                copy,
                maxProgress: maxProgress,
                progress: currentProgress,
                actions: actions,
              )
            : _detailsFor(spec, copy, actions: actions),
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
