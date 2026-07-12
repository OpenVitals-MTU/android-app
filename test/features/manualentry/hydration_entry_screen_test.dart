import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/core/presentation/measurement_input.dart';
import 'package:openvitals/features/manualentry/application/hydration_entry_notifier.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/manualentry/presentation/hydration_catalog_widgets.dart';
import 'package:openvitals/features/manualentry/presentation/hydration_entry_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';


/// An in-memory drink store + write log, standing in for prefs + Health Connect.
class _FakeHydrationRepository implements HydrationRepository {
  final List<CustomHydrationDrink> drinks = [];
  final Map<String, double> containerOverrides = {};
  final List<HydrationWriteRequest> writes = [];
  List<String>? lastReorder;
  double? lastCustomAmount;

  @override
  Set<String> get hydrationWritePermissions => {HcPermissions.writeHydration};

  @override
  Map<String, double> hydrationContainerVolumeMilliliters() =>
      Map<String, double>.of(containerOverrides);

  @override
  void setHydrationContainerVolumeMilliliters(String id, double milliliters) =>
      containerOverrides[id] = milliliters;

  @override
  double hydrationDailyGoalLiters() => 2.5;

  @override
  double? lastCustomHydrationAmountMilliliters() => lastCustomAmount;

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) =>
      lastCustomAmount = milliliters;

  @override
  Future<List<CustomHydrationDrink>> customHydrationDrinks() async =>
      List<CustomHydrationDrink>.of(drinks);

  @override
  Future<void> saveCustomHydrationDrink(CustomHydrationDrink drink) async {
    final index = drinks.indexWhere((it) => it.id == drink.id);
    if (index >= 0) {
      drinks[index] = drink;
    } else {
      drinks.add(drink);
    }
  }

  @override
  Future<void> deleteCustomHydrationDrink(String drinkId) async =>
      drinks.removeWhere((it) => it.id == drinkId);

  @override
  Future<void> reorderCustomHydrationDrinks(List<String> drinkIds) async {
    lastReorder = drinkIds;
    drinks.sort((a, b) => drinkIds.indexOf(a.id).compareTo(drinkIds.indexOf(b.id)));
  }

  @override
  Future<void> moveCustomHydrationDrinkToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  ) async {
    final index = drinks.indexWhere((it) => it.id == drinkId);
    if (index >= 0) drinks[index] = drinks[index].copyWith(category: category);
  }

  @override
  Future<bool> hasHydrationWritePermission() async => true;

  @override
  Future<List<DailyHydration>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <DailyHydration>[];

  /// Entries the frequent-drink ranking reads back.
  List<HydrationEntry> hydrationEntries = const <HydrationEntry>[];

  @override
  Future<List<HydrationEntry>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      hydrationEntries;

  @override
  Future<String> writeHydrationEntry(HydrationWriteRequest request) async {
    writes.add(request);
    return 'hydration-id';
  }

  /// Edits go through update, not write; keyed by the record they replace.
  final Map<String, HydrationWriteRequest> updates = {};

  @override
  Future<void> updateHydrationEntry(
    String id,
    HydrationWriteRequest request,
  ) async =>
      updates[id] = request;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  final List<NutritionWriteRequest> writes = [];

  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<bool> hasNutritionWritePermission() async => true;

  List<NutritionEntry> nutritionEntries = const <NutritionEntry>[];

  @override
  Future<List<NutritionEntry>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      nutritionEntries;

  @override
  Future<String> writeNutritionEntry(NutritionWriteRequest request) async {
    writes.add(request);
    return 'nutrition-id';
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}


