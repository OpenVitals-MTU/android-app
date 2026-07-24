import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/result/result.dart';
import '../../bootstrap/background_health_access.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/reminders/alarm_manager_reminder_scheduler.dart';
import '../../data/prefs/preferences_repository.dart';
import '../../data/repository/body_energy_timeline_cache_store.dart';
import '../../data/repository/impl/activity_marker_repository_impl.dart';
import '../../data/repository/impl/activity_repository_impl.dart';
import '../../data/repository/impl/body_energy_repository_impl.dart';
import '../../data/repository/impl/body_repository_impl.dart';
import '../../data/repository/impl/health_repository_impl.dart';
import '../../data/repository/impl/heart_repository_impl.dart';
import '../../data/repository/impl/sleep_repository_impl.dart';
import '../../data/repository/impl/vitals_repository_impl.dart';
import '../../data/repository/dashboard/dashboard_data_loader.dart';
import '../../domain/usecase/load_dashboard_day_use_case.dart';
import '../../data/source/health/health_data_source.dart';
import 'home_widget_refresher.dart';
import 'home_widget_service.dart';

/// The Android alarm id for the home-widget refresh. 5001 (hydration) and 5002
/// (mindfulness) are taken.
const int homeWidgetRefreshAlarmId = 5003;

/// How often the widgets refresh in the background, matching the Kotlin
/// `appwidget-provider android:updatePeriodMillis="1800000"`.
const Duration homeWidgetRefreshInterval = Duration(minutes: 30);

/// Arms the periodic widget refresh. Reuses the reminder scheduler: an exact
/// alarm that wakes a background isolate is exactly what Glance's
/// `updatePeriodMillis` gives Kotlin for free.
const AlarmManagerReminderScheduler homeWidgetRefreshAlarmScheduler =
    AlarmManagerReminderScheduler(
  alarmId: homeWidgetRefreshAlarmId,
  callback: homeWidgetRefreshAlarmCallback,
);

/// Arms the next refresh, [homeWidgetRefreshInterval] from now.
///
/// Android's alarm manager has no repeating exact alarm worth relying on, so the
/// chain is self-perpetuating: each firing re-arms the next (the same shape as
/// the hydration/mindfulness reminders).
Future<void> scheduleHomeWidgetRefresh({
  AlarmManagerReminderScheduler scheduler = homeWidgetRefreshAlarmScheduler,
  DateTime? now,
}) =>
    scheduler.schedule((now ?? DateTime.now()).add(homeWidgetRefreshInterval));

/// Runs in a **background isolate** when the refresh alarm fires — the Dart
/// stand-in for Glance's periodic `onUpdate`.
///
/// The isolate is fresh: no `main()` has run, no plugins are registered, and no
/// Riverpod container exists, so this registers the plugins and hand-builds the
/// object graph. It must be a top-level `@pragma('vm:entry-point')` function or
/// tree-shaking drops it and the stored callback handle will not resolve.
///
/// Everything is swallowed: an exception escaping here kills the alarm service,
/// and Android will not retry — the refresh chain would die silently. The re-arm
/// therefore runs in a `finally`, so even a failed refresh schedules the next.
@pragma('vm:entry-point')
Future<void> homeWidgetRefreshAlarmCallback() async {
  try {
    DartPluginRegistrant.ensureInitialized();
    final refresher = await buildBackgroundHomeWidgetRefresher();
    await refresher.refresh();
  } catch (error, stack) {
    debugPrint('Home widget refresh alarm failed: $error\n$stack');
  } finally {
    try {
      await scheduleHomeWidgetRefresh();
    } catch (error) {
      debugPrint('Home widget refresh re-arm failed: $error');
    }
  }
}

/// Builds a [HomeWidgetRefresher] with no Riverpod container — for the alarm
/// isolate, where the app's provider graph does not exist.
///
/// Deliberately omits the drift database (and so the `BeverageStore`): the
/// dashboard loader reads through Health Connect and SharedPreferences only, and
/// opening a second connection to the database from a background isolate would
/// be a real risk for no benefit. That is also why the beverage widgets, whose
/// drink catalog *is* in drift, are not refreshed from here.
@visibleForTesting
Future<HomeWidgetRefresher> buildBackgroundHomeWidgetRefresher() async {
  final sharedPreferences = await SharedPreferences.getInstance();
  // The alarm plugin reuses one long-lived background engine, so this isolate's
  // prefs cache is whatever it read on the FIRST fire — stale to any unit/goal
  // change the user has since made in the foreground. Reload before building the
  // repository so the widgets render current values.
  await sharedPreferences.reload();
  final preferences = PreferencesRepository(sharedPreferences);
  final HealthDataSource dataSource =
      (await openBackgroundHealthAccess()).orThrow();

  final loader = DashboardDataLoader(
    dataSource,
    // caffeineRepository stays unwired: the metric widget catalog excludes
    // caffeine, so activeCaffeineMg is never rendered on a widget and the
    // extra Health Connect read would be pure background cost.
    preferencesRepository: preferences,
    bodyEnergyRepository: BodyEnergyRepositoryImpl(
      heartRepository: HeartRepositoryImpl(dataSource),
      sleepRepository: SleepRepositoryImpl(dataSource),
      activityRepository: ActivityRepositoryImpl(
        dataSource,
        preferencesRepository: preferences,
        markerRepository: ActivityMarkerRepositoryImpl(sharedPreferences),
      ),
      vitalsRepository: VitalsRepositoryImpl(dataSource),
      bodyRepository: BodyRepositoryImpl(dataSource),
      healthRepository: HealthRepositoryImpl(dataSource),
      preferencesRepository: preferences,
      cacheStore: BodyEnergyTimelineCacheStore(sharedPreferences),
    ),
  );

  return HomeWidgetRefresher(
    service: const HomeWidgetService(),
    // Resolves Health Connect access before each load. Without it this isolate's
    // freshly-built data source stays at `notSupported`, every permission reads
    // as missing, and the widgets all render "Grant permission in OpenVitals".
    health: HealthRepositoryImpl(dataSource),
    loadDashboardDay: LoadDashboardDayUseCase(loader),
    unitFormatter:
        UnitFormatter(unitSystemProvider: () => preferences.unitSystem),
    localizations: homeWidgetLocalizations(),
    goals: homeWidgetReadinessGoals(preferences),
    sleepWindow: preferences.sleepWindow,
    activityWeekMode: preferences.activityWeekMode,
  );
}
