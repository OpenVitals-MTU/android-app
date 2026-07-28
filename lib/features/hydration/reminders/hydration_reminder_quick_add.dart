import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../bootstrap/background_health_access.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_notifications.dart';
import '../../../core/result/result.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/contract/health_repository.dart';
import '../../../data/repository/contract/hydration_repository.dart';
import '../../../data/repository/contract/nutrition_repository.dart';
import '../../../data/repository/impl/health_repository_impl.dart';
import '../../../data/repository/impl/hydration_repository_impl.dart';
import '../../../data/repository/impl/nutrition_repository_impl.dart';
import '../../../data/source/health/health_data_source.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../domain/usecase/save_hydration_entry_use_case.dart';
import '../../../ui/theme/app_colors.dart';
import 'hydration_reminder_controller.dart';
import 'hydration_reminder_device.dart';

/// One-tap water logging from the hydration reminder notification.
///
/// The reminder carries two Android action buttons — "Add 350 ml" / "Add
/// 250 ml" — offering the last two cup sizes the user logged (falling back to
/// the last custom amount, then to a glass and a bottle). Tapping one logs
/// that volume silently, without opening the app: the plugin runs
/// [handleHydrationReminderQuickAdd] in a fresh **background isolate**, exactly
/// like the quick-beverage home widget's tap — and the same two constraints
/// from `home_widget_beverage_log.dart` apply:
///
/// 1. **No drift** — everything this needs is Health Connect and
///    SharedPreferences;
/// 2. **Nothing may throw** — an exception escaping the isolate is fatal, so
///    every step is guarded and a failed tap simply logs nothing.
///
/// Android-only: iOS notification action categories are fixed at plugin
/// initialization, so they cannot carry per-schedule "last used" labels; the
/// iOS reminder stays a plain tap-to-open notification.

/// Action ids encode the volume they log: `hydration_quick_add:350.0`. The id
/// is the only channel from the scheduled notification to the background
/// handler (the payload is the tap route), so the amount rides inside it.
const String hydrationQuickAddActionIdPrefix = 'hydration_quick_add:';

/// When the user has never logged anything: a glass and a bottle, matching the
/// `medium_glass`-ish and `water_bottle` presets of the entry screen.
const List<double> _fallbackQuickAddAmountsMilliliters = [250.0, 500.0];

/// How many one-tap sizes the reminder offers.
const int hydrationQuickAddActionCount = 2;

String hydrationQuickAddActionId(double milliliters) =>
    '$hydrationQuickAddActionIdPrefix$milliliters';

/// The volume [actionId] asks to log, or null when it is not a quick-add
/// action (a plain notification tap, a mindfulness action) or carries an
/// invalid volume (a stale schedule from a build with different bounds).
double? hydrationQuickAddAmountMilliliters(String? actionId) {
  if (actionId == null ||
      !actionId.startsWith(hydrationQuickAddActionIdPrefix)) {
    return null;
  }
  final value =
      double.tryParse(actionId.substring(hydrationQuickAddActionIdPrefix.length));
  if (value == null || !isValidHydrationContainerMilliliters(value)) {
    return null;
  }
  return value;
}

/// The two volumes the reminder offers: the last two used cup sizes, padded
/// with the last custom amount and then the defaults so there are always
/// [hydrationQuickAddActionCount] distinct, valid sizes.
List<double> hydrationQuickAddAmountsMilliliters(
  PreferencesRepository preferences,
) {
  final amounts = <double>[];
  void add(double? value) {
    if (amounts.length >= hydrationQuickAddActionCount) return;
    if (value == null || !isValidHydrationContainerMilliliters(value)) return;
    if (amounts.contains(value)) return;
    amounts.add(value);
  }

  preferences.recentHydrationAmountsMilliliters().forEach(add);
  add(preferences.lastCustomHydrationAmountMilliliters());
  _fallbackQuickAddAmountsMilliliters.forEach(add);
  return amounts;
}

/// The reminder's action buttons, resolved at (re)schedule time so they track
/// the user's latest sizes — every hydration log reschedules the batch, which
/// is what keeps these fresh.
List<AndroidNotificationAction> hydrationReminderQuickAddActions(
  PreferencesRepository preferences,
) {
  final formatter =
      UnitFormatter(unitSystemProvider: () => preferences.unitSystem);
  return hydrationQuickAddAmountsMilliliters(preferences)
      .map(
        (milliliters) => AndroidNotificationAction(
          hydrationQuickAddActionId(milliliters),
          // Hardcoded English, like the rest of the reminder's copy (see the
          // localization note on [hydrationReminderNotificationSpec]).
          'Add ${_quickAddLabel(preferences.unitSystem, formatter, milliliters)}',
          // The hydration accent, so the actions render as colored buttons
          // rather than plain shade text (where the skin honors action title
          // spans; elsewhere the notification's accentColor tints them).
          titleColor: AppColors.hydration,
          // Silent one-tap log in a background isolate; the tap must not bring
          // the app forward. The default cancelNotification dismisses the
          // tapped reminder immediately, and the reschedule after the write
          // clears the rest of the batch's stale progress.
          showsUserInterface: false,
        ),
      )
      .toList();
}