void main() {
  /// Pumps the screen with the unit system pinned. Without the override the
  /// preference falls back to the *host* locale, so an en_US machine renders
  /// the amount field in fluid ounces and a metric machine in millilitres —
  /// the same test would assert different numbers on different laptops.
  Future<void> pumpScreen(
    WidgetTester tester,
    _FakeHydrationRepository hydrationRepo,
    _FakeNutritionRepository nutritionRepo, {
    String? logDrinkId,
    UnitSystem unitSystem = UnitSystem.metric,
  }) async {
    tester.view.physicalSize = const Size(900, 2000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeHydration}),
          hydrationRepositoryProvider.overrideWithValue(hydrationRepo),
          nutritionRepositoryProvider.overrideWithValue(nutritionRepo),
          unitSystemProvider.overrideWithValue(unitSystem),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: HydrationEntryScreen(logDrinkId: logDrinkId),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('creates a custom drink and logs it from the saved list',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository();
    final nutritionRepo = _FakeNutritionRepository();
    await pumpScreen(tester, hydrationRepo, nutritionRepo);

    // The custom-drink dialog carries the category selector and all three
    // hydration-impact choices.
    await tester.tap(find.text('New drink'));
    await tester.pumpAndSettle();
    expect(
      find.byType(RadioListTile<HydrationImpactOption>),
      findsNWidgets(3),
    );
    expect(
      find.byType(DropdownButtonFormField<CaffeineSourceCategory?>),
      findsOneWidget,
    );

    await tester.enterText(find.widgetWithText(TextField, 'Name'), 'Cola');
    await tester
        .enterText(find.widgetWithText(TextField, 'Amount (ml)').last, '330');
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    expect(hydrationRepo.drinks.single.name, 'Cola');
    expect(find.textContaining('Cola'), findsOneWidget);
    expect(tester.takeException(), isNull);

    // Tapping the row opens the entry dialog; saving writes the drink's volume.
    await tester.tap(find.textContaining('Cola'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    expect(hydrationRepo.writes.single.volumeLiters, closeTo(0.33, 1e-9));
    // Tagged with the generated drink id so the entry is attributable.
    expect(
      hydrationRepo.writes.single.drinkId,
      hydrationRepo.drinks.single.id,
    );
  });

  /// Pushes the screen onto a route so that a `pop` actually removes it —
  /// as `home:` it would survive one regardless, hiding the behaviour.
  Future<void> pushScreen(
    WidgetTester tester,
    _FakeHydrationRepository hydrationRepo, {
    String? hydrationEntryId,
  }) async {
    tester.view.physicalSize = const Size(900, 2000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {HcPermissions.writeHydration}),
          hydrationRepositoryProvider.overrideWithValue(hydrationRepo),
          nutritionRepositoryProvider
              .overrideWithValue(_FakeNutritionRepository()),
          unitSystemProvider.overrideWithValue(UnitSystem.metric),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Builder(
            builder: (context) => Scaffold(
              body: ElevatedButton(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) =>
                        HydrationEntryScreen(hydrationEntryId: hydrationEntryId),
                  ),
                ),
                child: const Text('open'),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
  }

  testWidgets('logging a drink keeps the catalog open for the next one',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository()
      ..drinks.add(const CustomHydrationDrink(
        id: 'd-cola',
        name: 'Cola',
        volumeMilliliters: 330,
      ));
    await pushScreen(tester, hydrationRepo);

    await tester.tap(find.textContaining('Cola'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    expect(hydrationRepo.writes, hasLength(1));
    // The confirmation shows, but the screen stays put: you log several drinks
    // in a row without re-navigating from Add entry.
    expect(find.text('Hydration entry saved'), findsOneWidget);
    expect(find.byType(HydrationEntryScreen), findsOneWidget);
  });

  testWidgets('editing an existing entry returns after saving', (tester) async {
    final now = DateTime.now();
    final hydrationRepo = _FakeHydrationRepository()
      ..hydrationEntries = [
        HydrationEntry(
          id: 'entry-1',
          startTime: now.subtract(const Duration(hours: 1)),
          endTime: now.subtract(const Duration(hours: 1)),
          liters: 0.33,
          source: 'openvitals',
          isOpenVitalsEntry: true,
        ),
      ];
    await pushScreen(tester, hydrationRepo, hydrationEntryId: 'entry-1');

    // Edit mode shows the timestamp form and a Save, not the catalog.
    expect(find.byType(HydrationCatalogCarousel), findsNothing);
    await tester.tap(find.widgetWithText(FilledButton, 'Save'));
    await tester.pumpAndSettle();

    // The edit updates the record in place rather than writing a new one.
    expect(hydrationRepo.updates.keys, ['entry-1']);
    expect(hydrationRepo.writes, isEmpty);

    // A one-shot edit pops back to where it was opened from.
    expect(find.byType(HydrationEntryScreen), findsNothing);
    expect(find.text('open'), findsOneWidget);
  });

  testWidgets('an imperial user types fluid ounces and stores millilitres',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository();
    await pumpScreen(
      tester,
      hydrationRepo,
      _FakeNutritionRepository(),
      unitSystem: UnitSystem.imperial,
    );

    await tester.tap(find.text('New drink'));
    await tester.pumpAndSettle();

    // The field announces its own unit; there is no "mL" label in sight.
    expect(find.widgetWithText(TextField, 'Amount (fl oz)'), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Amount (ml)'), findsNothing);

    await tester.enterText(find.widgetWithText(TextField, 'Name'), 'Cola');
    await tester.enterText(
      find.widgetWithText(TextField, 'Amount (fl oz)').last,
      '12',
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    // Storage is metric regardless of what the user sees: 12 fl oz ≈ 354.9 ml.
    expect(
      hydrationRepo.drinks.single.volumeMilliliters,
      closeTo(12 * kMillilitersPerFluidOunce, 1e-9),
    );

    // ...and the write that follows carries the same canonical volume.
    await tester.tap(find.textContaining('Cola'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    // The dialog re-seeds the field as "12.0" fl oz, so it round-trips.
    expect(
      hydrationRepo.writes.single.volumeLiters,
      closeTo(12 * kMillilitersPerFluidOunce / 1000, 1e-9),
    );
  });

  testWidgets('rejects an out-of-range custom drink volume', (tester) async {
    final hydrationRepo = _FakeHydrationRepository();
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    await tester.tap(find.text('New drink'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Name'), 'Vat');
    await tester
        .enterText(find.widgetWithText(TextField, 'Amount (ml)').last, '0');
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    // The dialog stays open with an error and nothing is persisted.
    expect(hydrationRepo.drinks, isEmpty);
    expect(find.byType(AlertDialog), findsOneWidget);
  });

  testWidgets('a partial-hydration drink needs a percent strictly under 100',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository();
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    await tester.tap(find.text('New drink'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Name'), 'Beer');
    await tester
        .enterText(find.widgetWithText(TextField, 'Amount (ml)').last, '330');
    await tester.tap(find.text('Counts partially'));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.widgetWithText(TextField, 'Counts as hydration (%)'),
      '100',
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();

    expect(hydrationRepo.drinks, isEmpty);
    expect(find.text('Enter a percentage above 0 and below 100.'), findsOneWidget);

    // 40% is accepted and round-trips as a 0.4 multiplier.
    await tester.enterText(
      find.widgetWithText(TextField, 'Counts as hydration (%)'),
      '40',
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();
    expect(hydrationRepo.drinks.single.hydrationMultiplier, closeTo(0.4, 1e-9));
  });

  testWidgets('the logDrinkId deep link opens that drink\'s entry dialog',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository()
      ..drinks.add(const CustomHydrationDrink(
        id: 'd-cola',
        name: 'Cola',
        volumeMilliliters: 330,
      ));
    await pumpScreen(
      tester,
      hydrationRepo,
      _FakeNutritionRepository(),
      logDrinkId: 'd-cola',
    );

    // Straight into the entry dialog for that drink, not the plain form.
    expect(find.byType(AlertDialog), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Save').last);
    await tester.pumpAndSettle();
    expect(hydrationRepo.writes.single.volumeLiters, closeTo(0.33, 1e-9));
  });

  testWidgets('an unknown logDrinkId opens the plain form', (tester) async {
    final hydrationRepo = _FakeHydrationRepository();
    await pumpScreen(
      tester,
      hydrationRepo,
      _FakeNutritionRepository(),
      logDrinkId: 'missing',
    );

    expect(find.byType(AlertDialog), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('shows today\'s hydration against the daily goal', (tester) async {
    final hydrationRepo = _FakeHydrationRepository();
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    expect(find.byType(HydrationTodayCounter), findsOneWidget);
    // "<today> / <goal>", in whatever unit system the formatter is on.
    expect(find.textContaining(' / '), findsOneWidget);
    expect(find.byType(LinearProgressIndicator), findsOneWidget);
  });

  testWidgets('category sections start collapsed and expand on tap',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository()
      ..drinks.addAll(const [
        CustomHydrationDrink(
          id: 'd-cola',
          name: 'Cola',
          volumeMilliliters: 330,
          category: CaffeineSourceCategory.soda,
        ),
        CustomHydrationDrink(
          id: 'd-plain',
          name: 'Tap water',
          volumeMilliliters: 500,
        ),
      ]);
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    // The section header is there, but its rows are hidden until expanded.
    expect(find.text('Carbonated soft drinks'), findsOneWidget);
    expect(find.textContaining('Cola'), findsNothing);
    // An uncategorized drink is listed flat, always visible.
    expect(find.textContaining('Tap water'), findsOneWidget);

    await tester.tap(find.text('Carbonated soft drinks'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Cola'), findsOneWidget);
  });

  testWidgets('searching force-expands the sections and filters the rows',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository()
      ..drinks.addAll(const [
        CustomHydrationDrink(
          id: 'd-cola',
          name: 'Cola',
          volumeMilliliters: 330,
          category: CaffeineSourceCategory.soda,
        ),
        CustomHydrationDrink(
          id: 'd-brew',
          name: 'Cold brew',
          volumeMilliliters: 250,
          category: CaffeineSourceCategory.coffee,
        ),
        CustomHydrationDrink(
          id: 'd-plain',
          name: 'Tap water',
          volumeMilliliters: 500,
        ),
      ]);
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    await tester.enterText(
      find.widgetWithText(TextField, 'Search drinks'),
      'brew',
    );
    await tester.pumpAndSettle();

    // Its collapsed coffee section opens because a search is active.
    expect(find.textContaining('Cold brew'), findsOneWidget);
    expect(find.textContaining('Cola'), findsNothing);
    expect(find.textContaining('Tap water'), findsNothing);
  });

  testWidgets('the edit toggle swaps logging for edit/move/delete actions',
      (tester) async {
    final hydrationRepo = _FakeHydrationRepository()
      ..drinks.add(const CustomHydrationDrink(
        id: 'd-plain',
        name: 'Tap water',
        volumeMilliliters: 500,
      ));
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    // Not editing: no row actions.
    expect(find.byTooltip('Edit drink'), findsNothing);
    expect(find.byTooltip('Delete drink'), findsNothing);

    await tester.tap(find.byTooltip('Edit saved drinks'));
    await tester.pumpAndSettle();

    expect(find.byTooltip('Edit drink'), findsOneWidget);
    expect(find.byTooltip('Delete drink'), findsOneWidget);
    expect(find.byTooltip('Move drink category'), findsOneWidget);

    // Deleting removes it from storage.
    await tester.tap(find.byTooltip('Delete drink'));
    await tester.pumpAndSettle();
    expect(hydrationRepo.drinks, isEmpty);
  });

  testWidgets('a frequently-logged drink surfaces in its own section',
      (tester) async {
    const cola = CustomHydrationDrink(
      id: 'd-cola',
      name: 'Cola',
      volumeMilliliters: 330,
      category: CaffeineSourceCategory.soda,
    );
    final when = DateTime.now().subtract(const Duration(days: 1));
    final hydrationRepo = _FakeHydrationRepository()
      ..drinks.add(cola)
      ..hydrationEntries = [
        HydrationEntry(
          startTime: when,
          endTime: when,
          liters: 0.33,
          source: 'openvitals',
          isOpenVitalsEntry: true,
          clientRecordId:
              'openvitals_hydration_${when.millisecondsSinceEpoch}_drink_d-cola_u1',
        ),
      ];
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    expect(find.text('Frequently consumed'), findsOneWidget);
    // It is not repeated under its category section.
    expect(find.textContaining('Cola'), findsOneWidget);
    expect(find.text('Carbonated soft drinks'), findsNothing);
  });

  testWidgets('a drink logged only via a partial amount still hits the catalog',
      (tester) async {
    // Guards the zero-hydration case: nutrient-only drinks must still be
    // loggable when the nutrition permission is held.
    const shot = CustomHydrationDrink(
      id: 'd-shot',
      name: 'Espresso shot',
      volumeMilliliters: 30,
      hydrationMultiplier: 0,
      nutrientValues: {NutritionNutrient.caffeine: 0.08},
    );
    final hydrationRepo = _FakeHydrationRepository()..drinks.add(shot);
    await pumpScreen(tester, hydrationRepo, _FakeNutritionRepository());

    // Labelled as not counting toward hydration.
    expect(find.textContaining('Espresso shot'), findsOneWidget);
    expect(find.textContaining('Does not count as hydration'), findsOneWidget);
  });
}
