import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/homewidgets/home_widget_beverage.dart';
import 'package:openvitals/features/homewidgets/home_widget_configure.dart';
import 'package:openvitals/features/homewidgets/home_widget_refresher.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';

import 'fake_home_widget_client.dart';

const int _appWidgetId = 42;
const String _beverageReceiver =
    'tech.mmarca.openvitals.features.homewidgets.HomeQuickBeverageWidgetReceiver';
const String _oneTapReceiver =
    'tech.mmarca.openvitals.features.homewidgets.HomeQuickBeverageOneTapWidgetReceiver';
const String _metricReceiver =
    'tech.mmarca.openvitals.features.homewidgets.HomeMetricWidgetReceiver';

final _water = CustomHydrationDrink(
  id: 'water',
  name: 'Water',
  volumeMilliliters: 500,
  category: CaffeineSourceCategory.water,
  isPreloaded: true,
);

final _espresso = CustomHydrationDrink(
  id: 'espresso',
  name: 'Espresso',
  volumeMilliliters: 30,
  hydrationMultiplier: 0.5,
  category: CaffeineSourceCategory.coffee,
  nutrientValues: const {NutritionNutrient.caffeine: 63.0},
);

class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository(this.drinks);

  final List<CustomHydrationDrink> drinks;

  @override
  Future<Result<List<CustomHydrationDrink>>> customHydrationDrinks() async =>
      Ok(drinks);

  /// The frequency ranking is a nicety: Health Connect being unreadable must
  /// still leave a usable picker (Kotlin's `runCatching { … }.getOrDefault`).
  @override
  Future<Result<List<HydrationEntry>>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      throw StateError('Health Connect is unavailable');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  @override
  Future<Result<List<NutritionEntry>>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<NutritionEntry>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _StubDashboardDataLoader implements DashboardDataLoader {
  @override
  Future<Result<DashboardData>> loadDashboard(DashboardQuery query) async =>
      Ok(DashboardData(date: LocalDate.now(), loadedMetrics: const {}));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeConfigureChannel implements HomeWidgetConfigureChannel {
  final List<int> finished = [];

  @override
  Future<void> finish(int appWidgetId) async => finished.add(appWidgetId);
}

void main() {
  late AppLocalizations l10n;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  HomeWidgetRefresher refresher(HomeWidgetClient client) => HomeWidgetRefresher(
        service: HomeWidgetService(client: client),
        health: FakeHealthRepository(),
        loadDashboardDay: LoadDashboardDayUseCase(_StubDashboardDataLoader()),
        unitFormatter:
            UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
        localizations: l10n,
        goals: const DailyReadinessGoalInputs(),
      );

  /// The configure launch, exactly as `main()` mounts it: the widget's own
  /// configuration activity says WHAT is being configured, so the picker is
  /// handed a type and an id and has nothing to resolve.
  Future<void> pumpConfigure(
    WidgetTester tester, {
    required FakeHomeWidgetClient client,
    required HomeWidgetConfigureChannel channel,
    required String route,
    List<CustomHydrationDrink> drinks = const [],
  }) async {
    final request = parseHomeWidgetConfigureRoute(route);
    expect(request, isNotNull, reason: 'unparseable configure route: $route');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          homeWidgetServiceProvider
              .overrideWithValue(HomeWidgetService(client: client)),
          homeWidgetRefresherProvider.overrideWithValue(refresher(client)),
          homeWidgetConfigureChannelProvider.overrideWithValue(channel),
          hydrationRepositoryProvider
              .overrideWithValue(_FakeHydrationRepository(drinks)),
          nutritionRepositoryProvider
              .overrideWithValue(_FakeNutritionRepository()),
          unitSystemProvider.overrideWithValue(UnitSystem.metric),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: HomeWidgetConfigurePicker(request: request!),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  /// The instance the widget's own configure activity was launched for. The
  /// class name only matters to the *refresher* now — the picker is told its type.
  FakeHomeWidgetClient clientFor(String className) => FakeHomeWidgetClient(
        installed: [
          HomeWidgetInstance(appWidgetId: _appWidgetId, className: className),
        ],
      );

  group('configure route', () {
    test('carries the widget type and the appWidgetId', () {
      expect(
        parseHomeWidgetConfigureRoute('/widget-configure/metric?appWidgetId=13'),
        const HomeWidgetConfigureRequest(
          widget: HomeWidgetId.metric,
          appWidgetId: 13,
        ),
      );
      expect(
        parseHomeWidgetConfigureRoute(
          '/widget-configure/quickBeverage?appWidgetId=14',
        ),
        const HomeWidgetConfigureRequest(
          widget: HomeWidgetId.quickBeverage,
          appWidgetId: 14,
        ),
      );
      expect(
        parseHomeWidgetConfigureRoute(
          '/widget-configure/quickBeverageOneTap?appWidgetId=15',
        ),
        const HomeWidgetConfigureRequest(
          widget: HomeWidgetId.quickBeverageOneTap,
          appWidgetId: 15,
        ),
      );
    });

    test('an ordinary launch is not a configure launch', () {
      // `main()` must boot the real app for these, not a picker.
      expect(parseHomeWidgetConfigureRoute(null), isNull);
      expect(parseHomeWidgetConfigureRoute('/'), isNull);
      expect(parseHomeWidgetConfigureRoute('/dashboard'), isNull);
    });

    test('a route it cannot fully understand is refused', () {
      // No id, an invalid id (0 == INVALID_APPWIDGET_ID), an unknown widget, and
      // a widget that is not configured per instance: each would leave a picker
      // wired to nothing.
      expect(parseHomeWidgetConfigureRoute('/widget-configure/metric'), isNull);
      expect(
        parseHomeWidgetConfigureRoute('/widget-configure/metric?appWidgetId=0'),
        isNull,
      );
      expect(
        parseHomeWidgetConfigureRoute(
          '/widget-configure/metric?appWidgetId=nope',
        ),
        isNull,
      );
      expect(
        parseHomeWidgetConfigureRoute('/widget-configure/nope?appWidgetId=13'),
        isNull,
      );
      expect(
        parseHomeWidgetConfigureRoute(
          '/widget-configure/todayVitals?appWidgetId=13',
        ),
        isNull,
      );
    });
  });

  group('widget-type resolution', () {
    testWidgets('a beverage configure route opens the beverage picker',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_beverageReceiver),
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/quickBeverage?appWidgetId=$_appWidgetId',
        drinks: [_water, _espresso],
      );

      // The type comes from the activity, not from the placed instances.
      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsOneWidget);
      expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsNothing);
    });

    testWidgets('the 1x1 opens the same beverage picker', (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_oneTapReceiver),
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/quickBeverageOneTap?appWidgetId=$_appWidgetId',
        drinks: [_water],
      );

      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsOneWidget);
    });

    testWidgets('a metric configure route NEVER opens the beverage picker',
        (tester) async {
      // The bug this whole flow was rebuilt for: a metric tile showed the drink
      // list, because the type was guessed from a stale appWidgetId. Even with a
      // beverage instance sitting under that very id, the metric route wins.
      await pumpConfigure(
        tester,
        client: clientFor(_beverageReceiver),
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/metric?appWidgetId=$_appWidgetId',
        drinks: [_water, _espresso],
      );

      expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsOneWidget);
      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsNothing);
      expect(find.text('Water - 500 ml'), findsNothing);
    });

    testWidgets('a beverage route NEVER opens the metric picker, either',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_metricReceiver),
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/quickBeverage?appWidgetId=$_appWidgetId',
        drinks: [_water],
      );

      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsOneWidget);
      expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsNothing);
    });
  });

  group('beverage picker', () {
    testWidgets('lists the drinks as "<name> - <amount>", in catalog order',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_beverageReceiver),
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/quickBeverage?appWidgetId=$_appWidgetId',
        drinks: [_water, _espresso],
      );

      // The user's espresso outranks the preloaded water, whatever their
      // categories say.
      final tiles = tester
          .widgetList<ListTile>(find.byType(ListTile))
          .map((tile) => (tile.title! as Text).data)
          .toList();
      expect(tiles, ['Espresso - 30 ml', 'Water - 500 ml']);
    });

    testWidgets('an empty catalog says so rather than showing a blank list',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_beverageReceiver),
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/quickBeverage?appWidgetId=$_appWidgetId',
      );

      expect(find.text(l10n.homeQuickBeverageWidgetNoDrinks), findsOneWidget);
      expect(find.byType(ListTile), findsNothing);
    });

    testWidgets(
        'picking a drink persists the selection and the payload, pushes, finishes',
        (tester) async {
      final client = clientFor(_beverageReceiver);
      final channel = _FakeConfigureChannel();

      await pumpConfigure(
        tester,
        client: client,
        channel: channel,
        route: '/widget-configure/quickBeverage?appWidgetId=$_appWidgetId',
        drinks: [_water, _espresso],
      );
      await tester.tap(find.text('Espresso - 30 ml'));
      await tester.pumpAndSettle();

      // The handshake a refresh reads back.
      expect(client.saved['beverage.$_appWidgetId.selection_id'], 'espresso');
      // The payload: what makes the background tap loggable without drift — the
      // caffeine included.
      final drink = decodeQuickBeverageDrink(
        client.saved['beverage.$_appWidgetId.$homeWidgetDrinkPayloadKey']
            as String?,
      );
      expect(drink, isNotNull);
      expect(drink!.id, 'espresso');
      expect(drink.hydrationMultiplier, 0.5);
      expect(drink.nutrientValues, {NutritionNutrient.caffeine: 63.0});
      // …and the first snapshot, so the tile is not blank until the next refresh.
      expect(client.saved['beverage.$_appWidgetId.title'], 'Espresso');
      expect(client.saved['beverage.$_appWidgetId.value'], '30ml');
      expect(
        client.saved['beverage.$_appWidgetId.subtitle'],
        l10n.homeQuickBeverageWidgetTapToLog,
      );
      expect(
        client.saved['beverage.$_appWidgetId.route'],
        'manual_entry/hydration/log/espresso',
      );
      expect(client.updated, contains(_beverageReceiver));
      // Only now may the widget be kept — for the id the activity was launched
      // with, which is the id the launcher is waiting on.
      expect(channel.finished, [_appWidgetId]);
    });

    testWidgets('the 1x1 pick pushes to the 1x1 receiver', (tester) async {
      final client = clientFor(_oneTapReceiver);

      await pumpConfigure(
        tester,
        client: client,
        channel: _FakeConfigureChannel(),
        route: '/widget-configure/quickBeverageOneTap?appWidgetId=$_appWidgetId',
        drinks: [_water],
      );
      await tester.tap(find.text('Water - 500 ml'));
      await tester.pumpAndSettle();

      // Both beverage widgets share the `beverage.<id>.` namespace, but each is
      // its own provider: the wrong receiver would leave the tile stale.
      expect(client.updated, contains(_oneTapReceiver));
      expect(client.updated, isNot(contains(_beverageReceiver)));
    });

    testWidgets('backing out without picking never finishes the configuration',
        (tester) async {
      final client = clientFor(_beverageReceiver);
      final channel = _FakeConfigureChannel();

      await pumpConfigure(
        tester,
        client: client,
        channel: channel,
        route: '/widget-configure/quickBeverage?appWidgetId=$_appWidgetId',
        drinks: [_water],
      );

      // RESULT_CANCELED stands, so Android drops the half-placed widget.
      expect(channel.finished, isEmpty);
      expect(client.saved, isEmpty);
    });
  });

  group('refresh', () {
    test('re-pushes a configured instance from its cached payload', () async {
      final client = FakeHomeWidgetClient(
        installed: const [
          HomeWidgetInstance(
            appWidgetId: _appWidgetId,
            className: _beverageReceiver,
          ),
        ],
        stored: {
          'beverage.$_appWidgetId.selection_id': 'espresso',
          'beverage.$_appWidgetId.$homeWidgetDrinkPayloadKey':
              encodeQuickBeverageDrink(_espresso),
          // A stale error left behind by a failed tap.
          'beverage.$_appWidgetId.subtitle': 'Unable to update',
        },
      );

      await refresher(client)
          .push(DashboardData(date: LocalDate.now(), loadedMetrics: const {}));

      // Rebuilt from the payload — the alarm isolate never opens drift — and the
      // stale error is cleared back to the resting subtitle.
      expect(client.saved['beverage.$_appWidgetId.title'], 'Espresso');
      expect(client.saved['beverage.$_appWidgetId.value'], '30ml');
      expect(
        client.saved['beverage.$_appWidgetId.subtitle'],
        l10n.homeQuickBeverageWidgetTapToLog,
      );
      expect(client.updated, contains(_beverageReceiver));
    });

    test('leaves an unconfigured instance on its native state', () async {
      final client = FakeHomeWidgetClient(
        installed: const [
          HomeWidgetInstance(
            appWidgetId: _appWidgetId,
            className: _beverageReceiver,
          ),
        ],
      );

      await refresher(client)
          .push(DashboardData(date: LocalDate.now(), loadedMetrics: const {}));

      // Pushing a snapshot over it would claim it is configured.
      expect(
        client.saved.keys.where((key) => key.startsWith('beverage.')),
        isEmpty,
      );
    });
  });
}
