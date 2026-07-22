import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/features/settings/presentation/watch_device_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Seeds the REAL registry rather than stubbing the stream: the rename writes
/// through the repository, so a canned stream would show a device that the
/// write path then cannot find.
Future<ProviderContainer> _container() async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      bleDiscoveredDevicesProvider.overrideWith(
        (ref) => Stream.value(const <BleDiscoveredDevice>[]),
      ),
    ],
  );
  addTearDown(container.dispose);
  final paired = container.read(bleDeviceRepositoryProvider).addDevice(
        displayName: 'vívoactive 5',
        address: 'E0:48:24:D5:F7:10',
        bluetoothName: 'vívoactive 5',
        capabilities: const {},
        kind: BleDeviceKind.watch,
      );
  _deviceId = paired.id;
  return container;
}

late String _deviceId;

Widget _harness(ProviderContainer container) => UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: WatchDeviceScreen(deviceId: _deviceId),
      ),
    );

void main() {
  testWidgets('opening and dismissing the rename dialog does not throw',
      (tester) async {
    // Regression: the controller was disposed the moment showDialog returned,
    // while the route was still animating out and its TextField still depended
    // on it — which blew up as an `_dependents.isEmpty` assertion during the
    // dialog's own teardown, on a real device, every time.
    final container = await _container();
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.edit_outlined));
    await tester.pumpAndSettle();
    expect(find.byType(TextField), findsOneWidget);

    await tester.tap(find.text('Cancel'));
    // Pump PAST the exit animation: settling is where the crash surfaced.
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(TextField), findsNothing);
  });

  testWidgets('renaming applies the new name', (tester) async {
    final container = await _container();
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.edit_outlined));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '  Wrist watch  ');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // Trimmed, because a name padded with spaces is a typo, not a choice.
    expect(
      container
          .read(bleDeviceRepositoryProvider)
          .devices
          .firstWhere((d) => d.id == _deviceId)
          .displayName,
      'Wrist watch',
    );
  });

  testWidgets('the device view offers Data and Sync, and nothing measured',
      (tester) async {
    final container = await _container();
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(find.text('Data'), findsOneWidget);
    expect(find.text('Sync'), findsOneWidget);
    // The "Latest" band was removed: it duplicated what Data opens.
    expect(find.text('Latest'), findsNothing);
    expect(find.text('Remove watch'), findsOneWidget);
  });

  testWidgets('Alarms and Find are live; the settings tree is not yet',
      (tester) async {
    // Alarms opens a screen in the watch's own settings tree, so it is no
    // longer a drawing. Browsing the whole tree still is — that row stays
    // greyed, which says the watch CAN do it and the app cannot yet.
    final container = await _container();
    await tester.pumpWidget(_harness(container));
    await tester.pumpAndSettle();

    expect(find.text('Alarms'), findsOneWidget);
    expect(find.text('Find'), findsOneWidget);

    final alarms = tester.widget<IconButton>(
      find.descendant(
        of: find
            .ancestor(of: find.text('Alarms'), matching: find.byType(Column))
            .first,
        matching: find.byType(IconButton),
      ),
    );
    expect(alarms.onPressed, isNotNull, reason: 'Alarms must be tappable');

    final settingsRow = tester.widget<ListTile>(
      find.ancestor(
        of: find.text('Settings on the watch'),
        matching: find.byType(ListTile),
      ),
    );
    expect(settingsRow.enabled, isFalse);
  });
}