/// Millilitres read better than "0.35 L" on a button, so metric formats as
/// whole ml rather than through [UnitFormatter.hydration]; imperial keeps the
/// formatter's fluid ounces.
String _quickAddLabel(
  UnitSystem unitSystem,
  UnitFormatter formatter,
  double milliliters,
) {
  switch (unitSystem) {
    case UnitSystem.metric:
      return '${milliliters.round()} ml';
    case UnitSystem.imperial:
      return formatter.hydration(milliliters / kMillilitersPerLiter).text;
  }
}

/// Runs in a **background isolate** when a quick-add action button is tapped.
///
/// Registered through [initializeReminderNotifications]'s `onBackgroundAction`.
/// Must be top-level and `@pragma('vm:entry-point')`, or tree-shaking drops it
/// and the plugin's stored callback handle will not resolve. Ignores anything
/// that is not a hydration quick-add action (the handler is shared by every
/// reminder the app schedules). Swallows everything — see the file header.
@pragma('vm:entry-point')
Future<void> handleHydrationReminderQuickAdd(
  NotificationResponse response,
) async {
  try {
    DartPluginRegistrant.ensureInitialized();
    final milliliters = hydrationQuickAddAmountMilliliters(response.actionId);
    if (milliliters == null) return;
    final logger = await buildBackgroundHydrationQuickAddLogger();
    await logger.log(milliliters);
  } catch (error, stack) {
    debugPrint('Hydration quick-add failed: $error\n$stack');
  }
}

/// Builds a [HydrationQuickAddLogger] with no Riverpod container — for the
/// action's background isolate, where the app's provider graph does not exist.
/// The mirror of `buildBackgroundQuickBeverageLogger`.
@visibleForTesting
Future<HydrationQuickAddLogger> buildBackgroundHydrationQuickAddLogger() async {
  final sharedPreferences = await SharedPreferences.getInstance();
  // A prior tap's isolate (or the foreground app) may have written since this
  // engine's prefs cache was filled. Reload so the recents and the reminder
  // config are current.
  await sharedPreferences.reload();
  final preferences = PreferencesRepository(sharedPreferences);
  final HealthDataSource dataSource =
      (await openBackgroundHealthAccess()).orThrow();

  // This isolate has its own notifications plugin instance, so it must
  // initialize it and its channel before it can reschedule the batch.
  final plugin = FlutterLocalNotificationsPlugin();
  await initializeReminderNotifications(
    plugin,
    onBackgroundAction: handleHydrationReminderQuickAdd,
  );
  await ensureHydrationReminderChannel(plugin);

  final hydrationRepository = HydrationRepositoryImpl(
    dataSource,
    preferencesRepository: preferences,
  );

  final reminderController = HydrationReminderController(
    preferences: preferences,
    hydrationRepository: hydrationRepository,
    scheduler: BatchZonedNotificationReminderScheduler(
      plugin: plugin,
      spec: hydrationReminderNotificationSpec,
      canScheduleExact: () => canScheduleExactReminders(plugin),
      buildActions: () => hydrationReminderQuickAddActions(preferences),
    ),
    hasNotificationPermission: () => areReminderNotificationsEnabled(plugin),
  );

  return HydrationQuickAddLogger(
    // Resolves Health Connect access before the write — without it this
    // isolate's fresh data source stays at `notSupported` and the write is
    // silently refused as "missing permission".
    health: HealthRepositoryImpl(dataSource),
    hydrationRepository: hydrationRepository,
    nutritionRepository: NutritionRepositoryImpl(dataSource),
    onHydrationLogged: reminderController.onHydrationLogged,
  );
}

/// Logs a plain-water volume from a notification action tap.
class HydrationQuickAddLogger {
  const HydrationQuickAddLogger({
    required this.health,
    required this.hydrationRepository,
    required this.nutritionRepository,
    required this.onHydrationLogged,
  });

  final HealthRepository health;
  final HydrationRepository hydrationRepository;
  final NutritionRepository nutritionRepository;

  /// Re-anchors the reminder countdown to this drink and reschedules the batch
  /// (with actions rebuilt from the just-updated recents). Injected so tests
  /// need no notifications plugin.
  final Future<void> Function() onHydrationLogged;

  Future<void> log(double milliliters) async {
    // MUST come before the write; see [buildBackgroundHydrationQuickAddLogger].
    (await health.refreshAvailability()).orThrow();
    // Remember the size before the write, and independently of whether it
    // succeeds — the same reasoning as `SaveLastCustomHydrationAmountUseCase`.
    hydrationRepository.setLastCustomHydrationAmountMilliliters(milliliters);
    hydrationRepository.recordRecentHydrationAmountMilliliters(milliliters);
    final outcome = await SaveHydrationEntryUseCase(
      hydrationRepository,
      nutritionRepository,
    )(
      rawLiters: milliliters / kMillilitersPerLiter,
      hydrationMultiplier: kFullHydrationImpactMultiplier,
      nutrientValues: const {},
      canWriteHydration:
          (await hydrationRepository.hasHydrationWritePermission()).orThrow(),
      // Plain water writes no nutrition record, so the permission is never
      // consulted.
      canWriteNutrition: false,
    );
    // Reschedule only after a real write: a refused one (revoked permission)
    // must not re-anchor the countdown to a drink that never landed. There is
    // no UI to report the refusal to — the action already dismissed its
    // notification — so it ends here either way.
    if (outcome is HydrationDrinkLogSuccess && outcome.wroteHydration) {
      try {
        await onHydrationLogged();
      } catch (_) {
        // Re-anchoring is a nicety; never surface an error over a logged drink.
      }
    }
  }
}
