import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';
import 'package:openvitals/domain/query/nutrition_period_data.dart';
import 'package:openvitals/core/presentation/metric_detail_sections.dart';
import 'package:openvitals/features/nutrition/presentation/nutrition_metric_screen.dart';
import 'package:openvitals/features/nutrition/presentation/nutrition_screen.dart';
import 'package:openvitals/features/nutrition/presentation/nutrition_sections.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/components/daily_goal_components.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

/// A fake [NutritionRepository] returning canned period data. Nutrition entries
/// are read-only, so only the read methods are backed.
class _FakeNutritionRepository implements NutritionRepository {
  _FakeNutritionRepository({
    this.dailyMacros = const <DailyMacros>[],
    this.entries = const <NutritionEntry>[],
  });

  final List<DailyMacros> dailyMacros;
  final List<NutritionEntry> entries;

  @override
  Future<Result<NutritionPeriodData>> loadNutritionPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      Ok(NutritionPeriodData(dailyMacros: dailyMacros, entries: entries));

  // The notifier folds in the previous + baseline windows for the statistics
  // section; those windows are empty in these tests.
  @override
  Future<Result<List<DailyMacros>>> loadDailyMacros(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyMacros>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

DailyMacros _macros(LocalDate date) => DailyMacros(
      date: date,
      energyKcal: 1900,
      proteinGrams: 85,
      carbsGrams: 210,
      fatGrams: 62,
      nutrientValues: const {
        NutritionNutrient.energy: 1900,
        NutritionNutrient.protein: 85,
        NutritionNutrient.totalCarbohydrate: 210,
        NutritionNutrient.totalFat: 62,
        NutritionNutrient.sodium: 2.1,
        NutritionNutrient.vitaminC: 0.06,
      },
    );

NutritionEntry _entry(DateTime time) => NutritionEntry(
      time: time,
      mealType: 0,
      name: 'Lunch',
      energyKcal: 700,
      proteinGrams: 35,
      carbsGrams: 80,
      fatGrams: 20,
      fiberGrams: 8,
      sugarGrams: 12,
      source: 'Test source',
    );

Future<Widget> _bootstrap({
  required _FakeNutritionRepository repository,
  required Set<String> granted,
  required Widget home,
  Map<String, Object> initialPrefs = const <String, Object>{},
}) async {
  SharedPreferences.setMockInitialValues(initialPrefs);
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      nutritionRepositoryProvider.overrideWithValue(repository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: home,
    ),
  );
}

void main() {
  final today = LocalDate.now();

  testWidgets(
      'Protein metric screen renders hero, chart, goal card, statistics and meals',
      (tester) async {
    // A tall surface so every ordered section lays out (the screen scrolls in a
    // lazy ListView otherwise, leaving lower sections unbuilt).
    await tester.binding.setSurfaceSize(const Size(1000, 3200));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final repo = _FakeNutritionRepository(
      dailyMacros: [_macros(today), _macros(today.minusDays(1))],
      entries: [_entry(DateTime.now())],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        granted: {HcPermissions.readNutrition},
        home: const ProteinScreen(),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // Hero summary card + the per-metric trend chart.
    expect(find.byType(MetricCard), findsWidgets);
    expect(find.byType(MetricBarChart), findsOneWidget);
    // The daily-goal card is preserved as its own ordered section.
    expect(find.byType(DailyGoalCard), findsOneWidget);
    // The statistics section header.
    expect(find.text('Statistics'), findsWidgets);
    // The meals (ENTRIES) section renders the logged nutrition entry.
    expect(find.byType(NutritionEntryRow), findsWidgets);
    expect(find.text('Protein'), findsWidgets);
  });

  testWidgets('Protein metric screen shows placeholder with no data',
      (tester) async {
    final repo = _FakeNutritionRepository();
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        granted: {HcPermissions.readNutrition},
        home: const ProteinScreen(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
    expect(find.byType(DailyGoalCard), findsNothing);
  });

  testWidgets('Nutrition overview renders grouped nutrient statistics',
      (tester) async {
    await tester.binding.setSurfaceSize(const Size(1000, 3200));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final repo = _FakeNutritionRepository(
      dailyMacros: [_macros(today)],
      entries: [_entry(DateTime.now())],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        granted: {HcPermissions.readNutrition},
        home: const NutritionScreen(),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // The all-nutrient statistics section: primary macros header + grouped
    // totals for the tracked non-primary nutrients.
    expect(find.text('Statistics'), findsWidgets);
    expect(find.text('Minerals'), findsWidgets);
    expect(find.text('Sodium'), findsWidgets);
    // The overview also lists the logged meal.
    expect(find.byType(NutritionEntryRow), findsWidgets);
  });

  testWidgets('Nutrition screen shows the access gate when permission missing',
      (tester) async {
    final repo = _FakeNutritionRepository();
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        granted: const <String>{},
        home: const NutritionScreen(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
  });

  test('Reordering a metric detail section persists across rebuilds', () async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);

    final defaultOrder = container.read(metricDetailSectionOrderProvider);
    expect(defaultOrder.first, MetricDetailSectionId.activitySummary);

    // Nudge the first section down; the notifier must persist the new order.
    container
        .read(metricDetailSectionOrderProvider.notifier)
        .moveSection(MetricDetailSectionId.activitySummary, 1);

    final movedOrder = container.read(metricDetailSectionOrderProvider);
    expect(movedOrder.first, isNot(MetricDetailSectionId.activitySummary));

    // A fresh container reading the same preferences sees the persisted order.
    final reopened = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(reopened.dispose);
    expect(
      reopened.read(metricDetailSectionOrderProvider),
      movedOrder,
    );
  });
}
