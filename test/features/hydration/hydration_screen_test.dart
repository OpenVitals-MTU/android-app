import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/hydration_period_data.dart';
import 'package:openvitals/features/hydration/presentation/hydration_screen.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';
import 'package:openvitals/ui/components/ov_card.dart';

/// This app's own package name — what Health Connect reports as the `source` of
/// every record OpenVitals writes. It is never a drink name.
const String _ownPackage = 'tech.mmarca.openvitals';

/// A fake [HydrationRepository] returning canned period data.
class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository({
    this.dailyHydration = const <DailyHydration>[],
    this.entries = const <HydrationEntry>[],
  });

  final List<DailyHydration> dailyHydration;
  final List<HydrationEntry> entries;

  @override
  double hydrationDailyGoalLiters() => 2.0;

  @override
  Future<Result<HydrationPeriodData>> loadHydrationPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      Ok(HydrationPeriodData(
        dailyHydration: dailyHydration,
        hydrationEntries: entries,
      ));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// A fake [NutritionRepository] serving the nutrition half of the beverage log.
class _FakeNutritionRepository implements NutritionRepository {
  _FakeNutritionRepository({this.entries = const <NutritionEntry>[]});

  final List<NutritionEntry> entries;

  @override
  Future<Result<List<NutritionEntry>>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      Ok(entries);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

HydrationEntry _drink(
  DateTime start,
  double liters,
  String source, {
  String id = '',
  String? clientRecordId,
  bool isOpenVitalsEntry = false,
}) =>
    HydrationEntry(
      startTime: start,
      endTime: start.add(const Duration(minutes: 1)),
      liters: liters,
      source: source,
      id: id,
      clientRecordId: clientRecordId,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

/// The nutrition record OpenVitals writes next to a logged drink: it is the only
/// place the drink's *name* lives, tied to the hydration record by prefixing its
/// client-record-id.
NutritionEntry _drinkNutrition(
  DateTime time,
  String name, {
  required String id,
  String? pairedHydrationClientRecordId,
  String? standaloneClientRecordId,
  Map<NutritionNutrient, double> nutrients =
      const <NutritionNutrient, double>{},
}) =>
    NutritionEntry(
      time: time,
      mealType: 0,
      name: name,
      energyKcal: null,
      proteinGrams: null,
      carbsGrams: null,
      fatGrams: null,
      fiberGrams: null,
      sugarGrams: null,
      source: _ownPackage,
      nutrientValues: nutrients,
      id: id,
      clientRecordId: pairedHydrationClientRecordId != null
          ? 'openvitals_hydration_nutrition_$pairedHydrationClientRecordId'
          : standaloneClientRecordId,
      isOpenVitalsEntry: true,
    );

Future<Widget> _bootstrap({
  required _FakeHydrationRepository repository,
  required Set<String> granted,
  _FakeNutritionRepository? nutrition,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      hydrationRepositoryProvider.overrideWithValue(repository),
      nutritionRepositoryProvider
          .overrideWithValue(nutrition ?? _FakeNutritionRepository()),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: HydrationScreen(),
    ),
  );
}

/// The hydration screen is a long scroller (summary, chart, goal, breakdown,
/// statistics, reminders, entries). The default 800×600 test surface builds only
/// the top of it, so the sections under test are given a viewport tall enough to
/// hold the whole screen rather than being scrolled to one by one.
void _useTallViewport(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 3000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

void main() {
  final today = LocalDate.now();

  testWidgets('Hydration screen renders summary + bar chart once loaded',
      (tester) async {
    final now = DateTime.now();
    final repo = _FakeHydrationRepository(
      dailyHydration: [
        DailyHydration(date: today, liters: 1.8),
        DailyHydration(date: today.minusDays(1), liters: 2.2),
      ],
      entries: [
        _drink(now, 0.5, 'Water bottle'),
        _drink(now.subtract(const Duration(hours: 2)), 0.3, 'Water bottle'),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readHydration}),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(MetricCard), findsWidgets);
    expect(find.byType(MetricBarChart), findsOneWidget);
    expect(find.text('Total hydration'), findsOneWidget);
  });

  testWidgets('the drink breakdown names the drink, never its package',
      (tester) async {
    _useTallViewport(tester);
    // Regression: the breakdown labelled each slice `displayName ?? source`, and
    // nothing ever populated `displayName` — so a drink this app logged itself
    // was rendered as its own package name, "tech.mmarca.openvitals".
    final now = DateTime.now();
    const coffeeRecordId = 'openvitals_hydration_1700_drink_coffee_abc';
    final repo = _FakeHydrationRepository(
      dailyHydration: [DailyHydration(date: today, liters: 0.5)],
      entries: [
        _drink(now, 0.5, _ownPackage,
            id: 'h1',
            clientRecordId: coffeeRecordId,
            isOpenVitalsEntry: true),
      ],
    );
    final nutrition = _FakeNutritionRepository(
      entries: [
        _drinkNutrition(
          now,
          'Flat white',
          id: 'n1',
          pairedHydrationClientRecordId: coffeeRecordId,
          nutrients: const {NutritionNutrient.caffeine: 80.0},
        ),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        nutrition: nutrition,
        granted: {HcPermissions.readHydration, HcPermissions.readNutrition},
      ),
    );
    await tester.pumpAndSettle();

    final breakdown = find.ancestor(
      of: find.text('Drink breakdown'),
      matching: find.byType(OpenVitalsCard),
    );
    expect(breakdown, findsOneWidget);
    // The name off the paired nutrition record — not the package.
    expect(
      find.descendant(of: breakdown, matching: find.text('Flat white')),
      findsOneWidget,
    );
    expect(
      find.descendant(of: breakdown, matching: find.text(_ownPackage)),
      findsNothing,
      reason: 'the drink breakdown rendered a package name as a drink name',
    );
  });

  testWidgets('the Day view lists each logged beverage', (tester) async {
    _useTallViewport(tester);
    // Regression: the hydration screen rendered no entry list at all, so the
    // per-entry "line info" (drink, time, source, volume) was simply absent —
    // and nutrition-only beverages were never merged in to begin with.
    final now = DateTime.now();
    const teaRecordId = 'openvitals_hydration_1700_drink_tea_abc';
    final repo = _FakeHydrationRepository(
      dailyHydration: [DailyHydration(date: today, liters: 0.4)],
      entries: [
        _drink(now, 0.4, _ownPackage,
            id: 'h1', clientRecordId: teaRecordId, isOpenVitalsEntry: true),
      ],
    );
    final nutrition = _FakeNutritionRepository(
      entries: [
        _drinkNutrition(
          now,
          'Green tea',
          id: 'n1',
          pairedHydrationClientRecordId: teaRecordId,
        ),
        // A beverage logged with nutrients but no volume: it has no hydration
        // record of its own, and must still appear in the history.
        _drinkNutrition(
          now.subtract(const Duration(hours: 1)),
          'Espresso',
          id: 'n2',
          standaloneClientRecordId: 'openvitals_nutrition_1699_xyz',
          nutrients: const {NutritionNutrient.caffeine: 60.0},
        ),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        nutrition: nutrition,
        granted: {HcPermissions.readHydration, HcPermissions.readNutrition},
      ),
    );
    await tester.pumpAndSettle();

    // The entries section exists at all...
    expect(find.text('Entries'), findsOneWidget,
        reason: 'the Day view lists no entries');
    // ...the hydrating drink is named and shows its volume...
    expect(find.text('Green tea'), findsWidgets);
    // ...and the nutrition-only beverage is listed, reporting no volume.
    expect(find.text('Espresso'), findsOneWidget);
    expect(find.text('No hydration impact'), findsOneWidget);
    // Each row is attributed to its source.
    expect(find.byType(SourceChip), findsWidgets);
  });

  testWidgets('Hydration screen shows the empty placeholder with no data',
      (tester) async {
    final repo = _FakeHydrationRepository();
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readHydration}),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
  });

  testWidgets('Hydration screen shows the access gate when permission missing',
      (tester) async {
    final repo = _FakeHydrationRepository();
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: const <String>{}),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
  });
}
