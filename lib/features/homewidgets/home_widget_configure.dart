import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/dashboard_query.dart';
import '../../domain/model/nutrition_models.dart';
import '../../l10n/app_localizations.dart';
import '../../state/app_providers.dart';
import '../../ui/theme/app_theme.dart';
import '../manualentry/presentation/hydration_catalog_widgets.dart';
import '../../domain/hydration/hydration_drink_usage.dart';
import '../manualentry/application/hydration_entry_view_model.dart';
import 'home_widget_beverage.dart';
import 'home_widget_service.dart';
import 'home_widget_snapshots.dart';

/// Configuring a placed home-screen widget — the Flutter half of the Kotlin
/// `HomeMetricWidgetConfigurationActivity` and
/// `HomeQuickBeverageWidgetConfigurationActivity`.
///
/// Kotlin could put each picker in a native `ListView` activity because the metric
/// catalog and the drink catalog both live in Kotlin. Here they live in Dart, so
/// the picker's *contents* are a Flutter screen — but the activity hosting it is
/// still the widget's own dedicated one (`HomeWidgetConfigureActivity.kt`), for
/// two reasons that a single shared `MainActivity` cannot satisfy:
///
/// 1. It is genuinely `startActivityForResult`-ed by the launcher, so the
///    `RESULT_OK` a pick sets actually reaches it. MainActivity is `singleTop`:
///    a configure intent arriving while the app ran went to the *running*
///    instance, whose result nobody was waiting for, and the launcher silently
///    dropped the widget.
/// 2. It *knows* which widget it is configuring and hands that over up front, so
///    nothing has to guess the type from an appWidgetId that may be stale (a
///    stale id resolving to a beverage instance is how a metric tile ended up
///    showing the beverage picker).
///
/// The native side hands both facts over as the engine's initial route —
/// `/widget-configure/<HomeWidgetId.name>?appWidgetId=<id>` — which `main()`
/// parses with [parseHomeWidgetConfigureRoute].
///
/// The picked metric/drink is persisted as the instance's `selection_id`, which is
/// the contract `HomeWidgetRefresher` reads back to know what each tile shows.
/// Kotlin additionally kept the choice in an `AppWidgetOptions` bundle and a
/// private SharedPreferences file, with a "pending metric" TTL to bridge the two —
/// none of which is needed here: the plugin's preferences file is the single
/// source of truth for both sides.

/// Prefix of the initial route the configure activities boot Dart on. Mirrors
/// `HomeWidgetConfigureActivity.CONFIGURE_ROUTE_PREFIX`.
const String homeWidgetConfigureRoutePrefix = '/widget-configure/';

/// Mirrors `HomeWidgetConfigureActivity.APP_WIDGET_ID_PARAM`.
const String homeWidgetConfigureAppWidgetIdParam = 'appWidgetId';

/// Mirrors `HomeWidgetConfigureActivity.CONFIGURE_CHANNEL`.
const String homeWidgetConfigureChannelName =
    'tech.mmarca.openvitals/home_widget_configure';

/// What the app was launched to configure: which widget, and which placed
/// instance of it.
class HomeWidgetConfigureRequest {
  const HomeWidgetConfigureRequest({
    required this.widget,
    required this.appWidgetId,
  });

  final HomeWidgetId widget;
  final int appWidgetId;

  @override
  bool operator ==(Object other) =>
      other is HomeWidgetConfigureRequest &&
      other.widget == widget &&
      other.appWidgetId == appWidgetId;

  @override
  int get hashCode => Object.hash(widget, appWidgetId);

  @override
  String toString() =>
      'HomeWidgetConfigureRequest(${widget.name}, appWidgetId: $appWidgetId)';
}

