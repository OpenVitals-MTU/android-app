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
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/mindfulness_period_data.dart';
import 'package:openvitals/features/mindfulness/application/mindfulness_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard — is what is
/// under test.
class _FakeMindfulnessRepository implements MindfulnessRepository {
  _FakeMindfulnessRepository(this.answer);

  Result<MindfulnessPeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;

  Result<void> deleteAnswer = const Ok(null);
  final List<String> deletedIds = [];

  @override
  Future<Result<void>> deleteMindfulnessSessionEntry(String id) async {
    deletedIds.add(id);
    return deleteAnswer;
  }

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<MindfulnessPeriodData>> gates = [];
  bool gated = false;

  @override
  Future<Result<MindfulnessPeriodData>> loadMindfulnessPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    if (gated) {
      final completer = Completer<MindfulnessPeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

MindfulnessSession _session(DateTime start, Duration duration,
        {bool owned = false}) =>
    MindfulnessSession(
      id: start.toIso8601String(),
      title: null,
      startTime: start,
      endTime: start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'Test',
      isOpenVitalsEntry: owned,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeMindfulnessRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<MindfulnessPeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeMindfulnessRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      mindfulnessRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  final monday = DateTime(2026, 3, 2, 6);
  final selection = PeriodSelection(TimeRange.week, const LocalDate(2026, 3, 2));

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(MindfulnessPeriodData(sessions: [
      _session(monday, const Duration(minutes: 30)),
    ])));
    container.listen(mindfulnessProvider, (_, _) {});

    await container.read(mindfulnessProvider.notifier).load(selection);

    final state = container.read(mindfulnessProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    expect(state.selectedRange, TimeRange.week);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.sessionCount, 1);
    expect(state.display!.totalMinutes, 30);
    expect(state.display!.cumulativeSamples.single.value, 30.0);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('mindfulness read')));
    container.listen(mindfulnessProvider, (_, _) {});

    await container.read(mindfulnessProvider.notifier).load(selection);

    final state = container.read(mindfulnessProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(mindfulnessProvider, (_, _) {});

    await container.read(mindfulnessProvider.notifier).load(selection);

    expect(
      container.read(mindfulnessProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('refresh reloads the current selection in force mode', () async {
    await boot(const Ok(MindfulnessPeriodData()));
    container.listen(mindfulnessProvider, (_, _) {});
    final viewModel = container.read(mindfulnessProvider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(repository.loads, 2);
    expect(repository.lastRefreshMode, RefreshMode.force);
    expect(container.read(mindfulnessProvider).selectedRange, TimeRange.week);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(const Ok(MindfulnessPeriodData()));
    container.listen(mindfulnessProvider, (_, _) {});
    final viewModel = container.read(mindfulnessProvider.notifier);
    repository.gated = true;

    // Loads are single-flight: the second parks while the first is on the wire.
    final first = viewModel.load(selection);
    final second = viewModel.load(
      PeriodSelection(TimeRange.month, const LocalDate(2026, 3, 2)),
    );
    expect(repository.gates, hasLength(1));
    // The first's answer lands after it was superseded: dropped, and the
    // parked month load dispatches.
    repository.gates[0].complete(MindfulnessPeriodData(sessions: [
      _session(monday, const Duration(minutes: 99)),
    ]));
    await Future<void>.delayed(Duration.zero);
    repository.gates[1].complete(MindfulnessPeriodData(sessions: [
      _session(monday, const Duration(minutes: 10)),
    ]));
    await Future.wait([first, second]);

    // The month load won: the week's late answer is dropped, not painted.
    final state = container.read(mindfulnessProvider);
    expect(state.selectedRange, TimeRange.month);
    expect(state.display!.totalMinutes, 10);
  });

  group('deleteMindfulnessSession', () {
    test('removes an owned session and deletes it through the repository',
        () async {
      final session = _session(monday, const Duration(minutes: 10), owned: true);
      await boot(Ok(MindfulnessPeriodData(sessions: [session])));
      container.listen(mindfulnessProvider, (_, _) {});
      await container.read(mindfulnessProvider.notifier).load(selection);

      repository.answer = const Ok(MindfulnessPeriodData());
      await container
          .read(mindfulnessProvider.notifier)
          .deleteMindfulnessSession(session.id);

      expect(repository.deletedIds, [session.id]);
      expect(container.read(mindfulnessProvider).data!.sessions, isEmpty);
      expect(
        container.read(mindfulnessProvider).display!.sortedSessions,
        isEmpty,
      );
    });

    test('ignores a foreign session it does not own', () async {
      final session = _session(monday, const Duration(minutes: 10));
      await boot(Ok(MindfulnessPeriodData(sessions: [session])));
      container.listen(mindfulnessProvider, (_, _) {});
      await container.read(mindfulnessProvider.notifier).load(selection);

      await container
          .read(mindfulnessProvider.notifier)
          .deleteMindfulnessSession(session.id);

      expect(repository.deletedIds, isEmpty);
      expect(container.read(mindfulnessProvider).data!.sessions, hasLength(1));
    });

    test('rolls the row back and surfaces the error when the delete fails',
        () async {
      final session = _session(monday, const Duration(minutes: 10), owned: true);
      await boot(Ok(MindfulnessPeriodData(sessions: [session])));
      container.listen(mindfulnessProvider, (_, _) {});
      await container.read(mindfulnessProvider.notifier).load(selection);

      repository.deleteAnswer =
          const Err(UnexpectedFailure('Health Connect is gone'));
      await container
          .read(mindfulnessProvider.notifier)
          .deleteMindfulnessSession(session.id);

      final state = container.read(mindfulnessProvider);
      expect(state.data!.sessions, hasLength(1));
      expect(state.error, isNotNull);
    });
  });
}
