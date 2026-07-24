import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/nutrition_period_data.dart';
import 'package:openvitals/features/nutrition/application/nutrition_view_model.dart';
import 'package:openvitals/features/nutrition/presentation/nutrition_metric.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the goal-nudge rebuild, the failure mapping, the
/// staleness guard — is what is under test.
class _FakeNutritionRepository implements NutritionRepository {
  _FakeNutritionRepository(this.answer);

  Result<NutritionPeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;

  Result<void> deleteAnswer = const Ok(null);
  final List<String> deletedIds = [];

  @override
  Future<Result<void>> deleteNutritionEntry(String id) async {
    deletedIds.add(id);
    return deleteAnswer;
  }

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<NutritionPeriodData>> gates = [];
  bool gated = false;

  @override
  Future<Result<NutritionPeriodData>> loadNutritionPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    if (gated) {
      final completer = Completer<NutritionPeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  Future<Result<List<DailyMacros>>> loadDailyMacros(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyMacros>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

DailyMacros _macros(LocalDate date, double energy) => DailyMacros(
      date: date,
      energyKcal: energy,
      proteinGrams: 0,
      carbsGrams: 0,
      fatGrams: 0,
      nutrientValues: {NutritionNutrient.energy: energy},
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeNutritionRepository repository;
  late ProviderContainer container;

  final provider = nutritionMetricProvider(NutritionMetric.caloriesIn);

  Future<ProviderContainer> boot(Result<NutritionPeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeNutritionRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      nutritionRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  const monday = LocalDate(2026, 3, 2);
  final selection = PeriodSelection(TimeRange.week, monday);

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(NutritionPeriodData(
      dailyMacros: [_macros(monday, 1800), _macros(monday.plusDays(1), 2200)],
    )));
    container.listen(provider, (_, _) {});

    await container.read(provider.notifier).load(selection);

    final state = container.read(provider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.hasData, isTrue);
    expect(state.display!.metricSeries.total, 4000.0);
    expect(state.display!.metricSeries.average, 2000.0);
    expect(state.display!.goalProgress.target, state.dailyGoal);
  });

  test('nudging the daily goal rebuilds the display against the new target',
      () async {
    await boot(Ok(NutritionPeriodData(
      dailyMacros: [_macros(monday, 1800)],
    )));
    container.listen(provider, (_, _) {});
    final viewModel = container.read(provider.notifier);
    await viewModel.load(selection);

    final before = container.read(provider);
    expect(before.dailyGoal, 2000.0);
    expect(before.display!.goalProgress.goalMetDays, 1,
        reason: 'calories-in is an at-MOST goal: 1800 kcal is under 2000');

    // Step the goal down under the logged day (5 × 50 kcal → 1750). No reload:
    // the same loaded days, a new line to count them against.
    for (var i = 0; i < 5; i++) {
      viewModel.decreaseDailyGoal();
    }

    final after = container.read(provider);
    expect(after.dailyGoal, 1750.0);
    expect(after.display!.goalProgress.target, 1750.0);
    expect(after.display!.goalProgress.goalMetDays, 0,
        reason: 'the display must be rebuilt against the new goal');
    expect(repository.loads, 1, reason: 'a goal nudge must not reload');
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('nutrition read')));
    container.listen(provider, (_, _) {});

    await container.read(provider.notifier).load(selection);

    final state = container.read(provider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(provider, (_, _) {});

    await container.read(provider.notifier).load(selection);

    expect(
      container.read(provider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('refresh reloads the current selection in force mode', () async {
    await boot(const Ok(NutritionPeriodData()));
    container.listen(provider, (_, _) {});
    final viewModel = container.read(provider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(repository.loads, 2);
    expect(repository.lastRefreshMode, RefreshMode.force);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(const Ok(NutritionPeriodData()));
    container.listen(provider, (_, _) {});
    final viewModel = container.read(provider.notifier);
    repository.gated = true;

    // Loads are single-flight: the second parks while the first is on the wire.
    final first = viewModel.load(selection);
    final second = viewModel.load(PeriodSelection(TimeRange.month, monday));
    expect(repository.gates, hasLength(1));
    // The first's answer lands after it was superseded: dropped, and the
    // parked month load dispatches.
    repository.gates[0].complete(NutritionPeriodData(
      dailyMacros: [_macros(monday, 9000)],
    ));
    await Future<void>.delayed(Duration.zero);
    repository.gates[1].complete(NutritionPeriodData(
      dailyMacros: [_macros(monday, 1000)],
    ));
    await Future.wait([first, second]);

    // The month load won: the week's late answer is dropped, not painted.
    final state = container.read(provider);
    expect(state.selectedRange, TimeRange.month);
    expect(state.display!.metricSeries.total, 1000.0);
  });

  group('deleteNutritionEntry', () {
    NutritionEntry meal(String id, {required bool owned}) => NutritionEntry(
          time: DateTime(2026, 3, 2, 12),
          mealType: 0,
          name: 'Lunch',
          energyKcal: 700,
          proteinGrams: 0,
          carbsGrams: 0,
          fatGrams: 0,
          fiberGrams: 0,
          sugarGrams: 0,
          source: 'test',
          id: id,
          isOpenVitalsEntry: owned,
        );

    test('removes an owned meal and deletes it through the repository',
        () async {
      final entry = meal('m1', owned: true);
      await boot(Ok(NutritionPeriodData(
        dailyMacros: [_macros(monday, 700)],
        entries: [entry],
      )));
      container.listen(provider, (_, _) {});
      await container.read(provider.notifier).load(selection);

      repository.answer = const Ok(NutritionPeriodData());
      await container.read(provider.notifier).deleteNutritionEntry('m1');

      expect(repository.deletedIds, ['m1']);
      expect(container.read(provider).entries, isEmpty);
      expect(
        container.read(provider).display!.entriesNewestFirst,
        isEmpty,
      );
    });

    test('ignores a foreign meal it does not own', () async {
      final entry = meal('m1', owned: false);
      await boot(Ok(NutritionPeriodData(
        dailyMacros: [_macros(monday, 700)],
        entries: [entry],
      )));
      container.listen(provider, (_, _) {});
      await container.read(provider.notifier).load(selection);

      await container.read(provider.notifier).deleteNutritionEntry('m1');

      expect(repository.deletedIds, isEmpty);
      expect(container.read(provider).entries, hasLength(1));
    });

    test('rolls the row back and surfaces the error when the delete fails',
        () async {
      final entry = meal('m1', owned: true);
      await boot(Ok(NutritionPeriodData(
        dailyMacros: [_macros(monday, 700)],
        entries: [entry],
      )));
      container.listen(provider, (_, _) {});
      await container.read(provider.notifier).load(selection);

      repository.deleteAnswer =
          const Err(UnexpectedFailure('Health Connect is gone'));
      await container.read(provider.notifier).deleteNutritionEntry('m1');

      final state = container.read(provider);
      expect(state.entries, hasLength(1));
      expect(state.error, isNotNull);
    });
  });
}
