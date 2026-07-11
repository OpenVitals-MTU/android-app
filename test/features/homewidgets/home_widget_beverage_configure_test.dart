import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
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
  Future<List<CustomHydrationDrink>> customHydrationDrinks() async => drinks;

  /// The frequency ranking is a nicety: Health Connect being unreadable must
  /// still leave a usable picker (Kotlin's `runCatching { … }.getOrDefault`).
  @override
  Future<List<HydrationEntry>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      throw StateError('Health Connect is unavailable');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  @override
  Future<List<NutritionEntry>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <NutritionEntry>[];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _StubDashboardDataLoader implements DashboardDataLoader {
  @override
  Future<DashboardData> loadDashboard(DashboardQuery query) async =>
      DashboardData(date: LocalDate.now(), loadedMetrics: const {});

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeConfigureChannel implements HomeWidgetConfigureChannel {
  int finished = 0;

  @override
  Future<int?> pendingAppWidgetId() async => _appWidgetId;

  @override
  Future<void> finish() async => finished++;
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

  /// The configure launch, exactly as `main()` mounts it: only the appWidgetId is
  /// known, and the picker has to work out which widget it belongs to.
  Future<void> pumpConfigure(
    WidgetTester tester, {
    required FakeHomeWidgetClient client,
    required HomeWidgetConfigureChannel channel,
    List<CustomHydrationDrink> drinks = const [],
  }) async {
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
          home: const HomeWidgetConfigurePicker(appWidgetId: _appWidgetId),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  FakeHomeWidgetClient clientFor(String className) => FakeHomeWidgetClient(
        installed: [
          HomeWidgetInstance(appWidgetId: _appWidgetId, className: className),
        ],
      );

  group('widget-type resolution', () {
    testWidgets('a beverage appWidgetId opens the beverage picker',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_beverageReceiver),
        channel: _FakeConfigureChannel(),
        drinks: [_water, _espresso],
      );

      // The configure launch hands over an id, not a type: the receiver class
      // name behind it is what picks the screen.
      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsOneWidget);
      expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsNothing);
    });

    testWidgets('the 1x1 opens the same beverage picker', (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_oneTapReceiver),
        channel: _FakeConfigureChannel(),
        drinks: [_water],
      );

      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsOneWidget);
    });

    testWidgets('a metric appWidgetId still opens the metric picker',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_metricReceiver),
        channel: _FakeConfigureChannel(),
      );

      expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsOneWidget);
      expect(find.text(l10n.homeQuickBeverageWidgetConfigPrompt), findsNothing);
    });

    testWidgets('an unplaceable id falls back to the metric picker',
        (tester) async {
      await pumpConfigure(
        tester,
        client: FakeHomeWidgetClient(),
        channel: _FakeConfigureChannel(),
      );

      expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsOneWidget);
    });
  });

  group('beverage picker', () {
    testWidgets('lists the drinks as "<name> - <amount>", in catalog order',
        (tester) async {
      await pumpConfigure(
        tester,
        client: clientFor(_beverageReceiver),
        channel: _FakeConfigureChannel(),
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
        drinks: [_water, _espresso],
        // ignore: require_trailing_commas
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
      // Only now may the widget be kept (the plugin already set RESULT_CANCELED).
      expect(channel.finished, 1);
    });

    testWidgets('the 1x1 pick pushes to the 1x1 receiver', (tester) async {
      final client = clientFor(_oneTapReceiver);

      await pumpConfigure(
        tester,
        client: client,
        channel: _FakeConfigureChannel(),
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
        drinks: [_water],
      );

      // RESULT_CANCELED stands, so Android drops the half-placed widget.
      expect(channel.finished, 0);
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
