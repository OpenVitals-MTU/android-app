import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:home_widget/home_widget.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/presentation/unit_formatter.dart';
import '../../data/prefs/preferences_repository.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../../data/repository/contract/health_repository.dart';
import '../../data/repository/impl/health_repository_impl.dart';
import '../../data/repository/impl/hydration_repository_impl.dart';
import '../../data/repository/impl/nutrition_repository_impl.dart';
import '../../di/providers.dart' show openVitalsPackageName;
import '../../data/source/health/health_data_source.dart';
import '../../domain/model/nutrition_models.dart';
import '../../data/source/health/native/health_connect_native_data_source.dart';
import '../../l10n/app_localizations.dart';
import '../manualentry/application/hydration_entry_view_model.dart';
import 'home_widget_beverage.dart';
import 'home_widget_refresher.dart';
import 'home_widget_service.dart';

/// One-tap beverage logging from the home screen — the Dart stand-in for the
/// Kotlin `HomeQuickBeverageLogAction`.
///
/// Kotlin can log straight from the Glance `ActionCallback`, in-process, with
/// Hilt handing it live repositories. Here the tap crosses into a **background
/// isolate**: the native widget fires `HomeWidgetBackgroundIntent`, and the
/// `home_widget` plugin runs [homeWidgetInteractivityCallback] in a fresh isolate
/// with no `main()`, no plugins and no Riverpod container.
///
/// Two consequences drive this file:
///
/// 1. **No drift.** `hydrationRepository.customHydrationDrinks()` reads the
///    drift-backed `BeverageStore`, and a second database connection from a
///    background isolate is a real risk (the same call the reminder alarm and the
///    widget-refresh alarm both refuse to make). The drink is therefore read from
///    the payload cached at configure time — see `home_widget_beverage.dart`.
///    Health Connect *is* reachable from a background isolate; the reminder
///    alarms already depend on that.
/// 2. **Nothing may throw.** An exception escaping the isolate is fatal and
///    Android will not retry the broadcast, so every step is guarded and the tap
///    degrades to an error subtitle instead.

/// URI scheme/host the widgets fire at the background receiver:
/// `openvitals://beverage_log?appWidgetId=<id>`.
///
/// The appWidgetId is the only thing the widget can tell us — everything else
/// (which widget owns it, which drink it holds) is resolved from widget storage
/// on this side, which is also what keeps a spoofed broadcast harmless: an
/// unknown id simply has no configured drink.
const String quickBeverageLogScheme = 'openvitals';
const String quickBeverageLogHost = 'beverage_log';
const String quickBeverageLogAppWidgetIdParam = 'appWidgetId';

/// How long the "Saved now" confirmation stays up before the widget falls back
/// to "Tap to log" (Kotlin `SavedConfirmationDurationMillis`).
const Duration quickBeverageSavedConfirmationDuration =
    Duration(milliseconds: 1200);

/// The appWidgetId a background [uri] targets, or null when it is not one of our
/// beverage-log broadcasts.
int? quickBeverageLogAppWidgetId(Uri? uri) {
  if (uri == null) return null;
  if (uri.scheme != quickBeverageLogScheme) return null;
  if (uri.host != quickBeverageLogHost) return null;
  final raw = uri.queryParameters[quickBeverageLogAppWidgetIdParam];
  if (raw == null) return null;
  return int.tryParse(raw);
}

/// Registers [homeWidgetInteractivityCallback] with the plugin.
///
/// Must run on **every** app start, not once: the plugin stores a raw AOT
/// callback handle, and those are invalidated by an app update or reinstall — a
/// stale handle silently drops every widget tap until the app is next opened.
Future<void> registerHomeWidgetInteractivity() async {
  try {
    await HomeWidget.registerInteractivityCallback(
      homeWidgetInteractivityCallback,
    );
  } on PlatformException catch (error) {
    debugPrint('registerInteractivityCallback failed: $error');
  } on MissingPluginException {
    // No host to register against (tests, desktop).
  }
}

/// Runs in a **background isolate** when a beverage widget is tapped.
///
/// Must be a top-level function annotated with `@pragma('vm:entry-point')`, or
/// tree-shaking drops it and the raw handle registered above will not resolve.
/// Swallows everything: see the file header.
@pragma('vm:entry-point')
Future<void> homeWidgetInteractivityCallback(Uri? uri) async {
  try {
    DartPluginRegistrant.ensureInitialized();
    final appWidgetId = quickBeverageLogAppWidgetId(uri);
    if (appWidgetId == null) return;
    final logger = await buildBackgroundQuickBeverageLogger();
    await logger.log(appWidgetId);
  } catch (error, stack) {
    debugPrint('Quick beverage widget log failed: $error\n$stack');
  }
}

/// Builds a [QuickBeverageWidgetLogger] with no Riverpod container — for the
/// interactivity isolate, where the app's provider graph does not exist.
///
/// Deliberately omits the drift `BeverageStore` (see the file header): the drink
/// comes from the cached payload, and everything else this needs — the write
/// permissions, the hydration and nutrition writes, the unit system — is Health
/// Connect and SharedPreferences.
@visibleForTesting
Future<QuickBeverageWidgetLogger> buildBackgroundQuickBeverageLogger() async {
  final preferences =
      PreferencesRepository(await SharedPreferences.getInstance());
  final HealthDataSource dataSource =
      HealthConnectNativeDataSource(appPackageName: openVitalsPackageName);

  return QuickBeverageWidgetLogger(
    service: const HomeWidgetService(),
    // Resolves Health Connect access before the write. Without it this isolate's
    // freshly-built data source stays at `notSupported`, `grantedPermissions()`
    // comes back empty, `hasHydrationWritePermission()` is false, and the tap is
    // silently discarded as "missing permission" — invisibly on the 2x1, which
    // does not render a subtitle at all.
    health: HealthRepositoryImpl(dataSource),
    hydrationRepository: HydrationRepositoryImpl(
      dataSource,
      preferencesRepository: preferences,
    ),
    nutritionRepository: NutritionRepositoryImpl(dataSource),
    unitFormatter:
        UnitFormatter(unitSystemProvider: () => preferences.unitSystem),
    localizations: homeWidgetLocalizations(),
  );
}

