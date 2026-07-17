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
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/query/heart_period_data.dart';
import 'package:openvitals/domain/query/vitals_period_data.dart';
import 'package:openvitals/features/vitals/application/heart_vitals_overview_view_model.dart';

/// Both repositories answer with whatever the test tells them to, so the
/// view-model's own behaviour — the display precompute, the failure mapping, the
/// staleness guard — is what is under test.
class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository(this.answer);

  Result<HeartPeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<HeartPeriodData>> gates = [];
  bool gated = false;

  @override
  Future<Result<HeartPeriodData>> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    if (gated) {
      final completer = Completer<HeartPeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  Future<Result<List<DailyHrv>>> loadDailyHRV(
          LocalDate start, LocalDate end) async =>
      const Ok(<DailyHrv>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeVitalsRepository implements VitalsRepository {
  _FakeVitalsRepository(this.answer);

  Result<VitalsPeriodData> answer;

  @override
  Set<String> get phase3Permissions => const <String>{};

  @override
  Future<Result<VitalsPeriodData>> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      answer;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

HeartPeriodData _heart({int avgBpm = 70}) => HeartPeriodData(
      dailySummaries: [
        HeartRateSummary(
          date: const LocalDate(2026, 3, 2),
          avgBpm: avgBpm,
          minBpm: 55,
          maxBpm: 120,
        ),
      ],
    );

// A week load reads native daily aggregates plus the latest reading, not the
// raw list — so seed both, the way the repository fills them for a long range.
VitalsPeriodData _vitals() => VitalsPeriodData(
      spO2Daily: const [
        DailyVitalPoint(date: LocalDate(2026, 3, 2), value: 96, count: 1),
      ],
      latestSpO2: SpO2Entry(
        time: DateTime.utc(2026, 3, 2, 8),
        percent: 96,
        source: 'Ring',
      ),
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeHeartRepository heartRepository;
  late _FakeVitalsRepository vitalsRepository;
  late ProviderContainer container;

  Future<ProviderContainer> boot({
    required Result<HeartPeriodData> heart,
    Result<VitalsPeriodData> vitals = const Ok(VitalsPeriodData()),
  }) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    heartRepository = _FakeHeartRepository(heart);
    vitalsRepository = _FakeVitalsRepository(vitals);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      heartRepositoryProvider.overrideWithValue(heartRepository),
      vitalsRepositoryProvider.overrideWithValue(vitalsRepository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  final selection = PeriodSelection(TimeRange.week, const LocalDate(2026, 3, 2));

  test('a loaded period lands with its display precomputed', () async {
    await boot(heart: Ok(_heart()), vitals: Ok(_vitals()));
    container.listen(heartVitalsOverviewProvider, (_, _) {});

    await container.read(heartVitalsOverviewProvider.notifier).load(selection);

    final state = container.read(heartVitalsOverviewProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    expect(state.selectedRange, TimeRange.week);
    expect(state.result, isNotNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.heartRate!.averageBpm, 70);
    expect(state.display!.heartRate!.periodChart!.highestBpm, 120);
    expect(state.display!.spO2!.averagePercent, 96.0);
    // Nothing loaded for these metrics, so nothing to show.
    expect(state.display!.bloodPressure, isNull);
    expect(state.display!.skinTemperature, isNull);
  });

  test('the display follows the range the load carried', () async {
    await boot(heart: Ok(_heart()));
    container.listen(heartVitalsOverviewProvider, (_, _) {});

    await container.read(heartVitalsOverviewProvider.notifier).load(
          PeriodSelection(TimeRange.day, const LocalDate(2026, 3, 2)),
        );

    // A day range reads the raw samples, of which there are none — the daily
    // summaries the fixture carries belong to the longer ranges.
    final state = container.read(heartVitalsOverviewProvider);
    expect(state.selectedRange, TimeRange.day);
    expect(state.display!.heartRate, isNull);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(heart: const Err(PermissionFailure('heart rate read')));
    container.listen(heartVitalsOverviewProvider, (_, _) {});

    await container.read(heartVitalsOverviewProvider.notifier).load(selection);

    final state = container.read(heartVitalsOverviewProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(heart: const Err(UnexpectedFailure('the provider hung up')));
    container.listen(heartVitalsOverviewProvider, (_, _) {});

    await container.read(heartVitalsOverviewProvider.notifier).load(selection);

    expect(
      container.read(heartVitalsOverviewProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('either half failing fails the combined load', () async {
    await boot(
      heart: Ok(_heart()),
      vitals: const Err(UnexpectedFailure('vitals are down')),
    );
    container.listen(heartVitalsOverviewProvider, (_, _) {});

    await container.read(heartVitalsOverviewProvider.notifier).load(selection);

    final state = container.read(heartVitalsOverviewProvider);
    expect(state.error, const ScreenErrorMessage('vitals are down'));
    expect(state.display, isNull);
  });

  test('refresh reloads the current selection in force mode', () async {
    await boot(heart: Ok(_heart()));
    container.listen(heartVitalsOverviewProvider, (_, _) {});
    final viewModel = container.read(heartVitalsOverviewProvider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(heartRepository.loads, 2);
    expect(heartRepository.lastRefreshMode, RefreshMode.force);
    expect(
        container.read(heartVitalsOverviewProvider).selectedRange,
        TimeRange.week);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(heart: Ok(_heart()));
    container.listen(heartVitalsOverviewProvider, (_, _) {});
    final viewModel = container.read(heartVitalsOverviewProvider.notifier);
    heartRepository.gated = true;

    // Two loads in flight; the FIRST one answers last.
    final first = viewModel.load(selection);
    final second = viewModel.load(
      PeriodSelection(TimeRange.month, const LocalDate(2026, 3, 2)),
    );
    heartRepository.gates[1].complete(_heart(avgBpm: 61));
    await second;
    heartRepository.gates[0].complete(_heart(avgBpm: 99));
    await first;

    // The month load won: the week's late answer is dropped, not painted.
    final state = container.read(heartVitalsOverviewProvider);
    expect(state.selectedRange, TimeRange.month);
    expect(state.display!.heartRate!.averageBpm, 61);
  });
}
