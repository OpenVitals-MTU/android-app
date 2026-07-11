import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/dashboard/dashboard_notifier.dart';
import 'package:openvitals/health/health_data_source.dart';
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

/// Records every day the notifier asks for, so a reload is observable.
class _RecordingUseCase extends LoadDashboardDayUseCase {
  _RecordingUseCase() : super(DashboardDataLoader(HealthDataSource()));

  final List<LocalDate> loadedDates = <LocalDate>[];

  @override
  Future<DashboardData> call(DashboardQuery query) async {
    loadedDates.add(query.date);
    return DashboardData(
      date: query.date,
      loadedMetrics: query.visibleMetrics,
      supportedMetrics: DashboardMetric.values.toSet(),
    );
  }
}

Future<(ProviderContainer, _RecordingUseCase)> _boot() async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final useCase = _RecordingUseCase();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(_FakeHealthDataSource()),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider
          .overrideWith((ref) async => const <String>{}),
      loadDashboardDayUseCaseProvider.overrideWithValue(useCase),
    ],
  );
  addTearDown(container.dispose);
  // Keep the notifier alive and let its initial load settle.
  container.listen(dashboardNotifierProvider, (_, _) {});
  await pumpEventQueue();
  useCase.loadedDates.clear();
  return (container, useCase);
}

void main() {
  final today = LocalDate.now();
  final yesterday = today.minusDays(1);

  test('resumeCurrentDay reloads today by default', () async {
    final (container, useCase) = await _boot();
    final notifier = container.read(dashboardNotifierProvider.notifier);

    notifier.resumeCurrentDay();
    await pumpEventQueue();

    expect(useCase.loadedDates, isNotEmpty);
    expect(useCase.loadedDates.every((d) => d == today), isTrue);
    expect(container.read(dashboardNotifierProvider).selectedDate, today);
  });

  test('resumeCurrentDay honours a day the user pinned in the past', () async {
    final (container, useCase) = await _boot();
    final notifier = container.read(dashboardNotifierProvider.notifier);

    notifier.previousDay();
    await pumpEventQueue();
    useCase.loadedDates.clear();

    notifier.resumeCurrentDay();
    await pumpEventQueue();

    // Refreshed in place — never yanked forward to today.
    expect(useCase.loadedDates, isNotEmpty);
    expect(useCase.loadedDates.every((d) => d == yesterday), isTrue);
    expect(container.read(dashboardNotifierProvider).selectedDate, yesterday);
  });

  test('selectDate on a past day pins it; selecting today clears the pin',
      () async {
    final (container, useCase) = await _boot();
    final notifier = container.read(dashboardNotifierProvider.notifier);

    notifier.selectDate(today.minusDays(3));
    await pumpEventQueue();
    useCase.loadedDates.clear();

    notifier.resumeCurrentDay();
    await pumpEventQueue();
    expect(useCase.loadedDates.every((d) => d == today.minusDays(3)), isTrue);

    notifier.selectDate(today);
    await pumpEventQueue();
    useCase.loadedDates.clear();

    notifier.resumeCurrentDay();
    await pumpEventQueue();
    expect(useCase.loadedDates, isNotEmpty);
    expect(useCase.loadedDates.every((d) => d == today), isTrue);
  });

  test('nextDay onto a still-past day keeps the pin', () async {
    final (container, useCase) = await _boot();
    final notifier = container.read(dashboardNotifierProvider.notifier);

    notifier.previousDay();
    await pumpEventQueue();
    notifier.previousDay();
    await pumpEventQueue();
    // Two days back, then forward one: still in the past, so still pinned.
    notifier.nextDay();
    await pumpEventQueue();
    useCase.loadedDates.clear();

    notifier.resumeCurrentDay();
    await pumpEventQueue();

    expect(useCase.loadedDates, isNotEmpty);
    expect(useCase.loadedDates.every((d) => d == yesterday), isTrue);
  });

  test('nextDay back onto today clears the pin', () async {
    final (container, useCase) = await _boot();
    final notifier = container.read(dashboardNotifierProvider.notifier);

    notifier.previousDay();
    await pumpEventQueue();
    notifier.nextDay();
    await pumpEventQueue();
    expect(container.read(dashboardNotifierProvider).selectedDate, today);
    useCase.loadedDates.clear();

    notifier.resumeCurrentDay();
    await pumpEventQueue();

    expect(useCase.loadedDates, isNotEmpty);
    expect(useCase.loadedDates.every((d) => d == today), isTrue);
  });
}
