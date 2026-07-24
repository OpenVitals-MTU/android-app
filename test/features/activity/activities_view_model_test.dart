import '../../support/today_fixtures.dart';
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
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/usecase/load_activities_use_case.dart';
import 'package:openvitals/features/activity/application/activities_view_model.dart';

/// Answers with whatever it is told to, so what is under test is the
/// view-model's own behaviour: the display precompute, the failure mapping, the
/// re-slice on filter/goal, and the staleness guard.
class _FakeLoadActivities implements LoadActivitiesUseCase {
  _FakeLoadActivities(this.answer);

  Result<ActivitiesLoadResult> answer;
  int loads = 0;

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<ActivitiesLoadResult>> gates = [];
  bool gated = false;

  @override
  Future<Result<ActivitiesLoadResult>> call(PeriodLoadQuery query) async {
    loads += 1;
    if (gated) {
      final completer = Completer<ActivitiesLoadResult>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

ExerciseData _workout(
  DateTime start, {
  String id = 'w',
  int type = 56,
  Duration duration = const Duration(minutes: 30),
}) =>
    ExerciseData(
      id: id,
      title: null,
      exerciseType: type,
      startTime: start,
      endTime: start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'test',
    );

ActivitiesLoadResult _result(List<ExerciseData> workouts) =>
    ActivitiesLoadResult(
      workouts: workouts,
      plannedWorkouts: const <PlannedExerciseData>[],
      previousWorkouts: const <ExerciseData>[],
      baselineWorkouts: const <ExerciseData>[],
      overviewDays: const <ActivityOverviewDay>[],
      crossDailyRestingHR: const [],
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeLoadActivities useCase;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<ActivitiesLoadResult> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    useCase = _FakeLoadActivities(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      loadActivitiesUseCaseProvider.overrideWithValue(useCase),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  final today = LocalDate.now();
  final morning = earlierTodayUtc(const Duration(hours: 2));
  final selection = PeriodSelection(TimeRange.week, today);

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(_result([
      _workout(morning, id: 'run', duration: const Duration(minutes: 45)),
    ])));
    container.listen(activitiesProvider, (_, _) {});

    await container.read(activitiesProvider.notifier).load(selection);

    final state = container.read(activitiesProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    expect(state.workouts, hasLength(1));
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.workoutCount, 1);
    expect(state.display!.totalDurationMs,
        const Duration(minutes: 45).inMilliseconds);
    expect(state.display!.hasAnyData, isTrue);
    expect(state.display!.chartValues.single.value, 45.0);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('exercise read')));
    container.listen(activitiesProvider, (_, _) {});

    await container.read(activitiesProvider.notifier).load(selection);

    final state = container.read(activitiesProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(activitiesProvider, (_, _) {});

    await container.read(activitiesProvider.notifier).load(selection);

    expect(
      container.read(activitiesProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('the type filter re-slices the display without reloading', () async {
    await boot(Ok(_result([
      _workout(morning, id: 'run', type: 56),
      _workout(morning, id: 'ride', type: 8, duration: const Duration(hours: 1)),
    ])));
    container.listen(activitiesProvider, (_, _) {});
    final viewModel = container.read(activitiesProvider.notifier);

    await viewModel.load(selection);
    expect(container.read(activitiesProvider).display!.workoutCount, 2);

    viewModel.selectActivityType(8);

    final state = container.read(activitiesProvider);
    expect(useCase.loads, 1, reason: 'the cached result is re-sliced, not refetched');
    expect(state.workouts.single.id, 'ride');
    expect(state.display!.workoutCount, 1);
    expect(state.display!.totalDurationMs, const Duration(hours: 1).inMilliseconds);
    // The dropdown still offers both types, whatever the slice.
    expect(state.display!.filterOptions, [8, 56]);
  });

  test('moving the daily goal re-derives the goal progress', () async {
    // 45 minutes today: it clears a 30-minute goal and misses a 60-minute one.
    await boot(Ok(_result([
      _workout(morning, id: 'run', duration: const Duration(minutes: 45)),
    ])));
    container.listen(activitiesProvider, (_, _) {});
    final viewModel = container.read(activitiesProvider.notifier);

    await viewModel.load(selection);
    expect(container.read(activitiesProvider).display!.goalProgress.goalMetDays, 1);

    // Six 5-minute steps: 30 → 60.
    for (var i = 0; i < 6; i++) {
      viewModel.increaseDailyGoal();
    }

    final state = container.read(activitiesProvider);
    expect(state.dailyGoalMinutes, 60);
    expect(useCase.loads, 1);
    expect(state.display!.goalProgress.goalMetDays, 0);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(Ok(_result(const <ExerciseData>[])));
    container.listen(activitiesProvider, (_, _) {});
    final viewModel = container.read(activitiesProvider.notifier);
    useCase.gated = true;

    // Loads are single-flight: the second parks while the first is on the wire.
    final first = viewModel.load(selection);
    final second = viewModel.load(PeriodSelection(TimeRange.month, today));
    expect(useCase.gates, hasLength(1));
    // The first's answer lands after it was superseded: dropped, and the
    // parked month load dispatches.
    useCase.gates[0].complete(_result([
      _workout(morning, id: 'week', duration: const Duration(minutes: 99)),
    ]));
    await Future<void>.delayed(Duration.zero);
    useCase.gates[1].complete(_result([
      _workout(morning, id: 'month', duration: const Duration(minutes: 10)),
    ]));
    await Future.wait([first, second]);

    // The month load won: the week's late answer is dropped, not painted.
    final state = container.read(activitiesProvider);
    expect(state.selectedRange, TimeRange.month);
    expect(state.workouts.single.id, 'month');
    expect(state.display!.totalDurationMs,
        const Duration(minutes: 10).inMilliseconds);
  });
}
