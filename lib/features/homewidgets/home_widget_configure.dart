import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:home_widget/home_widget.dart';

import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/dashboard_query.dart';
import '../../domain/model/nutrition_models.dart';
import '../../l10n/app_localizations.dart';
import '../../state/app_providers.dart';
import '../../ui/theme/app_theme.dart';
import '../manualentry/hydration_catalog_widgets.dart';
import '../manualentry/hydration_drink_usage.dart';
import '../manualentry/hydration_entry_notifier.dart';
import 'home_widget_beverage.dart';
import 'home_widget_service.dart';
import 'home_widget_snapshots.dart';

/// Configuring a placed metric widget — the Flutter half of the Kotlin
/// `HomeMetricWidgetConfigurationActivity`.
///
/// Kotlin could put the picker in a native `ListView` activity because the metric
/// catalog, its titles and its formatting all live in Kotlin. Here they live in
/// Dart, so the *Flutter activity is the configuration activity*: the metric
/// widget's `android:configure` points at `MainActivity`, and the `home_widget`
/// plugin recognises the `APPWIDGET_CONFIGURE` intent —
/// [HomeWidget.initiallyLaunchedFromHomeWidgetConfigure] hands us the appWidgetId
/// (and sets `RESULT_CANCELED` for us, so backing out drops the widget, exactly
/// as Kotlin's `setResult(RESULT_CANCELED)` in `onCreate` does).
///
/// The picked metric is persisted as the instance's `selection_id`, which is the
/// contract `HomeWidgetRefresher` reads back to know what each tile shows. Kotlin
/// additionally kept the choice in an `AppWidgetOptions` bundle and a private
/// SharedPreferences file, with a "pending metric" TTL to bridge the two — none of
/// which is needed here: the plugin's preferences file is the single source of
/// truth for both sides.

/// Thin seam over the plugin's configuration channel, so the picker is testable
/// without an Android host (the same shape as `HomeWidgetLaunchChannel`).
class HomeWidgetConfigureChannel {
  const HomeWidgetConfigureChannel();

  /// The appWidgetId the app was launched to configure, or null on a normal
  /// launch (and on every host without the plugin).
  Future<int?> pendingAppWidgetId() async {
    try {
      final id = await HomeWidget.initiallyLaunchedFromHomeWidgetConfigure();
      // The plugin hands the id over as a String.
      return id == null ? null : int.tryParse(id);
    } on PlatformException catch (error) {
      debugPrint('initiallyLaunchedFromHomeWidgetConfigure failed: $error');
      return null;
    } on MissingPluginException {
      return null;
    }
  }

  /// Completes the configuration with `RESULT_OK`, which is what tells Android to
  /// keep the widget. Not calling it leaves the `RESULT_CANCELED` the plugin set,
  /// so a user who backs out never gets a half-configured tile.
  Future<void> finish() async {
    try {
      await HomeWidget.finishHomeWidgetConfigure();
    } on PlatformException catch (error) {
      debugPrint('finishHomeWidgetConfigure failed: $error');
    } on MissingPluginException {
      // No host to finish against (tests, desktop).
    }
  }
}

final homeWidgetConfigureChannelProvider = Provider<HomeWidgetConfigureChannel>(
  (ref) => const HomeWidgetConfigureChannel(),
);

/// The whole app, when it was launched to configure a widget.
///
/// Deliberately *not* the normal [OpenVitalsApp] with a route pushed on top: a
/// configuration launch is a modal, single-purpose activity that finishes as soon
/// as the user picks (or backs out), and the router's dashboard has no business
/// booting up behind it. `main()` mounts this instead of the app.
///
/// Three widgets configure through here — the metric tile and the two
/// quick-beverage tiles — but the plugin hands over only the `appWidgetId`, not
/// which of them was placed. So the id is resolved back to a widget type through
/// the installed-widget list (its receiver class name), and the matching picker
/// is shown. Kotlin had no such step: each widget pointed `android:configure` at
/// its own native activity.
class HomeWidgetConfigureApp extends ConsumerWidget {
  const HomeWidgetConfigureApp({super.key, required this.appWidgetId});

