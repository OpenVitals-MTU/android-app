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
import 'package:openvitals/domain/query/heart_period_data.dart';
import 'package:openvitals/domain/query/vitals_period_data.dart';
import 'package:openvitals/features/heart/application/heart_metric_view_model.dart';
import 'package:openvitals/features/heart/presentation/heart_metric.dart';

/// Answers with whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard — is what is
/// under test.
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
  @override
  Set<String> get phase3Permissions => const <String>{};

  @override
  Future<Result<VitalsPeriodData>> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      const Ok(VitalsPeriodData());

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

HeartRateSummary _summary(LocalDate date, int avg) => HeartRateSummary(
      date: date,
      avgBpm: avg,
      minBpm: avg - 10,
      maxBpm: avg + 20,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeHeartRepository repository;
  late ProviderContainer container;

  final provider = heartMetricProvider(HeartMetric.averageHeartRate);

  Future<ProviderContainer> boot(Result<HeartPeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeHeartRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      heartRepositoryProvider.overrideWithValue(repository),
      vitalsRepositoryProvider.overrideWithValue(_FakeVitalsRepository()),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  const monday = LocalDate(2026, 3, 2);
  const tuesday = LocalDate(2026, 3, 3);
  const selection = PeriodSelection(TimeRange.week, monday);

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(HeartPeriodData(dailySummaries: [
      _summary(tuesday, 72),
      _summary(monday, 68),
    ])));
    container.listen(provider, (_, _) {});

    await container.read(provider.notifier).load(selection);

    final state = container.read(provider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    expect(state.selectedRange, TimeRange.week);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    final period = state.display!.heartRatePeriod!;
    expect([for (final s in period.summaries) s.date], [monday, tuesday]);
    expect(period.averageBpm, 70);
    expect(period.lowestBpm, 58);
    expect(period.highestBpm, 92);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('heart rate read')));
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
    await boot(const Ok(HeartPeriodData()));
    container.listen(provider, (_, _) {});
    final viewModel = container.read(provider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(repository.loads, 2);
    expect(repository.lastRefreshMode, RefreshMode.force);
    expect(container.read(provider).selectedRange, TimeRange.week);
  });

  test('a moved threshold rebuilds the checks against the loaded samples',
      () async {
    await boot(Ok(HeartPeriodData(dailySummaries: [_summary(monday, 68)])));
    container.listen(provider, (_, _) {});
    final viewModel = container.read(provider.notifier);

    await viewModel.load(selection);
    // The day's max is 88, under the 120 default: nothing to flag.
    expect(container.read(provider).display!.highHeartRateCheck.count, 0);

    for (var i = 0; i < 7; i++) {
      viewModel.decreaseHighHeartRateThreshold();
    }

    final state = container.read(provider);
    expect(state.highHeartRateThresholdBpm, 85);
    expect(state.display!.highHeartRateCheck.thresholdBpm, 85);
    expect(state.display!.highHeartRateCheck.count, 1);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(const Ok(HeartPeriodData()));
    container.listen(provider, (_, _) {});
    final viewModel = container.read(provider.notifier);
    repository.gated = true;

    // Loads are single-flight: the second parks while the first is on the wire.
    final first = viewModel.load(selection);
    final second = viewModel.load(
      const PeriodSelection(TimeRange.month, monday),
    );
    expect(repository.gates, hasLength(1));
    // The first's answer lands after it was superseded: dropped, and the
    // parked month load dispatches.
    repository.gates[0].complete(HeartPeriodData(dailySummaries: [
      _summary(monday, 99),
    ]));
    await Future<void>.delayed(Duration.zero);
    repository.gates[1].complete(HeartPeriodData(dailySummaries: [
      _summary(monday, 60),
    ]));
    await Future.wait([first, second]);

    // The month load won: the week's late answer is dropped, not painted.
    final state = container.read(provider);
    expect(state.selectedRange, TimeRange.month);
    expect(state.display!.heartRatePeriod!.averageBpm, 60);
  });
}
