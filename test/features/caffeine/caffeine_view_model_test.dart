import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/caffeine_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/features/caffeine/application/caffeine_display.dart';
import 'package:openvitals/features/caffeine/application/caffeine_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// insight run, the display precompute, the failure mapping — is what is under
/// test.
class _FakeCaffeineRepository implements CaffeineRepository {
  _FakeCaffeineRepository(this.answer);

  Result<CaffeinePeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;
  DatePeriod? lastPeriod;

  @override
  Future<Result<CaffeinePeriodData>> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    lastPeriod = period;
    return answer;
  }

  @override
  Future<Result<CaffeinePeriodData>> loadCaffeinePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      answer;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

CaffeineEntry _entry(DateTime start, double mg) => CaffeineEntry(
      id: 'entry-${start.millisecondsSinceEpoch}',
      startTime: start,
      endTime: start.add(const Duration(minutes: 10)),
      caffeineMg: mg,
      name: 'Coffee',
      source: 'Test source',
      mealType: 0,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeCaffeineRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<CaffeinePeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeCaffeineRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      caffeineRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  test('a loaded window lands with its display precomputed', () async {
    final now = DateTime.now();
    await boot(Ok(CaffeinePeriodData(entries: [
      _entry(now.subtract(const Duration(hours: 2)), 95),
      _entry(now.subtract(const Duration(hours: 6)), 120),
    ])));
    container.listen(caffeineProvider, (_, _) {});

    await container.read(caffeineProvider.notifier).load();
    // [build] self-triggers a first load; let it settle before asserting.
    await pumpEventQueue();

    final state = container.read(caffeineProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    // The curve is the whole point of the today card.
    expect(state.display!.home.insights.curvePoints, isNotEmpty);
    expect(state.display!.home.curveEntryTimes.length, 2);
    expect(state.display!.home.curveMaxMg, greaterThan(0));
    // Both drinks were today, so the analytics window sees them too.
    expect(state.display!.analytics.insights.periodTotalMg, 215.0);
    expect(state.display!.analytics.topSourceLabel, isNotNull);
    expect(state.display!.analytics.sourceBars.first.fraction, 1.0);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('nutrition read')));
    container.listen(caffeineProvider, (_, _) {});

    await container.read(caffeineProvider.notifier).load();
    // [build] self-triggers a first load; let it settle before asserting.
    await pumpEventQueue();

    final state = container.read(caffeineProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(caffeineProvider, (_, _) {});

    await container.read(caffeineProvider.notifier).load();
    // [build] self-triggers a first load; let it settle before asserting.
    await pumpEventQueue();

    expect(
      container.read(caffeineProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('picking an analytics range reloads over the wider window', () async {
    await boot(const Ok(CaffeinePeriodData(entries: [])));
    container.listen(caffeineProvider, (_, _) {});
    final viewModel = container.read(caffeineProvider.notifier);
    await viewModel.load();
    await pumpEventQueue();

    final loadsBefore = repository.loads;
    viewModel.selectAnalyticsRange(CaffeineAnalyticsRange.last90Days);
    await pumpEventQueue();

    expect(container.read(caffeineProvider).analyticsRange,
        CaffeineAnalyticsRange.last90Days);
    expect(repository.loads, greaterThan(loadsBefore));
    expect(container.read(caffeineProvider).display, isNotNull);

    // The same range again is not a reload.
    final loadsAfter = repository.loads;
    viewModel.selectAnalyticsRange(CaffeineAnalyticsRange.last90Days);
    await pumpEventQueue();
    expect(repository.loads, loadsAfter);
  });

  test('refresh reloads in force mode', () async {
    await boot(const Ok(CaffeinePeriodData(entries: [])));
    container.listen(caffeineProvider, (_, _) {});
    final viewModel = container.read(caffeineProvider.notifier);

    await viewModel.load();
    await pumpEventQueue();
    await viewModel.refresh();

    expect(repository.lastRefreshMode, RefreshMode.force);
  });

  test('an empty load still gives the screen a display to render', () async {
    await boot(const Ok(CaffeinePeriodData(entries: [])));
    container.listen(caffeineProvider, (_, _) {});

    await container.read(caffeineProvider.notifier).load();
    // [build] self-triggers a first load; let it settle before asserting.
    await pumpEventQueue();

    final display = container.read(caffeineProvider).display!;
    expect(display.home.sleepImpactStatus, CaffeineSleepImpactStatus.unlikely);
    expect(display.analytics.sourceBars, isEmpty);
    expect(display.home.curveMaxMg, greaterThan(0));
  });
}