  final int appWidgetId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final language = ref.watch(appLanguageProvider);
    final tag = language.languageTag;
    return MaterialApp(
      title: 'OpenVitals',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.themeFrom(AppTheme.lightScheme),
      darkTheme: AppTheme.themeFrom(AppTheme.darkScheme),
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      locale: tag == null ? null : Locale(tag),
      home: HomeWidgetConfigurePicker(appWidgetId: appWidgetId),
    );
  }
}

/// Resolves which widget [appWidgetId] belongs to, then shows its picker.
///
/// An id that resolves to nothing (the instance vanished, or the host has no
/// widgets) falls back to the metric picker — the pre-beverage behaviour, and the
/// only picker that needs no drink catalog.
class HomeWidgetConfigurePicker extends ConsumerStatefulWidget {
  const HomeWidgetConfigurePicker({super.key, required this.appWidgetId});

  final int appWidgetId;

  @override
  ConsumerState<HomeWidgetConfigurePicker> createState() =>
      _HomeWidgetConfigurePickerState();
}

class _HomeWidgetConfigurePickerState
    extends ConsumerState<HomeWidgetConfigurePicker> {
  late final Future<HomeWidgetId?> _widget = _resolve();

  Future<HomeWidgetId?> _resolve() async {
    try {
      return await ref
          .read(homeWidgetServiceProvider)
          .widgetOfInstance(widget.appWidgetId);
    } catch (error) {
      debugPrint('Home widget type resolution failed: $error');
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<HomeWidgetId?>(
      future: _widget,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        return switch (snapshot.data) {
          HomeWidgetId.quickBeverage => HomeQuickBeverageWidgetConfigureScreen(
              appWidgetId: widget.appWidgetId,
              widgetId: HomeWidgetId.quickBeverage,
            ),
          HomeWidgetId.quickBeverageOneTap =>
            HomeQuickBeverageWidgetConfigureScreen(
              appWidgetId: widget.appWidgetId,
              widgetId: HomeWidgetId.quickBeverageOneTap,
            ),
          _ => HomeMetricWidgetConfigureScreen(appWidgetId: widget.appWidgetId),
        };
      },
    );
  }
}

/// The metric picker (Kotlin's `ListView` of `homeMetricWidgetCatalog()`).
class HomeMetricWidgetConfigureScreen extends ConsumerStatefulWidget {
  const HomeMetricWidgetConfigureScreen({super.key, required this.appWidgetId});

  final int appWidgetId;

  @override
  ConsumerState<HomeMetricWidgetConfigureScreen> createState() =>
      _HomeMetricWidgetConfigureScreenState();
}

class _HomeMetricWidgetConfigureScreenState
    extends ConsumerState<HomeMetricWidgetConfigureScreen> {
  /// Set while the pick is being persisted and pushed. The load can take a
  /// moment, and a second tap would configure the instance twice.
  bool _configuring = false;

  Future<void> _select(DashboardMetric metric) async {
    if (_configuring) return;
    setState(() => _configuring = true);
    await ref
        .read(homeWidgetRefresherProvider)
        .configureMetricInstance(metric, appWidgetId: widget.appWidgetId);
    await ref.read(homeWidgetConfigureChannelProvider).finish();
    // The activity is finishing; nothing to reset if it somehow is not.
    if (mounted) setState(() => _configuring = false);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final metrics = homeMetricWidgetCatalog();
    return Scaffold(
      appBar: AppBar(title: Text(l10n.homeMetricWidgetConfigTitle)),
      body: metrics.isEmpty
          ? Center(child: Text(l10n.homeMetricWidgetNoMetrics))
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                  child: Align(
                    alignment: AlignmentDirectional.centerStart,
                    child: Text(
                      l10n.homeMetricWidgetConfigPrompt,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                ),
                if (_configuring) const LinearProgressIndicator(),
                Expanded(
                  child: ListView.builder(
                    itemCount: metrics.length,
                    itemBuilder: (context, index) {
                      final metric = metrics[index];
                      return ListTile(
                        title: Text(homeWidgetMetricTitle(metric, l10n)),
                        onTap: _configuring ? null : () => _select(metric),
                      );
                    },
                  ),
                ),
              ],
            ),
    );
  }
}

