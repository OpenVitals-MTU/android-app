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
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/hydration_period_data.dart';
import 'package:openvitals/features/hydration/application/hydration_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard — is what is
/// under test.
class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository(this.answer);

  Result<HydrationPeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<HydrationPeriodData>> gates = [];
  bool gated = false;

  @override
  double hydrationDailyGoalLiters() => 2.0;

  @override
  Future<Result<HydrationPeriodData>> loadHydrationPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    if (gated) {
      final completer = Completer<HydrationPeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// The hydration load joins the nutrition records that carry the drink names.
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

HydrationEntry _drink(DateTime start, double liters) => HydrationEntry(
      id: start.toIso8601String(),
      startTime: start,
      endTime: start.add(const Duration(minutes: 1)),
      liters: liters,
      source: 'test',
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeHydrationRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<HydrationPeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeHydrationRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      hydrationRepositoryProvider.overrideWithValue(repository),
      nutritionRepositoryProvider.overrideWithValue(_FakeNutritionRepository()),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  const monday = LocalDate(2026, 3, 2);
  final morning = DateTime(2026, 3, 2, 8);
  final selection = PeriodSelection(TimeRange.week, monday);

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(HydrationPeriodData(
      dailyHydration: [DailyHydration(date: monday, liters: 2.5)],
      hydrationEntries: [_drink(morning, 2.5)],
    )));
    container.listen(hydrationProvider, (_, _) {});

    await container.read(hydrationProvider.notifier).load(selection);

    final state = container.read(hydrationProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.hasData, isTrue);
    expect(state.display!.summary.totalLiters, 2.5);
    // The goal read is synchronous, and the summary is built against it.
    expect(state.dailyGoalLiters, 2.0);
    expect(state.display!.summary.goalMetDays, 1);
    expect(state.display!.cumulativeSamples.single.value, 2.5);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('hydration read')));
    container.listen(hydrationProvider, (_, _) {});

    await container.read(hydrationProvider.notifier).load(selection);

    final state = container.read(hydrationProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(hydrationProvider, (_, _) {});

    await container.read(hydrationProvider.notifier).load(selection);

    expect(
      container.read(hydrationProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('refresh reloads the current selection in force mode', () async {
    await boot(const Ok(HydrationPeriodData()));
    container.listen(hydrationProvider, (_, _) {});
    final viewModel = container.read(hydrationProvider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(repository.loads, 2);
    expect(repository.lastRefreshMode, RefreshMode.force);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(const Ok(HydrationPeriodData()));
    container.listen(hydrationProvider, (_, _) {});
    final viewModel = container.read(hydrationProvider.notifier);
    repository.gated = true;

    // Two loads in flight; the FIRST one answers last.
    final first = viewModel.load(selection);
    final second = viewModel.load(PeriodSelection(TimeRange.month, monday));
    repository.gates[1].complete(HydrationPeriodData(
      dailyHydration: [DailyHydration(date: monday, liters: 1.0)],
    ));
    await second;
    repository.gates[0].complete(HydrationPeriodData(
      dailyHydration: [DailyHydration(date: monday, liters: 9.0)],
    ));
    await first;

    // The month load won: the week's late answer is dropped, not painted.
    final state = container.read(hydrationProvider);
    expect(state.selectedRange, TimeRange.month);
    expect(state.display!.summary.totalLiters, 1.0);
  });
}
