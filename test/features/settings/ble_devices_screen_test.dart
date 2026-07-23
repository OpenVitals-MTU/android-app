import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/core/registry/ble_device_repository_impl.dart';
import 'package:openvitals/devices/core/ble/ble_sensor_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/features/settings/application/ble_devices_view_model.dart';
import 'package:openvitals/features/settings/presentation/ble_devices_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Never touches flutter_blue_plus.
class _FakeCoordinator implements BleSensorRepository {
  @override
  Stream<List<BleDiscoveredDevice>> get discoveredDevicesStream =>
      const Stream.empty();

  @override
  Future<Set<BleSensorCapability>> discoverCapabilities(String address) async =>
      const {};

  @override
  Future<void> startScan({bool showAllDevices = false}) async {}

  @override
  Future<void> stopScan() async {}

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<ProviderContainer> _container() async {
  SharedPreferences.setMockInitialValues(const {});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(overrides: [
    sharedPreferencesProvider.overrideWithValue(prefs),
    bleDeviceRepositoryProvider.overrideWithValue(BleDeviceRepositoryImpl(prefs)),
    bleSensorRepositoryProvider.overrideWithValue(_FakeCoordinator()),
  ]);
  addTearDown(container.dispose);
  return container;
}

Widget _harness(ProviderContainer container, {required Widget home}) =>
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: home,
      ),
    );

void main() {
  testWidgets('leaving the screen with a flow open resets it without '
      'notifying the dying element', (tester) async {
    // Regression (c02ef672a): dispose() reset the add/edit flows INLINE, but
    // those mutate the very provider this widget watches — so Riverpod
    // notified an element that was already tearing down, and leaving the
    // Sensors screen crashed with `_lifecycleState != _ElementLifecycle.defunct`
    // on device. The reset is now deferred past the frame.
    final container = await _container();
    await tester.pumpWidget(_harness(container, home: const BleDevicesScreen()));
    await tester.pumpAndSettle();

    container.read(bleDevicesViewModelProvider.notifier).openAddFlow();
    await tester.pumpAndSettle();
    expect(container.read(bleDevicesViewModelProvider).showAddFlow, isTrue);

    // Navigate away: the screen disposes with the flow still open.
    await tester.pumpWidget(_harness(container, home: const SizedBox()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // The deferred reset still ran: re-entry starts clean.
    expect(container.read(bleDevicesViewModelProvider).showAddFlow, isFalse);
  });
}