/// Logs the drink a beverage widget instance holds, and reports the outcome back
/// onto that instance's subtitle. Port of the Kotlin `HomeQuickBeverageLogAction`.
class QuickBeverageWidgetLogger {
  const QuickBeverageWidgetLogger({
    required this.service,
    required this.health,
    required this.hydrationRepository,
    required this.nutritionRepository,
    required this.unitFormatter,
    required this.localizations,
    this.savedConfirmationDuration = quickBeverageSavedConfirmationDuration,
  });

  final HomeWidgetService service;

  /// Resolves Health Connect access before the permission check — without it the
  /// write is always refused. See [buildBackgroundQuickBeverageLogger].
  final HealthRepository health;
  final HydrationRepository hydrationRepository;
  final NutritionRepository nutritionRepository;
  final UnitFormatter unitFormatter;
  final AppLocalizations localizations;

  /// How long "Saved now" stays up. Overridable so tests need not really wait.
  final Duration savedConfirmationDuration;

  Future<void> log(int appWidgetId) async {
    // Which of the two beverage widgets owns this id decides which receiver is
    // told to redraw — they share a key namespace but are separate providers, so
    // updating the wrong one would leave the tapped tile stale. Kotlin resolves
    // the same thing through `AppWidgetManager.getAppWidgetInfo().provider`.
    final widget = await service.widgetOfInstance(appWidgetId);
    if (widget == null ||
        (widget != HomeWidgetId.quickBeverage &&
            widget != HomeWidgetId.quickBeverageOneTap)) {
      return;
    }

    final drink = await readQuickBeverageDrink(
      service,
      widget: widget,
      appWidgetId: appWidgetId,
    );
    if (drink == null) {
      // The drink was deleted from the catalog (or never configured): say so,
      // rather than logging something the user did not pick.
      await service.pushSnapshot(
        widget,
        unconfiguredQuickBeverageSnapshot(localizations),
        appWidgetId: appWidgetId,
      );
      return;
    }

    try {
      // MUST come before the write. This isolate builds its own HealthDataSource,
      // whose cachedAvailability starts at `notSupported` — and the repositories
      // report no granted permissions while it does, so the write below would be
      // refused as "missing permission" and the tap would do nothing.
      await health.refreshAvailability();
      // Kotlin remembers the tapped volume as the last custom amount, so the
      // entry screen opens on it next time.
      hydrationRepository
          .setLastCustomHydrationAmountMilliliters(drink.volumeMilliliters);
      final outcome = await logCustomHydrationDrinkEntry(
        hydrationRepository: hydrationRepository,
        nutritionRepository: nutritionRepository,
        drink: drink,
        canWriteHydration: await hydrationRepository.hasHydrationWritePermission(),
        canWriteNutrition: await nutritionRepository.hasNutritionWritePermission(),
      );
      switch (outcome) {
        case HydrationDrinkLogInvalid(:final error):
          // Not auto-cleared: an error the user has to act on stays put until
          // the next refresh or the next successful tap.
          await _push(widget, drink, appWidgetId, _errorSubtitle(error));
        case HydrationDrinkLogSuccess(:final wroteHydration):
          await _push(
            widget,
            drink,
            appWidgetId,
            wroteHydration
                ? localizations.homeQuickBeverageWidgetSaved
                : localizations.homeQuickBeverageWidgetSavedNutrition,
          );
          // Briefly confirm the tap, then revert to the normal widget text.
          await Future<void>.delayed(savedConfirmationDuration);
          await _push(widget, drink, appWidgetId, null);
      }
    } catch (error, stack) {
      debugPrint('Quick beverage widget write failed: $error\n$stack');
      await _push(
        widget,
        drink,
        appWidgetId,
        localizations.homeMetricWidgetUpdateFailed,
      );
    }
  }

  /// Re-pushes the instance with [subtitle] (null = the resting "Tap to log").
  Future<void> _push(
    HomeWidgetId widget,
    CustomHydrationDrink drink,
    int appWidgetId,
    String? subtitle,
  ) =>
      service.pushSnapshot(
        widget,
        buildQuickBeverageSnapshot(
          drink,
          unitFormatter,
          localizations,
          subtitle: subtitle,
        ),
        appWidgetId: appWidgetId,
        selectionId: drink.id,
      );

  /// Kotlin `HydrationEntryError.quickBeverageWidgetMessage`.
  String _errorSubtitle(HydrationEntryError error) => switch (error) {
        HydrationEntryError.missingWritePermission ||
        HydrationEntryError.missingNutritionWritePermission =>
          localizations.homeMetricWidgetPermissionNeeded,
        HydrationEntryError.invalidAmount ||
        HydrationEntryError.invalidCustomDrink ||
        HydrationEntryError.writeFailed =>
          localizations.homeMetricWidgetUpdateFailed,
      };
}
