// The training-readiness detail used to load through a `FutureProvider.family`
// and navigate days with `setState`. It has a view-model now: these pin the load
// (display precomputed), the day navigation, the staleness guard the ad-hoc
// provider never had, and the failure mapping.

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/readiness/application/training_readiness_details_view_model.dart';

/// Records the days it is asked for; any day given a [gate] blocks until the
/// test releases it.
class _FakeUseCase extends LoadDashboardDayUseCase {
  _FakeUseCase() : super(DashboardDataLoader(HealthDataSource()));

  final List<DashboardQuery> queries = <DashboardQuery>[];
  final Map<LocalDate, Completer<void>> gates = <LocalDate, Completer<void>>{};

  Completer<void> gate(LocalDate date) =>
      gates.putIfAbsent(date, Completer<void>.new);

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async {
    queries.add(query);
    final gate = gates[query.date];
    if (gate != null) await gate.future;
    return Ok(DashboardData(
      date: query.date,
      avgHeartRateBpm: 72,
      restingHeartRateBpm: 55,
      restingHeartRateBaselineBpm: 54,
      loadedMetrics: query.visibleMetrics,
    ));
  }
}

class _ThrowingUseCase extends LoadDashboardDayUseCase {
  _ThrowingUseCase() : super(DashboardDataLoader(HealthDataSource()));

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async =>
      const Err(UnexpectedFailure('boom'));
}

Future<ProviderContainer> _boot(LoadDashboardDayUseCase useCase) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      loadDashboardDayUseCaseProvider.overrideWithValue(useCase),
    ],
  );
  addTearDown(container.dispose);
  container.listen(trainingReadinessDetailsProvider, (_, _) {});
  return container;
}

void main() {
  final today = LocalDate.now();

  test('load publishes the insight and its precomputed display', () async {
    final container = await _boot(_FakeUseCase());
    final notifier = container.read(trainingReadinessDetailsProvider.notifier);

    // Nothing is loaded until the screen asks: the route argument picks the day.
    expect(container.read(trainingReadinessDetailsProvider).isLoading, isTrue);

    await notifier.load(today);

    final state = container.read(trainingReadinessDetailsProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    expect(state.insight, isNotNull);
    expect(state.display, isNotNull);
    expect(state.display!.score, state.insight!.trainingReadinessScore);
    expect(state.display!.signals, isNotEmpty);
    expect(state.display!.guidance, isNotEmpty);
    expect(state.selectedDate, today);
    expect(state.canGoForward, isFalse);
  });

  test('a future day is clamped to today', () async {
    final container = await _boot(_FakeUseCase());

    await container
        .read(trainingReadinessDetailsProvider.notifier)
        .load(today.plusDays(5));

    expect(container.read(trainingReadinessDetailsProvider).selectedDate, today);
  });

  test('day navigation loads the day it moves to, and stops at today', () async {
    final useCase = _FakeUseCase();
    final container = await _boot(useCase);
    final notifier = container.read(trainingReadinessDetailsProvider.notifier);
    await notifier.load(today);

    notifier.previousDay();
    await pumpEventQueue();
    expect(
      container.read(trainingReadinessDetailsProvider).selectedDate,
      today.minusDays(1),
    );
    expect(container.read(trainingReadinessDetailsProvider).canGoForward, isTrue);

    notifier.nextDay();
    await pumpEventQueue();
    expect(container.read(trainingReadinessDetailsProvider).selectedDate, today);

    // Tomorrow is not a day: no load, no move.
    final loads = useCase.queries.length;
    notifier.nextDay();
    await pumpEventQueue();
    expect(useCase.queries, hasLength(loads));
    expect(container.read(trainingReadinessDetailsProvider).selectedDate, today);
  });

  test('refresh reloads the selected day, forcing it', () async {
    final useCase = _FakeUseCase();
    final container = await _boot(useCase);
    final notifier = container.read(trainingReadinessDetailsProvider.notifier);
    await notifier.load(today.minusDays(2));

    await notifier.refresh();

    expect(useCase.queries.last.date, today.minusDays(2));
    expect(useCase.queries.last.refreshMode, RefreshMode.force);
  });

  test('a stale day cannot overwrite the day that overtook it', () async {
    final useCase = _FakeUseCase();
    final container = await _boot(useCase);
    final notifier = container.read(trainingReadinessDetailsProvider.notifier);

    final stale = today.minusDays(3);
    final gate = useCase.gate(stale);

    unawaited(notifier.load(stale));
    await pumpEventQueue();
    await notifier.load(today);
    expect(container.read(trainingReadinessDetailsProvider).selectedDate, today);

    // The overtaken load answers late, and is dropped.
    gate.complete();
    await pumpEventQueue();

    final state = container.read(trainingReadinessDetailsProvider);
    expect(state.selectedDate, today);
    expect(state.isLoading, isFalse);
    expect(state.display, isNotNull);
  });

  test('a failed load becomes a ScreenError, not an exception', () async {
    final container = await _boot(_ThrowingUseCase());

    await container.read(trainingReadinessDetailsProvider.notifier).load(today);

    final state = container.read(trainingReadinessDetailsProvider);
    expect(state.error, isA<ScreenErrorMessage>());
    expect(state.isLoading, isFalse);
    expect(state.display, isNull);
  });
}
