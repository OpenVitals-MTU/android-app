import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/features/bodyenergy/application/body_energy_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard — is what is
/// under test.
class _FakeBodyEnergyRepository implements BodyEnergyRepository {
  _FakeBodyEnergyRepository(this.answer);

  Result<BodyEnergyTimelineResult> Function(BodyEnergyTimelineQuery) answer;
  int loads = 0;

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<BodyEnergyTimeline>> gates = [];
  bool gated = false;
  BodyEnergyTimelineQuery? lastQuery;

  @override
  Future<Result<BodyEnergyTimelineResult>> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async {
    loads += 1;
    lastQuery = query;
    if (gated) {
      final completer = Completer<BodyEnergyTimeline>();
      gates.add(completer);
      final timeline = await completer.future;
      return Ok(BodyEnergyTimelineResult(query: query, days: [timeline]));
    }
    return answer(query);
  }
}

BodyEnergyTimelinePoint _point(LocalDate date, int hour, int score) =>
    BodyEnergyTimelinePoint(
      time: date.atTimeInstant(hour),
      score: score,
      delta: 1,
      state: BodyEnergyBucketState.rest,
      confidence: BodyEnergyConfidence.high,
    );

BodyEnergyTimeline _timeline(LocalDate date, {int currentScore = 62}) =>
    BodyEnergyTimeline(
      date: date,
      startScore: 50,
      currentScore: currentScore,
      charged: 14,
      drained: 2,
      points: [
        _point(date, 7, 54),
        _point(date, 12, 60),
        _point(date, 17, currentScore),
      ],
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'test',
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeBodyEnergyRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(
    Result<BodyEnergyTimelineResult> Function(BodyEnergyTimelineQuery) answer,
  ) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeBodyEnergyRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      bodyEnergyRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  final yesterday = LocalDate.now().minusDays(1);

  test('a loaded day lands with its display precomputed', () async {
    await boot((query) => Ok(BodyEnergyTimelineResult(
          query: query,
          days: [_timeline(yesterday)],
        )));
    container.listen(bodyEnergyProvider, (_, _) {});

    await container.read(bodyEnergyProvider.notifier).load(yesterday);

    final state = container.read(bodyEnergyProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.isEmpty, isFalse);
    expect(state.display!.chartPoints.length, 3);
    expect(state.display!.timeline!.currentScore, 62);
    expect(state.display!.inputRows, isNotEmpty);
  });

  test('a day with no timeline at all still gives the screen a display',
      () async {
    await boot((query) =>
        Ok(BodyEnergyTimelineResult(query: query, days: const [])));
    container.listen(bodyEnergyProvider, (_, _) {});

    await container.read(bodyEnergyProvider.notifier).load(yesterday);

    final display = container.read(bodyEnergyProvider).display!;
    expect(display.isEmpty, isTrue);
    expect(display.chartPoints, isEmpty);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot((_) => const Err(PermissionFailure('heart rate read')));
    container.listen(bodyEnergyProvider, (_, _) {});

    await container.read(bodyEnergyProvider.notifier).load(yesterday);

    final state = container.read(bodyEnergyProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot((_) => const Err(UnexpectedFailure('the timeline blew up')));
    container.listen(bodyEnergyProvider, (_, _) {});

    await container.read(bodyEnergyProvider.notifier).load(yesterday);

    expect(
      container.read(bodyEnergyProvider).error,
      const ScreenErrorMessage('the timeline blew up'),
    );
  });

  test('a future day is clamped to today', () async {
    await boot((query) => Ok(BodyEnergyTimelineResult(
          query: query,
          days: [_timeline(LocalDate.now())],
        )));
    container.listen(bodyEnergyProvider, (_, _) {});

    await container
        .read(bodyEnergyProvider.notifier)
        .load(LocalDate.now().plusDays(5));

    expect(container.read(bodyEnergyProvider).selectedDate, LocalDate.now());
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot((query) =>
        Ok(BodyEnergyTimelineResult(query: query, days: const [])));
    container.listen(bodyEnergyProvider, (_, _) {});
    final viewModel = container.read(bodyEnergyProvider.notifier);
    repository.gated = true;

    // Two loads in flight; the FIRST one answers last.
    final first = viewModel.load(yesterday.minusDays(1));
    final second = viewModel.load(yesterday);
    repository.gates[1].complete(_timeline(yesterday, currentScore: 71));
    await second;
    repository.gates[0]
        .complete(_timeline(yesterday.minusDays(1), currentScore: 11));
    await first;

    // The newer day won: the older day's late answer is dropped, not painted.
    final state = container.read(bodyEnergyProvider);
    expect(state.selectedDate, yesterday);
    expect(state.display!.timeline!.currentScore, 71);
  });
}