/// The beverage picker (Kotlin's `HomeQuickBeverageWidgetConfigurationActivity`
/// `ListView` of `quickBeverageWidgetDrinkOptions()`).
///
/// [widgetId] is which of the two beverage widgets was placed: they share a key
/// namespace, but each is its own provider, so the push has to name the right
/// receiver.
///
/// This is the one place the drink catalog is read from drift — the foreground,
/// where the database is already open. What the picker persists (the drink's
/// cached payload) is what makes the *background* tap loggable without it.
class HomeQuickBeverageWidgetConfigureScreen extends ConsumerStatefulWidget {
  const HomeQuickBeverageWidgetConfigureScreen({
    super.key,
    required this.appWidgetId,
    required this.widgetId,
  });

  final int appWidgetId;
  final HomeWidgetId widgetId;

  @override
  ConsumerState<HomeQuickBeverageWidgetConfigureScreen> createState() =>
      _HomeQuickBeverageWidgetConfigureScreenState();
}

class _HomeQuickBeverageWidgetConfigureScreenState
    extends ConsumerState<HomeQuickBeverageWidgetConfigureScreen> {
  late final Future<List<CustomHydrationDrink>> _drinks = _loadDrinks();
  bool _configuring = false;

  /// The catalog, ordered as Kotlin's `loadBeverageOptions` orders it: frequent
  /// drinks first, then the user's own, then the preloaded catalog.
  ///
  /// The frequency ranking is derived from Health Connect entries and is a
  /// nicety, so it fails soft (Kotlin's `runCatching { … }.getOrDefault(empty)`):
  /// a Health Connect that cannot be read still leaves a usable picker.
  Future<List<CustomHydrationDrink>> _loadDrinks() async {
    final hydration = ref.read(hydrationRepositoryProvider);
    final drinks = (await hydration.customHydrationDrinks())
        .where(isValidCustomHydrationDrink)
        .toList();
    if (drinks.isEmpty) return const <CustomHydrationDrink>[];

    var frequent = const <CustomHydrationDrink>[];
    try {
      final end = LocalDate.now();
      final start = end.minusDays(kFrequentHydrationDrinkLookbackDays - 1);
      frequent = frequentHydrationDrinkOptions(
        drinks: drinks,
        hydrationEntries: await hydration.loadHydrationEntries(start, end),
        nutritionEntries: await ref
            .read(nutritionRepositoryProvider)
            .loadNutritionEntries(start, end),
      );
    } catch (error) {
      debugPrint('Quick beverage frequent drinks unavailable: $error');
    }

    return quickBeverageWidgetDrinkOptions(
      drinks: drinks,
      frequentDrinks: frequent,
    );
  }

  Future<void> _select(CustomHydrationDrink drink) async {
    if (_configuring) return;
    setState(() => _configuring = true);
    await ref.read(homeWidgetRefresherProvider).configureBeverageInstance(
          drink,
          widget: widget.widgetId,
          appWidgetId: widget.appWidgetId,
        );
    await ref.read(homeWidgetConfigureChannelProvider).finish();
    // The activity is finishing; nothing to reset if it somehow is not.
    if (mounted) setState(() => _configuring = false);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final formatter = ref.watch(unitFormatterProvider);
    return Scaffold(
      appBar: AppBar(title: Text(l10n.homeQuickBeverageWidgetConfigTitle)),
      body: FutureBuilder<List<CustomHydrationDrink>>(
        future: _drinks,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          final drinks = snapshot.data ?? const <CustomHydrationDrink>[];
          if (drinks.isEmpty) {
            return Center(child: Text(l10n.homeQuickBeverageWidgetNoDrinks));
          }
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                child: Align(
                  alignment: AlignmentDirectional.centerStart,
                  child: Text(
                    l10n.homeQuickBeverageWidgetConfigPrompt,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ),
              if (_configuring) const LinearProgressIndicator(),
              Expanded(
                child: ListView.builder(
                  itemCount: drinks.length,
                  itemBuilder: (context, index) {
                    final drink = drinks[index];
                    return ListTile(
                      title: Text(
                        // Kotlin's row label: the entry screen's amount format
                        // ("330 ml"), not the widget's compact one ("330ml").
                        '${drink.name} - '
                        '${hydrationAmountLabel(drink.volumeLiters, formatter)}',
                      ),
                      onTap: _configuring ? null : () => _select(drink),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
