import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';
import 'bootstrap/reminder_bootstrap.dart';
import 'bootstrap/reminder_resume_bootstrap.dart';
import 'bootstrap/reminder_tap_bootstrap.dart';
import 'data/migration/kotlin_data_migration.dart';
import 'data/migration/legacy_data_source.dart';
import 'di/providers.dart';
import 'features/homewidgets/home_widget_beverage_log.dart';
import 'features/homewidgets/home_widget_configure.dart';
import 'features/homewidgets/home_widget_launch.dart';
import 'features/homewidgets/home_widget_service.dart';
import 'features/imports/presentation/route_import_intent.dart';

/// App entry point.
///
/// Resolves the platform [SharedPreferences] instance up front and injects it
/// into the Riverpod graph via [sharedPreferencesProvider] (the standard
/// bootstrap override pattern documented on that provider). Drift and the
/// health data source resolve lazily from their own providers on first use, so
/// they do not need to be awaited here.
///
/// The container is created explicitly rather than by [ProviderScope] so that
/// [bootstrapReminders] can read it. Reminders come up *after* [runApp] and are
/// never awaited: re-arming an alarm touches Health Connect and must not hold
/// the first frame.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Load date-formatting symbols for every locale. Without this (and the
  // Intl.defaultLocale set in app.dart), DateFormat only has en_US data and
  // every date across the app renders in English regardless of the user's
  // language. The symbols are embedded, so this is fast.
  await initializeDateFormatting();
  // Receives activity-recording notification-button presses relayed from the
  // foreground-service isolate (see activity_recording_task_handler.dart).
  FlutterForegroundTask.initCommunicationPort();
  // Re-registered on every start, never once: the plugin stores a raw AOT
  // callback handle for the quick-beverage widgets' one-tap logging, and an app
  // update or reinstall invalidates it (see home_widget_beverage_log.dart).
  unawaited(registerHomeWidgetInteractivity());
  final prefs = await SharedPreferences.getInstance();
  // In-place upgrade from the Kotlin app: its data survives the update but lives
  // in files Flutter does not read. This lifts it across exactly once, and never
  // throws. It must run before the container: `prefs` is handed in so its
  // in-memory cache reflects the writes, and drift opens lazily, so the copied
  // database only has to land before the container is first used — which it does.
  await migrateKotlinDataIfNeeded(
    prefs: prefs,
    native: const MethodChannelLegacyDataSource(),
    documentsDir: await getApplicationDocumentsDirectory(),
    widgets: const PluginHomeWidgetClient(),
  );
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
    ],
  );

  // A metric or quick-beverage widget being placed (or reconfigured) starts that
  // widget's own configuration activity (HomeWidgetConfigureActivity.kt), which
  // boots this entrypoint in a fresh engine on the initial route
  // `/widget-configure/<widget>?appWidgetId=<id>`. It is a modal, single-purpose
  // launch: show that widget's picker and nothing else, and skip the reminder
  // bootstrap, which belongs to a real app start.
  //
  // Both facts come from the route because the activity knows them: nothing here
  // resolves a type from an appWidgetId, which is what used to put the beverage
  // picker in front of a metric tile.
  final configure = parseHomeWidgetConfigureRoute(
    WidgetsBinding.instance.platformDispatcher.defaultRouteName,
  );
  if (configure != null) {
    runApp(
      UncontrolledProviderScope(
        container: container,
        child: HomeWidgetConfigureApp(request: configure),
      ),
    );
    return;
  }

  runApp(
    UncontrolledProviderScope(
      container: container,
      // Drains any route file the app was opened with ("Open with" on a
      // .gpx/.kml/.kmz/.fit) into the activity-entry form — the Kotlin
      // `ExternalRouteImportRequest` path — and routes a home-screen-widget tap
      // to the screen it points at (the Kotlin `EXTRA_OPENVITALS_ROUTE` path).
      child: const RouteImportIntentBootstrap(
        child: HomeWidgetLaunchBootstrap(
          child: ReminderResumeBootstrap(
            child: ReminderTapBootstrap(child: OpenVitalsApp()),
          ),
        ),
      ),
    ),
  );

  unawaited(bootstrapReminders(container));
}
