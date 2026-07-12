import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/settings/application/route_import_view_model.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

/// A [HealthDataSource] that grants nothing, and either records the request or
/// blows up on it.
class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource({this.throwOnRequest});

  final Object? throwOnRequest;
  final List<Set<String>> requested = <Set<String>>[];

  @override
  Future<HealthConnectAvailability> availability() async =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async => const <String>{};

  @override
  Future<bool> requestPermissions(Set<String> permissions) async {
    if (throwOnRequest != null) throw throwOnRequest!;
    requested.add(permissions);
    return true;
  }
}

Future<ProviderContainer> _container(_FakeHealthDataSource dataSource) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(dataSource),
    ],
  );
  addTearDown(container.dispose);
  container.read(routeImportCardProvider);
  await container.read(healthConnectAvailabilityProvider.future);
  await container.read(grantedHealthPermissionsProvider.future);
  return container;
}

void main() {
  test('build derives the permission counts from the activity repository',
      () async {
    final container = await _container(_FakeHealthDataSource());

    final state = container.read(routeImportCardProvider);
    expect(state.importPermissions, isNotEmpty);
    expect(state.grantedCount, 0);
    expect(state.missingPermissions, state.importPermissions);
    expect(state.healthConnectAvailable, isTrue);
    expect(state.isGranting, isFalse);
  });

  test('grantPermissions requests exactly the missing set', () async {
    final dataSource = _FakeHealthDataSource();
    final container = await _container(dataSource);
    final missing = container.read(routeImportCardProvider).missingPermissions;

    await container.read(routeImportCardProvider.notifier).grantPermissions();

    expect(dataSource.requested, [missing]);
    expect(
      container.read(routeImportCardProvider).grant,
      const CommandState<void>.success(null),
    );
  });

  test('a failed grant lands as CommandFailure with the error message',
      () async {
    final container = await _container(
      _FakeHealthDataSource(throwOnRequest: StateError('no launcher')),
    );

    await container.read(routeImportCardProvider.notifier).grantPermissions();

    expect(
      container.read(routeImportCardProvider).grant,
      const CommandState<void>.failure(
        ScreenErrorMessage('Bad state: no launcher'),
      ),
    );
  });
}
