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
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/sleep_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/sleep_range_mode.dart';
import 'package:openvitals/domain/query/sleep_period_data.dart';
import 'package:openvitals/features/sleep/application/sleep_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard — is what is
/// under test.
class _FakeSleepRepository implements SleepRepository {
  _FakeSleepRepository(this.answer);

  Result<SleepPeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<SleepPeriodData>> gates = [];
  bool gated = false;

  @override
  Future<Result<SleepPeriodData>> loadSleepPeriod(
    PeriodLoadQuery query,
    SleepRangeMode sleepRangeMode, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    if (gated) {
      final completer = Completer<SleepPeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// The cross-metric HRV read the sleep use case issues on the side.
class _FakeHeartRepository implements HeartRepository {
  @override
  Future<Result<List<DailyHrv>>> loadDailyHRV(
          LocalDate start, LocalDate end) async =>
      const Ok(<DailyHrv>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// One night, ending at 07:00 on [date].
SleepData _night(LocalDate date, {double hours = 8}) {
  final end = DateTime(date.year, date.month, date.day, 7);
  final durationMs = (hours * 3600000).round();
  return SleepData(
    id: 'night-$date',
    startTime: end.subtract(Duration(milliseconds: durationMs)),
    endTime: end,
    durationMs: durationMs,
    source: 'test',
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeSleepRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<SleepPeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeSleepRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      sleepRepositoryProvider.overrideWithValue(repository),
      heartRepositoryProvider.overrideWithValue(_FakeHeartRepository()),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  // A Wednesday, safely in the past: the week is a whole, unclipped 7 days.
  const wednesday = LocalDate(2026, 3, 4);
  final selection = PeriodSelection(TimeRange.week, wednesday);

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(SleepPeriodData(sessions: [_night(wednesday)])));
    container.listen(sleepProvider, (_, _) {});

    await container.read(sleepProvider.notifier).load(selection);

    final state = container.read(sleepProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    expect(state.selectedRange, TimeRange.week);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.durationPoints.length, 7);
    expect(state.display!.nights.length, 1);
    expect(state.display!.averageHours, 8.0);
    expect(state.display!.goalProgress.target, 8.0);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('sleep read')));
    container.listen(sleepProvider, (_, _) {});

    await container.read(sleepProvider.notifier).load(selection);

    final state = container.read(sleepProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(sleepProvider, (_, _) {});

    await container.read(sleepProvider.notifier).load(selection);

    expect(
      container.read(sleepProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('refresh reloads the current selection in force mode', () async {
    await boot(const Ok(SleepPeriodData()));
    container.listen(sleepProvider, (_, _) {});
    final viewModel = container.read(sleepProvider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(repository.loads, 2);
    expect(repository.lastRefreshMode, RefreshMode.force);
    expect(container.read(sleepProvider).selectedRange, TimeRange.week);
  });

  test('moving the goal rebuilds the display without reloading', () async {
    await boot(Ok(SleepPeriodData(sessions: [_night(wednesday, hours: 8)])));
    container.listen(sleepProvider, (_, _) {});
    final viewModel = container.read(sleepProvider.notifier);

    await viewModel.load(selection);
    expect(container.read(sleepProvider).display!.goalProgress.target, 8.0);

    viewModel.increaseDailyGoal();

    final state = container.read(sleepProvider);
    expect(state.dailyGoalHours, 8.25);
    // The goal card and the goal statistics read this: it has to move with it.
    expect(state.display!.goalProgress.target, 8.25);
    expect(state.display!.targetInterpretation!.targetHours, 8.25);
    // ...and it did not go back to the repository to find that out.
    expect(repository.loads, 1);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(const Ok(SleepPeriodData()));
    container.listen(sleepProvider, (_, _) {});
    final viewModel = container.read(sleepProvider.notifier);
    repository.gated = true;

    // Two loads in flight; the FIRST one answers last.
    final first = viewModel.load(selection);
    final second = viewModel.load(PeriodSelection(TimeRange.day, wednesday));
    repository.gates[1].complete(SleepPeriodData(sessions: [
      _night(wednesday, hours: 6),
    ]));
    await second;
    repository.gates[0].complete(SleepPeriodData(sessions: [
      _night(wednesday, hours: 11),
    ]));
    await first;

    // The day load won: the week's late answer is dropped, not painted.
    final state = container.read(sleepProvider);
    expect(state.selectedRange, TimeRange.day);
    expect(state.display!.isDay, isTrue);
    expect(state.display!.averageHours, 6.0);
  });
}