/// The configure request behind an initial [route], or null when this is an
/// ordinary launch.
///
/// Rejects anything it does not fully understand — an unknown widget name, a
/// widget that is not configurable, a missing or unparseable appWidgetId — so a
/// malformed route boots the normal app rather than a picker wired to nothing.
HomeWidgetConfigureRequest? parseHomeWidgetConfigureRoute(String? route) {
  if (route == null || !route.startsWith(homeWidgetConfigureRoutePrefix)) {
    return null;
  }
  final uri = Uri.tryParse(route);
  if (uri == null) return null;
  final name = uri.path.substring(homeWidgetConfigureRoutePrefix.length);
  final widget = HomeWidgetId.values
      .where((widget) => widget.isPerInstance && widget.name == name)
      .firstOrNull;
  if (widget == null) return null;
  final id = int.tryParse(
    uri.queryParameters[homeWidgetConfigureAppWidgetIdParam] ?? '',
  );
  // 0 is AppWidgetManager.INVALID_APPWIDGET_ID.
  if (id == null || id == 0) return null;
  return HomeWidgetConfigureRequest(widget: widget, appWidgetId: id);
}

/// Thin seam over the configure activity's channel, so the pickers are testable
/// without an Android host (the same shape as `HomeWidgetLaunchChannel`).
///
/// Deliberately *not* `home_widget`'s `finishHomeWidgetConfigure`: that API reads
/// the appWidgetId back off the hosting activity's intent, which only works when
/// the configure target is the activity the intent launched — the single-activity
/// setup this replaces.
class HomeWidgetConfigureChannel {
  const HomeWidgetConfigureChannel();

  static const MethodChannel _channel =
      MethodChannel(homeWidgetConfigureChannelName);

  /// Completes the configuration with `RESULT_OK` and finishes the activity,
  /// which is what tells the launcher to keep the widget. Not calling it leaves
  /// the `RESULT_CANCELED` the activity set in `onCreate`, so a user who backs
  /// out never gets a half-configured tile.
  Future<void> finish(int appWidgetId) async {
    try {
      await _channel.invokeMethod<void>('finishConfigure', {
        homeWidgetConfigureAppWidgetIdParam: appWidgetId,
      });
    } on PlatformException catch (error) {
      debugPrint('finishConfigure failed: $error');
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
class HomeWidgetConfigureApp extends ConsumerWidget {
  const HomeWidgetConfigureApp({super.key, required this.request});

  final HomeWidgetConfigureRequest request;

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
      // Shipped locales only, same as the main app shell — an in-progress ARB
      // must not win the platform-locale resolution here either.
      supportedLocales: OpenVitalsApp.supportedLocales,
      locale: tag == null ? null : Locale(tag),
      home: HomeWidgetConfigurePicker(request: request),
    );
  }
}

/// The picker for the widget named in [request].
///
/// Synchronous by construction: the configure activity already told us the type,
/// so there is nothing to resolve and no way to land on the wrong picker. (The
/// old flow had to guess it from `installedWidgets()`, because every widget
/// configured through the same activity.)
class HomeWidgetConfigurePicker extends StatelessWidget {
  const HomeWidgetConfigurePicker({super.key, required this.request});

  final HomeWidgetConfigureRequest request;

  @override
  Widget build(BuildContext context) => switch (request.widget) {
        HomeWidgetId.quickBeverage || HomeWidgetId.quickBeverageOneTap =>
          HomeQuickBeverageWidgetConfigureScreen(
            appWidgetId: request.appWidgetId,
            widgetId: request.widget,
          ),
        _ => HomeMetricWidgetConfigureScreen(
            appWidgetId: request.appWidgetId,
          ),
      };
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
    await ref
        .read(homeWidgetConfigureChannelProvider)
        .finish(widget.appWidgetId);
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
        .orThrow()
        .where(isValidCustomHydrationDrink)
        .toList();
    if (drinks.isEmpty) return const <CustomHydrationDrink>[];

    var frequent = const <CustomHydrationDrink>[];
    try {
      final end = LocalDate.now();
      final start = end.minusDays(kFrequentHydrationDrinkLookbackDays - 1);
      frequent = frequentHydrationDrinkOptions(
        drinks: drinks,
        hydrationEntries:
            (await hydration.loadHydrationEntries(start, end)).orThrow(),
        nutritionEntries: (await ref
                .read(nutritionRepositoryProvider)
                .loadNutritionEntries(start, end))
            .orThrow(),
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
    await ref
        .read(homeWidgetConfigureChannelProvider)
        .finish(widget.appWidgetId);
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
