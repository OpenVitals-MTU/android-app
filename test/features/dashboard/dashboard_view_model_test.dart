// The dashboard notifier owns the derivation now: it loads in two passes (a fast
// quick-metric pass, then a background pass merged into it) and publishes a
// precomputed DashboardDisplay from each one. These pin that contract — the
// merge, the display, the staleness guard, and the failure mapping — without a
// widget in sight.

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/data/repository/impl/health_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/usecase/check_minimum_health_permissions_use_case.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/dashboard/application/dashboard_view_model.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource() {
    cachedAvailability = HealthConnectAvailability.available;
  }

  @override
  Future<HealthConnectAvailability> availability() async => cachedAvailability;

  @override
  Future<Set<String>> grantedPermissions() async => const <String>{};
}

/// A use case whose answer depends on the pass: the quick pass carries the steps
/// (and no HRV), the background pass carries the HRV (and no steps). Merging them
/// wrongly is therefore visible in the data itself.
///
/// Any date with a [gate] blocks until the test releases it, which is how a stale
/// pass is made to land after a newer one.
class _PassUseCase extends LoadDashboardDayUseCase {
  _PassUseCase() : super(DashboardDataLoader(HealthDataSource()));

  final List<DashboardQuery> queries = <DashboardQuery>[];
  final Map<LocalDate, Completer<void>> gates = <LocalDate, Completer<void>>{};
  Completer<void>? backgroundGate;

  Completer<void> gate(LocalDate date) =>
      gates.putIfAbsent(date, Completer<void>.new);

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async {
    queries.add(query);
    final quick = query.visibleMetrics.contains(DashboardMetric.steps);
    final dateGate = gates[query.date];
    if (dateGate != null) await dateGate.future;
    if (!quick && backgroundGate != null) await backgroundGate!.future;
    return Ok(DashboardData(
      date: query.date,
      steps: quick ? 8000 : 0,
      hrvRmssdMs: quick ? null : 42,
      hrvSampleCount: quick ? 0 : 30,
      loadedMetrics: query.visibleMetrics,
      supportedMetrics: DashboardMetric.values.toSet(),
    ));
  }
}

/// A minimum-permission check that fails. The dashboard used to `.orThrow()` this
/// one — out of an unawaited load, with nothing to catch it.
class _FailingPermissionCheck extends CheckMinimumHealthPermissionsUseCase {
  _FailingPermissionCheck() : super(HealthRepositoryImpl(HealthDataSource()));

  @override
  Future<Result<bool>> call(HealthConnectAvailability availability) async =>
      const Err(PermissionFailure('denied'));
}

/// A use case that throws — [LoadDashboardDayUseCase] does not return a Result,
/// so this is the failure path the dashboard still bridges with a try/catch.
class _ThrowingUseCase extends LoadDashboardDayUseCase {
  _ThrowingUseCase() : super(DashboardDataLoader(HealthDataSource()));

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async =>
      const Err(UnexpectedFailure('boom'));
}

Future<ProviderContainer> _boot({
  required LoadDashboardDayUseCase useCase,
  CheckMinimumHealthPermissionsUseCase? permissionCheck,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(_FakeHealthDataSource()),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider
          .overrideWith((ref) async => const <String>{}),
      loadDashboardDayUseCaseProvider.overrideWithValue(useCase),
      if (permissionCheck != null)
        checkMinimumHealthPermissionsUseCaseProvider
            .overrideWithValue(permissionCheck),
    ],
  );
  addTearDown(container.dispose);
  // Keep the notifier alive across the load.
  container.listen(dashboardProvider, (_, _) {});
  return container;
}

StatTileData _tile(DashboardDisplay display, String title) =>
    display.orderedTiles.firstWhere((t) => t.title == title);

