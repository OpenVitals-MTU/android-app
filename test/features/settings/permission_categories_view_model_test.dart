import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/data/repository/contract/repository_exceptions.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/settings/application/permission_categories_view_model.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

/// A [HealthDataSource] whose permission request either records the set it was
/// asked for, or throws — the two branches the grant command has to sort out.
class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource({this.throwOnRequest});

  final Object? throwOnRequest;
  final List<Set<String>> requested = <Set<String>>[];
  bool openedSettings = false;

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

  @override
  Future<bool> openHealthConnectSettings() async {
    openedSettings = true;
    return true;
  }
}

/// Builds the container and settles the two async gates the notifier watches,
/// so `build` has seen a resolved availability and granted set.
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
  container.read(permissionCategoriesProvider);
  await container.read(healthConnectAvailabilityProvider.future);
  await container.read(grantedHealthPermissionsProvider.future);
  return container;
}

void main() {
  test('build exposes the category taxonomy once the gates resolve', () async {
    final container = await _container(_FakeHealthDataSource());

    final state = container.read(permissionCategoriesProvider);
    expect(state.availability, HealthConnectAvailability.available);
    expect(state.granted, isEmpty);
    expect(state.categories.map((c) => c.id), contains('activity_sleep'));
    expect(state.request, const CommandState<void>.idle());
  });

  test('a granted request succeeds and asks for exactly the missing set',
      () async {
    final dataSource = _FakeHealthDataSource();
    final container = await _container(dataSource);

    await container.read(permissionCategoriesProvider.notifier).requestCategory(
      requestable: {'android.permission.health.READ_STEPS'},
      manual: const <String>{},
    );

    expect(dataSource.requested, [
      {'android.permission.health.READ_STEPS'},
    ]);
    expect(
      container.read(permissionCategoriesProvider).request,
      const CommandState<void>.success(null),
    );
  });

  test('a manual-only category opens the Health Connect settings screen',
      () async {
    final dataSource = _FakeHealthDataSource();
    final container = await _container(dataSource);

    await container.read(permissionCategoriesProvider.notifier).requestCategory(
      requestable: const <String>{},
      manual: {'android.permission.health.READ_EXERCISE_ROUTES'},
    );

    expect(dataSource.openedSettings, isTrue);
    expect(dataSource.requested, isEmpty);
  });

  test('a refused request lands as CommandFailure carrying the ScreenError',
      () async {
    final dataSource = _FakeHealthDataSource(
      throwOnRequest: const MissingHealthPermissionException('nope'),
    );
    final container = await _container(dataSource);

    await container.read(permissionCategoriesProvider.notifier).requestCategory(
      requestable: {'android.permission.health.READ_STEPS'},
      manual: const <String>{},
    );

    // A PermissionFailure maps to ScreenErrorPermissionDenied, not to a message.
    expect(
      container.read(permissionCategoriesProvider).request,
      const CommandState<void>.failure(ScreenErrorPermissionDenied()),
    );
  });
}
