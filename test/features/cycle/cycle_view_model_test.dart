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
import 'package:openvitals/data/repository/contract/cycle_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/cycle_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/cycle_period_data.dart';
import 'package:openvitals/features/cycle/application/cycle_display.dart';
import 'package:openvitals/features/cycle/application/cycle_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard — is what is
/// under test.
class _FakeCycleRepository implements CycleRepository {
  _FakeCycleRepository(this.answer);

  Result<CyclePeriodData> answer;
  int loads = 0;
  RefreshMode? lastRefreshMode;

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<CyclePeriodData>> gates = [];
  bool gated = false;

  @override
  Set<String> get phase4Permissions => const <String>{};

  @override
  Future<Result<Set<String>>> missingPermissions() async => const Ok(<String>{});

  @override
  Future<Result<CyclePeriodData>> loadCyclePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    lastRefreshMode = refreshMode;
    if (gated) {
      final completer = Completer<CyclePeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  Future<Result<CycleData>> loadCycleData(LocalDate start, LocalDate end) async =>
      const Ok(CycleData());
}

CyclePeriodData _period(CycleData data) =>
    CyclePeriodData(data: data, missingPermissions: const <String>{});

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeCycleRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<CyclePeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeCycleRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      cycleRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  final march = DateTime(2026, 3, 2, 7);
  final selection = PeriodSelection(TimeRange.month, const LocalDate(2026, 3, 2));

  CycleData oneTest(int result) => CycleData(
        ovulationTests: [
          OvulationTestEntry(time: march, result: result, source: 'test'),
        ],
      );

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(_period(oneTest(1))));
    container.listen(cycleProvider, (_, _) {});

    await container.read(cycleProvider.notifier).load(selection);

    final state = container.read(cycleProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.hasData, isTrue);
    expect(state.display!.ovulationTestCount, 1);
    expect(state.display!.observations.single.kind,
        CycleObservationKind.ovulationTest);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('cycle read')));
    container.listen(cycleProvider, (_, _) {});

    await container.read(cycleProvider.notifier).load(selection);

    final state = container.read(cycleProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(cycleProvider, (_, _) {});

    await container.read(cycleProvider.notifier).load(selection);

    expect(
      container.read(cycleProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('refresh reloads the current selection in force mode', () async {
    await boot(Ok(_period(const CycleData())));
    container.listen(cycleProvider, (_, _) {});
    final viewModel = container.read(cycleProvider.notifier);

    await viewModel.load(selection);
    await viewModel.refresh();

    expect(repository.loads, 2);
    expect(repository.lastRefreshMode, RefreshMode.force);
    expect(container.read(cycleProvider).selectedRange, TimeRange.month);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(Ok(_period(const CycleData())));
    container.listen(cycleProvider, (_, _) {});
    final viewModel = container.read(cycleProvider.notifier);
    repository.gated = true;

    // Two loads in flight; the FIRST one answers last.
    final first = viewModel.load(selection);
    final second = viewModel.load(
      PeriodSelection(TimeRange.week, const LocalDate(2026, 3, 2)),
    );
    repository.gates[1].complete(_period(oneTest(1)));
    await second;
    repository.gates[0].complete(_period(CycleData(
      ovulationTests: [
        OvulationTestEntry(time: march, result: 3, source: 'test'),
        OvulationTestEntry(time: march, result: 2, source: 'test'),
      ],
    )));
    await first;

    // The week load won: the month's late answer is dropped, not painted.
    final state = container.read(cycleProvider);
    expect(state.selectedRange, TimeRange.week);
    expect(state.display!.ovulationTestCount, 1);
  });
}