void main() {
  final today = LocalDate.now();

  test('the load publishes a precomputed display, not just the data', () async {
    final container = await _boot(useCase: _PassUseCase());
    await pumpEventQueue();

    final state = container.read(dashboardProvider);
    expect(state.isLoading, isFalse);
    expect(state.data, isNotNull);
    // The screen renders this and derives nothing.
    expect(state.display, isNotNull);
    expect(state.display!.visibleRings, hasLength(2));
    expect(state.display!.visibleTiles, isNotEmpty);
    // The goals moved onto the state — the widget no longer reads preferences.
    expect(state.goals.steps, kDefaultDashboardGoals.steps);
  });

  test('the two passes merge, and the display is rebuilt from each', () async {
    final useCase = _PassUseCase();
    final background = Completer<void>();
    useCase.backgroundGate = background;
    final container = await _boot(useCase: useCase);
    await pumpEventQueue();

    // Pass 1 (quick): the steps are in, the HRV is not — and the tile for it is
    // empty, because the background pass has not landed.
    final quick = container.read(dashboardProvider);
    expect(quick.data!.steps, 8000);
    expect(quick.data!.hrvRmssdMs, isNull);
    expect(quick.loadingMetrics, isNotEmpty);
    expect(quick.isLoading, isFalse);
    expect(_tile(quick.display!, 'HRV').value, isEmpty);
    expect(quick.display!.orderedRings.first.value, '8,000');

    background.complete();
    await pumpEventQueue();

    // Pass 2 (background): merged into the quick pass, not over it. The steps
    // survive, the HRV arrives, and the display was rebuilt for it.
    final merged = container.read(dashboardProvider);
    expect(merged.data!.steps, 8000, reason: 'the quick pass was clobbered');
    expect(merged.data!.hrvRmssdMs, 42);
    expect(merged.loadingMetrics, isEmpty);
    expect(_tile(merged.display!, 'HRV').value, '42.0');
    expect(merged.display!.orderedRings.first.value, '8,000');
  });

  test('a stale pass cannot overwrite the day that overtook it', () async {
    final useCase = _PassUseCase();
    final container = await _boot(useCase: useCase);
    await pumpEventQueue();

    final stale = today.minusDays(3);
    final gate = useCase.gate(stale);

    container.read(dashboardProvider.notifier).selectDate(stale);
    await pumpEventQueue();
    // The stale day is still hanging on its gate.
    expect(container.read(dashboardProvider).data!.date, today);

    // The user moves back to today before it lands.
    container.read(dashboardProvider.notifier).selectDate(today);
    await pumpEventQueue();
    expect(container.read(dashboardProvider).selectedDate, today);

    // The overtaken load finally answers — and is dropped.
    gate.complete();
    await pumpEventQueue();

    final state = container.read(dashboardProvider);
    expect(state.selectedDate, today);
    expect(state.data!.date, today);
    expect(state.display!.orderedRings.first.value, '8,000');
  });

  test('a failed permission check becomes a ScreenError, not a lost load',
      () async {
    final container = await _boot(
      useCase: _PassUseCase(),
      permissionCheck: _FailingPermissionCheck(),
    );
    await pumpEventQueue();

    final state = container.read(dashboardProvider);
    expect(state.error, const ScreenErrorPermissionDenied());
    expect(state.isLoading, isFalse);
    expect(state.isRefreshing, isFalse);
    // Nothing was published, so the screen shows the error rather than an
    // eternal loader.
    expect(state.display, isNull);
  });

  test('a failed day load becomes a ScreenError', () async {
    final container = await _boot(useCase: _ThrowingUseCase());
    await pumpEventQueue();

    final state = container.read(dashboardProvider);
    expect(state.error, isA<ScreenErrorMessage>());
    expect(state.isLoading, isFalse);
    expect(state.data, isNull);
  });

  test('toggling edit mode rebuilds the display without reloading', () async {
    final useCase = _PassUseCase();
    final container = await _boot(useCase: useCase);
    await pumpEventQueue();
    final loads = useCase.queries.length;

    container.read(dashboardProvider.notifier).toggleEditing();

    final state = container.read(dashboardProvider);
    expect(state.editing, isTrue);
    // Synchronously rebuilt — no frame, no load, in between.
    expect(useCase.queries, hasLength(loads));
    expect(state.display, isNotNull);
    expect(state.display!.orderedTiles, isNotEmpty);
  });

  test('hiding a tile drops it from the display and offers it in the tray',
      () async {
    final container = await _boot(useCase: _PassUseCase());
    await pumpEventQueue();
    final notifier = container.read(dashboardProvider.notifier);

    // Hiding is by stable id, not by the title on the card.
    notifier.setTileHidden(DashboardMetric.distance.name, true);

    var display = container.read(dashboardProvider).display!;
    expect([for (final t in display.visibleTiles) t.title],
        isNot(contains('Distance')));
    expect([for (final e in display.trayEntries) e.title], contains('Distance'));

    notifier.addWidget(DashboardMetric.distance.name);

    display = container.read(dashboardProvider).display!;
    expect([for (final t in display.visibleTiles) t.title],
        contains('Distance'));
    expect([for (final e in display.trayEntries) e.title],
        isNot(contains('Distance')));
  });
}
