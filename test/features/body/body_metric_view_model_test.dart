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
import 'package:openvitals/data/repository/contract/body_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/body_period_data.dart';
import 'package:openvitals/features/body/application/body_metric_view_model.dart';

/// Returns whatever it is told to, so the view-model's own behaviour — the
/// display precompute, the failure mapping, the staleness guard, the optimistic
/// delete — is what is under test.
class _FakeBodyRepository implements BodyRepository {
  _FakeBodyRepository(this.answer);

  Result<BodyPeriodData> answer;
  Result<void> deletion = const Ok(null);
  int loads = 0;
  final List<(BodyMeasurementType, String)> deletedEntries = [];

  /// Completed by the test, so two loads can be held in flight at once.
  final List<Completer<BodyPeriodData>> gates = [];
  bool gated = false;

  @override
  Future<Result<BodyPeriodData>> loadBodyPeriod(
    PeriodLoadQuery query,
    BodyPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loads += 1;
    if (gated) {
      final completer = Completer<BodyPeriodData>();
      gates.add(completer);
      return Ok(await completer.future);
    }
    return answer;
  }

  @override
  Future<Result<void>> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) async {
    deletedEntries.add((type, id));
    return deletion;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

WeightEntry _weight(
  DateTime time,
  double kg, {
  String id = '',
  bool isOpenVitalsEntry = false,
}) =>
    WeightEntry(
      time: time,
      weightKg: kg,
      source: 'test',
      id: id,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeBodyRepository repository;
  late ProviderContainer container;

  Future<ProviderContainer> boot(Result<BodyPeriodData> answer) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeBodyRepository(answer);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      bodyRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  final monday = DateTime(2026, 3, 2, 8);
  final selection = PeriodSelection(TimeRange.month, const LocalDate(2026, 3, 2));

  test('a loaded period lands with its display precomputed', () async {
    await boot(Ok(BodyPeriodData(weightEntries: [_weight(monday, 70.5)])));
    container.listen(bodyMetricProvider, (_, _) {});

    await container.read(bodyMetricProvider.notifier).load(selection);

    final state = container.read(bodyMetricProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull);
    // The screen renders this; it must exist by the time loading ends.
    expect(state.display, isNotNull);
    expect(state.display!.summary.latestWeightKg, 70.5);
    expect(state.display!.hasAnyBodyData, isTrue);
    expect(state.display!.readingsNewestFirst.single.value, 70.5);
  });

  test('a permission failure becomes ScreenErrorPermissionDenied', () async {
    await boot(const Err(PermissionFailure('body read')));
    container.listen(bodyMetricProvider, (_, _) {});

    await container.read(bodyMetricProvider.notifier).load(selection);

    final state = container.read(bodyMetricProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.display, isNull);
  });

  test('an unexpected failure carries its message to the screen', () async {
    await boot(const Err(UnexpectedFailure('the provider hung up')));
    container.listen(bodyMetricProvider, (_, _) {});

    await container.read(bodyMetricProvider.notifier).load(selection);

    expect(
      container.read(bodyMetricProvider).error,
      const ScreenErrorMessage('the provider hung up'),
    );
  });

  test('deleting an entry rebuilds the display without waiting for the reload',
      () async {
    await boot(Ok(BodyPeriodData(weightEntries: [
      _weight(monday, 70.5, id: 'w1', isOpenVitalsEntry: true),
      _weight(monday.add(const Duration(days: 1)), 71.0),
    ])));
    container.listen(bodyMetricProvider, (_, _) {});
    final viewModel = container.read(bodyMetricProvider.notifier);
    await viewModel.load(selection);

    // The reload after the delete answers with the entry already gone, as
    // Health Connect would.
    repository.answer = Ok(BodyPeriodData(weightEntries: [
      _weight(monday.add(const Duration(days: 1)), 71.0),
    ]));
    await viewModel.deleteBodyMeasurementEntry(BodyMeasurementType.weight, 'w1');

    expect(repository.deletedEntries, [(BodyMeasurementType.weight, 'w1')]);
    final display = container.read(bodyMetricProvider).display!;
    // The deleted reading is gone from the list the screen renders — not merely
    // from the data behind it.
    expect(display.readingsNewestFirst.length, 1);
    expect(display.readingsNewestFirst.single.editId, isNull);
  });

  test('a failed delete restores the previous display, with an error', () async {
    await boot(Ok(BodyPeriodData(weightEntries: [
      _weight(monday, 70.5, id: 'w1', isOpenVitalsEntry: true),
    ])));
    container.listen(bodyMetricProvider, (_, _) {});
    final viewModel = container.read(bodyMetricProvider.notifier);
    await viewModel.load(selection);

    repository.deletion = const Err(UnexpectedFailure('delete refused'));
    await viewModel.deleteBodyMeasurementEntry(BodyMeasurementType.weight, 'w1');

    final state = container.read(bodyMetricProvider);
    expect(state.error, const ScreenErrorMessage('delete refused'));
    // The entry is back: the optimistic removal is rolled back with the display.
    expect(state.display!.readingsNewestFirst.length, 1);
  });

  test('a stale load cannot overwrite the newer one it lost to', () async {
    await boot(const Ok(BodyPeriodData()));
    container.listen(bodyMetricProvider, (_, _) {});
    final viewModel = container.read(bodyMetricProvider.notifier);
    repository.gated = true;

    // Two loads in flight; the FIRST one answers last.
    final first = viewModel.load(selection);
    final second = viewModel.load(
      PeriodSelection(TimeRange.week, const LocalDate(2026, 3, 2)),
    );
    repository.gates[1]
        .complete(BodyPeriodData(weightEntries: [_weight(monday, 60.0)]));
    await second;
    repository.gates[0]
        .complete(BodyPeriodData(weightEntries: [_weight(monday, 99.0)]));
    await first;

    // The week load won: the month's late answer is dropped, not painted.
    final state = container.read(bodyMetricProvider);
    expect(state.selectedRange, TimeRange.week);
    expect(state.display!.summary.latestWeightKg, 60.0);
  });
}
