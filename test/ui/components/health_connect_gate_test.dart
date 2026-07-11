import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

Future<Widget> _bootstrap({
  required HealthConnectAvailability availability,
  Set<String> granted = const <String>{},
  Set<String> required = const <String>{},
  Map<String, Object> prefs = const <String, Object>{},
}) async {
  SharedPreferences.setMockInitialValues(prefs);
  final resolved = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(resolved),
      healthConnectAvailabilityProvider.overrideWith((ref) async => availability),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: MaterialApp(
      home: Scaffold(
        body: HealthConnectGate(
          requiredPermissions: required,
          child: const Text('CHILD'),
        ),
      ),
    ),
  );
}

void main() {
  testWidgets('shows the access gate when Health Connect is unavailable',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.notSupported),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Health Connect unavailable'), findsOneWidget);
    expect(find.text('CHILD'), findsNothing);
  });

  testWidgets('shows the child when available and permitted', (tester) async {
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('CHILD'), findsOneWidget);
    expect(find.text('Health Connect unavailable'), findsNothing);
  });

  testWidgets('shows the permission gate when a required permission is missing',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        availability: HealthConnectAvailability.available,
        required: {'steps'},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.text('CHILD'), findsNothing);
  });

  testWidgets('shows the sync-paused gate when sync is disabled',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        availability: HealthConnectAvailability.available,
        prefs: {'health_connect_sync_enabled': false},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Sync paused'), findsOneWidget);
    expect(find.text('CHILD'), findsNothing);
  });
}
